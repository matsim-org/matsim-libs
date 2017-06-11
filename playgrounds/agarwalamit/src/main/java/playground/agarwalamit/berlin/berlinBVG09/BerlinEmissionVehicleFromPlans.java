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
import playground.agarwalamit.utils.FileUtils;
import playground.benjamin.scenarios.manteuffel.ManteuffelEmissionVehicleGenerator;

/**
 * @author benjamin (after {@link ManteuffelEmissionVehicleGenerator}
 *
 */
public class BerlinEmissionVehicleFromPlans {
	private static final Logger logger = Logger.getLogger(BerlinEmissionVehicleFromEvents.class);

	private final String populationFile = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/ITERS/it.100/bvg.run189.10pct.100.plans.filtered.selected.xml.gz";
	private final String netFile = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/rev554B-bvg00-0.1sample.network_withRoadTypes.xml";
	private final String transitVehicleFile = FileUtils.SHARED_SVN+"/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/transitVehicles.final.xml.gz";
	private final String transitScheduleFile = FileUtils.SHARED_SVN+"/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/transitSchedule.xml.gz";

	private final String outputVehicleFile = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/bvg.run189.10pct.100.emissionVehicle.xml.gz";

	private void run() {
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(netFile);
		config.transit().setVehiclesFile(transitVehicleFile);
		config.transit().setTransitScheduleFile(transitScheduleFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		logger.error("Transit vehicles are completely ignored.");

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
		
		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(outputVehicles);
		vehicleWriter.writeFile(outputVehicleFile);
	}

	public static void main(String[] args) {
		BerlinEmissionVehicleFromPlans evg = new BerlinEmissionVehicleFromPlans();
		evg.run();
	}
}