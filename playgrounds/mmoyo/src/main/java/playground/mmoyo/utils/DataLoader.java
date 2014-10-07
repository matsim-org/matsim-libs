/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mmoyo.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class DataLoader {

	public TransitSchedule readTransitSchedule(final String transitScheduleFile) {
		TransitScheduleFactoryImpl transitScheduleFactoryImpl = new TransitScheduleFactoryImpl();
		TransitSchedule transitSchedule = transitScheduleFactoryImpl.createTransitSchedule();
		ModeRouteFactory routeFactory = new ModeRouteFactory();
		TransitScheduleReaderV1 transitScheduleReaderV1 = new TransitScheduleReaderV1(transitSchedule, routeFactory);
		transitScheduleReaderV1.readFile(transitScheduleFile);
		transitScheduleFactoryImpl = null;
		transitScheduleReaderV1 = null;
		routeFactory = null;
		return transitSchedule;
	}

	public Network readNetwork (final String networkFile){
		Scenario scenario = this.createScenario();
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario);
		matsimNetReader.readFile(networkFile);
		Network network = ((ScenarioImpl)scenario).getNetwork(); 
		scenario = null;
		matsimNetReader = null;
		return network;
	}

	public Population readPopulation(final String populationFile){
		Scenario scenario = this.createScenario();
		PopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(populationFile);
		Population population = scenario.getPopulation();
		scenario = null;
		popReader = null;
		return population;
	}

	/**
	Caution: all previous references to scn.getPopulation will point to the new Population
	*/
	public void setNewPopulation(Scenario scenario, final String populationFile){
		scenario.getPopulation().getPersons().clear();
		PopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(populationFile);
		popReader = null;
	}
	
	public Scenario readNetwork_Population(String networkFile, String populationFile) {
		Scenario scenario = this.createScenario();
		
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario);
		matsimNetReader.readFile(networkFile);
		
		PopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(populationFile);

		return scenario;
	}

	public Scenario createScenario(){
		return ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}
	
	public ScenarioImpl loadScenario (final String configFile){
		Config config = this.readConfig(configFile); 
		return (ScenarioImpl)ScenarioUtils.loadScenario(config);
	}

	public Config readConfig(final String configFile){
		return ConfigUtils.loadConfig(configFile);
	}
	
	//returns a transitRoute object of the schedule
	public TransitRoute getTransitRoute(final String strRouteId, final TransitSchedule schedule){
		Id<TransitLine> lineId = Id.create(strRouteId.split("\\.")[0], TransitLine.class);
		return schedule.getTransitLines().get(lineId).getRoutes().get(Id.create(strRouteId, TransitRoute.class));
	}

	public Counts readCounts (String countFile){
		Counts counts = new Counts();
		MatsimCountsReader matsimCountsReader = new MatsimCountsReader(counts);
		matsimCountsReader.readFile(countFile);
		matsimCountsReader = null;
		return counts;
	}

}