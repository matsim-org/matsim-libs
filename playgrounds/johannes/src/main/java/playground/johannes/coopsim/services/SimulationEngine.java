/* *********************************************************************** *
 * project: org.matsim.*
 * SimulationEngine.java
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.sna.util.MultiThreading;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.coopsim.eval.Evaluator;
import playground.johannes.coopsim.mental.choice.ChoiceSelector;
import playground.johannes.coopsim.mental.planmod.Choice2ModAdaptor;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class SimulationEngine {

	public SimulationEngine(ChoiceSelector selector, Choice2ModAdaptor adaptor, Network network, TravelTime travelTimes, EventsManager eventsManager, Evaluator evaluator, Random random, long interval) {
		/*
		 * Initialize mental services
		 */
		StateService stateService = new StateService(selector, adaptor);
		TransitionService transitionService = new TransitionService(stateService, random);
		/*
		 * Initialize pre-processing services
		 */
		AltersRadius1Service alters1Service = new AltersRadius1Service(stateService);
		AltersRadius2Service alters2Service = new AltersRadius2Service(stateService, alters1Service);
		
		AltersScoreService alters1ScoreService = new AltersScoreService(alters1Service);
		AltersScoreService alters2ScoreService = new AltersScoreService(alters2Service);
		
		EgoAlterPlanService planService = new EgoAlterPlanService(stateService, alters1Service, alters2Service);
		/*
		 * Initialize modsim service
		 */
		PseudoMobsimService mobsimService = new PseudoMobsimService(planService, network, travelTimes, eventsManager, MultiThreading.getNumAllowedThreads());
		/*
		 * Initialize evaluation service
		 */
		EvaluationService evaluationService = new EvaluationService(mobsimService, evaluator);
		/*
		 * Initialize post-processing services
		 */
		Alters1ScoreResetService alters1ResetService = new Alters1ScoreResetService(alters1ScoreService, transitionService);
		AltersScoreResetService alters2ResetService = new AltersScoreResetService(alters2ScoreService);
	}
	
	public void run(long iterations, List<SimService<?>> services) {
		for(SimService<?> service : services)
			service.init();
		
		for(long i = 1; 0 <= iterations; i++) {
			for(int k = 0; k < services.size(); k++)
				services.get(k).run();
		}
		
		for(SimService<?> service : services)
			service.terminate();
	}
	
	private class AltersRadius1Service implements SimService<Set<SocialVertex>> {

		private final StateService stateService;

		private Set<SocialVertex> alters;
		
		public AltersRadius1Service(StateService stateService) {
			this.stateService = stateService;
		}
		
		@Override
		public void init() {
		}

		@Override
		public void run() {
			List<SocialVertex> egos = stateService.get();
			
			alters = new HashSet<SocialVertex>(100);
			
			for (SocialVertex ego : egos) {
				for (SocialVertex alter : ego.getNeighbours()) {
					if (!egos.contains(alter)) {
						alters.add(alter);
					}
				}
			}
		}

		@Override
		public Set<SocialVertex> get() {
			return alters;
		}

		@Override
		public void terminate() {
		}
		
	}
	
	private class AltersRadius2Service implements SimService<Set<SocialVertex>> {

		private final StateService stateService;
		
		private final AltersRadius1Service alters1Service;
		
		private Set<SocialVertex> alters;
		
		public AltersRadius2Service(StateService stateService, AltersRadius1Service alters1Service) {
			this.stateService = stateService;
			this.alters1Service = alters1Service;
		}
		
		@Override
		public void init() {
		}

		@Override
		public void run() {
			List<SocialVertex> egos = stateService.get();
			Set<SocialVertex> alters1 = alters1Service.get();
			
			alters = new HashSet<SocialVertex>();
			for (SocialVertex alter : alters1) {
				for (SocialVertex neighbour : alter.getNeighbours()) {
					if (!egos.contains(neighbour) && !alters1.contains(neighbour)) {
						alters.add(neighbour);
					}
				}
			}
		}

		@Override
		public Set<SocialVertex> get() {
			return alters;
		}

		@Override
		public void terminate() {
		}
		
	}

	private class EgoAlterPlanService implements SimService<Collection<Plan>> {

		private final StateService stateService;
		
		private final AltersRadius1Service alters1Service;
		
		private final AltersRadius2Service alters2Service;
		
		private List<Plan> plans;
		
		public EgoAlterPlanService(StateService stateService, AltersRadius1Service alters1Service, AltersRadius2Service alters2Service) {
			this.stateService = stateService;
			this.alters1Service = alters1Service;
			this.alters2Service = alters2Service;
		}

		@Override
		public void init() {
		}

		@Override
		public void run() {
			List<SocialVertex> egos = stateService.get();
			Set<SocialVertex> alters1 = alters1Service.get();
			Set<SocialVertex> alters2 = alters2Service.get();
			
			plans = new ArrayList<Plan>(egos.size() + alters1.size() + alters2.size());
			for (SocialVertex ego : egos)
				plans.add(ego.getPerson().getPerson().getSelectedPlan());

			for (SocialVertex alter : alters1) {
				Plan plan = alter.getPerson().getPerson().getSelectedPlan();
				plans.add(plan);
			}

			for (SocialVertex alter : alters2) {
				Plan plan = alter.getPerson().getPerson().getSelectedPlan();
				plans.add(plan);
			}
			
		}

		@Override
		public Collection<Plan> get() {
			return plans;
		}

		@Override
		public void terminate() {
		}
		
	}
	
	private class AltersScoreService implements SimService<List<Tuple<Plan, Double>>> {

		private final SimService<Set<SocialVertex>> altersService;
		
		private List<Tuple<Plan, Double>> scores;
		
		public AltersScoreService(SimService<Set<SocialVertex>> altersService) {
			this.altersService = altersService;
		}
		
		@Override
		public void init() {
		}

		@Override
		public void run() {
			Set<SocialVertex> alters = altersService.get();
			scores = new ArrayList<Tuple<Plan,Double>>(alters.size());
			for(SocialVertex v : alters) {
				Plan plan = v.getPerson().getPerson().getSelectedPlan();
				scores.add(new Tuple<Plan, Double>(plan, plan.getScore()));
			}
			
		}

		@Override
		public List<Tuple<Plan, Double>> get() {
			return scores;
		}

		@Override
		public void terminate() {
		}
		
	}

	private class AltersScoreResetService implements SimService<Object> {

		private final SimService<List<Tuple<Plan, Double>>> scoreService;
		
		public AltersScoreResetService(SimService<List<Tuple<Plan, Double>>> scoreService) {
			this.scoreService = scoreService;
		}
		
		@Override
		public void init() {
		}

		@Override
		public void run() {
			List<Tuple<Plan, Double>> scores = scoreService.get();
			
			for(int i = 0; i < scores.size(); i++) {
				Tuple<Plan, Double> tuple = scores.get(i);
				tuple.getFirst().setScore(tuple.getSecond());
			}
		}

		@Override
		public Object get() {
			return null;
		}

		@Override
		public void terminate() {
		}
		
	}

	private class Alters1ScoreResetService extends AltersScoreResetService {

		private final TransitionService transitionService;
		
		public Alters1ScoreResetService(SimService<List<Tuple<Plan, Double>>> scoreService, TransitionService transitionService) {
			super(scoreService);
			this.transitionService = transitionService;
		}
		
		public void run() {
			boolean accpet = transitionService.get();
			if(!accpet)
				super.run();
		}
		
	}
}