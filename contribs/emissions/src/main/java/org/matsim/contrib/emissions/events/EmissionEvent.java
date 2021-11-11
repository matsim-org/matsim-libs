package org.matsim.contrib.emissions.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

public abstract class EmissionEvent extends Event {

    public final static String ATTRIBUTE_LINK_ID = "linkId";
    public final static String ATTRIBUTE_VEHICLE_ID = "vehicleId";

    private final Id<Link> linkId;
    private final Id<Vehicle> vehicleId;
    private final Map<Pollutant, Double> emissions;

    EmissionEvent(double time, Id<Link> linkId, Id<Vehicle> vehicleId, Map<Pollutant, Double> emissions) {
        super(time);
        this.linkId = linkId;
        this.vehicleId = vehicleId;
        this.emissions = emissions;
    }

    public Id<Link> getLinkId() {
        return linkId;
    }

    public Id<Vehicle> getVehicleId() {
        return vehicleId;
    }

    public Map<Pollutant, Double> getEmissions() {
        return emissions;
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attributes = super.getAttributes();
        attributes.put(ATTRIBUTE_LINK_ID, this.linkId.toString());
        attributes.put(ATTRIBUTE_VEHICLE_ID, this.vehicleId.toString());
        for (Map.Entry<Pollutant, Double> entry : emissions.entrySet()) {
            attributes.put(entry.getKey().name(), entry.getValue().toString());
        }
        return attributes;
    }
}
