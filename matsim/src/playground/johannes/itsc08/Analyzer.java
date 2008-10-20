/* *********************************************************************** *
 * project: org.matsim.*
 * Analyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.johannes.itsc08;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.math.stat.StatUtils;
import org.matsim.basic.v01.Id;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.utils.io.IOUtils;

import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class Analyzer implements StartupListener, IterationEndsListener, AgentDepartureEventHandler,
		AgentArrivalEventHandler, LinkEnterEventHandler, ShutdownListener {

	private Set<Person> riskyUsers;
	
	private Set<Person> safeUsers;
	
	private Set<Plan> riskyPlans;
	
	private Set<Plan> safePlans;
	
	private HashMap<Person, AgentDepartureEvent> events;
	
	private HashMap<Person, Double> traveltimes;
	
	private double riskyTriptime;
	
	private double safeTriptime;
	
	private BufferedWriter writer;
	
	private Controler controler;
	
	private TIntObjectHashMap<TIntArrayList> riskyGoodTripTimes;
	
	private TIntObjectHashMap<TIntArrayList> riskyBadTripTimes;
	
	private TIntObjectHashMap<TIntArrayList> guidedTripTimes;
	
	public Analyzer(Controler controler) {
		this.controler = controler;
	}
	
	public void notifyStartup(StartupEvent event) {
		riskyGoodTripTimes = new TIntObjectHashMap<TIntArrayList>();
		riskyBadTripTimes = new TIntObjectHashMap<TIntArrayList>();
		guidedTripTimes = new TIntObjectHashMap<TIntArrayList>();
		
		event.getControler().getEvents().addHandler(this);
		try {
			writer = IOUtils.getBufferedWriter(event.getControler().getOutputFilename("analysis.txt"));
			writer.write("it\tsafe\trisky\ttt_safe\ttt_risky\tscore_safe\tscore_risky\tscore_plan_safe\tscore_plan_risky\tscore_guided\tscore_unguided\ttt_guided\ttt_unguided");
			writer.newLine();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		riskyPlans = new HashSet<Plan>();
		safePlans = new HashSet<Plan>();
		Id id = event.getControler().getNetwork().getLink("4").getId();
		for(Person p : event.getControler().getPopulation().getPersons().values()) {
			for(Plan plan : p.getPlans()) {
				Leg leg = (Leg) plan.getActsLegs().get(1);
				if(leg.getRoute().getLinkIds().contains(id)) {
					riskyPlans.add(plan);
				} else {
					safePlans.add(plan);
				}
			}
		}
	}

	public void reset(int iteration) {
		riskyUsers = new HashSet<Person>();
		safeUsers = new HashSet<Person>();
		riskyTriptime = 0;
		safeTriptime = 0;
		events = new HashMap<Person, AgentDepartureEvent>();
		traveltimes = new HashMap<Person, Double>();
		
		
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			writer.write(String.valueOf(event.getIteration()));
			writer.write("\t");
			/*
			 * Users
			 */
			writer.write(double2String(safeUsers.size()));
			writer.write("\t");
			writer.write(double2String(riskyUsers.size()));
			writer.write("\t");
			/*
			 * travel times
			 */
			writer.write(double2String(safeTriptime/safeUsers.size()));
			writer.write("\t");
			writer.write(double2String(riskyTriptime/riskyUsers.size()));
			writer.write("\t");
			/*
			 * scores executed
			 */
			writer.write(double2String(getAvrScore(safeUsers)));
			writer.write("\t");
			writer.write(double2String(getAvrScore(riskyUsers)));
			writer.write("\t");
			/*
			 * scores
			 */
			writer.write(double2String(getAvrScorePlan(safePlans)));
			writer.write("\t");
			writer.write(double2String(getAvrScorePlan(riskyPlans)));
			writer.write("\t");
			/*
			 * guidance
			 */
			Set<Person> guided = controler.getGuidedPersons();
			Collection<Person> unguided = CollectionUtils.subtract(event.getControler().getPopulation().getPersons().values(), guided);
			
			writer.write(double2String(getAvrScore(guided)));
			writer.write("\t");
			writer.write(double2String(getAvrScore(unguided)));
			writer.write("\t");
			writer.write(double2String(getAvrTripTime(guided)));
			writer.write("\t");
			writer.write(double2String(getAvrTripTime(unguided)));
			
			writer.newLine();
			
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private String double2String(double val) {
		if(val == 0 || Double.isNaN(val) || Double.isInfinite(val))
			return "";
		else
			return String.valueOf(val);
	}

	public void handleEvent(LinkEnterEvent event) {
		if(event.link.getId().toString().equals("4"))
			riskyUsers.add(event.agent);
		else if(event.link.getId().toString().equals("5"))
			safeUsers.add(event.agent);
		
	}

	private double getAvrScorePlan(Set<Plan> plans) {
		if(plans.size() == 0)
			return 0.0;
		
		double scoresum = 0;
		for(Plan p : plans) {
			scoresum += p.getScore();
		}
		double result = scoresum/(double)plans.size();
		if(Double.isNaN(result))
			return 0;
		else
			return result;	
	}
	
	private double getAvrScore(Collection<Person> persons) {
		if(persons.size() == 0)
			return 0.0;
		
		double scoresum = 0;
		for(Person p : persons) {
			scoresum += p.getSelectedPlan().getScore();
		}
		return scoresum/(double)persons.size();
	}
	
	private double getAvrTripTime(Collection<Person> persons) {
		if(persons.size() == 0)
			return 0.0;
		
		double scoresum = 0;
		for(Person p : persons) {
			scoresum += traveltimes.get(p);
		}
		return scoresum/(double)persons.size();
	}
	
	public void handleEvent(AgentDepartureEvent event) {
		if(event.link.getId().toString().equals("1"))
			events.put(event.agent, event);
		
	}

	public void handleEvent(AgentArrivalEvent event) {
		AgentDepartureEvent e = events.get(event.agent);
		if(e != null) {
			events.remove(event.agent);
			double triptime = event.time - e.time;
			traveltimes.put(event.agent, triptime);
			if(((Leg)event.agent.getSelectedPlan().getActsLegs().get(1)).getRoute().getRoute().get(1).getId().toString().equals("3")) {
				riskyTriptime += triptime;
				if(controler.getIteration() % 2 == 0) { //FIXME: needs to be consistent with IncidentGenerator!!!
					// bad day
					addTravelTime(riskyBadTripTimes, (int)e.time, (int)triptime);
				} else {
					// good day
					addTravelTime(riskyGoodTripTimes, (int)e.time, (int)triptime);
				}
				
			} else
				safeTriptime += triptime;
			
			if(controler.getGuidedPersons().contains(event.agent))
				addTravelTime(guidedTripTimes, (int)e.time, (int)triptime);
		}
		
	}
	
	private void addTravelTime(TIntObjectHashMap<TIntArrayList> map, int time, int traveltime) {
		if(controler.getIteration() >= 200) {
			TIntArrayList list = map.get(time);
			if(list == null) {
				list = new TIntArrayList();
				map.put(time, list);
			}
			list.add(traveltime);
		}
	}

	public void notifyShutdown(ShutdownEvent event) {
		double beta_travel = controler.getConfig().charyparNagelScoring().getTraveling();
		double beta_late = controler.getConfig().charyparNagelScoring().getLateArrival();
		int t_act_start = 21600;
		
		TIntDoubleHashMap avrRiskyBadTriptimes = calcAvrTriptimes(riskyBadTripTimes);
		TIntDoubleHashMap avrRiskyGoodTriptimes = calcAvrTriptimes(riskyGoodTripTimes);
		TIntDoubleHashMap avrGuidedTriptimes = calcAvrTriptimes(guidedTripTimes);
		
		TIntDoubleHashMap pi_avr_map = new TIntDoubleHashMap();
		TIntDoubleHashMap pi_guided_map = new TIntDoubleHashMap();
		
		for(Person p : controler.getPopulation()) {
			Plan plan = p.getSelectedPlan();
			int starttime = (int) plan.getFirstActivity().getEndTime();
			int t_good = (int)avrRiskyGoodTriptimes.get(starttime);
			int t_bad = (int)avrRiskyBadTriptimes.get(starttime);
			int t_guided = (int)avrGuidedTriptimes.get(starttime);
			
			double utilGood = calcUtil(starttime, (int) (starttime + t_good), t_act_start, beta_travel, beta_late);
			double utilBad =  calcUtil(starttime, (int) (starttime + t_bad), t_act_start, beta_travel, beta_late);
//			double utilActStart =  calcUtil(starttime, t_act_start, t_act_start, beta_travel, beta_late);
			double avrUtil = (utilGood + utilBad)/2.0;
			
			double t_ce = (avrUtil - (t_act_start - starttime)*beta_travel)/(beta_travel + beta_late) + t_act_start;
			double t_avr = (t_good + t_bad)/2.0;
			
			double pi_avr = t_ce - (t_avr + starttime);
			double pi_guided = t_ce - (t_guided + starttime);
			
			pi_avr_map.put(starttime, pi_avr);
			pi_guided_map.put(starttime, pi_guided);
		}
		
		try {
			BufferedWriter w = IOUtils.getBufferedWriter(controler.getOutputFilename("ce_analysis.txt"));
			w.write("tt_risky_good\ttt_risky_bad\ttt_guided\tpi_avr\tpi_guided");
			w.newLine();
			w.write(String.valueOf(StatUtils.mean(avrRiskyGoodTriptimes.getValues())));
			w.write("\t");
			w.write(String.valueOf(StatUtils.mean(avrRiskyBadTriptimes.getValues())));
			w.write("\t");
			w.write(String.valueOf(StatUtils.mean(avrGuidedTriptimes.getValues())));
			w.write("\t");
			w.write(String.valueOf(StatUtils.mean(pi_avr_map.getValues())));
			w.write("\t");
			w.write(String.valueOf(StatUtils.mean(pi_guided_map.getValues())));
			w.close();
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private double calcUtil(int departure, int arrival, int t_act_start, double beta_travel, double beta_late) {
		double score = 0;
		
		score += (arrival - departure) * beta_travel;
		int late = arrival - t_act_start;
		if(late > 0)
			score += late  * beta_late;
		
		return score;
	}
	
	private TIntDoubleHashMap calcAvrTriptimes(TIntObjectHashMap<TIntArrayList> map) {
		TIntDoubleHashMap ttmap = new TIntDoubleHashMap();
		for(int time : map.keys()) {
			int sum = 0;
			int[] array = map.get(time).toNativeArray();
			for(int tt : array)
				sum += tt;
			ttmap.put(time, sum/(double)array.length);
		}
		return ttmap;
	}
}
