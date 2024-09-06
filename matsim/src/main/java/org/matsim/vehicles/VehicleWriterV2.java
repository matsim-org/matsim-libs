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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;

/**
 * @author dgrether
 * @author jwjoubert
 * @author kturner
 */

final class VehicleWriterV2 extends MatsimXmlWriter {

	private static final Logger log = LogManager.getLogger(VehicleWriterV2.class);
	private AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();

	private List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();
	private Map<Id<VehicleType>, VehicleType> vehicleTypes;
	private Map<Id<Vehicle>, Vehicle> vehicles;


	public VehicleWriterV2(Vehicles vehicles) {
		this.vehicleTypes = vehicles.getVehicleTypes();
		this.vehicles = vehicles.getVehicles();
	}

	public void writeFile(String filename) throws UncheckedIOException, IOException {
		log.info(Gbl.aboutToWrite("vehicles", filename));
		this.openFile(filename);
		this.writeXmlHead();
		this.writeRootElement();
		this.close();
	}

	private void writeRootElement() throws UncheckedIOException, IOException {
		atts.clear();
		atts.add(this.createTuple(XMLNS, MatsimXmlWriter.MATSIM_NAMESPACE));
		atts.add(this.createTuple(XMLNS + ":xsi", DEFAULTSCHEMANAMESPACELOCATION));
		atts.add(this.createTuple("xsi:schemaLocation", MATSIM_NAMESPACE + " " + DEFAULT_DTD_LOCATION + "vehicleDefinitions_v2.0.xsd"));
		this.writeStartTag(VehicleSchemaV2Names.VEHICLEDEFINITIONS, atts);
		this.writeVehicleTypes(this.vehicleTypes);
		this.writeVehicles(this.vehicles);
		this.writeContent("\n", true);
		this.writeEndTag(VehicleSchemaV2Names.VEHICLEDEFINITIONS);
	}

	private void writeVehicles(Map<Id<Vehicle>, Vehicle> veh) throws UncheckedIOException, IOException {
		List<Vehicle> sortedVehicles = veh.values().stream().sorted(comparing(Vehicle::getId)).collect(toList());
		for (Vehicle v : sortedVehicles) {
			atts.clear();
			atts.add(this.createTuple(VehicleSchemaV2Names.ID, v.getId().toString()));
			atts.add(this.createTuple(VehicleSchemaV2Names.TYPE, v.getType().getId().toString()));

			/* The format of the vehicle depends on whether the particular vehicle has attributes specified. */
			if (v.getAttributes().isEmpty()) {
				this.writeStartTag(VehicleSchemaV2Names.VEHICLE, atts, true);
			} else {
				this.writeStartTag(VehicleSchemaV2Names.VEHICLE, atts, false);
				this.writer.newLine();
				attributesWriter.writeAttributes("\t\t", this.writer, v.getAttributes(), false);
				this.writeEndTag(VehicleSchemaV2Names.VEHICLE);
			}
		}
	}

