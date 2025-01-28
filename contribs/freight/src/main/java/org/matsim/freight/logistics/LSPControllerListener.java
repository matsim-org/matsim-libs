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

package org.matsim.freight.logistics;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlanWriter;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.controller.CarrierAgentTracker;
import org.matsim.freight.logistics.io.LSPPlanXmlWriter;
import org.matsim.freight.logistics.shipment.LspShipment;

class LSPControllerListener
    implements StartupListener,
        BeforeMobsimListener,
        AfterMobsimListener,
        ScoringListener,
        ReplanningListener,
        IterationStartsListener,
        IterationEndsListener,
        ShutdownListener {
  private static final Logger log = LogManager.getLogger(LSPControllerListener.class);
  private final Scenario scenario;
  private final List<EventHandler> registeredHandlers = new ArrayList<>();

  private static int addListenerCnt = 0;
  private  static final int maxAddListenerCnt = 1;

  @Inject private EventsManager eventsManager;
  @Inject private MatsimServices matsimServices;
  @Inject private LSPScorerFactory lspScoringFunctionFactory;
  @Inject @Nullable private LSPStrategyManager strategyManager;
  @Inject private OutputDirectoryHierarchy controlerIO;
  @Inject private CarrierAgentTracker carrierAgentTracker;


  @Inject
  LSPControllerListener(Scenario scenario) {
    this.scenario = scenario;
  }

  @Override
  public void notifyStartup(StartupEvent event) {
    //Ensure that all ressource Ids are only there once.

    checkForUniqueResourceIds();

  }

/**
* For later steps, e.g. scoring the Ids of the {@link LSPResource} ids must be unique.
 * Otherwise, there are scored several times.
 * <p></p>
 * For the future we may reduce it to unique {@link LSPResource} ids PER {@link LSP}.
 * This means, that the events (also from the carriers) need to have an information obout the LSP it belongs to and that
 * in scoring and analysis this must be taken into account. What itself is another source for errors...
 * KMT jul'24
*/
  private void checkForUniqueResourceIds() {
    List<String> duplicates = new ArrayList<>();
    Set<String> set = new HashSet<>();

    LSPs lsps = LSPUtils.getLSPs(scenario);
    for (LSP lsp : lsps.getLSPs().values()) {
      for (LSPResource lspResource : lsp.getResources()) {
        String idString = lspResource.getId().toString();
        if (set.contains(idString)) {
          duplicates.add(idString);
        } else {
          set.add(idString);
        }
      }
    }

    if (!duplicates.isEmpty()) {
      log.error("There are non-unique ressource Ids. This must not be! The duplicate ids are: {}.", duplicates.toString());
      log.error("You may also use output_lsp.xml to check were the duplicates are located");
      log.error("Aborting now ...");
      throw new RuntimeException();
    }
  }

  @Override
  public void notifyBeforeMobsim(BeforeMobsimEvent event) {
    LSPs lsps = LSPUtils.getLSPs(scenario);

    // TODO: Why do we add all simTrackers in every iteration beforeMobsim starts?
    // Doing so results in a lot of "not adding eventsHandler since already added" warnings.
    // @KN: Would it be possible to do it in (simulation) startup and therefor only oce?
    for (LSP lsp : lsps.getLSPs().values()) {
      ((LSPImpl) lsp).setScorer(lspScoringFunctionFactory.createScoringFunction());

      // simulation trackers of lsp:
      registerSimulationTrackers(lsp);

      // simulation trackers of resources:
      for (LSPResource resource : lsp.getResources()) {
        registerSimulationTrackers(resource);
      }

      // simulation trackers of shipments:
      for (LspShipment lspShipment : lsp.getLspShipments()) {
        registerSimulationTrackers(lspShipment);
      }

      // simulation trackers of solutions:
      for (LogisticChain solution : lsp.getSelectedPlan().getLogisticChains()) {
        registerSimulationTrackers(solution);

        // simulation trackers of solution elements:
        for (LogisticChainElement element : solution.getLogisticChainElements()) {
          registerSimulationTrackers(element);

          // simulation trackers of resources:
          registerSimulationTrackers(element.getResource());
        }
      }
    }
  }

  private void registerSimulationTrackers(HasSimulationTrackers<?> hasSimulationTrackers) {
    // get all simulation trackers ...
    for (LSPSimulationTracker<?> simulationTracker :
        hasSimulationTrackers.getSimulationTrackers()) {
      // ... register them ...
      if (!registeredHandlers.contains(simulationTracker)) {
        log.info("adding eventsHandler: {}", simulationTracker);
        eventsManager.addHandler(simulationTracker);
        registeredHandlers.add(simulationTracker);
        matsimServices.addControlerListener(simulationTracker);
        simulationTracker.setEventsManager(eventsManager);
      } else if ( addListenerCnt < maxAddListenerCnt ){
        log.warn("not adding eventsHandler since already added: {}", simulationTracker);
        addListenerCnt++;
        if (addListenerCnt == maxAddListenerCnt) {
          log.warn(Gbl.FUTURE_SUPPRESSED);
        }
      }
    }

  }

  @Override
  public void notifyReplanning(ReplanningEvent event) {
    if (strategyManager == null) {
      throw new RuntimeException(
          "You need to set LSPStrategyManager to something meaningful to run iterations.");
    }

    LSPs lsps = LSPUtils.getLSPs(scenario);
    strategyManager.run(
        lsps.getLSPs().values(), event.getIteration(), event.getReplanningContext());

    for (LSP lsp : lsps.getLSPs().values()) {
      lsp.getSelectedPlan()
          .getShipmentPlans()
          .clear(); // clear ShipmentPlans to start with clear(n) state. Otherwise, some of the times were
                    // accumulating over the time. :(
      lsp.scheduleLogisticChains();
    }

    // Update carriers in scenario and CarrierAgentTracker
    carrierAgentTracker.getCarriers().getCarriers().clear();
    for (Carrier carrier : getCarriersFromLSP().getCarriers().values()) {
      CarriersUtils.getCarriers(scenario).addCarrier(carrier);
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
  public void notifyAfterMobsim(AfterMobsimEvent event) {}

  Carriers getCarriersFromLSP() {
    LSPs lsps = LSPUtils.getLSPs(scenario);
    assert !lsps.getLSPs().isEmpty();

    Carriers carriers = new Carriers();
    for (LSP lsp : lsps.getLSPs().values()) {
      LSPPlan selectedPlan = lsp.getSelectedPlan();
      for (LogisticChain solution : selectedPlan.getLogisticChains()) {
        for (LogisticChainElement element : solution.getLogisticChainElements()) {
          if (element.getResource() instanceof LSPCarrierResource carrierResource) {
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
  public void notifyIterationStarts(IterationStartsEvent event) {}

  @Override
  public void notifyIterationEnds(IterationEndsEvent event) {
    new LSPPlanXmlWriter(LSPUtils.getLSPs(scenario))
        .write(controlerIO.getIterationFilename(event.getIteration(), "lsps.xml"));
  }

  @Override
  public void notifyShutdown(ShutdownEvent event) {
    new LSPPlanXmlWriter(LSPUtils.getLSPs(scenario))
        .write(controlerIO.getOutputPath() + "/output_lsps.xml.gz");
	CarriersUtils.writeCarriers(scenario,"output_carriers.xml.gz");
  }

}
