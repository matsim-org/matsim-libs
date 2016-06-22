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

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.charts.XYLineChart;

import com.google.inject.Inject;

import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.data.LinkInfo;
import playground.ikaddoura.decongestion.handler.DelayAnalysis;
import playground.ikaddoura.decongestion.handler.IntervalBasedTolling;
import playground.ikaddoura.decongestion.handler.PersonVehicleTracker;


/**
 * Interval-based decongestion pricing approach:
 * 
 * (1) Identify congested links and time intervals and set an initial toll for these links and time intervals.
 * (2) Let the demand adjust for x iterations.
 * (3) Increase the toll for congested links and time intervals. Reduce the toll for congested links and time intervals.
 * (4) GOTO (2)
 * 
 * All relevant parameters are specified in {@link DecongestionInfo}.
 * 
 * Ideas to try out:
 * - Only increase the toll if the average delay has increased.
 * - Disable innovative strategies for the final x iterations.
 * 
 * @author ikaddoura
 *
 */

public class DecongestionControlerListener implements StartupListener, AfterMobsimListener, IterationEndsListener {
		
	private static final Logger log = Logger.getLogger(DecongestionControlerListener.class);

	private final SortedMap<Integer, Double> iteration2totalDelay = new TreeMap<>();
	private final SortedMap<Integer, Double> iteration2totalTollPayments = new TreeMap<>();
	private final SortedMap<Integer, Double> iteration2totalTravelTime = new TreeMap<>();
	private final SortedMap<Integer, Double> iteration2userBenefits = new TreeMap<>();
	
	private final DecongestionInfo congestionInfo;
	private final DecongestionTollComputation tollComputation;
	
	private IntervalBasedTolling intervalBasedTolling;
	private DelayAnalysis delayComputation;

	@Inject
	public DecongestionControlerListener(Scenario scenario){
		this.congestionInfo = new DecongestionInfo(scenario);
		this.tollComputation = new DecongestionTollComputation(congestionInfo);
	}
	
	@Inject
	public DecongestionControlerListener(DecongestionInfo congestionInfo){
		this.congestionInfo = congestionInfo;
		this.tollComputation = new DecongestionTollComputation(congestionInfo);
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		event.getServices().getEvents().addHandler(new PersonVehicleTracker(congestionInfo));
				
		this.intervalBasedTolling = new IntervalBasedTolling(congestionInfo, event.getServices().getEvents());
		event.getServices().getEvents().addHandler(this.intervalBasedTolling);
		
		this.delayComputation = new DelayAnalysis(this.congestionInfo.getScenario());
		event.getServices().getEvents().addHandler(delayComputation);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		if (event.getIteration() % this.congestionInfo.getWRITE_OUTPUT_ITERATION() == 0. || event.getIteration() % this.congestionInfo.getUPDATE_PRICE_INTERVAL() == 0.) {
			computeDelays(event);
		}
		
		if (event.getIteration() == this.congestionInfo.getScenario().getConfig().controler().getFirstIteration()) {
			// skip first iteration
		
		} else if (event.getIteration() % this.congestionInfo.getUPDATE_PRICE_INTERVAL() == 0.) {			
			
			int totalNumberOfIterations = this.congestionInfo.getScenario().getConfig().controler().getLastIteration() - this.congestionInfo.getScenario().getConfig().controler().getFirstIteration();
			if (event.getIteration() < this.congestionInfo.getFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT() * totalNumberOfIterations) {
				
				log.info("+++ Iteration " + event.getIteration() + ". Update tolls per link and time bin.");
				tollComputation.updateTolls();
			}
		}
		
		if (event.getIteration() % this.congestionInfo.getWRITE_OUTPUT_ITERATION() == 0.) {
			CongestionInfoWriter.writeCongestionInfoTimeInterval(congestionInfo, this.congestionInfo.getScenario().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/");
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
				double avgDelay = travelTime.getLinkTravelTime(link, endTime, null, null) - freespeedTravelTime;
				time2avgDelay.put(timeBinCounter, avgDelay);				
				timeBinCounter++;
			}
			
			if (this.congestionInfo.getlinkInfos().containsKey(link.getId())) {
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
				
		// Store and write out some aggregated numbers for analysis purposes.
		
		this.iteration2totalDelay.put(event.getIteration(), this.delayComputation.getTotalDelay());
		this.iteration2totalTollPayments.put(event.getIteration(), this.intervalBasedTolling.getTotalTollPayments());
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
				this.congestionInfo.getScenario().getConfig().controler().getOutputDirectory()
				);
		
		XYLineChart chart1 = new XYLineChart("Total travel time and total delay", "Iteration", "Hours");
		double[] iterations1 = new double[event.getIteration() + 1];
		double[] values1a = new double[event.getIteration() + 1];
		double[] values1b = new double[event.getIteration() + 1];
		for (int i = 0; i <= event.getIteration(); i++) {
			iterations1[i] = i;
			values1a[i] = this.iteration2totalDelay.get(i) / 3600.;
			values1b[i] = this.iteration2totalTravelTime.get(i) / 3600.;
		}
		chart1.addSeries("Total delay", iterations1, values1a);
		chart1.addSeries("Total travel time", iterations1, values1b);
		chart1.saveAsPng(this.congestionInfo.getScenario().getConfig().controler().getOutputDirectory() + "travelTime_delay.png", 800, 600);
		
		XYLineChart chart2 = new XYLineChart("System welfare, user benefits and toll revenues", "Iteration", "Monetary units");
		double[] iterations2 = new double[event.getIteration() + 1];
		double[] values2a = new double[event.getIteration() + 1];
		double[] values2b = new double[event.getIteration() + 1];
		double[] values2c = new double[event.getIteration() + 1];

		for (int i = 0; i <= event.getIteration(); i++) {
			iterations2[i] = i;
			values2a[i] = this.iteration2userBenefits.get(i) + this.iteration2totalTollPayments.get(i);
			values2b[i] = this.iteration2userBenefits.get(i);
			values2c[i] = this.iteration2totalTollPayments.get(i);
		}
		chart2.addSeries("System welfare", iterations2, values2a);
		chart2.addSeries("User benefits", iterations2, values2b);
		chart2.addSeries("Toll revenues", iterations2, values2c);
		chart2.saveAsPng(this.congestionInfo.getScenario().getConfig().controler().getOutputDirectory() + "systemWelfare_userBenefits_tollRevenues.png", 800, 600);
		
	}

}
