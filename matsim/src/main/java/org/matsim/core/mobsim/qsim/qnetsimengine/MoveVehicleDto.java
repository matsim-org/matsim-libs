package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

public class MoveVehicleDto {

    private Id<Node> fromNodeId;
    private Id<Node> toNodeId;
    private Id<Link> fromLinkId;
    private Id<Lane> fromLaneId;
    private Id<Vehicle> vehicleId;
    private Id<Person> personId;
    private int personLinkIndex;
    private int planIndex;
    private Id<Link> toLinkId;
    private boolean aborted;

    public MoveVehicleDto(Id<Node> fromNodeId, Id<Node> toNodeId, Id<Link> fromLinkId, Id<Lane> fromLaneId, Id<Vehicle> vehicleId, Id<Person> personId, int personLinkIndex, int planIndex, Id<Link> toLinkId) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.fromLinkId = fromLinkId;
        this.fromLaneId = fromLaneId;
        this.vehicleId = vehicleId;
        this.personId = personId;
        this.personLinkIndex = personLinkIndex;
        this.planIndex = planIndex;
        this.toLinkId = toLinkId;
    }

    public static MoveVehicleDto aborted() {
        MoveVehicleDto moveVehicleDto = new MoveVehicleDto();
        moveVehicleDto.aborted = true;
        return moveVehicleDto;
    }

    public AcceptedVehiclesDto toAcceptedVehiclesDto() {
        return new AcceptedVehiclesDto(fromLinkId, fromLaneId, vehicleId);
    }

    public MoveVehicleDto() {
    }

    @Override
    public String toString() {
        return "MoveVehicleDto{" +
                "fromNodeId=" + fromNodeId +
                ", toNodeId=" + toNodeId +
                ", fromLinkId=" + fromLinkId +
                ", fromLaneId=" + fromLaneId +
                ", vehicleId=" + vehicleId +
                ", personId=" + personId +
                ", personLinkIndex=" + personLinkIndex +
                ", planIndex=" + planIndex +
                ", toLinkId=" + toLinkId +
                ", aborted=" + aborted +
                '}';
    }

    @JsonIgnore
    public Id<Node> getFromNodeId() {
        return fromNodeId;
    }

    @JsonIgnore
    public Id<Node> getToNodeId() {
        return toNodeId;
    }

    @JsonIgnore
    public Id<Link> getFromLinkId() {
        return fromLinkId;
    }

    @JsonIgnore
    public Id<Lane> getFromLaneId() {
        return fromLaneId;
    }

    @JsonIgnore
    public Id<Vehicle> getVehicleId() {
        return vehicleId;
    }

    @JsonIgnore
    public Id<Person> getPersonId() {
        return personId;
    }

    public int getPersonLinkIndex() {
        return personLinkIndex;
    }

    public int getPlanIndex() {
        return planIndex;
    }

    @JsonIgnore
    public Id<Link> getToLinkId() {
        return toLinkId;
    }

    @JsonSetter
    public void setToNodeId(String toNodeId) {
        this.toNodeId = Id.createNodeId(toNodeId);
    }

    @JsonSetter
    public void setFromNodeId(String toNodeId) {
        this.fromNodeId = Id.createNodeId(toNodeId);
    }

    @JsonSetter
    public void setFromLinkId(String fromLinkId) {
        this.fromLinkId = Id.createLinkId(fromLinkId);
    }

    @JsonSetter
    public void setFromLaneId(String fromLaneId) {
        this.fromLaneId = Id.create(fromLaneId, Lane.class);
    }

    @JsonSetter
    public void setVehicleId(String vehicleId) {
        this.vehicleId = Id.createVehicleId(vehicleId);
    }

    @JsonSetter
    public void setPersonId(String  personId) {
        this.personId = Id.createPersonId(personId);
    }

    @JsonSetter
    public void setPersonLinkIndex(int personLinkIndex) {
        this.personLinkIndex = personLinkIndex;
    }

    public void setPlanIndex(int planIndex) {
        this.planIndex = planIndex;
    }

    @JsonSetter
    public void setToLinkId(String toLinkId) {
        this.toLinkId = Id.createLinkId(toLinkId);
    }

    @JsonGetter("fromNodeId")
    public String getFromNodeIdAsString() {
        return fromNodeId.toString();
    }

    @JsonGetter("toNodeId")
    public String getToNodeIdAsString() {
        return toNodeId.toString();
    }

    @JsonGetter("fromLinkId")
    public String getFromLinkIdAsString() {
        return fromLinkId.toString();
    }

    @JsonGetter("fromLaneId")
    public String getFromLaneIdAsString() {
        return fromLaneId.toString();
    }

    @JsonGetter("vehicleId")
    public String getVehicleIdAsString() {
        return vehicleId.toString();
    }

    @JsonGetter("personId")
    public String getPersonIdAsString() {
        return personId.toString();
    }

    @JsonGetter("toLinkId")
    public String getToLinkIdAsString() {
        return toLinkId.toString();
    }

    @JsonIgnore
    public boolean isAborted() {
        return aborted;
    }
}

