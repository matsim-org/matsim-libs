/* *********************************************************************** *
 * project: org.matsim.*
 * ManteuffelEmissionVehicleGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.agarwalamit.berlin.berlinBVG09;

import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.types.HbefaVehicleAttributes;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionSpecificationMarker;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.vehicles.*;
import playground.agarwalamit.utils.FileUtils;
import playground.benjamin.scenarios.manteuffel.ManteuffelEmissionVehicleGenerator;

/**
 * @author benjamin (after {@link ManteuffelEmissionVehicleGenerator}
 *
 */
public class BerlinEmissionVehicleFromEvents implements VehicleEntersTrafficEventHandler, TransitDriverStartsEventHandler {
	private static final Logger LOGGER = Logger.getLogger(BerlinEmissionVehicleFromEvents.class);

	private final BerlinTransitVehicleTypeIdentifier berlinTransitVehicleTypeIdentifier;
	private final Map<Id<Person>,Id<Vehicle>> transitDriver2TransitVehicle = new HashMap<>();
	private final Vehicles outputVehicles;

	//BEGIN_EXAMPLE

	public static void main(String[] args) {
		String transitVehicleFile = FileUtils.SHARED_SVN+"/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/transitVehicles.final.xml.gz";
		String convertedEventsFile = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/bvg.run189.10pct.100.eventsWithNetworkModeInEvents.xml.gz";
		String outputVehiclesFile = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/bvg.run189.10pct.100.emissionVehicle.xml.gz";

		EventsManager eventsManager = EventsUtils.createEventsManager();
		BerlinEmissionVehicleFromEvents emissionVehicleFromEvents = new BerlinEmissionVehicleFromEvents(transitVehicleFile);
		eventsManager.addHandler(emissionVehicleFromEvents);
		new MatsimEventsReader(eventsManager).readFile(convertedEventsFile);

		new VehicleWriterV1(emissionVehicleFromEvents.getVehicles()).writeFile(outputVehiclesFile);
	}

	//END_EXAMPLE

	public BerlinEmissionVehicleFromEvents (final String transitVehiclesFile) {
		LOGGER.info("identifying different transit vehicle types from transit vehicle file.");
		berlinTransitVehicleTypeIdentifier = new BerlinTransitVehicleTypeIdentifier(transitVehiclesFile);
		outputVehicles = VehicleUtils.createVehiclesContainer();
	}

	private HbefaVehicleCategory getHBEFAVehicleCategory(Id<Vehicle> vehicleId) {
		HbefaVehicleCategory vehicleCategory = null;
		if(vehicleId.toString().startsWith("b")){ //these are Berliners
			vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
		}
		else if(vehicleId.toString().startsWith("u")){ //these are Brandenburgers
			vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
		}
		else if(vehicleId.toString().startsWith("tmiv")){// these are tourists car users
			vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
		}
		else if(vehicleId.toString().startsWith("fhmiv")){// these are car users driving to/from airport
			vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
		}
		else if(vehicleId.toString().startsWith("toev")){// these are tourist transit users, they dont need a vehicle
			LOGGER.warn("person id: " + vehicleId + " is not considered yet. No emission vehicle for this person will be generated.");
		}
		else if(vehicleId.toString().startsWith("fhoev")){// these are transit users driving to/from airport
			LOGGER.warn("person id: " + vehicleId + " is not considered yet. No emission vehicle for this person will be generated.");
		}
		else if(vehicleId.toString().startsWith("fernoev")){// these are DB transit users
			LOGGER.warn("person id: " + vehicleId + " is not considered yet. No emission vehicle for this person will be generated.");
		}
		else if(vehicleId.toString().startsWith("wv")){// this should be commercial transport -- vehicle type unclear; more likely a PASSENGER_CAR; TODO: CHK!
			vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
		}
		else if(vehicleId.toString().startsWith("lkw")){// these are HDVs
			vehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
		}
		else {
			LOGGER.warn("person id: " + vehicleId + " is not considered yet. No emission vehicle for this person will be generated.");
			// throw new RuntimeException("This case is not considered yet...");
		}
		if (vehicleCategory == null) throw new RuntimeException("not implemented yet.");
		else return vehicleCategory;
	}

	private void createAndAddVehicle(final HbefaVehicleCategory vehicleCategory, final Id<Vehicle> vehicleId){
		HbefaVehicleAttributes vehicleAttributes = new HbefaVehicleAttributes();
		Id<VehicleType> vehTypeId = Id.create(vehicleCategory + ";" +
				vehicleAttributes.getHbefaTechnology() + ";" +
				vehicleAttributes.getHbefaSizeClass() + ";" +
				vehicleAttributes.getHbefaEmConcept(), VehicleType.class);
		VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(vehTypeId);
		vehicleType.setDescription(EmissionSpecificationMarker.BEGIN_EMISSIONS+vehTypeId.toString()+EmissionSpecificationMarker.END_EMISSIONS);

		if(!(outputVehicles.getVehicleTypes().containsKey(vehTypeId))){
			outputVehicles.addVehicleType(vehicleType);
		}

		Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId, vehicleType);
		outputVehicles.addVehicle(vehicle);
	}

	public Vehicles getVehicles () {
		return this.outputVehicles;
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) { // driver id and vehicle id will be different here
		// NOTE: in all subsequent events, this driver id is reported as vehicle id which is not exactly true.
		this.transitDriver2TransitVehicle.put(event.getDriverId(), event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) { // driver id and vehicle id will be same
		if (this.transitDriver2TransitVehicle.containsKey(event.getPersonId())) { // a transit driver, create a vehicle id for him
			Id<Vehicle> vehicleId = this.transitDriver2TransitVehicle.get(event.getPersonId()); // this is not same as the vehicle id in the event
			HbefaVehicleCategory hbefaVehicleCategory = this.berlinTransitVehicleTypeIdentifier.getHBEFAVehicleCategory(vehicleId);
			if (! this.outputVehicles.getVehicles().containsKey(event.getVehicleId()) ) {
				createAndAddVehicle(hbefaVehicleCategory, event.getVehicleId());
			}
		} else {
			HbefaVehicleCategory hbefaVehicleCategory = getHBEFAVehicleCategory(event.getVehicleId());
			if (! this.outputVehicles.getVehicles().containsKey(event.getVehicleId()) ) {
				createAndAddVehicle(hbefaVehicleCategory, event.getVehicleId());
			}
		}
	}

	@Override
	public void reset(int iteration) {
		this.transitDriver2TransitVehicle.clear();
	}
}