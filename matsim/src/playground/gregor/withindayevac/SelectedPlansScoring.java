/* *********************************************************************** *
 * project: org.matsim.*
 * SelectedPlansScoring.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.ScoringEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.controler.listener.ScoringListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.AgentUtilityEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.events.handler.AgentUtilityEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;

public class SelectedPlansScoring implements StartupListener, ScoringListener, IterationStartsListener,
AgentArrivalEventHandler, AgentDepartureEventHandler, AgentStuckEventHandler, AgentUtilityEventHandler, LinkEnterEventHandler{

	
	protected Population population;
	private ScoringFunctionFactory sfFactory;
	protected final double learningRate;
	private final NetworkLayer network;
	HashMap<Id,ArrayList<String>> mappings = new HashMap<Id, ArrayList<String>>();
	protected final TreeMap<String, ScoringFunction> agentScorers = new TreeMap<String, ScoringFunction>();
	private double scoreSum = 0.0;
	private long scoreCount = 0;
	private double stuckPenalty;
	
	public SelectedPlansScoring() {
		this.network = (NetworkLayer) Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
		this.learningRate = Gbl.getConfig().charyparNagelScoring().getLearningRate();	
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
		this.mappings.clear();
		reset(event.getIteration());
		this.stuckPenalty = 0;
	}


	public void finish() {
		
	
		for (final Map.Entry<String, ScoringFunction> entry : this.agentScorers.entrySet()) {
			final ScoringFunction sf = entry.getValue();
			sf.finish();
			
			this.scoreSum += sf.getScore();
			this.scoreCount++;
		}
		

		
		for (Map.Entry<Id, ArrayList<String>> entry : this.mappings.entrySet()) {
			ArrayList<String> list = entry.getValue();
			String guideId = null;
			int count = 0;
			double scoreSum = this.stuckPenalty;
			for (String id : list) {
				if (guideId == null && id.contains("guide")){
					guideId = id;
					continue;
				}
				count++;
				ScoringFunction sf = this.agentScorers.get(id);
				
				scoreSum += sf.getScore();
				
				final Plan plan = this.population.getPerson(id).getSelectedPlan();
				plan.setScore(sf.getScore());
				
			}
			
			final Plan plan = this.population.getPerson(guideId).getSelectedPlan();
			final double oldScore = plan.getScore();
//			if (Double.isNaN(oldScore)) {
//				plan.setScore(scoreSum);
//			} else {
//				plan.setScore(this.learningRate * scoreSum + (1-this.learningRate) * oldScore);
//			}
			if (Double.isNaN(oldScore)) {
				plan.setScore(this.scoreSum + scoreSum);
			} else {
				plan.setScore(this.learningRate * (this.scoreSum + scoreSum) + (1-this.learningRate) * oldScore);
			}
		}
	}


	public void handleEvent(final AgentDepartureEvent event) {
		final ScoringFunction sf = getScoringFunctionForAgent(event.agentId);
		sf.startLeg(event.time, event.leg);
	}

	public void handleEvent(final AgentArrivalEvent event) {
		final ScoringFunction sf = getScoringFunctionForAgent(event.agentId);
		sf.endLeg(event.time);
	}

	public void handleEvent(final AgentUtilityEvent event) {
		final ScoringFunction sf = getScoringFunctionForAgent(event.agentId);
		sf.addUtility(event.amount);
	}

	public void handleEvent(final AgentStuckEvent event) {
		final ScoringFunction sf = getScoringFunctionForAgent(event.agentId);
		sf.agentStuck(event.time);
		this.stuckPenalty += -144.0;
	}
	

	public void handleEvent(final LinkEnterEvent event) {
		final String linkId = event.linkId;
		Link link = this.network.getLink(linkId);
		Node node = link.getFromNode();
		ArrayList<String> list = this.mappings.get(node.getId());
		if (list == null) {
			list = new ArrayList<String>();
			this.mappings.put(node.getId(), list);
		}
		String agentId = event.agentId;
		list.add(agentId);
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
}
