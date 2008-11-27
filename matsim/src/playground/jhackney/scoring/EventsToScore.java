/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToScore.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package playground.jhackney.scoring;

import java.util.Map;
import java.util.TreeMap;

import net.opengis.kml._2.AbstractFeatureType;
import net.opengis.kml._2.PlacemarkType;

import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicPlanImpl.ActLegIterator;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentMoneyEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentMoneyEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;

/**
 * Calculates continuously the score of the selected plans of a given population
 * based on events.<br>
 * Departure- and Arrival-Events *must* be provided to calculate the score,
 * AgentStuck-Events are used if available to add a penalty to the score.
 * The final score are written to the selected plans of each person in the
 * population.
 *
 * @author mrieser
 */
public class EventsToScore implements AgentArrivalEventHandler, AgentDepartureEventHandler, AgentStuckEventHandler, AgentMoneyEventHandler {

	private Population population = null;
	private playground.jhackney.scoring.SocScoringFactoryEvent sfFactory = null;
	private final TreeMap<String, playground.jhackney.scoring.SocScoringFunctionEvent> agentScorers = new TreeMap<String, playground.jhackney.scoring.SocScoringFunctionEvent>();
	private double scoreSum = 0.0;
	private long scoreCount = 0;
	private final double learningRate;

	public EventsToScore(final Population population, final playground.jhackney.scoring.SocScoringFactoryEvent factory) {
		this(population, factory, Gbl.getConfig().charyparNagelScoring().getLearningRate());
	}

	public EventsToScore(final Population population, final playground.jhackney.scoring.SocScoringFactoryEvent factory, final double learningRate) {
		super();
		this.population = population;
		this.sfFactory = factory;
		this.learningRate = learningRate;
	}

	public void handleEvent(final AgentDepartureEvent event) {
		playground.jhackney.scoring.SocScoringFunctionEvent sf = getScoringFunctionForAgent(event.agentId);
		sf.startLeg(event.time, event.leg);
	}

	public void handleEvent(final AgentArrivalEvent event) {
		playground.jhackney.scoring.SocScoringFunctionEvent sf = getScoringFunctionForAgent(event.agentId);
		sf.endLeg(event.time);
	}

	public void handleEvent(final AgentStuckEvent event) {
		playground.jhackney.scoring.SocScoringFunctionEvent sf = getScoringFunctionForAgent(event.agentId);
		sf.agentStuck(event.time);
	}

	public void handleEvent(final AgentMoneyEvent event) {
		playground.jhackney.scoring.SocScoringFunctionEvent sf = getScoringFunctionForAgent(event.agentId);
		sf.addMoney(event.amount);
	}

	/**
	 * Finishes the calculation of the plans' scores and assigns the new scores
	 * to the plans.
	 */
	public void finish() {

		for (Map.Entry<String, playground.jhackney.scoring.SocScoringFunctionEvent> entry : this.agentScorers.entrySet()) {
			String agentId = entry.getKey();
			playground.jhackney.scoring.SocScoringFunctionEvent sf = entry.getValue();
			sf.finish();

			double score = sf.getScore();
			Plan plan = this.population.getPerson(agentId).getSelectedPlan();
			double oldScore = plan.getScore();
			if (Double.isNaN(oldScore)) {
				plan.setScore(score);
			} else {
				plan.setScore(this.learningRate * score + (1-this.learningRate) * oldScore);
			}

			this.scoreSum += score;
			this.scoreCount++;
		}
		
		System.out.println("agentId\tUlegt\tactNumber\tactType\tUdur\tUed\tUla\tUld\tUs\tUw\tUsoc");
//		for (Map.Entry<String, playground.jhackney.scoring.SocScoringFunctionEvent> entry : this.agentScorers.entrySet()) {
//			String agentId = entry.getKey();
//			playground.jhackney.scoring.SocScoringFunctionEvent sf = entry.getValue();
//			Plan plan = this.population.getPerson(agentId).getSelectedPlan();
//			ActLegIterator actLegIter = plan.getIterator();
//			Act act = (Act) actLegIter.nextAct();
//
//
//			int actNumber=0;
//			int legNumber=-1;
//
//			while(actLegIter.hasNextLeg()){//alternates Act-Leg-Act-Leg and ends with Act
//
//				Leg leg = (Leg) actLegIter.nextLeg();
//				legNumber++;
//
//				System.out.print(agentId+"\t"+sf.getUlegt(leg));
//
//				act = (Act) actLegIter.nextAct();
//				actNumber++;
//				System.out.println("\t"+actNumber+"\t\""+act.getType()+"\"\t"+sf.getUdur(act)+"\t"+sf.getUed(act)+"\t"+sf.getUla(act)+"\t"+sf.getUld(act)+"\t"+sf.getUs(act)+"\t"+sf.getUw(act)+"\t"+sf.getUsoc(act));
//			}
//		}
	}

	/**
	 * Returns the actual average plans' score before it was assigned to the plan
	 * and possibility mixed with old scores (learningrate).
	 *
	 * @return the average score of the plans before mixing with the old scores (learningrate)
	 */
	public double getAveragePlanPerformance() {
		if (this.scoreSum == 0) return BasicPlan.UNDEF_SCORE;
		return (this.scoreSum / this.scoreCount);
	}

	/**
	 * Returns the score of a single agent. This method only returns useful values
	 * if the method {@link #finish() } was called before.
	 * description
	 *
	 * @param agentId The id of the agent the score is requested for.
	 * @return The score of the specified agent.
	 */
	public double getAgentScore(final Id agentId) {
		ScoringFunction sf = this.agentScorers.get(agentId.toString());
		if (sf == null) return BasicPlan.UNDEF_SCORE;
		return sf.getScore();
	}

	public void reset(final int iteration) {
		this.agentScorers.clear();
		this.scoreCount = 0;
		this.scoreSum = 0.0;
	}

	/**
	 * Returns the scoring function for the specified agent. If the agent already
	 * has a scoring function, that one is returned. If the agent does not yet
	 * have a scoring function, a new one is created and assigned to the agent
	 * and returned.
	 *
	 * @param agentId The id of the agent the scoring function is requested for.
	 * @return The scoring function for the specified agent.
	 */
	private playground.jhackney.scoring.SocScoringFunctionEvent getScoringFunctionForAgent(final String agentId) {
		playground.jhackney.scoring.SocScoringFunctionEvent sf = this.agentScorers.get(agentId);
		if (sf == null) {
			sf = this.sfFactory.getNewScoringFunction(this.population.getPerson(agentId).getSelectedPlan());
			this.agentScorers.put(agentId, sf);
		}
		return sf;
	}

}
