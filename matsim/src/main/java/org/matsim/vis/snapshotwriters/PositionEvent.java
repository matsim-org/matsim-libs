package org.matsim.vis.snapshotwriters;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

public class PositionEvent extends Event implements BasicLocation, HasPersonId {

    public static final String EVENT_TYPE = "position";

    private final AgentSnapshotInfo position;

    public PositionEvent(double now, AgentSnapshotInfo position) {
        super(now);
        this.position = position;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Override
    public Coord getCoord() {
        return new Coord(position.getEasting(), position.getNorthing());
    }

    @Override
    public Id<Person> getPersonId() {
        return position.getId();
    }

    public Id<Link> getLinkId() {
        return position.getLinkId();
    }

    public Id<Vehicle> getVehicleId() {
        return position.getVehicleId();
    }

    public double getColorValueBetweenZeroAndOne() {
        return position.getColorValueBetweenZeroAndOne();
    }

    public AgentSnapshotInfo.AgentState getState() {
        return position.getAgentState();
    }

    @Override
    public Map<String, String> getAttributes() {
        try {
            var attr = super.getAttributes();
            attr.put("state", position.getAgentState().toString());
            attr.put("linkId", position.getLinkId().toString());
            if (position.getVehicleId() != null)
                attr.put("vehicleId", position.getVehicleId().toString());
            return attr;
        } catch (Exception e) {
            throw new RuntimeException("oh no!");
        }
    }
}
