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
package org.matsim.vehicles;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

/**
 * @author dgrether
 * @author jwjoubert
 */
public final class VehicleWriterV1 extends MatsimXmlWriter {

	private static final Logger log = LogManager.getLogger(VehicleWriterV1.class);

	private List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();
	private Map<Id<VehicleType>, VehicleType> vehicleTypes;
	private Map<Id<Vehicle>, Vehicle> vehicles;

	/**
	 * @deprecated Please use {@link MatsimVehicleWriter} instead.
	 */
	@Deprecated
	public VehicleWriterV1(Vehicles vehicles) {
		log.warn("VehiclesV1 is deprecated; Please use MatsimVehicleWriter to always write with the currently supported version");
		this.vehicleTypes = vehicles.getVehicleTypes();
		this.vehicles = vehicles.getVehicles();
	}

	public void writeFile(String filename) throws UncheckedIOException {
		log.info( Gbl.aboutToWrite( "vehicles", filename) ) ;
		this.openFile(filename);
		this.writeXmlHead();
		this.writeRootElement();
		this.close();
	}

	private void writeRootElement() throws UncheckedIOException {
		atts.clear();
		atts.add(this.createTuple(XMLNS, MatsimXmlWriter.MATSIM_NAMESPACE));
		atts.add(this.createTuple(XMLNS + ":xsi", DEFAULTSCHEMANAMESPACELOCATION));
		atts.add(this.createTuple("xsi:schemaLocation", MATSIM_NAMESPACE + " " + DEFAULT_DTD_LOCATION + "vehicleDefinitions_v1.0.xsd"));
		this.writeStartTag(VehicleSchemaV1Names.VEHICLEDEFINITIONS, atts);
		this.writeVehicleTypes(this.vehicleTypes);
		this.writeVehicles(this.vehicles);
		this.writeEndTag(VehicleSchemaV1Names.VEHICLEDEFINITIONS);
	}

	private void writeVehicles(Map<Id<Vehicle>, Vehicle> veh) throws UncheckedIOException {
		List<Vehicle> sortedVehicles = veh.values().stream().sorted(comparing(Vehicle::getId)).collect(toList());
		for (Vehicle v : sortedVehicles) {
			atts.clear();
			atts.add(this.createTuple(VehicleSchemaV1Names.ID, v.getId().toString()));
			atts.add(this.createTuple(VehicleSchemaV1Names.TYPE, v.getType().getId().toString()));
			this.writeStartTag(VehicleSchemaV1Names.VEHICLE, atts, true);
		}
	}

	private void writeVehicleTypes(Map<Id<VehicleType>, VehicleType> vts) throws UncheckedIOException {
		List<VehicleType> sortedVehicleTypes = vts.values()
				.stream()
				.sorted(comparing(VehicleType::getId))
				.collect(toList());
		for (VehicleType vt : sortedVehicleTypes) {
			atts.clear();
			atts.add(this.createTuple(VehicleSchemaV1Names.ID, vt.getId().toString()));
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
			if (!Double.isNaN(vt.getMaximumVelocity()) && !Double.isInfinite(vt.getMaximumVelocity())){
				atts.clear();
				atts.add(this.createTuple(VehicleSchemaV1Names.METERPERSECOND, Double.toString(vt.getMaximumVelocity())));
				this.writeStartTag(VehicleSchemaV1Names.MAXIMUMVELOCITY, atts, true);
			}
			if (vt.getEngineInformation() != null && !vt.getEngineInformation().getAttributes().isEmpty()) {
				log.info("try to write EngineInformation for vehType" + vt.getId());
				this.writeEngineInformation(vt.getEngineInformation());
			}
			atts.clear();
			atts.add(this.createTuple(VehicleSchemaV1Names.SECONDSPERPERSON, VehicleUtils.getAccessTime(vt)));
			this.writeStartTag(VehicleSchemaV1Names.ACCESSTIME, atts, true);
			atts.clear();
			atts.add(this.createTuple(VehicleSchemaV1Names.SECONDSPERPERSON, VehicleUtils.getEgressTime(vt)));
      this.writeStartTag(VehicleSchemaV1Names.EGRESSTIME, atts, true);
      atts.clear();
			atts.add(this.createTuple(VehicleSchemaV1Names.MODE, VehicleUtils.getDoorOperationMode(vt).toString()));
      this.writeStartTag(VehicleSchemaV1Names.DOOROPERATION, atts, true);
      atts.clear();
      atts.add(this.createTuple(VehicleSchemaV1Names.PCE, vt.getPcuEquivalents()));
      this.writeStartTag(VehicleSchemaV1Names.PASSENGERCAREQUIVALENTS, atts, true);
			this.writeEndTag(VehicleSchemaV1Names.VEHICLETYPE);
		}
	}

	private void writeEngineInformation( EngineInformation ei ) throws UncheckedIOException {
		this.writeStartTag(VehicleSchemaV1Names.ENGINEINFORMATION, null);
		this.writeStartTag(VehicleSchemaV1Names.FUELTYPE, null);
//		this.writeContent(ei.getFuelType().toString(), false);
		if (VehicleUtils.getFuelType(ei) != null) {
			this.writeContent(VehicleUtils.getFuelType(ei).toString(), false);
		}
		this.writeEndTag(VehicleSchemaV1Names.FUELTYPE);
		atts.clear();
//		log.warn("EngineInformation: " + ei + " fc: " + ei.getFuelConsumption());
		if((VehicleUtils.getFuelConsumption(ei) != null)) {
			atts.add(this.createTuple(VehicleSchemaV1Names.LITERPERMETER, VehicleUtils.getFuelConsumption(ei)));
			this.writeStartTag(VehicleSchemaV1Names.GASCONSUMPTION, atts, true);
		}
		this.writeEndTag(VehicleSchemaV1Names.ENGINEINFORMATION);
	}

	private void writeCapacity(VehicleCapacity cap) throws UncheckedIOException {
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
		if( cap.getVolumeInCubicMeters() != null && !Double.isInfinite(cap.getVolumeInCubicMeters())) {
			this.writeFreightCapacity( cap.getVolumeInCubicMeters() );
		}
		this.writeEndTag(VehicleSchemaV1Names.CAPACITY);
	}

	private void writeFreightCapacity(double fc) throws UncheckedIOException {
		this.writeStartTag(VehicleSchemaV1Names.FREIGHTCAPACITY, null);
		atts.clear();
		atts.add(this.createTuple(VehicleSchemaV1Names.CUBICMETERS, Double.toString(fc)));
		this.writeStartTag(VehicleSchemaV1Names.VOLUME, atts, true);
		this.writeEndTag(VehicleSchemaV1Names.FREIGHTCAPACITY);
	}

}
