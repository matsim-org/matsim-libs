package org.matsim.dsim.simulation;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.DistributedMobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

@Data
@Builder(setterPrefix = "set")
@Getter
@Setter
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
	public void computeNextAction(double now) {
		throw new UnsupportedOperationException("Not supported in this test class");
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
