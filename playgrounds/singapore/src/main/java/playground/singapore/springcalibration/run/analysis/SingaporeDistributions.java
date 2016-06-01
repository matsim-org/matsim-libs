/* *********************************************************************** *
 * project: org.matsim.*
 * DistanceDistribution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.singapore.springcalibration.run.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.rank.Percentile;
import org.apache.log4j.Logger;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.singapore.springcalibration.run.SingaporeControlerListener;


/**
 * @author anhorni
 */
public class SingaporeDistributions implements IterationEndsListener {

	private final static Logger log = Logger.getLogger(SingaporeDistributions.class);
	
	private final Population population;
	private final MainModeIdentifier mainModeIdentifier;
	private final StageActivityTypes stageActivityTypes;
	private final List<DistributionClass> classes;
	private DecimalFormat df = new DecimalFormat("0.00");
	private DecimalFormat dfpercent = new DecimalFormat("0.0");
	private String measure = "";
	private Counter counter = new Counter();	
	private DistributionClass transit_walk_class = new DistributionClass();
	private ModesHistoryPlotter modesHistoryPlotter = new ModesHistoryPlotter();
	
	public SingaporeDistributions(final Population population,
			final MainModeIdentifier mainModeIdentifier, final StageActivityTypes stageActivityTypes, String measure) {
		this.population = population;
		this.mainModeIdentifier = mainModeIdentifier;
		this.stageActivityTypes = stageActivityTypes;
		this.measure = measure;
		this.classes = new ArrayList<DistributionClass>();
	}
	
	private void analyzePlans(Collection<Plan> plans) {
		log.info("Analyzing " + measure + " distribution of " + plans.size() + " plans.");
		for (Plan plan : plans) {
			if (measure.equals("distance")) analyzePlanDistances(plan);
			else analyzePlanTimes(plan);
		}
	}
	
	private String mapActivity(String origAct) {
		String mappedAct = origAct;
		
		// We need a mapping here
		// HITS plans: "home", "work", "leisure", "pudo", "personal", "primaryschool", "secondaryschool", "tertiaryschool", "foreignschool"
		// Sim  plans: home, w_*_*, leisure, pudo, personal, primaryschool, secondaryschool, tertiaryschool, foreign_*,						shop, biz, pt interaction
		
		if (origAct.startsWith("w")) mappedAct = "work";		
		if (origAct.startsWith("foreign")) mappedAct = "foreignschool";
		return mappedAct;		
	}
	
	private void analyzePlanTimes(Plan plan) {
		List<Trip> trips = TripStructureUtils.getTrips(plan , stageActivityTypes);
		
		for (Trip trip : trips) {
			String originActivityType = trip.getOriginActivity().getType();
			String destinationActivityType = trip.getDestinationActivity().getType();
						
			originActivityType = this.mapActivity(originActivityType);
			destinationActivityType = this.mapActivity(destinationActivityType);
												
			String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
			
			//log.info(plan.getPerson().getId().toString() + ": " + mainMode + " - " + originActivityType + " - " + destinationActivityType);
			
			Tuple<String, String> tuple = new Tuple<String, String>(originActivityType, destinationActivityType);
			for (DistributionClass distributionClass : classes) {
				boolean containsMainMode = distributionClass.mainModes.contains(mainMode);
				boolean containsActivityTypePair = distributionClass.activityTypesFromTo.contains(tuple) || distributionClass.activityTypesFromTo.size() == 0; 
				if (containsMainMode && containsActivityTypePair) {						
					double duration = 0.0;
					
					for (Leg leg : trip.getLegsOnly()) {		
						if (leg.getMode().equals(TransportMode.transit_walk)) { // actually the transit_walks are already included!
							transit_walk_class.values.add(leg.getTravelTime());
							//log.info("transit_walk_leg" + leg.getTravelTime());
						}
						
						if (leg.getTravelTime() > 120.0 * 60.0) {
							log.warn("Agent " + plan.getPerson().getId().toString() + " has a huge travel time for mode " +  
						leg.getMode() + ": distance " + leg.getRoute().getDistance() + " travel time " + leg.getTravelTime() / 60.0 + " min");
						}
							
						duration += leg.getTravelTime() / 60.0;												
					}
					
					
					
					
					for (Bin durationBin : distributionClass.distributionBins) {
						if (duration >= durationBin.low && duration < durationBin.high) {
							durationBin.count++;
							distributionClass.mean.increment(duration);
							distributionClass.values.add(duration);
							
							this.counter.incCounts(mainMode);
							this.counter.addTime(mainMode, duration);
						}
					}
				}
			}
		}
	}
	
