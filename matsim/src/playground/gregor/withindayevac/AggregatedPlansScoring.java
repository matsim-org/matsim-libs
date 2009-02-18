/* *********************************************************************** *
 * project: org.matsim.*
 * AggregatedPlansScoring.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.withindayevac;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.ScoringEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.controler.listener.ScoringListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentMoneyEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentMoneyEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicPlan;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;

public class AggregatedPlansScoring implements StartupListener, ScoringListener, IterationStartsListener,
AgentArrivalEventHandler, AgentDepartureEventHandler, AgentStuckEventHandler, AgentMoneyEventHandler, LinkEnterEventHandler {


	protected Population population;
	private ScoringFunctionFactory sfFactory;
	private final HashMap<String,String> egressMappings = new HashMap<String,String>();
	protected final double learningRate;
	protected final TreeMap<String, ScoringFunction> agentScorers = new TreeMap<String, ScoringFunction>();
	private final TreeMap<String, String> agentMappings = new TreeMap<String, String>();
	private final TreeMap<String, GroupScore> gs = new TreeMap<String, GroupScore>();
	private double scoreSum = 0.0;
	private long scoreCount = 0;

	public AggregatedPlansScoring() {
		this.learningRate = Gbl.getConfig().charyparNagelScoring().getLearningRate();	
		initEgressMappings();
	}


	public void notifyStartup(final StartupEvent event) {
		final Controler controler = event.getControler();
		this.sfFactory = controler.getScoringFunctionFactory();
		this.population = controler.getPopulation();
		event.getControler().getEvents().addHandler(this);
	}

	public void notifyScoring(final ScoringEvent event) {
		finish();
	}

	public void notifyIterationStarts(final IterationStartsEvent event) {
		this.agentMappings.clear();
		this.gs.clear();
		reset(event.getIteration());
	}

	/**
	 * Finishes the calculation of the plans' scores and assigns the new scores
	 * to the plans.
	 */
	public void finish() {
		
		for (final Map.Entry<String, ScoringFunction> entry : this.agentScorers.entrySet()) {
			final String agentId = entry.getKey();
			final ScoringFunction sf = entry.getValue();
			sf.finish();
			final double score = sf.getScore();
			
			final String mapping = this.agentMappings.get(agentId);
			GroupScore g = this.gs.get(mapping);
			if (g == null) {
				g = new GroupScore();
				this.gs.put(mapping, g);
			}
			g.nagents++;
			g.scoreSum += score;
			this.scoreSum += score;
			this.scoreCount++;
		}
			
		for (final Map.Entry<String, ScoringFunction> entry : this.agentScorers.entrySet()) {			
			final String agentId = entry.getKey();
			final String mapping = this.agentMappings.get(agentId);
			
			final GroupScore g = this.gs.get(mapping);
			final GroupScore nullG = this.gs.get("stuckAndAbord"); 
			final double score = (g.scoreSum + nullG.scoreSum ) / (g.nagents + nullG.nagents);
			final Plan plan = this.population.getPerson(agentId).getSelectedPlan();
			final double oldScore = plan.getScore();
			if (Double.isNaN(oldScore)) {
				plan.setScore(score);
			} else {
				plan.setScore(this.learningRate * score + (1-this.learningRate) * oldScore);
			}
		}
	}



	private void initEgressMappings() {
		this.egressMappings.put("el63", "el63");
		this.egressMappings.put("el14", "el63");
		this.egressMappings.put("el87", "el63");
		this.egressMappings.put("el13", "el63");
		this.egressMappings.put("el61", "el63");

		this.egressMappings.put("el48", "el48");
		this.egressMappings.put("el30", "el48");
		this.egressMappings.put("el48", "el48");
		this.egressMappings.put("el3", "el48");
		this.egressMappings.put("el38", "el48");
		this.egressMappings.put("el6", "el48");
		this.egressMappings.put("el10", "el48");
		this.egressMappings.put("el71", "el48");
		this.egressMappings.put("el69", "el48");

		this.egressMappings.put("el33", "el33");
		this.egressMappings.put("el24", "el33");

		this.egressMappings.put("el65", "el65");
		this.egressMappings.put("el77", "el65");
		this.egressMappings.put("el26", "el65");
		this.egressMappings.put("el64", "el65");

		this.egressMappings.put("el21", "el21");
		this.egressMappings.put("el58", "el21");

		this.egressMappings.put("el89", "el89");
		this.egressMappings.put("el12", "el89");

		this.egressMappings.put("el18", "el18");
		this.egressMappings.put("el17", "el18");
		this.egressMappings.put("el47", "el18");
		this.egressMappings.put("el86", "el18");

		this.egressMappings.put("el2", "el2");
		this.egressMappings.put("el39", "el2");

		this.egressMappings.put("el76", "el76");
		this.egressMappings.put("el4", "el76");
		this.egressMappings.put("el11", "el76");
		this.egressMappings.put("el5", "el76");
		this.egressMappings.put("el7", "el76");
		this.egressMappings.put("el59", "el76");
		this.egressMappings.put("el36", "el76");
		this.egressMappings.put("el80", "el76");

		this.egressMappings.put("el29", "el8");

		this.egressMappings.put("el22", "el88");
		this.egressMappings.put("el43", "el88");

		this.egressMappings.put("el45", "el50");
		this.egressMappings.put("el19", "el50");
		this.egressMappings.put("el84", "el84");
		this.egressMappings.put("el75", "el84");
		this.egressMappings.put("el53", "el84");



	}



	public void handleEvent(final AgentDepartureEvent event) {
		getScoringFunctionForAgent(event.agentId).startLeg(event.time, event.leg);
	}

	public void handleEvent(final AgentArrivalEvent event) {
		getScoringFunctionForAgent(event.agentId).endLeg(event.time);
	}

	public void handleEvent(final AgentStuckEvent event) {
		getScoringFunctionForAgent(event.agentId).agentStuck(event.time);
		this.agentMappings.put(event.agentId, "stuckAndAbord");
	}

	public void handleEvent(final AgentMoneyEvent event) {
		getScoringFunctionForAgent(event.agentId).addMoney(event.amount);
	}



	public void handleEvent(final LinkEnterEvent event) {
		final String linkId = event.linkId;
		if (linkId.contains("el")) {
			String mapping = this.egressMappings.get(linkId);
			if (mapping == null) {
				mapping = linkId;
			}
			this.agentMappings.put(event.agentId, mapping);
		}
		
		
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
		final ScoringFunction sf = this.agentScorers.get(agentId.toString());
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
	protected ScoringFunction getScoringFunctionForAgent(final String agentId) {
		ScoringFunction sf = this.agentScorers.get(agentId);
		if (sf == null) {
			sf = this.sfFactory.getNewScoringFunction(this.population.getPerson(agentId).getSelectedPlan());
			this.agentScorers.put(agentId, sf);
		}
		return sf;
	}
	
	private static class GroupScore {
		int nagents = 0;
		double scoreSum = 0;
	}
}
