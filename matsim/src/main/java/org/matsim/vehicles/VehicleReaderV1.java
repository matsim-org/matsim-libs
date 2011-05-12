/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.vehicles;

import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.VehicleType.DoorOperationMode;
import org.xml.sax.Attributes;

/**
 * @author dgrether
 */
public class VehicleReaderV1 extends MatsimXmlParser {

	private final Vehicles vehicles;
	private final VehiclesFactory builder;
	private VehicleType currentVehType = null;
	private VehicleCapacity currentCapacity = null;
	private FreightCapacity currentFreightCap = null;
	private EngineInformation.FuelType currentFuelType = null;
	private double currentGasConsumption = Double.NaN;

	public VehicleReaderV1(final Vehicles vehicles) {
		this.vehicles = vehicles;
		this.builder = this.vehicles.getFactory();
	}

	public void readFile(final String filename) throws UncheckedIOException {
		parse(filename);
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (VehicleSchemaV1Names.DESCRIPTION.equalsIgnoreCase(name) && (content.trim().length() > 0)){
			this.currentVehType.setDescription(content.trim());
		}
		else if (VehicleSchemaV1Names.ENGINEINFORMATION.equalsIgnoreCase(name)){
			EngineInformation currentEngineInfo = this.builder.createEngineInformation(this.currentFuelType, this.currentGasConsumption);
			this.currentVehType.setEngineInformation(currentEngineInfo);
			this.currentFuelType = null;
			this.currentGasConsumption = Double.NaN;
		}
		else if (VehicleSchemaV1Names.FUELTYPE.equalsIgnoreCase(name)){
			this.currentFuelType = this.parseFuelType(content.trim());
		}
		else if (VehicleSchemaV1Names.FREIGHTCAPACITY.equalsIgnoreCase(name)){
			this.currentCapacity.setFreightCapacity(this.currentFreightCap);
			this.currentFreightCap = null;
		}
		else if (VehicleSchemaV1Names.CAPACITY.equalsIgnoreCase(name)) {
			this.currentVehType.setCapacity(this.currentCapacity);
			this.currentCapacity = null;
		}
		else if (VehicleSchemaV1Names.VEHICLETYPE.equalsIgnoreCase(name)){
			this.vehicles.getVehicleTypes().put(this.currentVehType.getId(), this.currentVehType);
			this.currentVehType = null;
		}

	}

	private FuelType parseFuelType(final String content) {
		if (FuelType.gasoline.toString().equalsIgnoreCase(content)){
			return FuelType.gasoline;
		}
		else if (FuelType.diesel.toString().equalsIgnoreCase(content)){
			return FuelType.diesel;
		}
		else if (FuelType.electricity.toString().equalsIgnoreCase(content)){
			return FuelType.electricity;
		}
		else if (FuelType.biodiesel.toString().equalsIgnoreCase(content)){
			return FuelType.biodiesel;
		}
		else {
			throw new IllegalArgumentException("Fuel type: " + content + " is not supported!");
		}
	}
	
	private DoorOperationMode parseDoorOperationMode(final String modeString){
		if (DoorOperationMode.serial.toString().equalsIgnoreCase(modeString)){
			return DoorOperationMode.serial;
		}
		else if (DoorOperationMode.parallel.toString().equalsIgnoreCase(modeString)){
			return DoorOperationMode.parallel;
		}
		else {
			throw new IllegalArgumentException("Door operation mode " + modeString + " is not supported");
		}
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (VehicleSchemaV1Names.VEHICLETYPE.equalsIgnoreCase(name)) {
			this.currentVehType = this.builder.createVehicleType(new IdImpl(atts.getValue(VehicleSchemaV1Names.ID)));
		}
		else if (VehicleSchemaV1Names.LENGTH.equalsIgnoreCase(name)){
			this.currentVehType.setLength(Double.parseDouble(atts.getValue(VehicleSchemaV1Names.METER)));
		}
		else if (VehicleSchemaV1Names.WIDTH.equalsIgnoreCase(name)) {
			this.currentVehType.setWidth(Double.parseDouble(atts.getValue(VehicleSchemaV1Names.METER)));
		}
		else if (VehicleSchemaV1Names.MAXIMUMVELOCITY.equalsIgnoreCase(name)) {
			this.currentVehType.setMaximumVelocity(Double.parseDouble(atts.getValue(VehicleSchemaV1Names.METERPERSECOND)));
		}
		else if (VehicleSchemaV1Names.CAPACITY.equalsIgnoreCase(name)) {
			this.currentCapacity = this.builder.createVehicleCapacity();
		}
		else if(VehicleSchemaV1Names.SEATS.equalsIgnoreCase(name)){
			this.currentCapacity.setSeats(Integer.valueOf(atts.getValue(VehicleSchemaV1Names.PERSONS)));
		}
		else if (VehicleSchemaV1Names.STANDINGROOM.equalsIgnoreCase(name)){
			this.currentCapacity.setStandingRoom(Integer.valueOf(atts.getValue(VehicleSchemaV1Names.PERSONS)));
		}
		else if (VehicleSchemaV1Names.FREIGHTCAPACITY.equalsIgnoreCase(name)){
			this.currentFreightCap = this.builder.createFreigthCapacity();
		}
		else if (VehicleSchemaV1Names.VOLUME.equalsIgnoreCase(name)){
			this.currentFreightCap.setVolume(Double.parseDouble(atts.getValue(VehicleSchemaV1Names.CUBICMETERS)));
		}
		else if (VehicleSchemaV1Names.GASCONSUMPTION.equalsIgnoreCase(name)){
			this.currentGasConsumption = Double.parseDouble(atts.getValue(VehicleSchemaV1Names.LITERPERMETER));
		}
		else if (VehicleSchemaV1Names.VEHICLE.equalsIgnoreCase(name)){
			Id typeId = new IdImpl(atts.getValue(VehicleSchemaV1Names.TYPE));
			VehicleType type = this.vehicles.getVehicleTypes().get(typeId);
			String idString = atts.getValue(VehicleSchemaV1Names.ID);
			Id id = new IdImpl(idString);
			Vehicle v = this.builder.createVehicle(id, type);
			this.vehicles.getVehicles().put(id, v);
		}
		else if (VehicleSchemaV1Names.ACCESSTIME.equalsIgnoreCase(name)){
		  this.currentVehType.setAccessTime(Double.parseDouble(atts.getValue(VehicleSchemaV1Names.SECONDSPERPERSON)));
		}
		else if (VehicleSchemaV1Names.EGRESSTIME.equalsIgnoreCase(name)){
		  this.currentVehType.setEgressTime(Double.parseDouble(atts.getValue(VehicleSchemaV1Names.SECONDSPERPERSON)));
		}
		else if (VehicleSchemaV1Names.DOOROPERATION.equalsIgnoreCase(name)){
			this.currentVehType.setDoorOperationMode(this.parseDoorOperationMode(atts.getValue(VehicleSchemaV1Names.MODE)));
		}
	}

}