	private void analyzePlanDistances(Plan plan) {
		List<Trip> trips = TripStructureUtils.getTrips(plan , stageActivityTypes);
		
		for (Trip trip : trips) {
			String originActivityType = trip.getOriginActivity().getType();
			String destinationActivityType = trip.getDestinationActivity().getType();
			
			originActivityType = this.mapActivity(originActivityType);
			destinationActivityType = this.mapActivity(destinationActivityType);
												
			String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());

			Tuple<String, String> tuple = new Tuple<String, String>(originActivityType, destinationActivityType);
			for (DistributionClass distributionClass : classes) {
				boolean containsMainMode = distributionClass.mainModes.contains(mainMode);
				boolean containsActivityTypePair = distributionClass.activityTypesFromTo.contains(tuple) || distributionClass.activityTypesFromTo.size() == 0; 
				if (containsMainMode && containsActivityTypePair) {	
					
					double distance = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord());
					
					// actually this is already some kind of network route!
					// cannot be compared to HITS plans :(
					
//					double distance = 0.0;
//					for (Leg leg : trip.getLegsOnly()) {														
//						Activity actPrev = PlanUtils.getPreviousActivity(plan, leg);
//						Activity actNext = PlanUtils.getNextActivity(plan, leg);
//						
//						distance += CoordUtils.calcEuclideanDistance(actPrev.getCoord(), actNext.getCoord());												
//						
//					}
					for (Bin distanceBin : distributionClass.distributionBins) {
						if (distance >= distanceBin.low && distance < distanceBin.high) {
							distanceBin.count++;
							distributionClass.mean.increment(distance);
							distributionClass.values.add(distance);
							
							this.counter.incCounts(mainMode);
							this.counter.addDistance(mainMode, distance);
						}
					}
				}
			}
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {	
		// reset
		for (DistributionClass distributionClass : classes) {
			for (Bin bin : distributionClass.distributionBins) {
				bin.count = 0;
			}
			distributionClass.values.clear();
		}
		
		for (String mode : SingaporeControlerListener.modes) {
			counter.counts.put(mode, 0);
			counter.totalDistance.put(mode, 0.0);
			counter.totalTime.put(mode, 0.0);
		}
		
		List<Plan> plans = new ArrayList<Plan>();
		for (Person person : this.population.getPersons().values()) {
			Plan bestPlan = new BestPlanSelector<Plan, Person>().selectPlan(person);
			plans.add(bestPlan);
		}
		this.analyzePlans(plans);
		
		for (DistributionClass distributionClass : classes) {
			writeDistributionClass(distributionClass, event.getIteration(), event.getServices().getControlerIO());
		}		
		this.writeCounter(event.getServices().getControlerIO(), event.getIteration());
		this.updateAndRunModesHistoryPlotter(event.getServices().getControlerIO(), event.getIteration());
		
	}
	
