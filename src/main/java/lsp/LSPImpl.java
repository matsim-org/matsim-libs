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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.controler.events.ReplanningEvent;

import lsp.replanning.LSPReplanner;
import lsp.shipment.LSPShipment;

/* package-private */class LSPImpl extends LSPDataObject<LSP> implements LSP {
	private static final Logger log = Logger.getLogger( LSPImpl.class );

	private final Collection<LSPShipment> shipments;
	private final ArrayList<LSPPlan> plans;
	private final SolutionScheduler solutionScheduler;
	private LSPPlan selectedPlan;
	private final Collection<LSPResource> resources;
	private LSPScorer scorer;
	private LSPReplanner replanner;


	LSPImpl( LSPUtils.LSPBuilder builder ){
		super( builder.id );
		this.shipments = new ArrayList<>();
		this.plans= new ArrayList<>();
		this.solutionScheduler = builder.solutionScheduler;
		this.solutionScheduler.setEmbeddingContainer(this );
		this.selectedPlan=builder.initialPlan;
		this.selectedPlan.setLSP(this );
		this.plans.add(builder.initialPlan);
		this.resources = builder.resources;
		this.scorer = builder.scorer;
		if(this.scorer != null) {
			this.scorer.setEmbeddingContainer(this );
			this.addSimulationTracker( this.scorer );
		}
		this.replanner = builder.replanner;
		if(this.replanner != null) {
			this.replanner.setEmbeddingContainer(this );
		}	
	}
	
	@Override
	public void scheduleSolutions() {
		solutionScheduler.scheduleSolutions();
	}


	@Override
	public boolean addPlan(LSPPlan plan) {
		for(LogisticsSolution solution : plan.getSolutions()) {
			for(LogisticsSolutionElement element : solution.getSolutionElements()) {
				if(!resources.contains(element.getResource())) {
					resources.add(element.getResource());
				}
			}
		}
		return plans.add(plan);
	}


	@Override
	public LSPPlan createCopyOfSelectedPlanAndMakeSelected() {
		LSPPlan newPlan = LSPImpl.copyPlan(this.selectedPlan) ;
		this.setSelectedPlan( newPlan ) ;
		return newPlan ;
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
	public boolean removePlan(LSPPlan plan) {
		if(plans.contains(plan)) {
			plans.remove(plan);
			return true;
		}
		else {
			return false;
		}
	}


	@Override
	public void setSelectedPlan(LSPPlan selectedPlan) {
		if(!plans.contains(selectedPlan)) {
			plans.add(selectedPlan);
		}
		this.selectedPlan = selectedPlan;
		
	}

	public static LSPPlan copyPlan(LSPPlan plan2copy) {
		List<LogisticsSolution> copiedSolutions = new ArrayList<>();
		for (LogisticsSolution solution : plan2copy.getSolutions()) {
				LogisticsSolution copiedSolution = LSPUtils.LogisticsSolutionBuilder.newInstance(solution.getId() ).build();
				copiedSolution.getSolutionElements().addAll(solution.getSolutionElements());		
				copiedSolutions.add(copiedSolution);
		}
		LSPPlan copiedPlan = LSPUtils.createLSPPlan();
		copiedPlan.setAssigner(plan2copy.getAssigner());
		copiedPlan.setLSP(plan2copy.getLSP() );
		double initialScoreOfCopiedPlan = plan2copy.getScore();
		copiedPlan.setScore(initialScoreOfCopiedPlan);
		copiedPlan.getSolutions().addAll(copiedSolutions);
		return copiedPlan;
	}


	@Override
	public Collection<LSPResource> getResources() {
		return resources;
	}

	public void scoreSelectedPlan() {
		if(this.scorer != null) {
			this.selectedPlan.setScore( scorer.computeScoreForCurrentPlan() );
		} else {
			log.fatal("trying to score the current LSP plan, but scorer is not set.");
		}
	}


	@Override
	public void assignShipmentToLSP(LSPShipment shipment) {
		shipments.add(shipment);
		selectedPlan.getAssigner().assignToSolution(shipment);
	}
	
	public void replan( final ReplanningEvent arg0 ) {
		if ( this.replanner!=null ) {
			this.replanner.replan( arg0 );
		}
	}

	@Override
	public void setReplanner(LSPReplanner replanner) {
		this.replanner = replanner;
	}


	
	@Override
	public Collection<LSPShipment> getShipments() {
		return this.shipments ;
	}

}