	private void writeVehicleTypes(Map<Id<VehicleType>, VehicleType> vts) throws UncheckedIOException, IOException {
		this.writer.write("\n");
		List<VehicleType> sortedVehicleTypes = vts.values()
				.stream()
				.sorted(Comparator.comparing(VehicleType::getId))
				.collect(Collectors.toList());
		for (VehicleType vt : sortedVehicleTypes) {
			atts.clear();
			atts.add(this.createTuple(VehicleSchemaV2Names.ID, vt.getId().toString()));
			this.writeStartTag(VehicleSchemaV2Names.VEHICLETYPE, atts);

			//Write general vehicleType attributes
			this.writer.write("\n");
			attributesWriter.writeAttributes("\t\t", this.writer, vt.getAttributes(), false);

			//Write vehicleType description, if present
			if (vt.getDescription() != null) {
				this.writeElement(VehicleSchemaV2Names.DESCRIPTION, vt.getDescription());
			}

			//Write capacity, if present
			if (vt.getCapacity() != null) {
				VehicleCapacity vehicleCapacity = vt.getCapacity();
				atts.clear();
				if (vehicleCapacity.getSeats() != null) {
					atts.add(this.createTuple(VehicleSchemaV2Names.SEATS, vehicleCapacity.getSeats()));
				}
				if (vehicleCapacity.getStandingRoom() != null) {
					atts.add(this.createTuple(VehicleSchemaV2Names.STANDINGROOM, vehicleCapacity.getStandingRoom()));
				}
				if (vehicleCapacity.getVolumeInCubicMeters() != null && !Double.isNaN(vehicleCapacity.getVolumeInCubicMeters()) && !Double.isInfinite(vehicleCapacity.getVolumeInCubicMeters())) {
					atts.add(this.createTuple(VehicleSchemaV2Names.VOLUME, vehicleCapacity.getVolumeInCubicMeters()));
				}
				if (vehicleCapacity.getWeightInTons() != null && !Double.isNaN(vehicleCapacity.getWeightInTons()) && !Double.isInfinite(vehicleCapacity.getWeightInTons())) {
					atts.add(this.createTuple(VehicleSchemaV2Names.WEIGHT, vehicleCapacity.getWeightInTons()));
				}
				if (vehicleCapacity.getOther() != null && !Double.isNaN(vehicleCapacity.getOther()) && !Double.isInfinite(vehicleCapacity.getOther())) {
					atts.add(this.createTuple(VehicleSchemaV2Names.OTHER, vehicleCapacity.getOther()));
				}
				this.writeStartTag(VehicleSchemaV2Names.CAPACITY, atts);
				//attributes for capacity
				this.writer.write("\n");
				attributesWriter.writeAttributes("\t\t\t", this.writer, vehicleCapacity.getAttributes(), false);
				this.writeEndTag(VehicleSchemaV2Names.CAPACITY);
			}

			//Write length, if present
			if (!Double.isNaN(vt.getLength())) {
				atts.clear();
				atts.add(this.createTuple(VehicleSchemaV2Names.METER, Double.toString(vt.getLength())));
				this.writeStartTag(VehicleSchemaV2Names.LENGTH, atts, true);
			}

			//Write width, if present
			if (!Double.isNaN(vt.getWidth())) {
				atts.clear();
				atts.add(this.createTuple(VehicleSchemaV2Names.METER, Double.toString(vt.getWidth())));
				this.writeStartTag(VehicleSchemaV2Names.WIDTH, atts, true);
			}

			//Write maximumVelocity, if present
			if (!Double.isNaN(vt.getMaximumVelocity()) && !Double.isInfinite(vt.getMaximumVelocity())) {
				atts.clear();
				atts.add(this.createTuple(VehicleSchemaV2Names.METERPERSECOND, Double.toString(vt.getMaximumVelocity())));
				this.writeStartTag(VehicleSchemaV2Names.MAXIMUMVELOCITY, atts, true);
			}

			//Write vehicleType engineInformation, if present
			if (vt.getEngineInformation() != null & !vt.getEngineInformation().getAttributes().isEmpty()) {
				atts.clear();
				this.writeStartTag(VehicleSchemaV2Names.ENGINEINFORMATION, atts);
				this.writer.write("\n");
				attributesWriter.writeAttributes("\t\t\t", this.writer, vt.getEngineInformation().getAttributes(), false);
				this.writeEndTag(VehicleSchemaV2Names.ENGINEINFORMATION);
			}

			//Write vehicleType costInformation, if present
			if (vt.getCostInformation() != null) {
				CostInformation costInformation = vt.getCostInformation();
				atts.clear();
				if (costInformation.getFixedCosts() != null && !Double.isNaN(costInformation.getFixedCosts())) {
					atts.add(this.createTuple(VehicleSchemaV2Names.FIXEDCOSTSPERDAY, costInformation.getFixedCosts()));
				}
				if (costInformation.getCostsPerMeter() != null && !Double.isNaN(costInformation.getCostsPerMeter())) {
					atts.add(this.createTuple(VehicleSchemaV2Names.COSTSPERMETER, costInformation.getCostsPerMeter()));
				}
				if (costInformation.getCostsPerSecond() != null && !Double.isNaN(costInformation.getCostsPerSecond())) {
					atts.add(this.createTuple(VehicleSchemaV2Names.COSTSPERSECOND, costInformation.getCostsPerSecond()));
				}
				this.writeStartTag(VehicleSchemaV2Names.COSTINFORMATION, atts);
				//attributes for costInformation
				this.writer.write("\n");
				attributesWriter.writeAttributes("\t\t\t", this.writer, costInformation.getAttributes(), false);
				this.writeEndTag(VehicleSchemaV2Names.COSTINFORMATION);
			}

			//Write passengerCarEquivalents, if present
			if (!Double.isNaN(vt.getPcuEquivalents())) {
				atts.clear();
				atts.add(this.createTuple(VehicleSchemaV2Names.PCE, vt.getPcuEquivalents()));
				this.writeStartTag(VehicleSchemaV2Names.PASSENGERCAREQUIVALENTS, atts, true);
			}

			//Write networkMode, if present
			if (vt.getNetworkMode() != null) {
				atts.clear();
				atts.add(this.createTuple(VehicleSchemaV2Names.NETWORKMODE, vt.getNetworkMode()));
				this.writeStartTag(VehicleSchemaV2Names.NETWORKMODE, atts, true);
			}

			//Write flowEfficiencyFactor, if present
			if (!Double.isNaN(vt.getFlowEfficiencyFactor())) {
				atts.clear();
				atts.add(this.createTuple(VehicleSchemaV2Names.FACTOR, vt.getFlowEfficiencyFactor()));
				this.writeStartTag(VehicleSchemaV2Names.FLOWEFFICIENCYFACTOR, atts, true);
			}

			this.writeEndTag(VehicleSchemaV2Names.VEHICLETYPE);
			this.writer.write("\n");
		}
	}

	public void putAttributeConverters(Map<Class<?>, AttributeConverter<?>> converters) {
		this.attributesWriter.putAttributeConverters(converters);
	}
}
