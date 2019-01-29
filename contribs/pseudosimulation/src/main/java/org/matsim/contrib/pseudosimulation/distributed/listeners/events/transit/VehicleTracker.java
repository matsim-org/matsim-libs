package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.Serializable;
import java.util.*;

class VehicleTracker implements Serializable {
    public FullDeparture getFullDeparture() {
        return fullDeparture;
    }

    transient private final FullDeparture fullDeparture;
    transient private final Id driverId;
    private int ridership = 0;
    private int capacity;



    private DwellEvent lastDwellEvent;
    private List<DwellEvent> stopsVisited = new ArrayList<>();

    public VehicleTracker(FullDeparture fullDeparture, Id driverId, int capacity) {
        super();
        this.fullDeparture = fullDeparture;
        this.driverId = driverId;
        this.capacity = capacity;
    }

    public void ridershipIncrement(PersonEntersVehicleEvent event) {
        if (!event.getPersonId().equals(driverId))
            ridership++;
    }

    public void ridershipDecrement(PersonLeavesVehicleEvent event) {
        if (!event.getPersonId().equals(driverId))
            ridership--;
    }

    public DwellEvent registerArrival(VehicleArrivesAtFacilityEvent event) {
        DwellEvent dwellEvent = new DwellEvent(event.getTime(),event.getFacilityId().toString(), this,stopsVisited.size());
        stopsVisited.add(dwellEvent);
        this.lastDwellEvent = dwellEvent;
        return this.lastDwellEvent;
    }

    public void registerDeparture(VehicleDepartsAtFacilityEvent event) {
        lastDwellEvent.setDepartureTime(event.getTime());
        lastDwellEvent.setOccupancyAtDeparture(getOccupancy());
    }


    public double getInVehicleTime(DwellEvent dwellEvent, Id<TransitStopFacility> destinationStop) {
        for (int i = dwellEvent.getIndexInVehicleDwellEventList(); i < stopsVisited.size(); i++) {
            if(stopsVisited.get(i).getStopId().equals(destinationStop.toString())){
                return stopsVisited.get(i).getArrivalTime() - dwellEvent.getArrivalTime();
            }
        }
        return Double.POSITIVE_INFINITY;
    }
    public double getOccupancy(){
        return (double)ridership/(double)capacity;
    }
}
