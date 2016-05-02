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

package org.matsim.contrib.analysis.christoph;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

/**
 * Add filters for:
 * - mode
 * - activity combination
 * 
 * @author cdobler
 */
public class DistanceDistribution implements IterationEndsListener {

	private final static Logger log = Logger.getLogger(DistanceDistribution.class);
	
	private final Network network;
	private final Population population;
	private final MainModeIdentifier mainModeIdentifier;
	private final StageActivityTypes stageActivityTypes;
	private final List<DistributionClass> classes;
	
	public DistanceDistribution(final Network network, final Population population,
			final MainModeIdentifier mainModeIdentifier, final StageActivityTypes stageActivityTypes) {
		this.network = network;
		this.population = population;
		this.mainModeIdentifier = mainModeIdentifier;
		this.stageActivityTypes = stageActivityTypes;
		
		this.classes = new ArrayList<DistributionClass>();
	}
	
	private void analyzePlans(Collection<Plan> plans) {
		log.info("Analyzing distance distribution of " + plans.size() + " plans.");
		for (Plan plan : plans) {
			analyzePlan(plan);
		}
	}
	
	private void analyzePlan(Plan plan) {
		List<Trip> trips = TripStructureUtils.getTrips(plan , stageActivityTypes);
		
		for (Trip trip : trips) {
			String originActivityType = trip.getOriginActivity().getType();
			String destinationActivityType = trip.getDestinationActivity().getType();
			String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
			
			Tuple<String, String> tuple = new Tuple<String, String>(originActivityType, destinationActivityType);
			for (DistributionClass distributionClass : classes) {
				boolean containsMainMode = distributionClass.mainModes.contains(mainMode);
				boolean containsActivityTypePair = distributionClass.activityTypesFromTo.contains(tuple) || distributionClass.activityTypesFromTo.size() == 0; 
				if (containsMainMode && containsActivityTypePair) {
					for (Leg leg : trip.getLegsOnly()) {
						if (leg.getMode().equals(mainMode)) {
							Route route = leg.getRoute();
							double distance = 0.0;
							if (route instanceof NetworkRoute) {
								distance = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) route, this.network);
							} else distance = route.getDistance();
							
							for (DistanceBin distanceBin : distributionClass.distributionBins) {
								if (distance >= distanceBin.low && distance < distanceBin.high) {
									distanceBin.count++;
								}
							}
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
			for (DistanceBin distanceBin : distributionClass.distributionBins) {
				distanceBin.count = 0;
			}
		}
		
		List<Plan> plans = new ArrayList<Plan>();
		for (Person person : this.population.getPersons().values()) plans.add(person.getSelectedPlan());
		this.analyzePlans(plans);
		
		for (DistributionClass distributionClass : classes) {
			writeDistanceDistributionClass(distributionClass, event.getIteration(), event.getServices().getControlerIO());
		}
	}
	
	private void writeDistanceDistributionClass(DistributionClass distributionClass, int iteration, 
			OutputDirectoryHierarchy outputDirectoryHierarchy) {
		
		String fileName = outputDirectoryHierarchy.getIterationFilename(iteration, "DistanceDistribution_" + distributionClass.name);
		
		double[] referenceShare = new double[distributionClass.distributionBins.size()];
		double[] simulationShare = new double[distributionClass.distributionBins.size()];
		
		// accumulate total count
		int count = 0;
		for (DistanceBin distanceBin : distributionClass.distributionBins) {
			count += distanceBin.count;
		}
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(fileName + ".txt");
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append("low" + "\t");
			stringBuffer.append("high" + "\t");
			stringBuffer.append("reference share" + "\t");
			stringBuffer.append("simulation share");
			
			writer.write(stringBuffer.toString());
			writer.newLine();
			
			int i = 0;
			log.info("Distance distribution for class " + distributionClass.name);
			for (DistanceBin distanceBin : distributionClass.distributionBins) {
				
				referenceShare[i] = distanceBin.referenceShare;
				simulationShare[i] = (double) distanceBin.count / (double) count;
				
				stringBuffer = new StringBuffer();
				stringBuffer.append(distanceBin.low + "\t");
				stringBuffer.append(distanceBin.high + "\t");
				stringBuffer.append(distanceBin.referenceShare + "\t");
				stringBuffer.append(String.valueOf(simulationShare[i]));
				
				log.info("\t" + stringBuffer.toString());
				
				writer.write(stringBuffer.toString());
				writer.newLine();
				
				i++;
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String title = "Distance distribution for class " + distributionClass.name;
		String xAxisLabel = "Distance class";
		String yAxisLabel = "Share";
		String[] categories = new String[distributionClass.distributionBins.size()];
		int i = 0;
		for (DistanceBin distanceBin : distributionClass.distributionBins) {
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
	
	public void createAndAddDistanceBin(DistributionClass distributionClass, double low, 
			double high, double referenceShare) {
		DistanceBin distanceBin = new DistanceBin(low, high, referenceShare);
		distributionClass.distributionBins.add(distanceBin);
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
		Set<Tuple<String, String>> activityTypesFromTo = new LinkedHashSet<Tuple<String, String>>();
		Set<String> mainModes = new LinkedHashSet<String>();
		Set<DistanceBin> distributionBins = new LinkedHashSet<DistanceBin>();
	}
	
	private static class DistanceBin {
		final double low;
		final double high;
		final double referenceShare;
		int count = 0;
		
		public DistanceBin(double low, double high, double referenceShare) {
			this.low = low;
			this.high = high;
			this.referenceShare = referenceShare;
		}
	}
}