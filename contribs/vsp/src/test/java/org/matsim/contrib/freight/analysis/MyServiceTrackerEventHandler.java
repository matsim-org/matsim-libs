package org.matsim.contrib.freight.analysis;

import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.events.LSPServiceEndEvent;
import org.matsim.contrib.freight.events.LSPServiceStartEvent;
import org.matsim.contrib.freight.events.eventhandler.LSPServiceEndEventHandler;
import org.matsim.contrib.freight.events.eventhandler.LSPServiceStartEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

 class MyServiceTrackerEventHandler implements ActivityStartEventHandler, LSPServiceStartEventHandler, LSPServiceEndEventHandler {
    private final Vehicles vehicles;
    private final Network network;
    private final Carriers carriers;

    private FreightAnalysisServiceTracking serviceTracking = new FreightAnalysisServiceTracking();

    MyServiceTrackerEventHandler(Vehicles vehicles, Network network, Carriers carriers) {
        this.network = network;
        this.carriers = carriers;
        this.vehicles = vehicles;
        this.init();
    }

    private void init() {
        for (Carrier carrier : carriers.getCarriers().values()) {

            for (CarrierService service : carrier.getServices().values()) {
                serviceTracking.addTracker(service, carrier.getId());
            }
        }
        serviceTracking.estimateArrivalTimes(carriers);
    }

    @Override
    public void handleEvent(LSPServiceEndEvent event) {
        serviceTracking.handleEndEvent(event);
    }

    @Override
    public void handleEvent(LSPServiceStartEvent event) {
        serviceTracking.handleStartEvent(event);
    }

    @Override
    public void handleEvent(ActivityStartEvent activityStartEvent) {
        if (activityStartEvent.getActType().equals("service")) {
            serviceTracking.trackServiceActivityStart(activityStartEvent);
        }
    }

    public FreightAnalysisServiceTracking getServiceTracking() {
        return serviceTracking;
    }
}
