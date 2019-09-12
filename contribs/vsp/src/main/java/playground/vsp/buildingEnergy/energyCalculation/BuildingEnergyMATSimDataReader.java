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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.analysis.modules.simpleTripAnalyzer.SimpleTripAnalyzerModule;
import playground.vsp.buildingEnergy.linkOccupancy.LinkActivityOccupancyCounter;

/**
 * @author droeder
 *
 */
class BuildingEnergyMATSimDataReader {

	private static final Logger log = Logger
			.getLogger(BuildingEnergyMATSimDataReader.class);
	private List<Integer> timeBins;
	private Set<String> activityTypes;
	private double tmax;
	private double td;
	
	private HashMap<String, LinkOccupancyStats> type2OccupancyStats;
	private PopulationStats populationStats;
	private ArrayList<Id> links;
	private SimpleTripAnalyzerModule trips;

	BuildingEnergyMATSimDataReader(List<Integer> timeBins,
							double td,
							double tmax,
							Set<String> activityTypes) {
		this.timeBins = timeBins;
		this.activityTypes = activityTypes;
		this.td = td;
		this.tmax = tmax;
	}
	
	void run(String net, String plans, String events, String homeType, String workType, String runId){
		BuildingEnergyPlansAnalyzer plansAna = new BuildingEnergyPlansAnalyzer(homeType, workType);
		Scenario sc = prepareScenario(plans, net, plansAna);
		this.trips = new SimpleTripAnalyzerModule(ConfigUtils.createConfig(), sc.getNetwork(), plansAna.getPopulation(), runId);
		this.trips.preProcessData();
		log.warn("only persons with work- and home-Activities will be handled!");
		this.populationStats = plansAna.getStats();
		this.type2OccupancyStats = new HashMap<String, LinkOccupancyStats>();
		for(String s: activityTypes){
			this.type2OccupancyStats.put(s, initOccupancyCounter(s, plansAna.getPopulation()));
		}
		parseEvents(events, homeType, trips);
		this.trips.postProcessData();
	}
	
	SimpleTripAnalyzerModule getTripsAnalysis(){
		return trips;
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
	 * @param trips 
	 */
	private void parseEvents(String events, String homeType, SimpleTripAnalyzerModule trips) {
		EventsManager manager = EventsUtils.createEventsManager();
		// create a filter and add the handler 
		EventsFilter filter = new EventsFilter(this.populationStats.getHomeAndWorkAgentIds(), homeType);
		for(LinkOccupancyStats los: this.type2OccupancyStats.values()){
			for(LinkActivityOccupancyCounter laoc: los.getStats().values()){
				filter.addCounter(laoc);
			}
		}
		
		manager.addHandler(filter);
		for(EventHandler handler :trips.getEventHandler()){
			manager.addHandler(handler);
		}
		//handle events via filter
		new MatsimEventsReader(manager).readFile(events);
		//finish handler
		for(LinkOccupancyStats los: this.type2OccupancyStats.values()){
			for(LinkActivityOccupancyCounter laoc: los.getStats().values()){
				laoc.finish();
			}
		}		
	}
	
	/*
	 * this is very quick and dirty hack but I don't want to change the other impl here
	 * I want to use only agents with both, home and work activities. Further for the
	 * Berlin-scenario it is necessary to ``refactor'' ``not specified''-activities
	 * as they are mostly home-activities... //dr, nov'13
	 */
	private static class EventsFilter implements ActivityStartEventHandler, ActivityEndEventHandler{
		
		private List<Id> personFilter;
		private List<LinkActivityOccupancyCounter> handler = new ArrayList<LinkActivityOccupancyCounter>();
		private String homeType;
		private boolean warn;

		public EventsFilter(List<Id> personFilter, String homeType) {
			if(BuildingEnergyAnalyzer.berlin){
				log.warn("Will replace events with activity ``not specified'' with events of type ``" + homeType + "''.");
			}
			this.homeType = homeType;
			this.personFilter = personFilter;
			warn = true;
		}
		
		void addCounter(LinkActivityOccupancyCounter counter){
			handler.add(counter);
		}

		@Override
		public void reset(int iteration) {
			
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			if(personFilter.contains(event.getPersonId())){
				ActivityEndEvent e = modifyIfNecessary(event);
				for(LinkActivityOccupancyCounter laoc: handler){
					laoc.handleEvent(e);
				}
			}
		}

		/**
		 * @param event
		 * @return
		 */
		private ActivityEndEvent modifyIfNecessary(ActivityEndEvent event) {
			if(event.getActType().equals("not specified") && BuildingEnergyAnalyzer.berlin){
				if(warn){
					log.warn("modifying events for berlin-scenario. Thrown only once.");
					warn = false;
				}
				return new ActivityEndEvent(event.getTime(), 
								event.getPersonId(), 
								event.getLinkId(), 
								event.getFacilityId(), 
								homeType);
			}
			return event;
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			if(personFilter.contains(event.getPersonId())){
				ActivityStartEvent e = modifyIfNecessary(event);
				for(LinkActivityOccupancyCounter laoc: handler){
					laoc.handleEvent(e);
				}
			}
		}

		/**
		 * @param event
		 * @return
		 */
		private ActivityStartEvent modifyIfNecessary(ActivityStartEvent event) {
			if(event.getActType().equals("not specified") && BuildingEnergyAnalyzer.berlin){
				if(warn){
					log.warn("modifying events for berlin-scenario. Thrown only once.");
					warn = false;
				}
				return new ActivityStartEvent(event.getTime(), 
								event.getPersonId(), 
								event.getLinkId(), 
								event.getFacilityId(), 
								homeType);
			}
			return event;
		}
		
	}

	/**
	 * @param plansFile
	 * @param plansAna 
	 * @return
	 */
	private Scenario prepareScenario(String plansFile, String networkFile, BuildingEnergyPlansAnalyzer plansAna) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MutableScenario sc1 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        plansAna.setPopulation(PopulationUtils.createPopulation(sc1.getConfig(), sc1.getNetwork()));
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);
		if(links == null){
			this.links = new ArrayList<Id>(sc.getNetwork().getLinks().keySet());
		}
		Collections.sort(links);
		final PersonAlgorithm algo = plansAna;
		// TODO[dr] this slows down the very drastic. Streaming is only necessary here because we need a 
		// filtered population for the berlin-scenario. 
//		final Population reader = (Population) sc.getPopulation();
		StreamingPopulationReader reader = new StreamingPopulationReader( sc ) ;
		reader.addAlgorithm(algo);
		StreamingDeprecated.setIsStreaming(reader, true);
//		new MatsimPopulationReader(sc).readFile(plansFile);
		reader.readFile(plansFile);
		log.info("resulting population contains " + plansAna.getPopulation().getPersons().size() + " persons.");
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
		stats.add(BuildingEnergyAnalyzer.all , new LinkActivityOccupancyCounter(p, 0, tmax , string));
		return stats;
	}
	
	
