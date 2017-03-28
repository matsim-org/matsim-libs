/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */

package playground.ikaddoura.decongestion;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.charts.XYLineChart;

import com.google.inject.Inject;

import playground.ikaddoura.analysis.pngSequence2Video.MATSimVideoUtils;
import playground.ikaddoura.decongestion.data.CongestionInfoWriter;
import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.data.LinkInfo;
import playground.ikaddoura.decongestion.handler.DelayAnalysis;
import playground.ikaddoura.decongestion.handler.IntervalBasedTolling;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollSetting;


/**
 * Interval-based decongestion pricing approach:
 * 
 * (1) Identify congested links and time intervals and set an initial toll for these links and time intervals.
 * (2) Let the demand adjust for x iterations.
 * (3) Adjust the tolls (different implementations of {@link DecongestionTollSetting})
 * (4) GOTO (2)
 * 
 * All relevant parameters are specified in {@link DecongestionInfo}.
 * 
 * 
 * @author ikaddoura
 *
 */

public class DecongestionControlerListener implements StartupListener, AfterMobsimListener, IterationStartsListener, IterationEndsListener, ShutdownListener {
		
	private static final Logger log = Logger.getLogger(DecongestionControlerListener.class);

	private final SortedMap<Integer, Double> iteration2totalDelay = new TreeMap<>();
	private final SortedMap<Integer, Double> iteration2totalTollPayments = new TreeMap<>();
	private final SortedMap<Integer, Double> iteration2totalTravelTime = new TreeMap<>();
	private final SortedMap<Integer, Double> iteration2userBenefits = new TreeMap<>();
	
	@Inject
	private DecongestionInfo congestionInfo;
	
	@Inject(optional=true)
	private DecongestionTollSetting tollComputation;
	
	@Inject(optional=true)
	private IntervalBasedTolling intervalBasedTolling;
	
	@Inject(optional=true)
	private DelayAnalysis delayComputation;
	
	private int nextDisableInnovativeStrategiesIteration = -1;
	private int nextEnableInnovativeStrategiesIteration = -1;
	
	private String outputDirectory;
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		log.info("decongestion settings: " + congestionInfo.getDecongestionConfigGroup().toString());
		
		this.outputDirectory = this.congestionInfo.getScenario().getConfig().controler().getOutputDirectory();
		if (!outputDirectory.endsWith("/")) {
			log.info("Adjusting output directory.");
			outputDirectory = outputDirectory + "/";
		}		
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		if (event.getIteration() % this.congestionInfo.getDecongestionConfigGroup().getWRITE_OUTPUT_ITERATION() == 0. || event.getIteration() % this.congestionInfo.getDecongestionConfigGroup().getUPDATE_PRICE_INTERVAL() == 0.) {
			computeDelays(event);
		}
		
		if (event.getIteration() == this.congestionInfo.getScenario().getConfig().controler().getFirstIteration()) {
			// skip first iteration
		
		} else if (event.getIteration() % this.congestionInfo.getDecongestionConfigGroup().getUPDATE_PRICE_INTERVAL() == 0.) {			
			
			int totalNumberOfIterations = this.congestionInfo.getScenario().getConfig().controler().getLastIteration() - this.congestionInfo.getScenario().getConfig().controler().getFirstIteration();
			int iterationCounter = event.getIteration() - this.congestionInfo.getScenario().getConfig().controler().getFirstIteration();
			
			if (iterationCounter < this.congestionInfo.getDecongestionConfigGroup().getFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT() * totalNumberOfIterations
					&& iterationCounter > this.congestionInfo.getDecongestionConfigGroup().getFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT() * totalNumberOfIterations) {
				
				if (tollComputation != null) {
					log.info("+++ Iteration " + event.getIteration() + ". Update tolls per link and time bin.");
					tollComputation.updateTolls();
				}
			}
		}
		
