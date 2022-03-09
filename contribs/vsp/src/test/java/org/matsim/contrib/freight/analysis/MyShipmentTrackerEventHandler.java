package org.matsim.contrib.freight.analysis;

import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;
import org.matsim.contrib.freight.events.ShipmentPickedUpEvent;
import org.matsim.contrib.freight.events.eventhandler.ShipmentDeliveredEventHandler;
import org.matsim.contrib.freight.events.eventhandler.ShipmentPickedUpEventHandler;
import org.matsim.vehicles.Vehicles;

public class MyShipmentTrackerEventHandler implements ActivityStartEventHandler, ShipmentPickedUpEventHandler, ShipmentDeliveredEventHandler {

    private final Vehicles vehicles;
    private final Network network;
    private final Carriers carriers;

    private FreightAnalysisShipmentTracking shipmentTracking = new FreightAnalysisShipmentTracking();

    MyShipmentTrackerEventHandler(Vehicles vehicles, Network network, Carriers carriers) {
        this.network = network;
        this.carriers = carriers;
        this.vehicles = vehicles;
        this.init();
    }

    private void init(){
        for (Carrier carrier : carriers.getCarriers().values()) {
            // for all shipments and services of the carriers, tracking is started here.
            for (CarrierShipment shipment : carrier.getShipments().values()) {
                shipmentTracking.addTracker(shipment);
            }
        }
    }

    @Override
    public void handleEvent(ShipmentDeliveredEvent event) {
        shipmentTracking.trackDeliveryEvent(event);
    }

    @Override
    public void handleEvent(ShipmentPickedUpEvent event) {
        shipmentTracking.trackPickedUpEvent(event);
    }

    @Override
    public void reset(int iteration) {
        ShipmentPickedUpEventHandler.super.reset(iteration);
    }

    @Override
    public void handleEvent(ActivityStartEvent activityStartEvent) {

        if (activityStartEvent.getActType().equals("delivery")) {
            shipmentTracking.trackDeliveryActivity(activityStartEvent);
        }

        if (activityStartEvent.getActType().equals("pickup")){
            shipmentTracking.trackPickupActivity(activityStartEvent);
        }

    }

    public FreightAnalysisShipmentTracking getShipmentTracker(){
        return shipmentTracking;
    }
}
