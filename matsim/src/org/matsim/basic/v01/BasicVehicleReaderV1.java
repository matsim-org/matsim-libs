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

package org.matsim.basic.v01;

import java.io.IOException;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.BasicEngineInformation.FuelType;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 */
public class BasicVehicleReaderV1 extends MatsimXmlParser {

	private VehicleBuilder builder;
	private BasicVehicleType currentVehType;
	private BasicVehicleCapacity currentCapacity;
	private BasicFreightCapacity currentFreightCap;
	private BasicEngineInformation.FuelType currentFuelType;
	private double currentGasConsumption;
	
	public BasicVehicleReaderV1(VehicleBuilder vb) {
		this.builder = vb;
	}
	
	
	public BasicVehicleReaderV1(Map<String, BasicVehicleType> vehicleTypes, Map<Id, BasicVehicle> vehicles) {
		this.builder = new BasicVehicleBuilder(vehicleTypes, vehicles);
	}
	
	public void readFile(String filename) {
		try {
			parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (VehicleSchemaV1Names.DESCRIPTION.equalsIgnoreCase(name) && (content.trim().length() > 0)){
			this.currentVehType.setDescription(content.trim());
		}
		else if (VehicleSchemaV1Names.ENGINEINFORMATION.equalsIgnoreCase(name)){
			BasicEngineInformation currentEngineInfo = this.builder.createEngineInformation(currentFuelType, currentGasConsumption);
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
			this.currentVehType = null;
		}
	
	}

	private FuelType parseFuelType(String content) {
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

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (VehicleSchemaV1Names.VEHICLETYPE.equalsIgnoreCase(name)) {
			this.currentVehType = this.builder.createVehicleType(atts.getValue(VehicleSchemaV1Names.TYPEID));			
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
			this.builder.createVehicle(new IdImpl(atts.getValue(VehicleSchemaV1Names.ID)), atts.getValue(VehicleSchemaV1Names.TYPE));
		}
	}
		
}
