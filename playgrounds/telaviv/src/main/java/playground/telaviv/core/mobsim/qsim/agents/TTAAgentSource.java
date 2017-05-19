/* *********************************************************************** *
 * project: org.matsim.*
 * TTAAgentSource.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.telaviv.core.mobsim.qsim.agents;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.telaviv.config.TelAvivConfig;

public class TTAAgentSource implements AgentSource {
	
	private final static Logger log = Logger.getLogger(TTAAgentSource.class);
	
	// internal agents
	private final PopulationAgentSource defaultPopulationAgentSource;
	
	// external agents
	private final PopulationAgentSource carPopulationAgentSource;
	private final PopulationAgentSource truckPopulationAgentSource;
	private final PopulationAgentSource commercialPopulationAgentSource;
	
	public TTAAgentSource(Population population, AgentFactory agentFactory, QSim qsim) {
		
		Population defaultPopulation = new FilteredPopulation();
		Population carPopulation = new FilteredPopulation();
		Population truckPopulation = new FilteredPopulation();
		Population commercialPopulation = new FilteredPopulation();
		
		ObjectAttributes attributes = population.getPersonAttributes();
		for (Person person : population.getPersons().values()) {
			Object attribute = attributes.getAttribute(person.getId().toString(), TelAvivConfig.externalTripType);
			if (attribute == null) defaultPopulation.addPerson(person);
			else {
				String externalTripType = (String) attribute;
				if (externalTripType.equals(TelAvivConfig.externalTripTypeCar)) {
					carPopulation.addPerson(person);
				} else if (externalTripType.equals(TelAvivConfig.externalTripTypeTruck)) {
					truckPopulation.addPerson(person);
				} else if (externalTripType.equals(TelAvivConfig.externalTripTypeCommercial)) {
					commercialPopulation.addPerson(person);
				} else {
					throw new RuntimeException("Unknown value for attribute " + TelAvivConfig.externalTripType + 
							" was found: " + externalTripType + ". Aborting!");
				}
			}
		}

		this.defaultPopulationAgentSource = new PopulationAgentSource(defaultPopulation, agentFactory, qsim);
		this.carPopulationAgentSource = new PopulationAgentSource(carPopulation, agentFactory, qsim);
		this.truckPopulationAgentSource = new PopulationAgentSource(truckPopulation, agentFactory, qsim);
		this.commercialPopulationAgentSource = new PopulationAgentSource(commercialPopulation, agentFactory, qsim);
		
		log.info("total population size:\t" + population.getPersons().size());
		log.info("default population size:\t" + defaultPopulation.getPersons().size());
		log.info("external car population size:\t" + carPopulation.getPersons().size());
		log.info("external truck population size:\t" + truckPopulation.getPersons().size());
		log.info("external commercial population size:\t" + commercialPopulation.getPersons().size());
//		setter for mode vehicle types in agent source is not available any more. set vehicle type info to scenario.getVehicles and then use QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData. Amit May'17

//		Map<String, VehicleType> modeVehicleTypes;
//
//		modeVehicleTypes = new HashMap<String, VehicleType>();
//		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car", VehicleType.class));
//		car.setPcuEquivalents(TelAvivConfig.carPcuEquivalents);
//		car.setMaximumVelocity(TelAvivConfig.carMaximumVelocity);
//		modeVehicleTypes.put(TransportMode.car, car);
//		this.truckPopulationAgentSource.setModeVehicleTypes(modeVehicleTypes);
//
//		modeVehicleTypes = new HashMap<String, VehicleType>();
//		VehicleType truck = VehicleUtils.getFactory().createVehicleType(Id.create("truck", VehicleType.class));
//		truck.setPcuEquivalents(TelAvivConfig.truckPcuEquivalents);
//		truck.setMaximumVelocity(TelAvivConfig.truckMaximumVelocity);
//		modeVehicleTypes.put(TransportMode.car, truck);
//		this.truckPopulationAgentSource.setModeVehicleTypes(modeVehicleTypes);
//
//		modeVehicleTypes = new HashMap<String, VehicleType>();
//		VehicleType commercial = VehicleUtils.getFactory().createVehicleType(Id.create("commercial", VehicleType.class));
//		commercial.setPcuEquivalents(TelAvivConfig.commercialPcuEquivalents);
//		commercial.setMaximumVelocity(TelAvivConfig.commercialMaximumVelocity);
//		modeVehicleTypes.put(TransportMode.car, commercial);
//		this.commercialPopulationAgentSource.setModeVehicleTypes(modeVehicleTypes);
	}
	
	@Override
	public void insertAgentsIntoMobsim() {
		
		this.defaultPopulationAgentSource.insertAgentsIntoMobsim();
		this.carPopulationAgentSource.insertAgentsIntoMobsim();
		this.truckPopulationAgentSource.insertAgentsIntoMobsim();
		this.commercialPopulationAgentSource.insertAgentsIntoMobsim();
	}
}