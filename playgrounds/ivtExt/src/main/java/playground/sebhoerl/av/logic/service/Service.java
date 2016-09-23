package playground.sebhoerl.av.logic.service;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.population.routes.NetworkRoute;

import playground.sebhoerl.agentlock.events.Event;
import playground.sebhoerl.av.logic.agent.AVAgent;
import playground.sebhoerl.av.logic.events.DropoffEvent;
import playground.sebhoerl.av.logic.events.PickupEvent;
import playground.sebhoerl.av.utils.UncachedId;

public class Service {    
    // Initial
    final private UncachedId id;
    final private Request request;
    
    // Timing
    private double dispatchmentTime = Double.POSITIVE_INFINITY;
    private double startTime = Double.POSITIVE_INFINITY;
    private double departureTime = Double.POSITIVE_INFINITY;
    private double pickupArrivalTime = Double.POSITIVE_INFINITY;
    private double passengerArrivalTime = Double.POSITIVE_INFINITY;
    private double pickupTime = Double.POSITIVE_INFINITY;
    private double pickupDepartureTime = Double.POSITIVE_INFINITY;
    private double dropoffArrivalTime = Double.POSITIVE_INFINITY;
    private double dropoffTime = Double.POSITIVE_INFINITY;
    private double endTime = Double.POSITIVE_INFINITY;
    
    // Spatial
    private double pickupDriveDistance = 0.0;
    private double dropoffDriveDistance = 0.0;
    private Id<Link> startLinkId;
    
    // Todo: Is this necssary here?
    private NetworkRoute pickupRoute;
    private NetworkRoute dropoffRoute;
    
    // Agents
    private AVAgent driverAgent;
    private MobsimAgent passengerAgent;

    // Events    
    final private PickupEvent pickupEvent;
    final private DropoffEvent dropoffEvent;
    final private Event passengerArrivalEvent = new Event();
    final private Event finishTaskEvent = new Event();
    
    public Service(UncachedId id, Request request) {
        pickupEvent = new PickupEvent(this);
        dropoffEvent = new DropoffEvent(this);
        
        this.id = id;
        this.request = request;
    }
    
    public double getStartTime() {
        return startTime;
    }
    
    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }
    
    public double getPickupArrivalTime() {
        return pickupArrivalTime;
    }
    
    public void setPickupArrivalTime(double pickupArrivalTime) {
        this.pickupArrivalTime = pickupArrivalTime;
    }
    
    public double getPassengerArrivalTime() {
        return passengerArrivalTime;
    }
    
    public void setPassengerArrivalTime(double passengerArrivalTime) {
        this.passengerArrivalTime = passengerArrivalTime;
    }
    
    public double getPickupDepartureTime() {
        return pickupDepartureTime;
    }
    
    public void setPickupDepartureTime(double pickupDepartureTime) {
        this.pickupDepartureTime = pickupDepartureTime;
    }
    
    public double getDropoffTime() {
        return dropoffTime;
    }
    
    public void setDropoffTime(double dropoffTime) {
        this.dropoffTime = dropoffTime;
    }
    
    public double getEndTime() {
        return endTime;
    }
    
    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    public Id<Link> getStartLinkId() {
        return startLinkId;
    }

    public void setStartLinkId(Id<Link> startLinkId) {
        this.startLinkId = startLinkId;
    }

    public double getPickupDriveDistance() {
        return pickupDriveDistance;
    }

    public void setPickupDriveDistance(double pickupDriveDistance) {
        this.pickupDriveDistance = pickupDriveDistance;
    }

    public double getDropoffDriveDistance() {
        return dropoffDriveDistance;
    }

    public void setDropoffDriveDistance(double dropoffDriveDistance) {
        this.dropoffDriveDistance = dropoffDriveDistance;
    }

    public double getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(double pickupTime) {
        this.pickupTime = pickupTime;
    }

    public AVAgent getDriverAgent() {
        return driverAgent;
    }

    public void setDriverAgent(AVAgent driverAgent) {
        this.driverAgent = driverAgent;
    }

    public NetworkRoute getDropoffRoute() {
        return dropoffRoute;
    }

    public void setDropoffRoute(NetworkRoute dropoffRoute) {
        this.dropoffRoute = dropoffRoute;
    }

    public NetworkRoute getPickupRoute() {
        return pickupRoute;
    }

    public void setPickupRoute(NetworkRoute pickupRoute) {
        this.pickupRoute = pickupRoute;
    }

    public PickupEvent getPickupEvent() {
        return pickupEvent;
    }

    public DropoffEvent getDropoffEvent() {
        return dropoffEvent;
    }

    public Event getFinishTaskEvent() {
        return finishTaskEvent;
    }

    public MobsimAgent getPassengerAgent() {
        return passengerAgent;
    }

    public void setPassengerAgent(MobsimAgent passengerAgent) {
        if (!(passengerAgent instanceof PassengerAgent)) {
            throw new RuntimeException();
        }
        
        this.passengerAgent = passengerAgent;
    }

    public Request getRequest() {
        return request;
    }

    public Event getPassengerArrivalEvent() {
        return passengerArrivalEvent;
    }

    public double getDropoffArrivalTime() {
        return dropoffArrivalTime;
    }

    public void setDropoffArrivalTime(double dropoffArrivalTime) {
        this.dropoffArrivalTime = dropoffArrivalTime;
    }

    public UncachedId getId() {
        return id;
    }

    public double getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(double departureTime) {
        this.departureTime = departureTime;
    }

    public double getDispatchmentTime() {
        return dispatchmentTime;
    }

    public void setDispatchmentTime(double dispatchmentTime) {
        this.dispatchmentTime = dispatchmentTime;
    }
}
