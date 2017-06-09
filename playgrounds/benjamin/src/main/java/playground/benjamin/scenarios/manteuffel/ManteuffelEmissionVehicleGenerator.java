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
package playground.benjamin.scenarios.manteuffel;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.types.HbefaVehicleAttributes;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionSpecificationMarker;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.*;


/**
 * @author benjamin
 *
 */
public class ManteuffelEmissionVehicleGenerator {
	private static final Logger logger = Logger.getLogger(ManteuffelEmissionVehicleGenerator.class);
	
//	private final String populationFile = "../../runs-svn/manteuffelstrasse/bau/bvg.run190.25pct.dilution001.network20150727.v2.static.output_plans.xml.gz";
//	private final String netFile = "../../runs-svn/manteuffelstrasse/bau/bvg.run190.25pct.dilution001.network20150727.v2.static.output_network.xml.gz";
//	private final String transitVehicleFile = "../../runs-svn/manteuffelstrasse/bau/bvg.run190.25pct.dilution001.network20150727.v2.static.output_transitVehicles.xml.gz";
//	private final String transitScheduleFile = "../../runs-svn/manteuffelstrasse/bau/bvg.run190.25pct.dilution001.network20150727.v2.static.output_transitSchedule.xml.gz";
//	private final String eventsFile = "../../runs-svn/manteuffelstrasse/bau/ITERS/it.30/bvg.run190.25pct.dilution001.network20150727.v2.static.30.events.xml.gz";
//	
//	private final String outputVehicleFile = "../../runs-svn/manteuffelstrasse/bau/bvg.run190.25pct.dilution001.network20150727.v2.static.emissionVehicles.xml.gz";

	private final String populationFile = "../../../runs-svn/berlin-an-time/input/population_1agent.xml";
	private final String netFile = "../../../runs-svn/berlin-an-time/input/network_withRoadTypes.xml";
	private final String transitVehicleFile = null;
	private final String transitScheduleFile = null;
	private final String eventsFile = null;
	
	private final String outputVehicleFile = "../../../runs-svn/berlin-an-time/input/population_1agent.emissionVehicle.xml.gz";
	

	private void run() {
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(netFile);
		config.transit().setVehiclesFile(transitVehicleFile);
		config.transit().setTransitScheduleFile(transitScheduleFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Vehicles outputVehicles = VehicleUtils.createVehiclesContainer();
		
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			Id<Person> personId = person.getId();
			Id<Vehicle> vehicleId = Id.create(personId, Vehicle.class); //TODO: this should be rather the vehicle, not the person; re-think EmissionModule!

			HbefaVehicleCategory vehicleCategory = null;
			HbefaVehicleAttributes vehicleAttributes = null;
			
			boolean isCreateVehicle = true;

			if(personId.toString().startsWith("b")){ //these are Berliners
				vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
			}
			else if(personId.toString().startsWith("u")){ //these are Brandenburgers
				vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
			}
			else if(personId.toString().startsWith("tmiv")){// these are tourists car users
				vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
			}
			else if(personId.toString().startsWith("fhmiv")){// these are car users driving to/from airport
				vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
			}
			else if(personId.toString().startsWith("toev")){// these are tourist transit users, they dont need a vehicle
				isCreateVehicle = false;
			}
			else if(personId.toString().startsWith("fhoev")){// these are transit users driving to/from airport
				isCreateVehicle = false;
			}
			else if(personId.toString().startsWith("fernoev")){// these are DB transit users
				isCreateVehicle = false;
			}
			else if(personId.toString().startsWith("wv")){// this should be commercial transport -- vehicle type unclear; more likely a PASSENGER_CAR; TODO: CHK!
				vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
			}
			else if(personId.toString().startsWith("lkw")){// these are HDVs
				vehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
			}	
			else {
				logger.warn("person id: " + personId + " is not considered yet. No emission vehicle for this person will be generated.");
				isCreateVehicle = false;
				// throw new RuntimeException("This case is not considered yet...");
			}

			if(isCreateVehicle){
				vehicleAttributes = new HbefaVehicleAttributes();
				Id<VehicleType> vehTypeId = Id.create(vehicleCategory + ";" + 
						vehicleAttributes.getHbefaTechnology() + ";" + 
						vehicleAttributes.getHbefaSizeClass() + ";" + 
						vehicleAttributes.getHbefaEmConcept(), VehicleType.class);
				VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(vehTypeId);

				// either set following or use switch in EmissionConfigGroup to use vehicle id for vehicle description. Amit sep 2016
//				vehicleType.setDescription(vehTypeId.toString());
				vehicleType.setDescription(EmissionSpecificationMarker.BEGIN_EMISSIONS+vehTypeId.toString()+EmissionSpecificationMarker.END_EMISSIONS);

				if(!(outputVehicles.getVehicleTypes().containsKey(vehTypeId))){
					outputVehicles.addVehicleType(vehicleType);
				} else {
					// do nothing
				}
				
				Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId, vehicleType);
				outputVehicles.addVehicle(vehicle);
			}
		}
		
