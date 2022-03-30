package org.matsim.freightDemandGeneration;

import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;

final class NewCarrier {

	private String name;
	private String[] vehilceTypes;
	private int numberOfDepotsPerType;
	private String[] vehicleDepots;
	private String[] areaOfAdditonalDepots;
	private FleetSize fleetSize;
	private int vehicleStartTime;
	private int vehicleEndTime;
	private int jspritIterations;
	private int fixedNumberOfVehilcePerTypeAndLocation;

	public NewCarrier(String name, String[] vehilceTypes, int numberOfDepotsPerType, String[] vehicleDepots,
			String[] areaOfAdditonalDepots, FleetSize fleetSize, int vehicleStartTime, int vehicleEndTime,
			int jspritIterations, int fixedNumberOfVehilcePerTypeAndLocation) {
		this.setId(name);
		this.setVehicleTypes(vehilceTypes);
		this.setNumberOfDepotsPerType(numberOfDepotsPerType);
		this.setVehicleDepots(vehicleDepots);
		this.setAreaOfAdditonalDepots(areaOfAdditonalDepots);
		this.setJspritIterations(jspritIterations);
		this.setFleetSize(fleetSize);
		this.setVehicleStartTime(vehicleStartTime);
		this.setVehicleEndTime(vehicleEndTime);
		this.setFixedNumberOfVehilcePerTypeAndLocation(fixedNumberOfVehilcePerTypeAndLocation);
	}

	public String getName() {
		return name;
	}

	void setId(String name) {
		this.name = name;
	}

	public String[] getVehicleTypes() {
		return vehilceTypes;
	}

	void setVehicleTypes(String[] vehicleTypes) {
		this.vehilceTypes = vehicleTypes;
	}

	public String[] getVehicleDepots() {
		return vehicleDepots;
	}

	public void setVehicleDepots(String[] vehicleDepots) {
		this.vehicleDepots = vehicleDepots;
	}

	public void addVehicleDepots(String[] vehicleDepots, String newDepot) {
		String[] newdepotList = new String[vehicleDepots.length + 1];
		int count = 0;
		for (int cnt = 0; cnt < vehicleDepots.length; cnt++, count++) {
			newdepotList[cnt] = vehicleDepots[cnt];
		}
		newdepotList[count] = newDepot;
		this.vehicleDepots = newdepotList;
	}

	public FleetSize getFleetSize() {
		return fleetSize;
	}

	public void setFleetSize(FleetSize fleetSize) {
		this.fleetSize = fleetSize;
	}

	public int getVehicleStartTime() {
		return vehicleStartTime;
	}

	public void setVehicleStartTime(int carrierStartTime) {
		this.vehicleStartTime = carrierStartTime;
	}

	public int getVehicleEndTime() {
		return vehicleEndTime;
	}

	public void setVehicleEndTime(int vehicleEndTime) {
		this.vehicleEndTime = vehicleEndTime;
	}

	public int getJspritIterations() {
		return jspritIterations;
	}

	public void setJspritIterations(int jspritIterations) {
		this.jspritIterations = jspritIterations;
	}

	public int getNumberOfDepotsPerType() {
		return numberOfDepotsPerType;
	}

	public void setNumberOfDepotsPerType(int numberOfDepotsPerType) {
		this.numberOfDepotsPerType = numberOfDepotsPerType;
	}

	public String[] getAreaOfAdditonalDepots() {
		return areaOfAdditonalDepots;
	}

	public void setAreaOfAdditonalDepots(String[] areaOfAdditonalDepots) {
		this.areaOfAdditonalDepots = areaOfAdditonalDepots;
	}
	
	public int getFixedNumberOfVehilcePerTypeAndLocation() {
		return fixedNumberOfVehilcePerTypeAndLocation;
	}

	public void setFixedNumberOfVehilcePerTypeAndLocation(int fixedNumberOfVehilcePerTypeAndLocation) {
		this.fixedNumberOfVehilcePerTypeAndLocation = fixedNumberOfVehilcePerTypeAndLocation;
	}
	
}
