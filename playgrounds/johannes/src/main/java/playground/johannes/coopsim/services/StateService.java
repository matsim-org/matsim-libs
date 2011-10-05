/* *********************************************************************** *
 * project: org.matsim.*
 * StateService.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Plan;

import playground.johannes.coopsim.mental.choice.ActivityGroupSelector;
import playground.johannes.coopsim.mental.choice.ChoiceSelector;
import playground.johannes.coopsim.mental.planmod.Choice2ModAdaptor;
import playground.johannes.coopsim.mental.planmod.PlanModEngine;
import playground.johannes.coopsim.mental.planmod.SingleThreadedModEngine;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class StateService implements SimService<List<SocialVertex>> {

	private final ChoiceSelector choiceSelector;
	
	private final PlanModEngine modEngine;
	
	private List<SocialVertex> egos;
	
	public StateService(ChoiceSelector choiceSelector, Choice2ModAdaptor adaptor) {
		this.choiceSelector = choiceSelector;
		this.modEngine = new SingleThreadedModEngine(adaptor);
	}
	
	public StateService(ChoiceSelector choiceSelector, PlanModEngine modEngine) {
		this.choiceSelector = choiceSelector;
		this.modEngine = modEngine;
	}
	
	@Override
	public void init() {
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		/*
		 * make choices
		 */
		Map<String, Object> choices = new HashMap<String, Object>();
		choices = choiceSelector.select(choices);
		/*
		 * get plans do modify
		 */
		egos = (List<SocialVertex>) choices.get(ActivityGroupSelector.KEY);
		List<Plan> plans = new ArrayList<Plan>(egos.size());
		for(SocialVertex v : egos) {
			Plan plan = v.getPerson().getPerson().copySelectedPlan();
			if(plan == null)
				throw new NullPointerException("Outch! This person appears to have no selected plan!");
			plans.add(plan);
		}
		/*
		 * apply modifications
		 */
		modEngine.run(plans, choices);
	}

	@Override
	public List<SocialVertex> get() {
		return egos;
	}

	@Override
	public void terminate() {
		/*
		 * terminate mod engine;
		 */
		
	}

}
