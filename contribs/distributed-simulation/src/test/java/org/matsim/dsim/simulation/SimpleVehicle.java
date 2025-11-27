package org.matsim.dsim.simulation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Collection;

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

	@Override
	public Id<Vehicle> getId() {
		return id;
	}

	public void setId(Id<Vehicle> id) {
		this.id = id;
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return currentLinkId;
	}

	@Override
	public void setCurrentLinkId(Id<Link> currentLinkId) {
		this.currentLinkId = currentLinkId;
	}

	@Override
	public MobsimDriverAgent getDriver() {
		return driver;
	}

	public void setDriver(MobsimDriverAgent driver) {
		this.driver = driver;
	}

	@Override
	public double getLinkEnterTime() {
		return linkEnterTime;
	}

	@Override
	public void setLinkEnterTime(double linkEnterTime) {
		this.linkEnterTime = linkEnterTime;
	}

	@Override
	public double getEarliestLinkExitTime() {
		return earliestLinkExitTime;
	}

	@Override
	public void setEarliestLinkExitTime(double earliestLinkExitTime) {
		this.earliestLinkExitTime = earliestLinkExitTime;
	}

	@Override
	public double getMaximumVelocity() {
		return maximumVelocity;
	}

	public void setMaximumVelocity(double maximumVelocity) {
		this.maximumVelocity = maximumVelocity;
	}

	@Override
	public Vehicle getVehicle() {
		return vehicle;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	@Override
	public double getSizeInEquivalents() {
		return sizeInEquivalents;
	}

	public void setSizeInEquivalents(double sizeInEquivalents) {
		this.sizeInEquivalents = sizeInEquivalents;
	}

	@Override
	public int getPassengerCapacity() {
		return passengerCapacity;
	}

	public void setPassengerCapacity(int passengerCapacity) {
		this.passengerCapacity = passengerCapacity;
	}

	@Override
	public Collection<PassengerAgent> getPassengers() {
		return passengers;
	}

	public void setPassengers(Collection<PassengerAgent> passengers) {
		this.passengers = passengers;
	}

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

	@Override
	public String toString() {
		return "Id: " + this.id + " driver: " + driver.getId() + " exitTime: + " + earliestLinkExitTime;
	}

}
