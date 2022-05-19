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

import lsp.replanning.LSPReplanner;
import lsp.scoring.LSPScorer;
import lsp.controler.LSPSimulationTracker;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.events.handler.EventHandler;

import java.util.ArrayList;
import java.util.Collection;

public class LSPUtils{
	public static LSPPlan createLSPPlan(){
		return new LSPPlanImpl();
	}
	public static SolutionScheduler createForwardSolutionScheduler(){
		return new ForwardSolutionSchedulerImpl();
	}
	public static WaitingShipments createWaitingShipments(){
		return new WaitingShipmentsImpl();
	}
	private LSPUtils(){} // do not instantiate
	public static class LSPBuilder{
		Id<LSP> id;
		SolutionScheduler solutionScheduler;
		LSPPlan initialPlan;
		final Collection<LSPResource> resources;
		LSPScorer scorer;
		LSPReplanner replanner;


		public static LSPBuilder getInstance(Id<LSP> id){
			return new LSPBuilder(id);
		}

		private LSPBuilder(Id<LSP> id){
			this.resources = new ArrayList<>();

		}

		public LSPBuilder setSolutionScheduler( SolutionScheduler solutionScheduler ){
			this.solutionScheduler = solutionScheduler;
			return this;
		}

		public LSPBuilder setSolutionScorer( LSPScorer scorer ){
			this.scorer = scorer;
			return this;
		}

		public LSPBuilder setReplanner( LSPReplanner replanner ){
			this.replanner= replanner;
			return this;
		}


		public LSPBuilder setInitialPlan( LSPPlan plan ){
			this.initialPlan = plan;
			for(LogisticsSolution solution : plan.getSolutions()) {
				for(LogisticsSolutionElement element : solution.getSolutionElements()) {
					if(!resources.contains(element.getResource())) {
						resources.add(element.getResource());
					}
				}
			}
			return this;
		}


		public LSP build(){
			return new LSPImpl(this);
		}
	}

	public static class LogisticsSolutionBuilder{
		final Id<LogisticsSolution> id;
		final Collection<LogisticsSolutionElement> elements;
		final Collection<LSPInfo> solutionInfos;
		final Collection<EventHandler> eventHandlers;
		final Collection<LSPSimulationTracker>trackers;

		public static LogisticsSolutionBuilder newInstance( Id<LogisticsSolution>id ){
			return new LogisticsSolutionBuilder(id);
		}

		private LogisticsSolutionBuilder( Id<LogisticsSolution> id ){
			this.elements = new ArrayList<>();
			this.solutionInfos = new ArrayList<>();
			this.eventHandlers = new ArrayList<>();
			this.trackers = new ArrayList<>();
			this.id = id;
		}

		public LogisticsSolutionBuilder addSolutionElement( LogisticsSolutionElement element ){
			elements.add(element);
			return this;
		}

		public LogisticsSolutionBuilder addInfo( LSPInfo info ) {
			solutionInfos.add(info);
			return this;
		}

		public LogisticsSolutionBuilder addEventHandler( EventHandler handler ) {
			eventHandlers.add(handler);
			return this;
		}

		public LogisticsSolutionBuilder addTracker( LSPSimulationTracker tracker ) {
			trackers.add(tracker);
			return this;
		}

		public LogisticsSolution build(){
			return new LogisticsSolutionImpl(this);
		}
	}

	public static class LogisticsSolutionElementBuilder{
		final Id<LogisticsSolutionElement>id;
		LSPResource resource;
		final WaitingShipments incomingShipments;
		final WaitingShipments outgoingShipments;

		public static LogisticsSolutionElementBuilder newInstance( Id<LogisticsSolutionElement>id ){
			return new LogisticsSolutionElementBuilder(id);
		}

		private LogisticsSolutionElementBuilder( Id<LogisticsSolutionElement>id ){
			this.id = id;
			this.incomingShipments = createWaitingShipments();
			this.outgoingShipments = createWaitingShipments();
		}


		public LogisticsSolutionElementBuilder setResource( LSPResource resource ){
			this.resource = resource;
			return this;
		}

		public LogisticsSolutionElement build(){
			return new LogisticsSolutionElementImpl(this);
		}
	}
	private static final String lspsString = "lsps";
	public static void addLSPs( Scenario scenario, LSPs lsps ) {
		scenario.addScenarioElement( lspsString, lsps );
	}
	public static LSPs getLSPs( Scenario scenario ) {
		Object result = scenario.getScenarioElement( lspsString );
		if ( result==null ) {
			throw new RuntimeException( "there is no scenario element of type " + lspsString +
								    ".  You will need something like LSPUtils.addLSPs( scenario, lsps) somewhere." );
		}
		return (LSPs) result;
	}
//	The following would be closer to how we have done it elsewhere (scenario containers are mutable).  kai, may'22'
//	public static LSPs createOrGetLPSs( Scenario scenario ){
//		Object result = scenario.getScenarioElement( lspsString );
//		LSPs lsps;
//		if ( result != null ) {
//			lsps = (LSPs) result;
//		} else {
//			lsps = new LSPs(  );
//			scenario.addScenarioElement( lspsString, lsps );
//		}
//		return lsps;
//	}
}
