/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionVehicleGenerator.java
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
package playground.benjamin.scenarios.munich;

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
public class MunichEmissionVehicleGenerator {
	private static final Logger logger = Logger.getLogger(MunichEmissionVehicleGenerator.class);
	
//	private final String populationFile = "../../detailedEval/pop/merged/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4.xml.gz";
//	private final String inputVehicleFile = "../../detailedEval/pop/14k-synthetische-personen/vehicles.xml.gz";
//	private final String outputVehicleFile = "../../detailedEval/pop/merged/emissionVehicles_1pct.xml.gz";
	
	private final String populationFile = "../../detailedEval/pop/merged/mergedPopulation_All_10pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4.xml.gz";
	private final String inputVehicleFile = "../../detailedEval/pop/140k-synthetische-personen/vehicles.xml.gz";
	private final String outputVehicleFile = "../../detailedEval/pop/merged/emissionVehicles_10pct.xml.gz";
	
	private final String netFile = "../../detailedEval/Net/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml";

	Scenario scenario;
	Vehicles inputVehicles;
	
	public static void main(String[] args) {
		MunichEmissionVehicleGenerator evg = new MunichEmissionVehicleGenerator();
		evg.run();
	}

	private void run() {
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(netFile);
		this.scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(this.scenario);
		
		this.inputVehicles = VehicleUtils.createVehiclesContainer();
		VehicleReaderV1 vehicleReader = new VehicleReaderV1(this.inputVehicles);
		vehicleReader.readFile(inputVehicleFile);
		
		Vehicles outputVehicles = VehicleUtils.createVehiclesContainer();
		
		HbefaVehicleCategory vehicleCategory;
		HbefaVehicleAttributes vehicleAttributes;
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			Id personId = person.getId();
			
			if(this.inputVehicles.getVehicles().containsKey(personId)){
				Id vehicleId = personId;
				Vehicle vehicle = this.inputVehicles.getVehicles().get(vehicleId);
				VehicleType vehicleType = vehicle.getType();
				String ageFuelCcm = vehicleType.getDescription();
				
				vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
				vehicleAttributes = mapVehicleAttributesFromMiD2Hbefa(ageFuelCcm);
				
			} else {
				if(personId.toString().startsWith("gv")){
					vehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
				} else {
					vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
				}
				vehicleAttributes = new HbefaVehicleAttributes();
			}
			
			Id<VehicleType> vehTypeId = Id.create(vehicleCategory + ";" + 
									  vehicleAttributes.getHbefaTechnology() + ";" + 
									  vehicleAttributes.getHbefaSizeClass() + ";" + 
									  vehicleAttributes.getHbefaEmConcept(), VehicleType.class);
			VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(vehTypeId);

			// either set following or use switch in EmissionConfigGroup to use vehicle id for vehicle description. Amit sep 2016
//			vehicleType.setDescription(vehTypeId.toString());
			vehicleType.setDescription(EmissionSpecificationMarker.BEGIN_EMISSIONS+vehTypeId.toString()+EmissionSpecificationMarker.END_EMISSIONS);

			if(!(outputVehicles.getVehicles().containsKey(vehTypeId))){
				outputVehicles.addVehicleType(vehicleType);
			} else {
				// do nothing
			}
			
			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(personId, vehicleType);
			outputVehicles.addVehicle( vehicle);
		}
		
		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(outputVehicles);
		vehicleWriter.writeFile(outputVehicleFile);
	}

	private HbefaVehicleAttributes mapVehicleAttributesFromMiD2Hbefa(String ageFuelCcm) {
		HbefaVehicleAttributes vehicleAttributes = new HbefaVehicleAttributes();

		String[] ageFuelCcmArray = ageFuelCcm.split(";");

		if(ageFuelCcmArray.length == 3){
			int year = splitAndReduce(ageFuelCcmArray[0], ":");
			int fuelType = splitAndReduce(ageFuelCcmArray[1], ":");
			int cubicCap = splitAndReduce(ageFuelCcmArray[2], ":");

			if (fuelType == 1)
				vehicleAttributes.setHbefaTechnology("petrol (4S)");
			else if (fuelType == 2)
				vehicleAttributes.setHbefaTechnology("diesel");
			else {
				logger.info("Technology for " + ageFuelCcm + " can not be interpreted; Creating average vehicle...");
				return new HbefaVehicleAttributes();
			}

			if (cubicCap < 1400)
				vehicleAttributes.setHbefaSizeClass("<1,4L");
			else if (cubicCap < 2000 && cubicCap >= 1400)
				vehicleAttributes.setHbefaSizeClass("1,4-<2L");
			else if (cubicCap >= 2000 && cubicCap < 90000)
				vehicleAttributes.setHbefaSizeClass(">=2L");
			else {
				logger.info("SizeClass for " + ageFuelCcm + " can not be interpreted; Creating average vehicle...");
				return new HbefaVehicleAttributes();
			}

			if (year < 1993 && fuelType == 1)
				vehicleAttributes.setHbefaEmConcept("PC-P-Euro-0");
			else if (year < 1993 && fuelType == 2)
				vehicleAttributes.setHbefaEmConcept("PC-D-Euro-0");
			else if (year < 1997 && fuelType == 1)
				vehicleAttributes.setHbefaEmConcept("PC-P-Euro-1");
			else if (year < 1997 && fuelType == 2)
				vehicleAttributes.setHbefaEmConcept("PC-D-Euro-1");
			else if (year < 2001 && fuelType == 1)
				vehicleAttributes.setHbefaEmConcept("PC-P-Euro-2");
			else if (year < 2001 && fuelType == 2)
				vehicleAttributes.setHbefaEmConcept("PC-D-Euro-2");
			else if (year < 2006 && fuelType == 1)
				vehicleAttributes.setHbefaEmConcept("PC-P-Euro-3");
			else if (year < 2006 && fuelType == 2)
				vehicleAttributes.setHbefaEmConcept("PC-D-Euro-3");
			else if (year < 2011 && fuelType == 1)
				vehicleAttributes.setHbefaEmConcept("PC-P-Euro-4");
			else if (year < 2011 && fuelType == 2)
				vehicleAttributes.setHbefaEmConcept("PC-D-Euro-4");
			else if (year < 2015 && fuelType == 1)
				vehicleAttributes.setHbefaEmConcept("PC-P-Euro-5");
			else if (year < 2015 && fuelType == 2)
				vehicleAttributes.setHbefaEmConcept("PC-D-Euro-5");
			else {
				logger.info("EmConcept for " + ageFuelCcm + " can not be interpreted; Creating average vehicle...");
				return new HbefaVehicleAttributes();
			}
		}
		return vehicleAttributes;
	}

	private int splitAndReduce(String string, String splitSign) {
		String[] array = string.split(splitSign);
		return Integer.valueOf(array[1]);
	}
	
}