/* *********************************************************************** *
 * project: org.matsim.*
 * MnlChoice.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.tests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.yu.analysis.PersonPlanMonitor4travelingCarDist;
import playground.yu.utils.DebugTools;
import cadyts.utilities.math.Vector;

/**
 * @author yu
 * 
 */
public class ScoreCalTest implements AgentArrivalEventHandler,
		AgentDepartureEventHandler, AgentStuckEventHandler,
		ActivityStartEventHandler, ActivityEndEventHandler {

	/**
	 * Map<personId,legMonitor>
	 */
	private Map<Id, PersonPlanMonitor4travelingCarDist> planMonitors = new HashMap<Id, PersonPlanMonitor4travelingCarDist>();
	private Map<Id, Map<Plan, Double>> legDurs = new HashMap<Id, Map<Plan, Double>>(),
			actDurs = new HashMap<Id, Map<Plan, Double>>();
	private Population pop;
	private PlanCalcScoreConfigGroup scoring;
	private Network network;

	// private Set<Id> tollLinkIds = new HashSet<Id>();

	public ScoreCalTest(PlanCalcScoreConfigGroup scoringCfg,
			Population pop) {
		this.scoring = scoringCfg;
		this.pop = pop;
	}

	public void reset(int iteration) {
		this.planMonitors.clear();
	}

	public void reset(List<Tuple<Id, Plan>> toRemoves) {
		for (Tuple<Id, Plan> p_pl : toRemoves) {
			Id pId = p_pl.getFirst();
			Plan pl = p_pl.getSecond();
			Map<Plan, Double> legDurMap = this.legDurs.get(pId), actPerformMap = this.actDurs
					.get(pId);
			if (legDurMap == null || actPerformMap == null)
				throw new NullPointerException("BSE:\t\twasn't person\t" + pId
						+ "\tsimulated?????");

			legDurMap.remove(pl);
			actPerformMap.remove(pl);
		}
	}

	public void handleEvent(AgentArrivalEvent event) {
		Id pId = event.getPersonId();

		PersonPlanMonitor4travelingCarDist lm = planMonitors.get(pId);
		if (lm != null) {
			double time = event.getTime();
			lm.setLegArrTime(time, network);
		} else/* lm==null */{
			throw new NullPointerException(
					"BSE:\t\tAn arrival without departure ?????");
		}
	}

	public void handleEvent(AgentDepartureEvent event) {
		Id pId = event.getPersonId();

		// Plan plan = this.pop.getPersons().get(pId).getSelectedPlan();
		double time = event.getTime();

		PersonPlanMonitor4travelingCarDist ppm = planMonitors.get(pId);
		if (ppm == null)
			throw new NullPointerException("BSE:\t"
					+ ScoreCalTest.class.getName() + "\tline\t"
					+ DebugTools.getLineNumber(new Exception())
					+ "\ta leg as the first PlanElement?????");
		ppm.setLegDepTime(time);
	}

	public void handleEvent(AgentStuckEvent event) {
		Id pId = event.getPersonId();

		PersonPlanMonitor4travelingCarDist lm = planMonitors.get(pId);
		if (lm != null) {
			lm.notifyStuck();
		} else/* lm==null */{
			throw new NullPointerException(
					"BSE:\t\tAn stuck arrival hasn't departed ?????");
		}
	}

	/** save Attr values into Maps */
	public void finish() {
		for (Entry<Id, PersonPlanMonitor4travelingCarDist> entry : this.planMonitors.entrySet()) {
			Id pId = entry.getKey();
			PersonPlanMonitor4travelingCarDist ppm = entry.getValue();
			if (ppm == null)
				throw new NullPointerException(
						"BSE:\t\tAn arrival hasn't arrived ?????");

			Person ps = this.pop.getPersons().get(pId);
			Plan plan = ps.getSelectedPlan();
			// legDuration
			Map<Plan, Double> legDurMap = this.legDurs.get(pId);
			if (legDurMap == null) {
				legDurMap = new HashMap<Plan, Double>();
				this.legDurs.put(pId, legDurMap);
			}
			legDurMap.put(plan, ppm.getTotalTravelTimes_h());

			// actDuration
			Map<Plan, Double> actDurMap = this.actDurs.get(pId);
			if (actDurMap == null) {
				actDurMap = new HashMap<Plan, Double>();
				this.actDurs.put(pId, actDurMap);
			}
			actDurMap.put(plan, ppm.getTotalPerformTime_h(this.scoring));
		}
	}

	public void setPersonScore(Person person) {
		Id psId = person.getId();
		Map<Plan, Double> legDurMap = this.legDurs.get(psId), actDurMap = this.actDurs
				.get(psId);
		if (legDurMap == null || actDurMap == null)
			throw new NullPointerException("BSE:\t\twasn't person\t" + psId
					+ "\tsimulated?????");

		for (Entry<Plan, Double> entry : legDurMap.entrySet()) {
			Plan pl = entry.getKey();
			Double legDur = entry.getValue(), actDur = actDurMap.get(pl);
			if (legDur == null || actDur == null)
				throw new NullPointerException(
						"BSE:\t\tvergot to save some attr?");
			// calculate util of the plan
			// double toPay = this.planWithToll(pl) ? 1.0 : 0.0;
			double util = new Vector(-6.0, 6.0).innerProd(new Vector(legDur,
					actDur))
			// * this.scoring.getBrainExpBeta()
			;
			if (Double.isNaN(util))
				throw new RuntimeException(ScoreCalTest.class.getName() + "\t"
						+ DebugTools.getLineNumber(new Exception())
						+ "\tutil/score is a NaN.");

			if (pl.getScore().intValue() != (int) util)
				System.out.println("person\t" + psId + "\tplan\t" + pl
						+ "\tutil\t" + util);
		}
	}

	public void handleEvent(ActivityStartEvent event) {
		Id pId = event.getPersonId();

		Plan plan = this.pop.getPersons().get(pId).getSelectedPlan();
		double time = event.getTime();

		PersonPlanMonitor4travelingCarDist ppm = planMonitors.get(pId);
		if (ppm == null) {
			ppm = new PersonPlanMonitor4travelingCarDist(plan);
			this.planMonitors.put(pId, ppm);
		}
		ppm.setActStartTime(time);
	}

	public void handleEvent(ActivityEndEvent event) {
		Id pId = event.getPersonId();

		Plan plan = this.pop.getPersons().get(pId).getSelectedPlan();
		double time = event.getTime();

		PersonPlanMonitor4travelingCarDist ppm = planMonitors.get(pId);

		String actType = event.getActType();
		if (ppm == null)
			if (actType.startsWith("h")/* h or home */) {
				ppm = new PersonPlanMonitor4travelingCarDist(plan);
				this.planMonitors.put(pId, ppm);
			} else
				throw new NullPointerException("BSE:\t"
						+ ScoreCalTest.class.getName() + "\tline\t"
						+ DebugTools.getLineNumber(new Exception())
						+ "\tan \"un-home\" act without \"start\"?????");
		ppm.setActEndTime(time, this.scoring.getActivityParams(actType));
	}

	// public void setPersonScore(Person person, BasicStatistics travelingStats,
	// BasicStatistics performingStats, BasicStatistics betaTollStats) {
	// Id psId = person.getId();
	// Map<Plan, Double> legDurMap = this.legDurs.get(psId), distMap =
	// this.legDists
	// .get(psId), actDurMap = this.actDurs.get(psId);
	// if (legDurMap == null || distMap == null || actDurMap == null)
	// throw new NullPointerException("BSE:\t\twasn't person\t" + psId
	// + "\tsimulated?????");
	//
	// for (Entry<Plan, Double> entry : legDurMap.entrySet()) {
	// Plan pl = entry.getKey();
	// Double legDur = entry.getValue(), dist = distMap.get(pl), actDur =
	// actDurMap
	// .get(pl);
	// if (legDur == null || dist == null || actDur == null)
	// throw new NullPointerException(
	// "BSE:\t\tvergot to save some attr?");
	//
	// double toPay = this.planWithToll(pl) ? 1.0 : 0.0;
	// // show variablility of attributes
	// travelingStats.add(legDur);
	// performingStats.add(actDur);
	// betaTollStats.add(toPay);
	//
	// // calculate util of the plan
	// double util = this.mnl.getCoeff().innerProd(
	// new Vector(legDur, actDur, toPay))
	// * this.scoring.getBrainExpBeta();
	// if (Double.isNaN(util))
	// throw new RuntimeException(MnlChoiceWithoutToll.class.getName()
	// + "\t" + DebugTools.getLineNumber(new Exception())
	// + "\tutil/score is a NaN.");
	//
	// pl.setScore(util);
	// }
	// }
	public static void main(String[] args) {

		String configFilename = "test/matsim/configDummy.xml", //
		netFilename = "test/network.xml", //
		eventsFilename = "test/matsim/outputChangeExpBeta/ITERS/it.10/10.events.txt.gz", //
		popFilename = "test/matsim/outputChangeExpBeta/ITERS/it.10/10.plans.xml.gz";

		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Config cf = sc.getConfig();
		new MatsimConfigReader(cf).readFile(configFilename);
		PlanCalcScoreConfigGroup scoring = cf.planCalcScore();

		// Network network = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(netFilename);

		Population pop = sc.getPopulation();
		new MatsimPopulationReader(sc).readFile(popFilename);

		ScoreCalTest sct = new ScoreCalTest(scoring, pop);

		EventsManager events = new EventsManagerImpl();
		events.addHandler(sct);
		new EventsReaderTXTv1(events).readFile(eventsFilename);

		sct.finish();
		for (Person ps : pop.getPersons().values())
			sct.setPersonScore(ps);
	}
}
