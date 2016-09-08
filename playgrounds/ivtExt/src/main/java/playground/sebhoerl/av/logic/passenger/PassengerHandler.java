package playground.sebhoerl.av.logic.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import playground.sebhoerl.av.framework.AVModule;
import playground.sebhoerl.av.logic.service.Request;
import playground.sebhoerl.av.logic.service.RequestFactory;
import playground.sebhoerl.av.logic.service.Service;
import playground.sebhoerl.av.logic.service.ServiceManager;

public class PassengerHandler implements DepartureHandler {
    final private PassengerEngine passengerEngine;
    final private ServiceManager serviceManager;
    final private RequestFactory requestFactory;
    
    public PassengerHandler(PassengerEngine passengerEngine, RequestFactory requestFactory, ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.requestFactory = requestFactory;
        this.passengerEngine = passengerEngine;
    }
    
    @Override
    public boolean handleDeparture(double now, MobsimAgent passenger, Id<Link> pickupLinkId) {
        if (passenger.getMode() != AVModule.AV_MODE) {
            return false;
        }
        
        Id<Link> dropoffLinkId = passenger.getDestinationLinkId();
        Request request = requestFactory.createRequest(passenger.getId(), now, pickupLinkId, dropoffLinkId);
        request.setSubmissionTime(now);
        
        Service service = serviceManager.handleRequest(request);
        service.setPassengerAgent(passenger);
        service.setDepartureTime(now);
        
        service.getPickupEvent().addListener(passengerEngine);
        service.getDropoffEvent().addListener(passengerEngine);
        
        service.setPassengerArrivalTime(now);
        service.getPassengerArrivalEvent().fire();
        
        return true;
    }
}
