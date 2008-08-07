/* *********************************************************************** *
 * project: org.matsim.*
 * TRBAnalysis.java
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

package playground.marcel;

import java.util.HashMap;

import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.gbl.Gbl;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.utils.misc.Time;

public class TRBAnalysis implements EventHandlerAgentDepartureI, EventHandlerAgentArrivalI {

	final private Config config;
	final private Population population;
	final private double[][] sumTravelTimes = new double[3][12 * 30 + 1];
	final private int[][] cntTravelTimes = new int[sumTravelTimes.length][sumTravelTimes[0].length];
	final private HashMap<String, Double> agentDepartures = new HashMap<String, Double>();

	public TRBAnalysis(final String[] args) {
		this.config = Gbl.createConfig(args);
		final ScenarioData data = new ScenarioData(config);
		this.population = data.getPopulation();
	}

	public void run() {
		Events events = new Events();
		events.addHandler(this);
		// collect data from events
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		// analyze population
		double[] allTravelTimes = new double[this.sumTravelTimes[0].length];
		int[] allCounts = new int[allTravelTimes.length];
		for (Person person : this.population) {
			Plan ptPlan = getPtPlan(person);
			if (ptPlan != null) {
				Plan selectedPlan = person.getSelectedPlan();
				for (int i = 1; i < ptPlan.getActsLegs().size(); i += 2) {
					double travTime = ((Leg) ptPlan.getActsLegs().get(i)).getTravTime();
					double depTime = ((Leg) selectedPlan.getActsLegs().get(i)).getDepTime();
					int slot = getTimeSlot(depTime);
					allTravelTimes[slot] += travTime;
					allCounts[slot]++;
				}
			}
		}
		// output results
		System.out.println("time \t sumCAR \t cntCAR \t avgCAR \t sumPT \t cntPT \t avgPT \t sumAllPt \t cntAllPt \t avgAllPt");
		for (int i = 0; i < cntTravelTimes[0].length; i++) {
			double avgCAR = (this.cntTravelTimes[0][i] == 0) ? 0 : (this.sumTravelTimes[0][i] / this.cntTravelTimes[0][i]);
			double avgPT = (this.cntTravelTimes[1][i] == 0) ? 0 : (this.sumTravelTimes[1][i] / this.cntTravelTimes[1][i]);
			double allAvg = (allCounts[i] == 0) ? 0 : (allTravelTimes[i] / allCounts[i]);

			System.out.println(Time.writeTime(i * 300)
					+ "\t" + this.sumTravelTimes[0][i] + "\t" + this.cntTravelTimes[0][i] + "\t" + avgCAR
					+ "\t" + this.sumTravelTimes[1][i] + "\t" + this.cntTravelTimes[1][i] + "\t" + avgPT
					+ "\t" + allTravelTimes[i] + "\t" + allCounts[i] + "\t" + allAvg);
		}
	}

	public void handleEvent(AgentDepartureEvent event) {
		this.agentDepartures.put(event.agentId, Double.valueOf(event.time));
	}

	public void reset(int iteration) {
		// nothing to do
	}

	public void handleEvent(AgentArrivalEvent event) {
		double depTime = this.agentDepartures.remove(event.agentId).doubleValue();
		double travTime = event.time - depTime;
		int slot = getTimeSlot(depTime);
		final Person person = this.population.getPerson(new IdImpl(event.agentId));
		int modeSlot = getModeSlot(person);
//		System.out.println(modeSlot + " -- " + slot);
		this.sumTravelTimes[modeSlot][slot] += travTime;
		this.cntTravelTimes[modeSlot][slot]++;
	}

	private int getTimeSlot(final double time) {
		// 5 min slots
		return Math.min(12*30, (int) (time / 300.0));
	}

	private int getModeSlot(final Person person) {
		final BasicPlan.Type type = person.getSelectedPlan().getType();
		if (type == null || BasicPlan.Type.UNDEFINED.equals(type)) {
			return 2; // through traffic
		}
		if (BasicPlan.Type.PT.equals(type)) {
			return 1;
		}
		if (BasicPlan.Type.CAR.equals(type)) {
			return 0;
		}
		System.out.println(type);
		return -1; // will trigger an outofboundsexception
	}

	private Plan getPtPlan(final Person person) {
		for (Plan plan : person.getPlans()) {
			if (BasicPlan.Type.PT.equals(plan.getType())) {
				return plan;
			}
		}
		return null;
	}

	public static void main(String[] args) {
		new TRBAnalysis(args).run();
	}

}
