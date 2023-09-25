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


import jakarta.inject.Inject;
import lsp.io.LSPPlanXmlWriter;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanWriter;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierAgentTracker;
import org.matsim.contrib.freight.controler.FreightUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.events.handler.EventHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


class LSPControlerListener implements BeforeMobsimListener, AfterMobsimListener, ScoringListener,
		ReplanningListener, IterationStartsListener, IterationEndsListener, ShutdownListener {
	private static final Logger log = LogManager.getLogger( LSPControlerListener.class );
	private final Scenario scenario;
	private final List<EventHandler> registeredHandlers = new ArrayList<>();

	@Inject private EventsManager eventsManager;
	@Inject private MatsimServices matsimServices;
	@Inject private LSPScorerFactory lspScoringFunctionFactory;
	@Inject @Nullable private LSPStrategyManager strategyManager;
	@Inject private OutputDirectoryHierarchy controlerIO;
	@Inject private CarrierAgentTracker carrierAgentTracker;
	@Inject LSPControlerListener( Scenario scenario ) {
		this.scenario = scenario;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		LSPs lsps = LSPUtils.getLSPs(scenario);

		//TODO: Why do we add all simTrackers in every iteration beforeMobsim starts?
		// Doing so results in a lot of "not adding eventsHandler since already added" warnings.
		// @KN: Would it be possible to do it in (simulation) startup and therefor only oce?
		for (LSP lsp : lsps.getLSPs().values()) {
			((LSPImpl) lsp).setScorer( lspScoringFunctionFactory.createScoringFunction() );


			// simulation trackers of lsp:
			registerSimulationTrackers(lsp );

			// simulation trackers of resources:
			for (LSPResource resource : lsp.getResources()) {
				registerSimulationTrackers(resource);
			}

			// simulation trackers of shipments:
			for (LSPShipment shipment : lsp.getShipments()) {
				registerSimulationTrackers(shipment );
			}

			// simulation trackers of solutions:
			for (LogisticChain solution : lsp.getSelectedPlan().getLogisticChains()) {
				registerSimulationTrackers(solution );

				// simulation trackers of solution elements:
				for (LogisticChainElement element : solution.getLogisticChainElements()) {
					registerSimulationTrackers(element );

					// simulation trackers of resources:
					registerSimulationTrackers(element.getResource() );

				}
			}
		}
	}

	private void registerSimulationTrackers( HasSimulationTrackers<?> hasSimulationTrackers) {
		// get all simulation trackers ...
		for (LSPSimulationTracker<?> simulationTracker : hasSimulationTrackers.getSimulationTrackers()) {
			// ... register them ...
			if (!registeredHandlers.contains(simulationTracker)) {
				log.warn("adding eventsHandler: " + simulationTracker);
				eventsManager.addHandler(simulationTracker);
				registeredHandlers.add(simulationTracker);
				matsimServices.addControlerListener(simulationTracker);
				simulationTracker.setEventsManager( eventsManager );
			} else {
				log.warn("not adding eventsHandler since already added: " + simulationTracker);
			}
		}
	}


	@Override
	public void notifyReplanning(ReplanningEvent event) {
		if ( strategyManager==null ) {
			throw new RuntimeException( "You need to set LSPStrategyManager to something meaningful to run iterations." );
		}

		LSPs lsps = LSPUtils.getLSPs(scenario);
		strategyManager.run(lsps.getLSPs().values(), event.getIteration(), event.getReplanningContext());

		for (LSP lsp : lsps.getLSPs().values()) {
			lsp.scheduleLogisticChains();
		}

		//Update carriers in scenario and CarrierAgentTracker
		carrierAgentTracker.getCarriers().getCarriers().clear();
		for (Carrier carrier : getCarriersFromLSP().getCarriers().values()) {
			FreightUtils.getCarriers(scenario).addCarrier(carrier);
			carrierAgentTracker.getCarriers().addCarrier(carrier);
		}

	}

	@Override
	public void notifyScoring(ScoringEvent scoringEvent) {
		for (LSP lsp : LSPUtils.getLSPs(scenario).getLSPs().values()) {
			lsp.scoreSelectedPlan();
		}
		// yyyyyy might make more sense to register the lsps directly as scoring controler listener (??)
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
	}


	Carriers getCarriersFromLSP() {
		LSPs lsps = LSPUtils.getLSPs(scenario);
		assert ! lsps.getLSPs().isEmpty();

		Carriers carriers = new Carriers();
		for (LSP lsp : lsps.getLSPs().values()) {
			LSPPlan selectedPlan = lsp.getSelectedPlan();
			for (LogisticChain solution : selectedPlan.getLogisticChains()) {
				for (LogisticChainElement element : solution.getLogisticChainElements()) {
					if( element.getResource() instanceof LSPCarrierResource carrierResource ) {
						Carrier carrier = carrierResource.getCarrier();
						if (!carriers.getCarriers().containsKey(carrier.getId())) {
							carriers.addCarrier(carrier);
						}
					}
				}
			}
		}
		return carriers;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		new LSPPlanXmlWriter(LSPUtils.getLSPs(scenario)).write(controlerIO.getIterationFilename(event.getIteration(), "lsps.xml"));
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		new LSPPlanXmlWriter(LSPUtils.getLSPs(scenario)).write(controlerIO.getOutputPath() + "/output_lsps.xml.gz");
		new CarrierPlanWriter(FreightUtils.getCarriers(scenario)).write(controlerIO.getOutputPath() + "/output_carriers.xml.gz");
	}
	/**
	 * There is a possibility and also sometimes a need for rescheduling before each
	 * new iteration. This need results from changes to the plan of the LSP during
	 * the re-planning process. There, changes of the structure and/or number of the
	 * active LogisticsSolutions or to the assignment of the LSPShipments to the
	 * LogisticsSolutions can happen. Consequently, the transport operations that
	 * result from this change in behavior of the corresponding LSP will also change
	 * and thus the preparation of the simulation has to be redone.
	 * <p>
	 * The rescheduling process is triggered in the BeforeMobsimEvent of the following iteration which has
	 * a LSPRescheduler as one of its listeners.
	 * <p>
	 * In this case, all LogisticsSolutions,
	 * LogisticsSolutionElements and Resources are cleared of all shipments that
	 * were assigned to them in the previous iteration in order to allow a new assignment.
	 * <p>
	 * Then, all LSPShipments of each LSP are assigned to the corresponding
	 * LogisticsSolutions by the Assigner of the LSP in order to account for possible
	 * changes in the LogisticsSolutions as well as in the assignment itself due to
	 * the re-planning that took place before. After this assignment is done, the actual
	 * scheduling takes place. In cases, where no re-planning takes place (further details of
	 * the re-planning algorithm follow in 3.8), rescheduling will nevertheless take place.
	 * This is reasonable for example in cases where also other traffic takes place on the
	 * network, for example passenger traffic, and the network conditions change between
	 * subsequent iterations of the simulation due to congestion.
	 * <p>
	 * ---
	 * Let's do it in re-planning.
	 * If doing so, we have the time to handle the result (on Carrier level) in the beforeMobsim step of the CarrierControlerListener.
	 * kmt sep'22
	 */
	private static void notifyReplanning( LSPs lsps, ReplanningEvent event ) {
		if (event.getIteration() != 0) {
			for (LSP lsp : lsps.getLSPs().values()) {
				for (LogisticChain solution : lsp.getSelectedPlan().getLogisticChains()) {
					solution.getShipmentIds().clear();
					for (LogisticChainElement element : solution.getLogisticChainElements()) {
						element.getIncomingShipments().clear();
						element.getOutgoingShipments().clear();
					}
				}

				for (LSPShipment shipment : lsp.getShipments()) {
					ShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), shipment.getId() ).clear();
					shipment.getShipmentLog().clear();
					lsp.getSelectedPlan().getAssigner().assignToLogisticChain(shipment);
				}
				lsp.scheduleLogisticChains();
			}
		}
	}
}