		if (event.getIteration() % this.congestionInfo.getDecongestionConfigGroup().getWRITE_OUTPUT_ITERATION() == 0.) {
			CongestionInfoWriter.writeDelays(congestionInfo, event.getIteration(), this.outputDirectory + "ITERS/it." + event.getIteration() + "/");
			CongestionInfoWriter.writeTolls(congestionInfo, event.getIteration(), this.outputDirectory + "ITERS/it." + event.getIteration() + "/");
		}
	}

	private void computeDelays(AfterMobsimEvent event) {
				
		TravelTime travelTime = event.getServices().getLinkTravelTimes();
		int timeBinSize = this.congestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize();
		
		for (Link link : this.congestionInfo.getScenario().getNetwork().getLinks().values()) {
			
			Map<Integer, Double> time2avgDelay = new HashMap<>();
			double freespeedTravelTime = link.getLength() / link.getFreespeed();
			
			int timeBinCounter = 0;
			for (int endTime = timeBinSize ; endTime <= this.congestionInfo.getScenario().getConfig().travelTimeCalculator().getMaxTime(); endTime = endTime + timeBinSize ) {
				double avgDelay = travelTime.getLinkTravelTime(link, (endTime - timeBinSize/2.), null, null) - freespeedTravelTime;
				time2avgDelay.put(timeBinCounter, avgDelay);				
				timeBinCounter++;
			}
			
			if (this.congestionInfo.getlinkInfos().get(link.getId()) != null) {
				this.congestionInfo.getlinkInfos().get(link.getId()).setTime2avgDelay(time2avgDelay);
			} else {
				LinkInfo linkInfo = new LinkInfo(link.getId());
				linkInfo.setTime2avgDelay(time2avgDelay);
				this.congestionInfo.getlinkInfos().put(link.getId(), linkInfo);
			}
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
				
		if (this.delayComputation != null) {
			// Store and write out some aggregated numbers for analysis purposes.
			
			this.iteration2totalDelay.put(event.getIteration(), this.delayComputation.getTotalDelay());
			double totalPayments = 0.;
			if (this.intervalBasedTolling != null) totalPayments = this.intervalBasedTolling.getTotalTollPayments();
			this.iteration2totalTollPayments.put(event.getIteration(), totalPayments);
			this.iteration2totalTravelTime.put(event.getIteration(), this.delayComputation.getTotalTravelTime());
			
			double monetizedUserBenefits = 0.;
			for (Person person : this.congestionInfo.getScenario().getPopulation().getPersons().values()) {
				monetizedUserBenefits = monetizedUserBenefits + person.getSelectedPlan().getScore() / this.congestionInfo.getScenario().getConfig().planCalcScore().getMarginalUtilityOfMoney();
			}
			this.iteration2userBenefits.put(event.getIteration(), monetizedUserBenefits);
			
			CongestionInfoWriter.writeIterationStats(
					this.iteration2totalDelay,
					this.iteration2totalTollPayments,
					this.iteration2totalTravelTime,
					this.iteration2userBenefits,
					outputDirectory
					);
			
			XYLineChart chart1 = new XYLineChart("Total travel time and total delay", "Iteration", "Hours");
			double[] iterations1 = new double[event.getIteration() + 1];
			double[] values1a = new double[event.getIteration() + 1];
			double[] values1b = new double[event.getIteration() + 1];
			for (int i = this.congestionInfo.getScenario().getConfig().controler().getFirstIteration(); i <= event.getIteration(); i++) {
				iterations1[i] = i;
				values1a[i] = this.iteration2totalDelay.get(i) / 3600.;
				values1b[i] = this.iteration2totalTravelTime.get(i) / 3600.;
			}
			chart1.addSeries("Total delay", iterations1, values1a);
			chart1.addSeries("Total travel time", iterations1, values1b);
			chart1.saveAsPng(outputDirectory + "travelTime_delay.png", 800, 600);
			
			XYLineChart chart2 = new XYLineChart("System welfare, user benefits and toll revenues", "Iteration", "Monetary units");
			double[] iterations2 = new double[event.getIteration() + 1];
			double[] values2a = new double[event.getIteration() + 1];
			double[] values2b = new double[event.getIteration() + 1];
			double[] values2c = new double[event.getIteration() + 1];

			for (int i = this.congestionInfo.getScenario().getConfig().controler().getFirstIteration(); i <= event.getIteration(); i++) {
				iterations2[i] = i;
				values2a[i] = this.iteration2userBenefits.get(i) + this.iteration2totalTollPayments.get(i);
				values2b[i] = this.iteration2userBenefits.get(i);
				values2c[i] = this.iteration2totalTollPayments.get(i);
			}
			chart2.addSeries("System welfare", iterations2, values2a);
			chart2.addSeries("User benefits", iterations2, values2b);
			chart2.addSeries("Toll revenues", iterations2, values2c);
			chart2.saveAsPng(outputDirectory + "systemWelfare_userBenefits_tollRevenues.png", 800, 600);
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
			
		if (congestionInfo.getDecongestionConfigGroup().getUPDATE_PRICE_INTERVAL() > 1) {
			if (event.getIteration() == this.congestionInfo.getScenario().getConfig().controler().getFirstIteration()) {
				
				this.nextDisableInnovativeStrategiesIteration = (int) (congestionInfo.getScenario().getConfig().strategy().getFractionOfIterationsToDisableInnovation() * congestionInfo.getDecongestionConfigGroup().getUPDATE_PRICE_INTERVAL());
				log.info("next disable innovative strategies iteration: " + this.nextDisableInnovativeStrategiesIteration);

				if (this.nextDisableInnovativeStrategiesIteration != 0) {
					this.nextEnableInnovativeStrategiesIteration = (int) (congestionInfo.getDecongestionConfigGroup().getUPDATE_PRICE_INTERVAL() + 1);
				}
				log.info("next enable innovative strategies iteration: " + this.nextEnableInnovativeStrategiesIteration);

			} else {
				
				if (event.getIteration() == this.nextDisableInnovativeStrategiesIteration) {
					// set weight to zero
					log.warn("Strategy weight adjustment (set to zero) in iteration " + event.getIteration());
									
					for (GenericPlanStrategy<Plan, Person> strategy : event.getServices().getStrategyManager().getStrategies(null)) {
						
						String strategyName = strategy.toString();
						if (isInnovativeStrategy(strategyName)) {
							log.info("Setting weight for " + strategyName + " to zero.");
							event.getServices().getStrategyManager().changeWeightOfStrategy(strategy, null, 0.0);
						}
					}
					
					this.nextDisableInnovativeStrategiesIteration += congestionInfo.getDecongestionConfigGroup().getUPDATE_PRICE_INTERVAL();
					log.info("next disable innovative strategies iteration: " + this.nextDisableInnovativeStrategiesIteration);
					
				} else if (event.getIteration() == this.nextEnableInnovativeStrategiesIteration) {
					// set weight back to original value

					if (event.getIteration() >= congestionInfo.getScenario().getConfig().strategy().getFractionOfIterationsToDisableInnovation() * (congestionInfo.getScenario().getConfig().controler().getLastIteration() - congestionInfo.getScenario().getConfig().controler().getFirstIteration())) {
						
						log.info("Strategies are switched off by global settings. Do not set back the strategy parameters to original values...");
					
					} else {
						
						log.info("Strategy weight adjustment (set back to original value) in iteration " + event.getIteration());
						
						for (GenericPlanStrategy<Plan, Person> strategy : event.getServices().getStrategyManager().getStrategies(null)) {
							
							String strategyName = strategy.toString();
							if (isInnovativeStrategy(strategyName)) {
								
								double originalValue = -1.0;
								for (StrategySettings setting : event.getServices().getConfig().strategy().getStrategySettings()) {
									log.info("setting: " + setting.getStrategyName());
									log.info("strategyName: " + strategyName);

									if (strategyName.contains(setting.getStrategyName())) {
										originalValue = setting.getWeight();
									}
								}		
								
								if (originalValue == -1.0) {
									throw new RuntimeException("Aborting...");
								}
								
								log.warn("Setting weight for " + strategyName + " back to original value: " + originalValue);
								event.getServices().getStrategyManager().changeWeightOfStrategy(strategy, null, originalValue);
							}
						}			
						this.nextEnableInnovativeStrategiesIteration += congestionInfo.getDecongestionConfigGroup().getUPDATE_PRICE_INTERVAL();
					}
				}
			}	
		}
	}

	private boolean isInnovativeStrategy(String strategyName) {
		log.info("Strategy name: " + strategyName);
		boolean innovative = false ;
		for ( DefaultStrategy strategy : DefaultPlanStrategiesModule.DefaultStrategy.values() ) {
			log.info("default strategy: " +  strategy.toString());
			if ( strategyName.contains(strategy.toString()) ) {
				innovative = true ;
				break ;
			}
		}
		log.info("Innovative: " + innovative);
		
		return innovative;
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		
		if (this.congestionInfo.getDecongestionConfigGroup().isRUN_FINAL_ANALYSIS()) {
			log.info("Simulation is shut down. Running final analysis...");
			
			try {
				MATSimVideoUtils.createLegHistogramVideo(this.outputDirectory);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				if (this.congestionInfo.getDecongestionConfigGroup().isWRITE_LINK_INFO_CHARTS()) MATSimVideoUtils.createVideo(this.outputDirectory, this.congestionInfo.getDecongestionConfigGroup().getWRITE_OUTPUT_ITERATION(), "delays_perLinkAndTimeBin");
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (this.congestionInfo.getDecongestionConfigGroup().isWRITE_LINK_INFO_CHARTS()) MATSimVideoUtils.createVideo(this.outputDirectory, this.congestionInfo.getDecongestionConfigGroup().getWRITE_OUTPUT_ITERATION(), "toll_perLinkAndTimeBin");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