//	
	protected class LinkOccupancyStats{
		
		private Map<String, LinkActivityOccupancyCounter> l = new LinkedHashMap<String, LinkActivityOccupancyCounter>();
		
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
		private List<Id> home;
		private List<Id> work;
		private Population population;
		private boolean warn;

		BuildingEnergyPlansAnalyzer(String homeType, String workType) {
			this.homeType = homeType;
			this.workType = workType;
			this.work = new ArrayList<Id>();
			this.home = new ArrayList<Id>();
			if(BuildingEnergyAnalyzer.berlin){
				log.warn("currently activityType ``not specified'' is handled equals to ``" + homeType + "'' (necessary for Berlin-Scenario)");
			}
			warn = true;
		}

		/**
		 * @param population
		 */
		public void setPopulation(Population population) {
			this.population = population;
		}
		
		public Population getPopulation(){
			return population;
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
					if(a.getType().equals(homeType) || a.getType().equals("not specified")){
						home = true;
					}
				}
				// we know everything we need to know, return
				if(home && work){
					// this is not really necessary except for berlin-study and (maybe) it saves some memory
					Person p = this.population.getFactory().createPerson(person.getId());
					Plan plan =  this.population.getFactory().createPlan();
					for(int ii = 0; ii < pe.size(); ii++){
						PlanElement element = pe.get(ii);
						if(element instanceof Activity){
							Activity aa = (Activity) element;
							// this is a "hack", necessary for the berlin-scenario...
							if(aa.getType().equals("not specified") && BuildingEnergyAnalyzer.berlin){
								Coord c = aa.getCoord();
								aa = this.population.getFactory().createActivityFromLinkId(homeType, aa.getLinkId());
								((Activity)aa).setCoord(c);
								if(warn){
									log.warn("modifying activitytypes for berlin-scenario. Thrown only once...");
									warn = false;
								}
							}
							plan.addActivity(aa);
						}else{
							Leg l = (Leg) element;
							l = this.population.getFactory().createLeg(l.getMode());
							plan.addLeg(l);
						}
					}
					p.addPlan(plan);
					this.population.addPerson(p);
					this.home.remove(person.getId());
					this.work.remove(person.getId());
					return;
				}
			}
			if(home){
				this.home.add(person.getId());
			} 
			if (work){
				this.work.add(person.getId());
			}
		}
		
		public PopulationStats getStats(){
			return new PopulationStats(new ArrayList<Id>(this.population.getPersons().keySet()), home, work);
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
		
		public final List<Id> getHomeOnlyAgentIds(){
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

