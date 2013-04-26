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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class TTAAgentSource implements AgentSource {
	
	private final static Logger log = Logger.getLogger(TTAAgentSource.class);
	
	// internal agents
	private final PopulationAgentSource defaultPopulationAgentSource;
	
	// external agents
	private final PopulationAgentSource carPopulationAgentSource;
	private final PopulationAgentSource truckPopulationAgentSource;
	private final PopulationAgentSource commercialPopulationAgentSource;
	
	public TTAAgentSource(Population population, AgentFactory agentFactory, QSim qsim) {
		
		log.info("total population size:\t" + population.getPersons().size());
		FilterPopulation filterPopulation;

		filterPopulation = new FilterPopulation(population, "tta", false);
		this.defaultPopulationAgentSource = new PopulationAgentSource(filterPopulation, agentFactory, qsim);
		log.info("default population size:\t" + filterPopulation.getPersons().size());
		
		filterPopulation = new FilterPopulation(population, "car", true);
		this.carPopulationAgentSource = new PopulationAgentSource(filterPopulation, agentFactory, qsim);
		log.info("external car population size:\t" + filterPopulation.getPersons().size());
		
		filterPopulation = new FilterPopulation(population, "truck", true);
		this.truckPopulationAgentSource = new PopulationAgentSource(filterPopulation, agentFactory, qsim);
		log.info("external truck population size:\t" + filterPopulation.getPersons().size());
		
		filterPopulation = new FilterPopulation(population, "commercial", true);
		this.commercialPopulationAgentSource = new PopulationAgentSource(filterPopulation, agentFactory, qsim);
		log.info("external commercial population size:\t" + filterPopulation.getPersons().size());
		
		Map<String, VehicleType> modeVehicleTypes;
		
		modeVehicleTypes = new HashMap<String, VehicleType>();
		VehicleType truck = VehicleUtils.getFactory().createVehicleType(new IdImpl("truck"));
		truck.setPcuEquivalents(3.5);
		truck.setMaximumVelocity(25.0);
		modeVehicleTypes.put(TransportMode.car, truck);
		this.truckPopulationAgentSource.setModeVehicleTypes(modeVehicleTypes);
		
		modeVehicleTypes = new HashMap<String, VehicleType>();
		VehicleType commercial = VehicleUtils.getFactory().createVehicleType(new IdImpl("commercial"));
		commercial.setPcuEquivalents(1.0);
		commercial.setMaximumVelocity(30.0);
		modeVehicleTypes.put(TransportMode.car, commercial);
		this.commercialPopulationAgentSource.setModeVehicleTypes(modeVehicleTypes);
	}
	
	@Override
	public void insertAgentsIntoMobsim() {
		
		this.defaultPopulationAgentSource.insertAgentsIntoMobsim();
		this.carPopulationAgentSource.insertAgentsIntoMobsim();
		this.truckPopulationAgentSource.insertAgentsIntoMobsim();
		this.commercialPopulationAgentSource.insertAgentsIntoMobsim();
	}
	
	private static class FilterPopulation implements Population {

		private final Map<Id, Person> persons;
		
		public FilterPopulation(Population population, String filterString, boolean include) {
			
			this.persons = new LinkedHashMap<Id, Person>();
			
			for (Person p : population.getPersons().values()) {
				boolean contains = p.getId().toString().contains(filterString);
				
				if (contains && include) persons.put(p.getId(), p);
				else if (!contains && !include) persons.put(p.getId(), p);
			}
		}

		@Override
		public PopulationFactory getFactory() {
			return null;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public void setName(String name) {			
		}

		@Override
		public Map<Id, ? extends Person> getPersons() {
			return persons;
		}

		@Override
		public void addPerson(Person p) {
		}

		@Override
		public ObjectAttributes getPersonAttributes() {
			return null;
		}
		
	}

}