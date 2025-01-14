/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
package org.matsim.freight.receiver;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.io.algorithm.VehicleRoutingAlgorithms;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.freight.carriers.jsprit.NetworkRouter;
import org.matsim.freight.receiver.collaboration.CollaborationUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;

import jakarta.inject.Inject;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

class ReceiverTriggersCarrierReplanningListener implements IterationStartsListener {
    private final ReceiverConfigGroup receiverConfig;
    @Inject private Scenario sc;

    ReceiverTriggersCarrierReplanningListener(){
        this.receiverConfig = ConfigUtils.addOrGetModule( sc.getConfig(), ReceiverConfigGroup.class );
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        /* Replan the carrier at iteration zero, and one iteration after the receivers have replanned. */
        if(event.getIteration() > 0 &&
                           (event.getIteration()+1) % ConfigUtils.addOrGetModule(sc.getConfig(), ReceiverConfigGroup.class).getReceiverReplanningInterval() != 0) {
            return;
        }
        LogManager.getLogger(ReceiverTriggersCarrierReplanningListener.class).info("--> Receiver triggering carrier to replan.");
        // Adds the receiver agents that are part of the current (sub)coalition.
        CollaborationUtils.setCoalitionFromReceiverAttributes( sc );

        // clean out plans, services, shipments from carriers:
        Map<Id<Carrier>, Carrier> carriers = CarriersUtils.getCarriers(sc).getCarriers();
        for( Carrier carrier : carriers.values() ){
            carrier.clearPlans();
            carrier.getShipments().clear();
            carrier.getServices().clear();
        }

        // re-fill the carriers from the receiver orders:
        Map<Id<Receiver>, Receiver> receivers = ReceiverUtils.getReceivers( sc ).getReceivers();
        int nn = 0 ;
        for( Receiver receiver : receivers.values() ){
            ReceiverPlan receiverPlan = receiver.getSelectedPlan();
            for( ReceiverOrder receiverOrder : receiverPlan.getReceiverOrders() ){
                for( Order order : receiverOrder.getReceiverProductOrders() ){
                    nn++ ;
                    CarrierShipment.Builder builder = CarrierShipment.Builder.newInstance(
                                    Id.create("Order" + receiverPlan.getReceiver().getId().toString() + nn, CarrierShipment.class),
                                    order.getProduct().getProductType().getOriginLinkId(),
                                    order.getReceiver().getLinkId(),
                                    (int) (Math.round(order.getDailyOrderQuantity()*order.getProduct().getProductType().getRequiredCapacity())) );
                    CarrierShipment newShipment = builder
                                                                  .setDeliveryDuration( order.getServiceDuration() )
                                                                  .setDeliveryStartingTimeWindow( receiverPlan.getTimeWindows().get( 0 ) )
                                                                  // TODO This only looks at the FIRST time window. This may need revision once we handle multiple
                                                                  // time windows.
                                                                  .build();
                    if (newShipment.getCapacityDemand() != 0) {
                        receiverOrder.getCarrier().getShipments().put(newShipment.getId(), newShipment );
                    }
                }
            }
        }

        for( Carrier carrier : carriers.values() ){
            // for all carriers, re-run jsprit:

            VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, sc.getNetwork());

            NetworkBasedTransportCosts netBasedCosts = NetworkBasedTransportCosts.Builder.newInstance(sc.getNetwork(), carrier.getCarrierCapabilities().getVehicleTypes()).build();
            VehicleRoutingProblem vrp = vrpBuilder.setRoutingCost(netBasedCosts).build();

            //read and create a pre-configured algorithms to solve the vrp
            URL algoConfigFileName = IOUtils.extendUrl( sc.getConfig().getContext(), "initialPlanAlgorithm.xml");
            VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, algoConfigFileName);

            //solve the problem
            Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();

            //create a new carrierPlan from the best solution
            CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, Solutions.bestOf( solutions ) );

            //route plan
            NetworkRouter.routePlan(newPlan, netBasedCosts);

            //assign this plan now to the carrier and make it the selected carrier plan
            carrier.addPlan(newPlan);

        }
        String outputdirectory = sc.getConfig().controller().getOutputDirectory();
        outputdirectory += outputdirectory.endsWith("/") ? "" : "/";
//        new CarrierPlanWriter(CarrierControlerUtils.getCarriers(sc)).write(outputdirectory + ReceiverConfigGroup.CARRIERS_FILE);
        new CarrierPlanWriter(CarriersUtils.getCarriers(sc)).write(outputdirectory +receiverConfig.getCarriersFile() );
//        new ReceiversWriter( ReceiverUtils.getReceivers( sc ) ).write(outputdirectory + ReceiverConfigGroup.RECEIVERS_FILE);
        new ReceiversWriter( ReceiverUtils.getReceivers( sc ) ).write(outputdirectory + receiverConfig.getReceiversFile());
    }
}
