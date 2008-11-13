/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleDefinitionsWriterV1
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.BasicVehicleCapacity.BasicFreightCapacity;
import org.matsim.utils.collections.Tuple;
import org.matsim.writer.MatsimXmlWriter;


/**
 * @author dgrether
 *
 */
public class VehicleDefinitionsWriterV1 extends MatsimXmlWriter {

	private List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();
	private Map<String, BasicVehicleType> vehicleTypes;
	private List<BasicVehicle> vehicles;

	
	public VehicleDefinitionsWriterV1(Map<String, BasicVehicleType> vehicleTypes, List<BasicVehicle> vehicles) {
		this.vehicleTypes = vehicleTypes;
		this.vehicles = vehicles;
	}
	
	public void writeFile(String filename) throws FileNotFoundException, IOException {
		this.openFile(filename);
		this.writeXmlHead();
		this.writeRootElement();
		this.close();
	}

	private void writeRootElement() throws IOException {
		atts.clear();
		atts.add(this.createTuple(XMLNS, MatsimXmlWriter.MATSIM_NAMESPACE));
		atts.add(this.createTuple(XMLNS + ":xsi", DEFAULTSCHEMANAMESPACELOCATION));
		atts.add(this.createTuple("xsi:schemaLocation", MATSIM_NAMESPACE + " " + DEFAULT_DTD_LOCATION + "vehicleDefinitions_v1.00.xsd"));
		this.writeStartTag(VehicleSchemaV1Names.VEHICLEDEFINITIONS, atts);
		this.writeVehicleTypes(this.vehicleTypes);
		this.writeVehicles(this.vehicles);
		this.writeEndTag(VehicleSchemaV1Names.VEHICLEDEFINITIONS);
	}

	private void writeVehicles(List<BasicVehicle> veh) throws IOException {
		for (BasicVehicle v : veh) {
			atts.clear();
			atts.add(this.createTuple(VehicleSchemaV1Names.ID, v.getId().toString()));
			atts.add(this.createTuple(VehicleSchemaV1Names.TYPE, v.getType()));
			this.writeStartTag(VehicleSchemaV1Names.VEHICLE, atts, true);
		}
	}

	private void writeVehicleTypes(Map<String, BasicVehicleType> vts) throws IOException {
		for (BasicVehicleType vt : vts.values()){
			atts.clear();
			atts.add(this.createTuple(VehicleSchemaV1Names.TYPEID, vt.getTypeId()));
			this.writeStartTag(VehicleSchemaV1Names.VEHICLETYPE, atts);
			if (vt.getDescription() != null) {
				this.writeStartTag(VehicleSchemaV1Names.DESCRIPTION, null);
				this.writeContent(vt.getDescription(), true);
				this.writeEndTag(VehicleSchemaV1Names.DESCRIPTION);
			}
			if (vt.getCapacity() != null) {
				this.writeCapacity(vt.getCapacity());
			}
			if (!Double.isNaN(vt.getLength())){
				atts.clear();
				atts.add(this.createTuple(VehicleSchemaV1Names.METER, Double.toString(vt.getLength())));
				this.writeStartTag(VehicleSchemaV1Names.LENGTH, atts, true);
			}
			if (!Double.isNaN(vt.getWidth())){
				atts.clear();
				atts.add(this.createTuple(VehicleSchemaV1Names.METER, Double.toString(vt.getWidth())));
				this.writeStartTag(VehicleSchemaV1Names.WIDTH, atts, true);
			}
			if (!Double.isNaN(vt.getMaximumVelocity())){
				atts.clear();
				atts.add(this.createTuple(VehicleSchemaV1Names.METERPERSECOND, Double.toString(vt.getMaximumVelocity())));
				this.writeStartTag(VehicleSchemaV1Names.MAXIMUMVELOCITY, atts, true);
			}
			if (vt.getEngineInformation() != null) {
				this.writeEngineInformation(vt.getEngineInformation());
			}
			this.writeEndTag(VehicleSchemaV1Names.VEHICLETYPE);
		}
	}

	private void writeEngineInformation(BasicEngineInformation ei) throws IOException {
		this.writeStartTag(VehicleSchemaV1Names.ENGINEINFORMATION, null);
		this.writeStartTag(VehicleSchemaV1Names.FUELTYPE, null);
		this.writeContent(ei.getFuelType().toString(), false);
		this.writeEndTag(VehicleSchemaV1Names.FUELTYPE);
		atts.clear();
		atts.add(this.createTuple(VehicleSchemaV1Names.LITERPERMETER, Double.toString(ei.getGasConsumption())));
		this.writeStartTag(VehicleSchemaV1Names.GASCONSUMPTION, atts, true);
		this.writeEndTag(VehicleSchemaV1Names.ENGINEINFORMATION);
	}

	private void writeCapacity(BasicVehicleCapacity cap) throws IOException {
		this.writeStartTag(VehicleSchemaV1Names.CAPACITY, null);
		if (cap.getSeats() != null) {
			atts.clear();
			atts.add(this.createTuple(VehicleSchemaV1Names.PERSONS, cap.getSeats()));
			this.writeStartTag(VehicleSchemaV1Names.SEATS, atts, true);
		}
		if (cap.getStandingRoom() != null) {
			atts.clear();
			atts.add(this.createTuple(VehicleSchemaV1Names.PERSONS, cap.getStandingRoom()));
			this.writeStartTag(VehicleSchemaV1Names.STANDINGROOM, atts, true);
		}
		if (cap.getFreightCapacity() != null) {
			this.writeFreightCapacity(cap.getFreightCapacity());
		}
		this.writeEndTag(VehicleSchemaV1Names.CAPACITY);
	}

	private void writeFreightCapacity(BasicFreightCapacity fc) throws IOException {
		this.writeStartTag(VehicleSchemaV1Names.FREIGHTCAPACITY, null);
		atts.clear();
		atts.add(this.createTuple(VehicleSchemaV1Names.CUBICMETERS, Double.toString(fc.getVolume())));
		this.writeStartTag(VehicleSchemaV1Names.VOLUME, atts, true);
		this.writeEndTag(VehicleSchemaV1Names.FREIGHTCAPACITY);
	}
	
}
