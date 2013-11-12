/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.buildingEnergy.energyCalculation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.vsp.buildingEnergy.linkOccupancy.LinkActivityOccupancyCounter;

/**
 * @author droeder
 *
 */
class BuildingEnergyDataReader {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(BuildingEnergyDataReader.class);
	private List<Integer> timeBins;
	private Set<String> activityTypes;
	private double tmax;
	private double td;
	
	private HashMap<String, LinkOccupancyStats> type2OccupancyStats;
	private PopulationStats populationStats;
	private ArrayList<Id> links;

	BuildingEnergyDataReader(List<Integer> timeBins,
							double td,
							double tmax,
							Set<String> activityTypes) {
		this.timeBins = timeBins;
		this.activityTypes = activityTypes;
		this.td = td;
		this.tmax = tmax;
	}
	
	void run(String net, String plans, String events, String homeType, String workType){
		BuildingEnergyPlansAnalyzer plansAna = new BuildingEnergyPlansAnalyzer(homeType, workType);
		Population p = prepareScenario(plans, net, plansAna).getPopulation();
		this.populationStats = plansAna.getStats();
		this.type2OccupancyStats = new HashMap<String, LinkOccupancyStats>();
		for(String s: activityTypes){
			this.type2OccupancyStats.put(s, initOccupancyCounter(s, p));
		}
		parseEvents(events);
		
	}
	
	PopulationStats getPStats(){
		return populationStats;
	}
	
	Map<String, LinkOccupancyStats> getLinkActivityStats(){
		return this.type2OccupancyStats;
	}
	
	List<Id> getLinkIds(){
		return this.links;
	}
	
	/**
	 * @param events
	 */
	private void parseEvents(String events) {
		EventsManager manager = EventsUtils.createEventsManager();
		for(LinkOccupancyStats los: this.type2OccupancyStats.values()){
			for(LinkActivityOccupancyCounter laoc: los.getStats().values()){
				manager.addHandler(laoc);
			}
		}
		new MatsimEventsReader(manager).readFile(events);
		for(LinkOccupancyStats los: this.type2OccupancyStats.values()){
			for(LinkActivityOccupancyCounter laoc: los.getStats().values()){
				laoc.finish();
			}
		}		
	}

	/**
	 * @param plansFile
	 * @param plansAna 
	 * @return
	 */
	private Scenario prepareScenario(String plansFile, String networkFile, BuildingEnergyPlansAnalyzer plansAna) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(networkFile);
		if(links == null){
			this.links = new ArrayList<Id>(sc.getNetwork().getLinks().keySet());
		}
		Collections.sort(links);
		new MatsimPopulationReader(sc).readFile(plansFile);
		((PopulationImpl) sc.getPopulation()).addAlgorithm(plansAna);
		((PopulationImpl) sc.getPopulation()).runAlgorithms();
		return sc;
	}

	/**
	 * @param string
	 * @param td
	 * @param tmax
	 * @return
	 */
	private LinkOccupancyStats initOccupancyCounter(
			String string, Population p) {
		LinkOccupancyStats stats = new LinkOccupancyStats();
		for(int i : timeBins){
			stats.add(String.valueOf(i), new LinkActivityOccupancyCounter(p, i, i + td , string));
		}
		stats.add(BuildingEnergyAnalyzerMain.all , new LinkActivityOccupancyCounter(p, 0, tmax , string));
		return stats;
	}
	
	
//	
	protected class LinkOccupancyStats{
		
		private Map<String, LinkActivityOccupancyCounter> l = new HashMap<String, LinkActivityOccupancyCounter>();
		
		public LinkOccupancyStats() {
		}
		
		private void add(String s, LinkActivityOccupancyCounter l){
			this.l.put(s, l);
		}
		
		final Map<String, LinkActivityOccupancyCounter> getStats(){
			return l;
		}
	}
	
	/**
	 * 
	 * Analyzes the number of persons working/ performing home activities. 
	 * Note, a person performing a certain activity-type more than once, will
	 * be counted only once
	 * @author droeder
	 *
	 */
	private class BuildingEnergyPlansAnalyzer extends AbstractPersonAlgorithm {

		private String homeType;
		private String workType;
		private List<Id> homeWork;
		private List<Id> home;
		private List<Id> work;

		BuildingEnergyPlansAnalyzer(String homeType, String workType) {
			this.homeType = homeType;
			this.workType = workType;
			this.homeWork = new ArrayList<Id>();
			this.work = new ArrayList<Id>();
			this.home = new ArrayList<Id>();
		}

		@Override
		public void run(Person person) {
			boolean work = false;
			boolean home = false;
			List<PlanElement> pe = person.getSelectedPlan().getPlanElements();
			for(int i = 0; i< pe.size(); i += 2){
				Activity a = (Activity) pe.get(i);
				if(!work){
					if(a.getType().equals(workType)){
						work = true;
					}
				}
				if(!home){
					if(a.getType().equals(homeType)){
						home = true;
					}
				}
				// we know everything we need to know, return
				if(home && work){
					this.homeWork.add(person.getId());
					return;
				}
			}
			if(home){
				this.home.add(person.getId());
			}else if (work){
				this.work.add(person.getId());
			}
		}
		
		public PopulationStats getStats(){
			return new PopulationStats(homeWork, home, work);
		}

		
	}
	
	protected class PopulationStats{
		
		private List<Id> work;
		private List<Id> home;
		private List<Id> homeWork;

		private PopulationStats(List<Id> homeWork, List<Id> home, List<Id> work){
			this.homeWork = homeWork;
			this.home = home;
			this.work = work;
			
		}
		
		public final int getWorkCnt() {
			return (work.size() + homeWork.size());
		}
		
		public int getHomeAndWorkCnt() {
			return homeWork.size();
		}
		
		public final int getHomeCnt() {
			return (home.size() + homeWork.size());
		}
		
		public final List<Id> getHomeOnlyHomeAgentIds(){
			return home;
		}
		
		public final List<Id> getWorkOnlyAgentIds(){
			return work;
		}
		
		public final List<Id> getHomeAndWorkAgentIds(){
			return homeWork;
		}
		
	}

}

