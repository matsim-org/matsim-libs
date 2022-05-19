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

package lsp.controler;


import lsp.*;
import lsp.LSPInfo;
import lsp.replanning.LSPReplanningModule;
import lsp.LSPCarrierResource;
import lsp.scoring.LSPScoringModule;
import lsp.shipment.LSPShipment;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierAgentTracker;
import org.matsim.contrib.freight.events.eventsCreator.LSPEventCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.events.handler.EventHandler;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;


class LSPControlerListenerImpl implements BeforeMobsimListener, AfterMobsimListener, ScoringListener,
ReplanningListener, IterationStartsListener{

	
	private CarrierAgentTracker carrierResourceTracker;
	private final Carriers carriers;
	private final LSPs lsps;
	private final LSPReplanningModule replanningModule;
	private final LSPScoringModule scoringModule;
	private final Collection<LSPEventCreator> creators;
	
	private ArrayList <EventHandler> registeredHandlers;

	@Inject EventsManager eventsManager;

	@Inject LSPControlerListenerImpl( Scenario scenario, LSPReplanningModule replanningModule, LSPScoringModule scoringModule, Collection<LSPEventCreator> creators ) {
	        this.lsps = LSPUtils.getLSPs( scenario );
	        this.replanningModule = replanningModule;
	        this.scoringModule = scoringModule;
	        this.creators = creators;
	        this.carriers = getCarriers();
	}
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		
		LSPRescheduler rescheduler = new LSPRescheduler(lsps);
		rescheduler.notifyBeforeMobsim(event);
		
		carrierResourceTracker = new CarrierAgentTracker(carriers, creators, eventsManager );
		eventsManager.addHandler(carrierResourceTracker);
		registeredHandlers = new ArrayList<>();
		
		for(LSP lsp : lsps.getLSPs().values()) {
			for(LSPShipment shipment : lsp.getShipments()) {
				for(EventHandler handler : shipment.getEventHandlers()) {
					eventsManager.addHandler(handler);
				}
			}
			LSPPlan selectedPlan = lsp.getSelectedPlan();
				for(LogisticsSolution solution : selectedPlan.getSolutions()) {
					for(EventHandler handler : solution.getEventHandlers()) {
						eventsManager.addHandler(handler);
					}
					for(LogisticsSolutionElement element : solution.getSolutionElements()) {
						for(EventHandler handler : element.getEventHandlers()) {
							eventsManager.addHandler(handler);
						}	
						ArrayList <EventHandler> resourceHandlers = (ArrayList<EventHandler>)element.getResource().getEventHandlers();
							for(EventHandler handler : resourceHandlers) {
								if(!registeredHandlers.contains(handler)) {
									eventsManager.addHandler(handler);
									registeredHandlers.add(handler);
								}
							}
						}
					}		
			}
	}
	
	
	//Hier muss noch die Moeglichkeit reinkommen, dass nicht alle LSPs nach jeder Iteration neu planen, sondern nur ein Teil von denen
	//Das kann durch ein entsprechendes replanningModule erreicht werden. Hier muss man dann nix aendern
	@Override
	public void notifyReplanning(ReplanningEvent event) {
		replanningModule.replanLSPs(event);	
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		scoringModule.scoreLSPs(event);	
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		eventsManager.removeHandler(carrierResourceTracker);
		
		ArrayList<LSPSimulationTracker> alreadyUpdatedTrackers = new ArrayList<>();
		for(LSP lsp : lsps.getLSPs().values()) {
			for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
				for(LogisticsSolutionElement element : solution.getSolutionElements()) {
					for( LSPSimulationTracker tracker : element.getResource().getSimulationTrackers()) {
						if(!alreadyUpdatedTrackers.contains(tracker)) {
							tracker.notifyAfterMobsim(event);
							alreadyUpdatedTrackers.add(tracker);
						}
					}
					for( LSPSimulationTracker tracker : element.getSimulationTrackers()) {
						tracker.notifyAfterMobsim(event);
					}
				}
				for( LSPSimulationTracker tracker : solution.getSimulationTrackers()) {
					tracker.notifyAfterMobsim(event);
				}
			}
		}
	
		for(LSP lsp : lsps.getLSPs().values()) {
			for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
				for(LogisticsSolutionElement element : solution.getSolutionElements()) {
					for(LSPInfo info : element.getInfos()) {
						info.update();
					}
				}
				for(LSPInfo info : solution.getInfos()) {
					info.update();
				}			
			}
		}
	}

	
	private Carriers getCarriers() {
		Carriers carriers = new Carriers();
		for(LSP lsp : lsps.getLSPs().values()) {
			LSPPlan selectedPlan = lsp.getSelectedPlan();
			for(LogisticsSolution solution : selectedPlan.getSolutions()) {
				for(LogisticsSolutionElement element : solution.getSolutionElements()) {
					if(element.getResource() instanceof LSPCarrierResource) {
						
						LSPCarrierResource carrierResource = (LSPCarrierResource) element.getResource();
						Carrier carrier = carrierResource.getCarrier();
						if(!carriers.getCarriers().containsKey(carrier.getId())) {
							carriers.addCarrier(carrier);
						}
					}					
				}
			}
		}
		return carriers;
	}

	public CarrierAgentTracker getCarrierResourceTracker() {
		return carrierResourceTracker;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if(event.getIteration() > 0) {
			for(EventHandler handler : registeredHandlers) {
				eventsManager.removeHandler(handler);
			}
		
			for(LSP lsp : lsps.getLSPs().values()) {
				for(LSPShipment shipment : lsp.getShipments()) {
					shipment.getEventHandlers().clear();
				}
			
				for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
					for(EventHandler handler : solution.getEventHandlers()) {
						handler.reset(event.getIteration());
					}
					for( LSPSimulationTracker tracker : solution.getSimulationTrackers()) {
						tracker.reset();
					}			
					for(LogisticsSolutionElement element : solution.getSolutionElements()) {
						for(EventHandler handler : element.getEventHandlers()) {
							handler.reset(event.getIteration());
						}
						for( LSPSimulationTracker tracker : element.getSimulationTrackers()) {
							tracker.reset();
						}
						for(EventHandler handler : element.getResource().getEventHandlers()) {
							handler.reset(event.getIteration());
						}		
						for( LSPSimulationTracker tracker : element.getResource().getSimulationTrackers()) {
							tracker.reset();
						}
					}			
				}		
			}			
		}	
	}	
}