	private void writeDistributionClass(DistributionClass distributionClass, int iteration, 
			OutputDirectoryHierarchy outputDirectoryHierarchy) {
		
		String unit = "m";
		if (measure.equals("time")) unit = "min";
		
		String path = outputDirectoryHierarchy.getIterationPath(iteration) + "/" + measure +"/";
		String fileName = path + this.measure + "_" + distributionClass.name;
		new File(path).mkdir();
		
		double[] referenceShare = new double[distributionClass.distributionBins.size()];
		double[] simulationShare = new double[distributionClass.distributionBins.size()];
		
		// accumulate total count
		int count = 0;
		for (Bin distanceBin : distributionClass.distributionBins) {
			count += distanceBin.count;
		}
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(fileName + ".txt");
			
			int i = 0;
			log.info(measure + " best plan distribution for class " + distributionClass.name);
			
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append("low" + "\t");
			stringBuffer.append("high" + "\t");
			stringBuffer.append("reference share" + "\t");
			stringBuffer.append("simulation share");
			writer.write(stringBuffer.toString());
			writer.newLine();
			
			
			for (Bin distanceBin : distributionClass.distributionBins) {
				stringBuffer = new StringBuffer();
				referenceShare[i] = distanceBin.referenceShare;
				simulationShare[i] = (double) distanceBin.count / (double) count;
				
				stringBuffer.append(distanceBin.low + "\t");
				stringBuffer.append(distanceBin.high + "\t");
				stringBuffer.append(distanceBin.referenceShare + "\t");
				stringBuffer.append(String.valueOf(simulationShare[i]));	
				
				writer.write(stringBuffer.toString());
				writer.newLine();
				i++;
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Percentile percentile = new Percentile();
		Double[] valuesarray = distributionClass.values.toArray(new Double[distributionClass.values.size()]);
		percentile.setData(ArrayUtils.toPrimitive(valuesarray));
		String title = measure + " best plan distribution for class " + distributionClass.name;
		String xAxisLabel = measure + " class:" + "\n" + " mean: " + df.format(distributionClass.mean.getResult()) + unit 
				+ " - 50% percentile: " + df.format(percentile.evaluate(50)) + unit +
				" # values: " + distributionClass.values.size();
		
		String yAxisLabel = "Share";
		String[] categories = new String[distributionClass.distributionBins.size()];
		int i = 0;
		for (Bin distanceBin : distributionClass.distributionBins) {
			categories[i++] = distanceBin.low + "\n" + " .. " + "\n" + distanceBin.high;
		}
		BarChart chart = new BarChart(title, xAxisLabel, yAxisLabel, categories);
		
		CategoryPlot plot = chart.getChart().getCategoryPlot();
		CategoryAxis categoryAxis = plot.getDomainAxis();
		categoryAxis.setMaximumCategoryLabelLines(3);
		
		chart.addMatsimLogo();
		chart.addSeries("reference share", referenceShare);
		chart.addSeries("simulation share", simulationShare);
		chart.saveAsPng(fileName + ".png", 1024, 768);
	}
	
	public DistributionClass createAndAddDistributionClass(String className) {
		DistributionClass distributionClass = new DistributionClass();
		distributionClass.name = className;
		this.classes.add(distributionClass);
		return distributionClass;
	}
	
	public void createAndAddBin(DistributionClass distributionClass, double low, 
			double high, double referenceShare) {
		Bin bin = new Bin(low, high, referenceShare);
		distributionClass.distributionBins.add(bin);
	}
	
	public void addActivityCombination(DistributionClass distributionClass, String fromActivity,
			String toActivity) {
		distributionClass.activityTypesFromTo.add(new Tuple<String, String>(fromActivity, toActivity));
	}
	
	public void addMainMode(DistributionClass distributionClass, String mainMode) {
		distributionClass.mainModes.add(mainMode);
	}
	
	public static class DistributionClass {
		String name;
		String measure;
		Set<Tuple<String, String>> activityTypesFromTo = new LinkedHashSet<Tuple<String, String>>();
		Set<String> mainModes = new LinkedHashSet<String>();
		Set<Bin> distributionBins = new LinkedHashSet<Bin>();
		Mean mean = new Mean();
		List<Double> values = new ArrayList<Double>();
	}
	
	private static class Bin {
		final double low;
		final double high;
		final double referenceShare;
		int count = 0;
		
		public Bin(double low, double high, double referenceShare) {
			this.low = low;
			this.high = high;
			this.referenceShare = referenceShare;
		}
	}
	
	public class Counter {
		TreeMap<String, Integer> counts = new TreeMap<String, Integer>();
		TreeMap<String, Double> totalDistance = new TreeMap<String, Double>();
		TreeMap<String, Double> totalTime = new TreeMap<String, Double>();
		
		public void incCounts(String mode) {
			int i = this.counts.get(mode);
			this.counts.put(mode, i + 1);
		}
		
		public void addDistance(String mode, double v) {
			double d = this.totalDistance.get(mode);
			this.totalDistance.put(mode, d + v);
		}
		
		public void addTime(String mode, double v) {
			double d = this.totalTime.get(mode);
			this.totalTime.put(mode, d + v);
		}
		
		public double getShare(String mode, String type) {
			double share = 0.0;
			double total = 0.0;
			if (type.equals("time")) {
				for (Double v : totalTime.values()) {
					total += v;
					share = totalTime.get(mode);
				}
				log.info("Total time for mode " + mode + ": " + share);
			} else if (type.equals("distance")) {
				for (Double v : totalDistance.values()) {
					total += v;
					share = totalDistance.get(mode);			
				}
				log.info("Total distance for mode " + mode + ": " + share);
			} else {
				for (Integer v : counts.values()) {
					total += v;
					share = counts.get(mode);				
				}
				log.info("Total number for mode " + mode + ": " + share);
			}			
			return share / total;			
		}
	}
	
	private void updateAndRunModesHistoryPlotter(OutputDirectoryHierarchy outputDirectoryHierarchy, int iteration) {
		for (String mode : SingaporeControlerListener.modes) {
			double share = this.counter.getShare(mode, "counts");
			this.modesHistoryPlotter.addModeShare(iteration, mode, share);
		}
		this.modesHistoryPlotter.writeModesHistory(outputDirectoryHierarchy.getOutputPath(), iteration);
	}
	
	private void writeCounter(OutputDirectoryHierarchy outputDirectoryHierarchy, int iteration) {
		String path = outputDirectoryHierarchy.getIterationPath(iteration);
		String fileName = path + "/counter_" + measure;		
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append("mode" + "\t" + "best_plan_counts" + "\t" + this.measure);			
			writer.write(stringBuffer.toString());
			writer.newLine();
			
			
			for (String mode : SingaporeControlerListener.modes) {
				stringBuffer = new StringBuffer();
				stringBuffer.append(mode + "\t");
				stringBuffer.append(dfpercent.format(100.0 * this.counter.getShare(mode, "counts")) + "\t");
				if (measure.equals("distance")) stringBuffer.append(dfpercent.format(100.0 * this.counter.getShare(mode, "distance")) + "\t");
				if (measure.equals("time")) stringBuffer.append(dfpercent.format(100.0 * this.counter.getShare(mode, "time")));
				writer.write(stringBuffer.toString());
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}