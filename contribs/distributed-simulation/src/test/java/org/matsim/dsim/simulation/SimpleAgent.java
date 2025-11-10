package org.matsim.dsim.simulation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

public class SimpleAgent implements Message, DistributedMobsimAgent, MobsimDriverAgent {

	private Id<Person> id;
	private Id<Link> currentLinkId;
	private Id<Link> destinationLinkId;
	private String mode;
	private State state;
	private double activityEndTime;
	private OptionalTime expectedTravelTime;
	private Double expectedTravelDistance;
	private Facility currentFacility;
	private Facility destinationFacility;
	private MobsimVehicle vehicle;
	private Id<Vehicle> plannedVehicleId;
	private boolean isWantingToArriveOnCurrentLink;

	@Override
	public Id<Person> getId() {
		return id;
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return currentLinkId;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return destinationLinkId;
	}

	@Override
	public String getMode() {
		return mode;
	}

	@Override
	public State getState() {
		return state;
	}

	@Override
	public double getActivityEndTime() {
		return activityEndTime;
	}

	@Override
	public OptionalTime getExpectedTravelTime() {
		return expectedTravelTime;
	}

	@Override
	public Double getExpectedTravelDistance() {
		return expectedTravelDistance;
	}

	@Override
	public Facility getCurrentFacility() {
		return currentFacility;
	}

	@Override
	public Facility getDestinationFacility() {
		return destinationFacility;
	}

	@Override
	public MobsimVehicle getVehicle() {
		return vehicle;
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return plannedVehicleId;
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		return isWantingToArriveOnCurrentLink;
	}

	public void setId(Id<Person> id) {
		this.id = id;
	}

	public void setCurrentLinkId(Id<Link> currentLinkId) {
		this.currentLinkId = currentLinkId;
	}

	public void setDestinationLinkId(Id<Link> destinationLinkId) {
		this.destinationLinkId = destinationLinkId;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public void setState(State state) {
		this.state = state;
	}

	public void setActivityEndTime(double activityEndTime) {
		this.activityEndTime = activityEndTime;
	}

	public void setExpectedTravelTime(OptionalTime expectedTravelTime) {
		this.expectedTravelTime = expectedTravelTime;
	}

	public void setExpectedTravelDistance(Double expectedTravelDistance) {
		this.expectedTravelDistance = expectedTravelDistance;
	}

	public void setCurrentFacility(Facility currentFacility) {
		this.currentFacility = currentFacility;
	}

	public void setDestinationFacility(Facility destinationFacility) {
		this.destinationFacility = destinationFacility;
	}

	@Override
	public void setVehicle(MobsimVehicle vehicle) {
		this.vehicle = vehicle;
	}

	public void setPlannedVehicleId(Id<Vehicle> plannedVehicleId) {
		this.plannedVehicleId = plannedVehicleId;
	}

	public void setWantingToArriveOnCurrentLink(boolean wantingToArriveOnCurrentLink) {
		isWantingToArriveOnCurrentLink = wantingToArriveOnCurrentLink;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
	}

	@Override
	public void setStateToAbort(double now) {
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
	}

	@Override
	public void endLegAndComputeNextState(double now) {
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		return null;
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {

	}

	@Override
	public Message toMessage() {
		return this;
	}


}
