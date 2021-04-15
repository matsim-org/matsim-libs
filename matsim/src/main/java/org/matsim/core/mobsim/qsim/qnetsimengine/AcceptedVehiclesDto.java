package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AcceptedVehiclesDto {

    private Id<Link> linkId;
    private Id<Lane> laneId;
    private Id<Vehicle> vehicleId;

    public AcceptedVehiclesDto(Id<Link> linkId, Id<Lane> laneId, Id<Vehicle> vehicleId) {
        this.linkId = linkId;
        this.laneId = laneId;
        this.vehicleId = vehicleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AcceptedVehiclesDto that = (AcceptedVehiclesDto) o;
        return Objects.equals(vehicleId, that.vehicleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicleId);
    }

    public AcceptedVehiclesDto() {
    }

    @JsonIgnore
    public Id<Link> getLinkId() {
        return linkId;
    }

    @JsonIgnore
    public Id<Lane> getLaneId() {
        return laneId;
    }

    @JsonIgnore
    public Id<Vehicle> getVehicleId() {
        return vehicleId;
    }

    @JsonSetter
    public void setLinkId(String linkId) {
        this.linkId = Id.createLinkId(linkId);
    }

    @JsonSetter
    public void setLaneId(String laneId) {
        this.laneId = Id.create(laneId, Lane.class);
    }

    @JsonSetter
    public void setVehicleId(String vehicleId) {
        this.vehicleId = Id.createVehicleId(vehicleId);
    }

    @JsonGetter("linkId")
    public String getLinkIdAsString() {
        return linkId.toString();
    }

    @JsonGetter("laneId")
    public String getLaneIdAsString() {
        return laneId.toString();
    }

    @JsonGetter("vehicleId")
    public String getVehicleIdAsString() {
        return vehicleId.toString();
    }

}