		//===
//		Map<Id<TransitLine>, TransitLine> transitLines = scenario.getTransitSchedule().getTransitLines();
//		EventsManager eventsManager = EventsUtils.createEventsManager();
//		EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
//		eventsManager.addHandler(new ManteuffelLinkLeaveHandler(outputVehicles, transitLines));
//		reader.readFile(eventsFile);
		
		//=== TODO: fix this?
//		List<String> nonCondideredPersons = new ArrayList<>();
//		nonCondideredPersons.add("pt_tr_35415_1000");
//		nonCondideredPersons.add("pt_tr_35416_1000");
//		nonCondideredPersons.add("pt_tr_35417_1000");
//		nonCondideredPersons.add("pt_tr_35418_1000");
//		
//		nonCondideredPersons.add("pt_tr_35415r_1000");
//		nonCondideredPersons.add("pt_tr_35416r_1000");
//		nonCondideredPersons.add("pt_tr_35417r_1000");
//		nonCondideredPersons.add("pt_tr_35418r_1000");
//		
//		for(String stringId : nonCondideredPersons){
//			vehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
//			vehicleAttributes = new HbefaVehicleAttributes();
//			Id<Vehicle> vehicleId = Id.create(stringId, Vehicle.class);
//			Id<VehicleType> vehTypeId = Id.create(vehicleCategory + ";" + 
//					vehicleAttributes.getHbefaTechnology() + ";" + 
//					vehicleAttributes.getHbefaSizeClass() + ";" + 
//					vehicleAttributes.getHbefaEmConcept(), VehicleType.class);
//			VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(vehTypeId);
//
//			if(!(outputVehicles.getVehicleTypes().containsKey(vehTypeId))){
//				outputVehicles.addVehicleType(vehicleType);
//			} else {
//				// do nothing
//			}
//			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId, vehicleType);
//			outputVehicles.addVehicle(vehicle);
//		}
		
//		for(Vehicle transveh : scenario.getTransitVehicles().getVehicles().values()){ // all transit vehicles are for now HDV; TODO: CHK!
//			Id <Vehicle> transvehId = transveh.getId();
//			
//			vehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
//			vehicleAttributes = new HbefaVehicleAttributes();
//			
//			Id<VehicleType> vehTypeId = Id.create(vehicleCategory + ";" + 
//									  vehicleAttributes.getHbefaTechnology() + ";" + 
//									  vehicleAttributes.getHbefaSizeClass() + ";" + 
//									  vehicleAttributes.getHbefaEmConcept(), VehicleType.class);
//			VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(vehTypeId);
//			
//			if(!(outputVehicles.getVehicleTypes().containsKey(vehTypeId))){
//				outputVehicles.addVehicleType(vehicleType);
//			} else {
//				// do nothing
//			}
//			
//			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(transvehId, vehicleType);
//			outputVehicles.addVehicle( vehicle);
//		}
		
		//===
		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(outputVehicles);
		vehicleWriter.writeFile(outputVehicleFile);
	}

	public static void main(String[] args) {
		ManteuffelEmissionVehicleGenerator evg = new ManteuffelEmissionVehicleGenerator();
		evg.run();
	}
}