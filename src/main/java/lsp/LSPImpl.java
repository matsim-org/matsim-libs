/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package lsp;

import lsp.shipment.LSPShipment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/* package-private */class LSPImpl extends LSPDataObject<LSP> implements LSP {
	private static final Logger log = LogManager.getLogger(LSPImpl.class);

	private final Collection<LSPShipment> shipments;
	private final ArrayList<LSPPlan> plans;
	private final LogisticChainScheduler logisticChainScheduler;
	private final Collection<LSPResource> resources;
	private LSPPlan selectedPlan;
	private LSPScorer scorer;
//	private LSPReplanner replanner;


	LSPImpl(LSPUtils.LSPBuilder builder) {
		super(builder.id);
		this.shipments = new ArrayList<>();
		this.plans = new ArrayList<>();
		this.logisticChainScheduler = builder.logisticChainScheduler;
		this.logisticChainScheduler.setEmbeddingContainer(this);
		this.selectedPlan = builder.initialPlan;
		this.selectedPlan.setLSP(this);
		this.plans.add(builder.initialPlan);
		this.resources = builder.resources;
	}

	/**
	 * This is used from {@link LSPControlerListener} and not meant to be used from user code.  Users should bind {@link LSPScorerFactory}.
	 */
	/* package-private */ void setScorer(LSPScorer scorer){
		this.scorer = scorer;
		scorer.setEmbeddingContainer(this);
		this.addSimulationTracker(scorer);
	}

	public static LSPPlan copyPlan(LSPPlan plan2copy) {
		List<LogisticChain> copiedSolutions = new ArrayList<>();
		for (LogisticChain solution : plan2copy.getLogisticChain()) {
			LogisticChain copiedSolution = LSPUtils.LogisticChainBuilder.newInstance(solution.getId()).build();
			copiedSolution.getLogisticChainElements().addAll(solution.getLogisticChainElements());
			copiedSolutions.add(copiedSolution);
		}
		LSPPlan copiedPlan = LSPUtils.createLSPPlan();
		copiedPlan.setAssigner(plan2copy.getAssigner());
		copiedPlan.setLSP(plan2copy.getLSP());
		copiedPlan.setScore( plan2copy.getScore() );
		copiedPlan.getLogisticChain().addAll(copiedSolutions);
		return copiedPlan;
	}

	@Override
	public void scheduleLogisticChains() {
		logisticChainScheduler.scheduleLogisticChain();
	}

	@Override
	public boolean addPlan(LSPPlan plan) {
		for (LogisticChain solution : plan.getLogisticChain()) {
			for (LogisticChainElement element : solution.getLogisticChainElements()) {
				if (!resources.contains(element.getResource())) {
					resources.add(element.getResource());
				}
			}
		}
		plan.setLSP(this);
		return plans.add(plan);
	}

	@Override
	public LSPPlan createCopyOfSelectedPlanAndMakeSelected() {
		LSPPlan newPlan = LSPImpl.copyPlan(this.selectedPlan);
		this.setSelectedPlan(newPlan);
		return newPlan;
	}

	@Override
	public ArrayList<LSPPlan> getPlans() {
		return plans;
	}

	@Override
	public LSPPlan getSelectedPlan() {
		return selectedPlan;
	}

	@Override
	public void setSelectedPlan(LSPPlan selectedPlan) {
		if (!plans.contains(selectedPlan)) {
			plans.add(selectedPlan);
		}
		this.selectedPlan = selectedPlan;

	}

	@Override
	public boolean removePlan(LSPPlan plan) {
		if (plans.contains(plan)) {
			plans.remove(plan);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Collection<LSPResource> getResources() {
		return resources;
	}

	public void scoreSelectedPlan() {
		if (this.scorer != null) {
			this.selectedPlan.setScore(scorer.getScoreForCurrentPlan() );
		} else {
			throw new RuntimeException("trying to score the current LSP plan, but scorer is not set.");
		}
	}


	@Override
	public void assignShipmentToLSP(LSPShipment shipment) {
		shipments.add(shipment);
		selectedPlan.getAssigner().assignToLogisticChain(shipment);
	}

	@Override
	public Collection<LSPShipment> getShipments() {
		return this.shipments;
	}

}
