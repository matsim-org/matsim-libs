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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.math.stat.StatUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.RouteUtils;


/**
 * @author illenberger
 *
 */
public class Analyzer implements StartupListener, IterationEndsListener, AgentDepartureEventHandler,
		AgentArrivalEventHandler, LinkEnterEventHandler, ShutdownListener, IterationStartsListener {

	private Set<Person> riskyUsers;

	private Set<Person> safeUsers;

	private Set<Plan> riskyPlans;

	private Set<Plan> safePlans;

	private HashMap<Id, AgentDepartureEvent> events; // personId

	private HashMap<Id, AgentDepartureEvent> eventsReturn; // personId

	private HashMap<Id, Double> traveltimes; // personId

	private double riskyTriptime;

	private double safeTriptime;

	private double returnTripTime;

	private BufferedWriter writer;

	private Controler controler;

	private TIntObjectHashMap<TDoubleArrayList> riskyGoodTripTimes;

	private TIntObjectHashMap<TDoubleArrayList> riskyBadTripTimes;

	private TIntObjectHashMap<TDoubleArrayList> guidedTripTimes;

//	private BufferedWriter arrivalTimeWriter;

	private IncidentGenerator inicidents;

	private final Network network;

	public Analyzer(Controler controler, IncidentGenerator incidents) {
		this.controler = controler;
		this.inicidents = incidents;
		this.network = controler.getNetwork();
	}


	public void notifyStartup(StartupEvent event) {
		riskyGoodTripTimes = new TIntObjectHashMap<TDoubleArrayList>();
		riskyBadTripTimes = new TIntObjectHashMap<TDoubleArrayList>();
		guidedTripTimes = new TIntObjectHashMap<TDoubleArrayList>();

		event.getControler().getEvents().addHandler(this);
		try {
			writer = IOUtils.getBufferedWriter(event.getControler().getControlerIO().getOutputFilename("analysis.txt"));
			writer.write("it\tsafe\trisky\ttt_safe\ttt_risky\ttt_return\ttt_avr\tscore_safe\tscore_risky\tscore_plan_safe\tscore_plan_risky\tscore_guided\tscore_unguided\ttt_guided\ttt_unguided");
			writer.write("\tn_guidedSafe\ttt_guidedSafe\tn_guidedRisky\ttt_guidedRisky\tn_unguidedSafe\ttt_unguidedSafe\tn_unguidedRisky\ttt_unguidedRisky");
			writer.newLine();



		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		riskyPlans = new HashSet<Plan>();
		safePlans = new HashSet<Plan>();
		Id id = event.getControler().getNetwork().getLinks().get(new IdImpl("4")).getId();
		for(Person p : event.getControler().getPopulation().getPersons().values()) {
			for(Plan plan : p.getPlans()) {
				LegImpl leg = (LegImpl) plan.getPlanElements().get(1);
				if(((NetworkRoute) leg.getRoute()).getLinkIds().contains(id)) {
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
		returnTripTime = 0;
		events = new HashMap<Id, AgentDepartureEvent>();
		eventsReturn = new HashMap<Id, AgentDepartureEvent>();
		traveltimes = new HashMap<Id, Double>();


	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			writer.write(String.valueOf(event.getIteration()));
			writer.write("\t");
			/*
			 * Users
			 */
			writer.write(String.valueOf(safeUsers.size()));
			writer.write("\t");
			writer.write(String.valueOf(riskyUsers.size()));
			writer.write("\t");
			/*
			 * travel times
			 */
			writer.write(double2String(safeTriptime/safeUsers.size()));
			writer.write("\t");
			writer.write(double2String(riskyTriptime/riskyUsers.size()));
			writer.write("\t");
			writer.write(double2String(returnTripTime/(riskyUsers.size() + safeUsers.size())));
			writer.write("\t");

			double sum = 0;
			for(Double d : traveltimes.values()) {
				sum += d;
			}
			writer.write(String.valueOf(sum/traveltimes.size()));
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
			writer.write("\t");

			Collection<Person> guidedSafe = CollectionUtils.intersection(guided, safeUsers);
			Collection<Person> guidedRisky = CollectionUtils.intersection(guided, riskyUsers);
			Collection<Person> unguidedSafe = CollectionUtils.intersection(unguided, safeUsers);
			Collection<Person> unguidedRisky = CollectionUtils.intersection(unguided, riskyUsers);

			writer.write(String.valueOf(guidedSafe.size()));
			writer.write("\t");
			writer.write(double2String(getAvrTripTime(guidedSafe)));
			writer.write("\t");

			writer.write(String.valueOf(guidedRisky.size()));
			writer.write("\t");
			writer.write(double2String(getAvrTripTime(guidedRisky)));
			writer.write("\t");

			writer.write(String.valueOf(unguidedSafe.size()));
			writer.write("\t");
			writer.write(double2String(getAvrTripTime(unguidedSafe)));
			writer.write("\t");

			writer.write(String.valueOf(unguidedRisky.size()));
			writer.write("\t");
			writer.write(double2String(getAvrTripTime(unguidedRisky)));

			writer.newLine();
			writer.flush();


//			arrivalTimeWriter.close();



		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private String double2String(double val) {
		if(val == 0 || Double.isNaN(val) || Double.isInfinite(val))
			return "NA";
		else
			return String.valueOf(val);
	}

	public void handleEvent(LinkEnterEvent event) {
		if(event.getLinkId().toString().equals("4"))
			riskyUsers.add(controler.getPopulation().getPersons().get(event.getPersonId()));
		else if(event.getLinkId().toString().equals("5"))
			safeUsers.add(controler.getPopulation().getPersons().get(event.getPersonId()));
	}

	private double getAvrScorePlan(Set<Plan> plans) {
		if(plans.size() == 0)
			return 0.0;

		double scoresum = 0;
		for(Plan p : plans) {
			Double score = p.getScore();
			if(score == null)
				score = 0.0;
			scoresum += score.doubleValue();
		}
		double result = scoresum/plans.size();
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
			scoresum += p.getSelectedPlan().getScore().doubleValue();
		}
		return scoresum/persons.size();
	}

	private double getAvrTripTime(Collection<Person> persons) {
		if(persons.size() == 0)
			return 0.0;

		double scoresum = 0;
		for(Person p : persons) {
			scoresum += traveltimes.get(p.getId());
		}
		return scoresum/persons.size();
	}

	public void handleEvent(AgentDepartureEvent event) {
		if(event.getLinkId().toString().equals("1"))
			events.put(event.getPersonId(), event);
		else
			eventsReturn.put(event.getPersonId(), event);
	}

	public void handleEvent(AgentArrivalEvent event) {
		AgentDepartureEvent e = events.get(event.getPersonId());
		if(e != null) {
			events.remove(event.getPersonId());
			double triptime = event.getTime() - e.getTime();
			traveltimes.put(event.getPersonId(), triptime);
			Person person = this.controler.getPopulation().getPersons().get(event.getPersonId());
			if (RouteUtils.getNodes((NetworkRoute) ((LegImpl)person.getSelectedPlan().getPlanElements().get(1)).getRoute(), this.network).get(1).getId().toString().equals("3")) {
				riskyTriptime += triptime;
//				if(controler.getIteration() % 2 == 0) { //FIXME: needs to be consistent with IncidentGenerator!!!
				if(inicidents.isBadDay()) {
					// bad day
					addTravelTime(riskyBadTripTimes, (int)e.getTime(), triptime);
				} else {
					// good day
					addTravelTime(riskyGoodTripTimes, (int)e.getTime(), triptime);
				}

			} else
				safeTriptime += triptime;

			if(controler.getGuidedPersons().contains(person))
				addTravelTime(guidedTripTimes, (int)e.getTime(), triptime);


//
//			try {
//				arrivalTimeWriter.newLine();
//				arrivalTimeWriter.write(String.valueOf(event.time));
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}

		}

		e = eventsReturn.get(event.getPersonId());
		if(e != null) {
			events.remove(event.getPersonId());
			double triptime = event.getTime() - e.getTime();
			returnTripTime += triptime;
//			System.err.println(triptime);
		}
	}

	private void addTravelTime(TIntObjectHashMap<TDoubleArrayList> map, int time, double traveltime) {
		if(controler.getIterationNumber() >= 200) {
			TDoubleArrayList list = map.get(time);
			if(list == null) {
				list = new TDoubleArrayList();
				map.put(time, list);
			}
			list.add(traveltime);
		}
	}

	public void notifyShutdown(ShutdownEvent event) {
		double beta_travel = controler.getConfig().charyparNagelScoring().getTraveling();
		double beta_late = controler.getConfig().charyparNagelScoring().getLateArrival();
		int t_act_start = 21600;

		TDoubleDoubleHashMap avrRiskyBadTriptimes = calcAvrTriptimes(riskyBadTripTimes);
		TDoubleDoubleHashMap avrRiskyGoodTriptimes = calcAvrTriptimes(riskyGoodTripTimes);
		TDoubleDoubleHashMap avrGuidedTriptimes = calcAvrTriptimes(guidedTripTimes);

		TDoubleArrayList pi_avr_list = new TDoubleArrayList();
		TDoubleArrayList pi_guided_list = new TDoubleArrayList();
		TDoubleArrayList pi_avr_list2 = new TDoubleArrayList();
		TDoubleArrayList pi_guided_list2 = new TDoubleArrayList();
		TDoubleArrayList t_ce_list = new TDoubleArrayList();
		TDoubleArrayList pi_safe_list_plus = new TDoubleArrayList();
		TDoubleArrayList pi_safe_list_minus = new TDoubleArrayList();
		TDoubleArrayList pi_safe_list_avr = new TDoubleArrayList();

		TDoubleArrayList willingness = new TDoubleArrayList();

		TIntObjectHashMap<TDoubleArrayList> pi_avr_map = new TIntObjectHashMap<TDoubleArrayList>();
		TIntObjectHashMap<TDoubleArrayList> pi_guided_map = new TIntObjectHashMap<TDoubleArrayList>();

		int countPiAvrUsers = 0;
		int countPiGuidedUsers = 0;
		int t_last_guided = 327;
		for(Person p : controler.getPopulation().getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			int starttime = (int) ((PlanImpl) plan).getFirstActivity().getEndTime();
			int t_good = (int)avrRiskyGoodTriptimes.get(starttime);
			int t_bad = (int)avrRiskyBadTriptimes.get(starttime);
			int t_guided = (int)avrGuidedTriptimes.get(starttime);
			if(t_guided == 0) { // works because departure times increasing with agent-id
				t_guided = t_last_guided;
			} else {
				t_last_guided = t_guided;
			}
			if(t_good == 0 || t_bad == 0) {
//				System.err.println("No value found." + (starttime));
			} else {

			double utilGood = calcUtil(starttime, (starttime + t_good), t_act_start, beta_travel, beta_late);
			double utilBad =  calcUtil(starttime, (starttime + t_bad), t_act_start, beta_travel, beta_late);
//			double utilActStart =  calcUtil(starttime, t_act_start, t_act_start, beta_travel, beta_late);
			double avrUtil = (utilGood + utilBad)/2.0;

			double utilGuided = calcUtil(starttime, (starttime + t_guided), t_act_start, beta_travel, beta_late);
			willingness.add(avrUtil - utilGuided);

			double t_ce = (avrUtil - (t_act_start - starttime)*beta_travel)/(beta_travel + beta_late) + t_act_start;
			if(t_ce <= t_act_start) { // agent is in time or early
				t_ce = avrUtil/beta_travel + starttime;
			}

			t_ce_list.add(t_ce);

			double t_avr = (t_good + t_bad)/2.0;

			double pi_avr = t_ce - (t_avr + starttime);
			double pi_guided = t_ce - (t_guided + starttime);
			double pi_safe = t_ce - (435 + starttime);

			if(pi_avr > 0) {
				pi_avr_list2.add(pi_avr);
				countPiAvrUsers++;
			}
			if(pi_guided > 0) {
				pi_guided_list2.add(pi_guided);
				countPiGuidedUsers++;
			}

			if(pi_safe > 0) {
				pi_safe_list_plus.add(pi_safe);
			} else {
				pi_safe_list_minus.add(pi_safe);
			}
			pi_safe_list_avr.add(pi_safe);

			pi_avr_list.add(pi_avr);
			pi_guided_list.add(pi_guided);
			addTravelTime(pi_avr_map, starttime, pi_avr);
			addTravelTime(pi_guided_map, starttime, pi_guided);
			}
		}

		try {
			BufferedWriter w = IOUtils.getBufferedWriter(event.getControler().getControlerIO().getOutputFilename("ce_analysis.txt"));
			w.write("tt_risky_good\ttt_risky_bad\ttt_guided\tpi_avr\tpi_guided\tn_pi_avr\tn_pi_guided\tpi_avr2\tpi_guided2\tt_ce\tpi_safe_plus\tn_pi_safe_plus\tpi_safe_minus\tn_pi_safe_minus\tpi_safe_avr\twillingness");
			w.newLine();
			w.write(String.valueOf(StatUtils.mean(avrRiskyGoodTriptimes.getValues())));
			w.write("\t");
			w.write(String.valueOf(StatUtils.mean(avrRiskyBadTriptimes.getValues())));
			w.write("\t");
			w.write(String.valueOf(StatUtils.mean(avrGuidedTriptimes.getValues())));
			w.write("\t");
			w.write(String.valueOf(StatUtils.mean(pi_avr_list.toNativeArray())));
			w.write("\t");
			w.write(String.valueOf(StatUtils.mean(pi_guided_list.toNativeArray())));
			w.write("\t");
			w.write(String.valueOf(countPiAvrUsers));
			w.write("\t");
			w.write(String.valueOf(countPiGuidedUsers));
			w.write("\t");
			w.write(String.valueOf(StatUtils.mean(pi_avr_list2.toNativeArray())));
			w.write("\t");
			w.write(String.valueOf(StatUtils.mean(pi_guided_list2.toNativeArray())));
			w.write("\t");
			w.write(String.valueOf(StatUtils.mean(t_ce_list.toNativeArray())));
			w.write("\t");
			w.write(String.valueOf(StatUtils.mean(pi_safe_list_plus.toNativeArray())));
			w.write("\t");
			w.write(String.valueOf(pi_safe_list_plus.size()));
			w.write("\t");
			w.write(String.valueOf(StatUtils.mean(pi_safe_list_minus.toNativeArray())));
			w.write("\t");
			w.write(String.valueOf(pi_safe_list_minus.size()));
			w.write("\t");
			w.write(String.valueOf(StatUtils.mean(pi_safe_list_avr.toNativeArray())));
			w.write("\t");
			w.write(Double.toString(StatUtils.mean(willingness.toNativeArray())));
			w.close();

			Distribution.writeHistogram(avrRiskyGoodTriptimes, event.getControler().getControlerIO().getOutputFilename("riskyGood.txt"));
			Distribution.writeHistogram(avrRiskyBadTriptimes, event.getControler().getControlerIO().getOutputFilename("riskyBad.txt"));
			Distribution.writeHistogram(avrGuidedTriptimes, event.getControler().getControlerIO().getOutputFilename("guided.txt"));
			dumpMap(calcAvrTriptimes(pi_guided_map), event.getControler().getControlerIO().getOutputFilename("pi_guided.txt"));
			dumpMap(calcAvrTriptimes(pi_avr_map), event.getControler().getControlerIO().getOutputFilename("pi_avr.txt"));
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

	private TDoubleDoubleHashMap calcAvrTriptimes(TIntObjectHashMap<TDoubleArrayList> map) {
		TDoubleDoubleHashMap ttmap = new TDoubleDoubleHashMap();
		for(int time : map.keys()) {
			double sum = 0;
			double[] array = map.get(time).toNativeArray();
			for(double tt : array)
				sum += tt;
			ttmap.put(time, sum/array.length);
		}
		return ttmap;
	}

	private void dumpMap(TDoubleDoubleHashMap map, String filename) {
		try {
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		writer.write("time\tvalue");
		writer.newLine();
		double keys[] = map.keys();
		Arrays.sort(keys);
		for(double key : keys) {
			writer.write(String.valueOf(key));
			writer.write("\t");
			writer.write(String.valueOf(map.get(key)));
			writer.newLine();
		}
		writer.close();
		} catch (Exception e) {
		e.printStackTrace();
		}
	}

	public void notifyIterationStarts(IterationStartsEvent event) {
//		try {
//			arrivalTimeWriter = IOUtils.getBufferedWriter(event.getControler().getIterationFilename("arrivaltimes.txt"));
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

	}
}
