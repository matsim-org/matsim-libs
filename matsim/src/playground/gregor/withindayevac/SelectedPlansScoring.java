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

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.AgentMoneyEvent;
import org.matsim.core.events.AgentStuckEvent;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.handler.AgentMoneyEventHandler;
import org.matsim.core.events.handler.AgentStuckEventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

public class SelectedPlansScoring implements StartupListener, ScoringListener, IterationStartsListener,
AgentArrivalEventHandler, AgentDepartureEventHandler, AgentStuckEventHandler, AgentMoneyEventHandler, LinkEnterEventHandler{

	
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
				
				final Plan plan = this.population.getPerson(new IdImpl(id)).getSelectedPlan();
				plan.setScore(sf.getScore());
				
			}
			
			final Plan plan = this.population.getPerson(new IdImpl(guideId)).getSelectedPlan();
			final double oldScore = plan.getScoreAsPrimitiveType();
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
		getScoringFunctionForAgent(event.agentId).startLeg(event.getTime(), event.getLeg());
	}

	public void handleEvent(final AgentArrivalEvent event) {
		getScoringFunctionForAgent(event.agentId).endLeg(event.getTime());
	}

	public void handleEvent(final AgentMoneyEvent event) {
		getScoringFunctionForAgent(event.agentId).addMoney(event.getAmount());
	}

	public void handleEvent(final AgentStuckEvent event) {
		getScoringFunctionForAgent(event.agentId).agentStuck(event.getTime());
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
		if (this.scoreSum == 0) return Plan.UNDEF_SCORE;
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
		if (sf == null) return Plan.UNDEF_SCORE;
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
			sf = this.sfFactory.getNewScoringFunction(this.population.getPerson(new IdImpl(agentId)).getSelectedPlan());
			this.agentScorers.put(agentId, sf);
		}
		return sf;
	}
}
