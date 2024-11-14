package org.matsim.dsim.simulation;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.DistributedMobsimVehicle;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Collection;

@Data
@Builder(setterPrefix = "set")
@Getter
@Setter
public class SimpleVehicle implements Message, DistributedMobsimVehicle {

	private Id<Vehicle> id;
	private Id<Link> currentLinkId;
	private MobsimDriverAgent driver;
	private double linkEnterTime;
	private double earliestLinkExitTime;
	private double maximumVelocity;
	private Vehicle vehicle;
	private double sizeInEquivalents;
	private int passengerCapacity;

	@Builder.Default
	private Collection<PassengerAgent> passengers = new ArrayList<>();

	public void setDriver(DriverAgent driver) {
		this.driver = (MobsimDriverAgent) driver;
	}

	@Override
	public boolean addPassenger(PassengerAgent passenger) {
		return passengers.add(passenger);
	}

	@Override
	public boolean removePassenger(PassengerAgent passenger) {
		return passengers.remove(passenger);
	}

	@Override
	public Message toMessage() {
		return this;
	}

}
