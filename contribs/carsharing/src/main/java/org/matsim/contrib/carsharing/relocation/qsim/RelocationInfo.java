package org.matsim.contrib.carsharing.relocation.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Time;

public class RelocationInfo {
	private String timeSlot;
	private String companyId;
	private String vehicleId;
	private Id<Link> startLinkId;
	private Id<Link> endLinkId;
	private String startZoneId;
	private String endZoneId;
	private double startTime;
	private double endTime;
	private Id<Person> agentId;

	public RelocationInfo(String timeSlot, String companyId, String vehicleId, Id<Link> startLinkId, Id<Link> endLinkId, String startZoneId, String endZoneId) {
		this.timeSlot		= timeSlot;
		this.companyId		= companyId;
		this.vehicleId 		= vehicleId;
		this.startLinkId 	= startLinkId;
		this.endLinkId 		= endLinkId;
		this.startZoneId	= startZoneId;
		this.endZoneId		= endZoneId;
	}

	public String getCompanyId() {
		return this.companyId;
	}

	public String getVehicleId() {
		return this.vehicleId;
	}

	public Id<Link> getStartLinkId() {
		return this.startLinkId;
	}

	public Id<Link> getEndLinkId() {
		return this.endLinkId;
	}

	public String getStartZoneId() {
		return this.startZoneId;
	}

	public String getEndZoneId() {
		return this.endZoneId;
	}

	public String getTimeSlot() {
		return this.timeSlot;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public double getStartTime() {
		return this.startTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public double getEndTime() {
		return this.endTime;
	}

	public void setAgentId(Id<Person> id) {
		this.agentId = id;
	}

	public Id<Person> getAgentId() {
		return this.agentId;
	}

	public String toString() {
		return this.getTimeSlot() + "	" +
				this.getStartZoneId() + "	" +
				this.getEndZoneId() + "	" +
				Time.writeTime(this.getStartTime()) + "	" +
				Time.writeTime(this.getEndTime()) + "	" +
				(this.getStartLinkId() == null ? "null" : this.getStartLinkId().toString()) + "	" +
				(this.getEndLinkId() == null ? "null" : this.getEndLinkId().toString()) + "	" +
				this.getCompanyId() + "	" +
				this.getVehicleId() + "	" +
				(this.getAgentId() == null ? "null" : this.getAgentId().toString());
	}
}
