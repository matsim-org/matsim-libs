/* *********************************************************************** *
 * project: org.matsim.*
 * ActivitiesAnalyzer.java
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

import org.jfree.chart.axis.NumberAxis;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Counts the number of activities performed at a time based on events.
 * Agents who only perform a single activity create no events. Information about
 * them is collected using a IterationStartListener.
 *
 * @author cdobler
 */
public class ActivitiesAnalyzer implements ActivityStartEventHandler, ActivityEndEventHandler, 
		StartupListener, BeforeMobsimListener, IterationEndsListener {

	public static String defaultActivitiesFileName = "activityCounts";
	
	private final boolean autoConfig;
	
	private double endTime = 30*3600;
	
	private String activitiesFileName = defaultActivitiesFileName;
	private final Set<Id> observedAgents;
	private boolean createGraphs;
	private final Map<String, LinkedList<ActivityData>> activityCountData = new TreeMap<String, LinkedList<ActivityData>>();
	private final LinkedList<ActivityData> overallCount = new LinkedList<ActivityData>();
	
	/**
	 * This is how most people will probably will use this class.
	 * It has to be created an registered as ControlerListener.
	 * Then, it auto-configures itself (register as events handler,
	 * get paths to output files, ...).
	 */
	public ActivitiesAnalyzer() {
		
		this.autoConfig = true;
		this.createGraphs = true;
		this.observedAgents = null;
		
		reset(0);
	}
	
	public ActivitiesAnalyzer(String activitiesFileName, Set<String> activityTypes, boolean createGraphs) {
		this(activitiesFileName, activityTypes, null, createGraphs);
	}
	
	public ActivitiesAnalyzer(String activitiesFileName, Set<String> activityTypes, Set<Id> observedAgents, boolean createGraphs) {
		
		this.autoConfig = false;
		
		this.activitiesFileName = activitiesFileName;
		this.createGraphs = createGraphs;
		
		// use all activity defined in the set
		for (String activityType : activityTypes) {
			this.activityCountData.put(activityType, new LinkedList<ActivityData>());
		}
		
		if (observedAgents != null) {
			// make a copy to prevent people changing the set over the iterations
			this.observedAgents = new HashSet<Id>(observedAgents);			
		} else this.observedAgents = null;
		
		reset(0);
	}
	
	public void setCreateGraphs(boolean createGraphs) {
		this.createGraphs = createGraphs;
	}
	
	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		
		if (observedAgents != null && !observedAgents.contains(event.getPersonId())) return;
		
		LinkedList<ActivityData> list = this.activityCountData.get(event.getActType());
		
		// ignore not observed activity types
		if (list == null) return;
		
		changeCount(event.getTime(), list, -1);
		changeCount(event.getTime(), this.overallCount, -1);
	}


	@Override
	public void handleEvent(ActivityStartEvent event) {
		
		if (observedAgents != null && !observedAgents.contains(event.getPersonId())) return;
		
		LinkedList<ActivityData> list = this.activityCountData.get(event.getActType());

		// ignore not observed activity types
		if (list == null) return;
		
		changeCount(event.getTime(), list, 1);
		changeCount(event.getTime(), this.overallCount, 1);
	}
	
	private void changeCount(double time, LinkedList<ActivityData> list, int delta) {
		
		ActivityData activityData = list.getLast();
		
		/*
		 * If there is already another entry for the same time step, re-use it.
		 * Otherwise create a new one.
		 */
		if (time == activityData.time) {
			activityData.activityCount += delta;
		} else {
			list.add(new ActivityData(time, activityData.activityCount + delta));
		}
	}
	
	@Override
	public void reset(final int iter) {
		for (List<ActivityData> list : this.activityCountData.values()) {
			list.clear();
			list.add(new ActivityData(0.0, 0));
		}
		this.overallCount.clear();
		this.overallCount.add(new ActivityData(0.0, 0));
	}
	

	@Override
	public void notifyStartup(StartupEvent event) {
		
		MatsimServices controler = event.getServices();
		
		if (autoConfig) {
			// use all activity types defined in the config
			Set<String> activityTypes = new TreeSet<String>(event.getServices().getConfig().planCalcScore().getActivityTypes());
			for (String activityType : activityTypes) {
				this.activityCountData.put(activityType, new LinkedList<ActivityData>());
			}
			
			controler.getEvents().addHandler(this);
		}
	}
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		
		ActivityData overallActivityData = this.overallCount.getLast();

        for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {
			
			if (this.observedAgents != null && !this.observedAgents.contains(person.getId())) continue;
			
			Plan plan = person.getSelectedPlan();
			Activity firstActivity = (Activity) plan.getPlanElements().get(0);
			LinkedList<ActivityData> list = activityCountData.get(firstActivity.getType());
			
			// ignore not observed activity types
			if (list == null) continue;
			
			ActivityData activityData = list.getLast();
			activityData.activityCount += 1;
			overallActivityData.activityCount += 1;
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		OutputDirectoryHierarchy outputDirectoryHierarchy = event.getServices().getControlerIO();
		
		try {
			for (String activityType : this.activityCountData.keySet()) {
				String fileName = outputDirectoryHierarchy.getIterationFilename(event.getIteration(), this.activitiesFileName + "_" + activityType + ".txt");
				BufferedWriter activitiesWriter = IOUtils.getBufferedWriter(fileName);
				
				activitiesWriter.write("TIME");
				activitiesWriter.write("\t");
				activitiesWriter.write(activityType.toUpperCase());
				activitiesWriter.write("\n");
				
				List<ActivityData> list = this.activityCountData.get(activityType);
				for (ActivityData activityData : list) {
					activitiesWriter.write(String.valueOf(activityData.time));
					activitiesWriter.write("\t");
					activitiesWriter.write(String.valueOf(activityData.activityCount));
					activitiesWriter.write("\n");
				}
				
				activitiesWriter.flush();
				activitiesWriter.close();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		
		if (this.createGraphs) {
			// create chart when data of more than one iteration is available.
			XYLineChart chart;
						
			/*
			 * number of performed activities
			 */
			chart = new XYLineChart("Number of performed Activities", "time", "# activities");
			for (String activityType : this.activityCountData.keySet()) {
				List<ActivityData> list = this.activityCountData.get(activityType);
				int length = list.size();
				
				double[] times = new double[length * 2 - 1];
				double[] counts = new double[length * 2 - 1];
				Iterator<ActivityData> iter = list.iterator();
				int i = 0;
				double lastValue = 0.0;
				while (iter.hasNext()) {
					ActivityData activityData = iter.next();
					times[i] = Math.min(activityData.time, this.endTime) / 3600.0;
					counts[i] = activityData.activityCount;
					if (i > 0) {
						times[i - 1] = Math.min(activityData.time, this.endTime) / 3600.0;
						counts[i - 1] = lastValue;
					}
					lastValue = activityData.activityCount;
					i += 2;
				}
				chart.addSeries(activityType, times, counts);
			}
			int length = this.overallCount.size();
			double[] times = new double[length * 2 - 1];
			double[] counts = new double[length * 2 - 1];
			Iterator<ActivityData> iter = this.overallCount.iterator();
			int i = 0;
			double lastValue = 0.0;
			while (iter.hasNext()) {
				ActivityData activityData = iter.next();
				times[i] = Math.min(activityData.time, this.endTime) / 3600.0;
				counts[i] = activityData.activityCount;
				if (i > 0) {
					times[i - 1] = Math.min(activityData.time, this.endTime) / 3600.0;
					counts[i - 1] = lastValue;
				}
				lastValue = activityData.activityCount;
				i += 2;
			}
			chart.addSeries("overall", times, counts);
			
		    NumberAxis domainAxis = (NumberAxis) chart.getChart().getXYPlot().getDomainAxis();
		    domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));
		    domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		    domainAxis.setAutoRange(false);
		    domainAxis.setRange(0, endTime / 3600.0);
			
			chart.addMatsimLogo();
			String fileName = outputDirectoryHierarchy.getIterationFilename(event.getIteration(), this.activitiesFileName + ".png");
			chart.saveAsPng(fileName, 800, 600);
		}
	}

	private static class ActivityData {
		
		public final double time;
		public int activityCount;

		public ActivityData(double time, int activityCount) {
			this.time = time;
			this.activityCount = activityCount;
		}
	}

}