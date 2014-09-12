/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package air.demand;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;




/**
 * 
 * Creates a population based on the Data of DeStatis Passagierzahlen September 2010
 * 
 * @author treczka
 * @author dgrether
 *
 */
public class CtPopulationGenerator {
	
	private static final Logger log = Logger.getLogger(CtPopulationGenerator.class);
	private double startTimeUtcSeconds;
	private double durationAirportOpen;
	private boolean createAlternativeModePlan = false;
	private boolean writePlanType = false;
	
	public CtPopulationGenerator(double startTimeUtcSeconds, double durationAirportOpen){
		this.startTimeUtcSeconds = startTimeUtcSeconds;
		this.durationAirportOpen = durationAirportOpen;
	}
	
	public void createAirTransportDemand(String inputNetworkFile, String odDemand,
			String outputDirectory, String outputPopulationFile) {
		List<FlightODRelation> demandList;
		IOUtils.createDirectory(outputDirectory);
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);
			demandList = new DgDemandReader().readFile(odDemand);
			DgDemandUtils.convertToDailyDemand(demandList);
			Network network = this.readNetwork(inputNetworkFile);
			Population population = createPopulation(network, demandList);
			MatsimWriter popWriter = new org.matsim.api.core.v01.population.PopulationWriter(population, network);
			popWriter.write(outputPopulationFile);
			OutputDirectoryLogging.closeOutputDirLogging();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	private List<FlightODRelation> readCtEingangsdaten(String inputFile){
		List<FlightODRelation> airportdaten = null;
		CtFlightDemandReader eingang;
		eingang = new CtFlightDemandReader();
		try {
			airportdaten = eingang.readFlightODDataPerMonth(inputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}	
		return airportdaten;
	}
	
	private Network readNetwork(String inputNetworkFile){
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(inputNetworkFile);
		Scenario sc = ScenarioUtils.loadScenario(config);
		Network network = sc.getNetwork();
		return network;
	}

	
	private Population createPopulation(Network network, List<FlightODRelation> airportdaten){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = sc.getPopulation();
		Random random = MatsimRandom.getLocalInstance();
		PopulationFactory populationFactory = population.getFactory();
		int personIdCounter = 1;
		int removedTripsCounter = 0;
		Set<String> missingOriginAirports = new HashSet<String>();
		Set<String> missingDestinationAirports = new HashSet<String>();
		String missingOdPais = "";
		for (FlightODRelation od : airportdaten){	
			if (od.getNumberOfTrips() == null){
				continue;
			}

			String fromLinkIdString = od.getFromAirportCode();
			Id<Link> fromLinkId = Id.create(fromLinkIdString, Link.class);
			Link fromLink = network.getLinks().get(fromLinkId);
			if (fromLink == null) {
				log.warn("Link id " + fromLinkIdString + " not found in network!");
				missingOriginAirports.add(fromLinkIdString);
				removedTripsCounter += od.getNumberOfTrips();
				continue;
			}
			
			for ( int i=0; i< od.getNumberOfTrips(); i++){	
				String toLinkIdString = od.getToAirportCode();
				Id<Link> toLinkId = Id.create(toLinkIdString, Link.class);
				if (fromLinkIdString.compareTo(toLinkIdString) == 0) {
					removedTripsCounter += od.getNumberOfTrips();
					continue;
				}
				Link destinationLink = network.getLinks().get(toLinkId);
				if (destinationLink == null) {	// abfangen von Flughäfen die in den Passagierdaten von DeStatis vorkommen, allerdings nicht im verwendeten Flugnetzwerk vorkommen
					log.warn("Link id " + toLinkIdString + " not found in network!");
					missingOdPais = missingOdPais + "; " + fromLinkIdString + " -> " + toLinkIdString; 
					missingDestinationAirports.add(toLinkIdString);
					continue;
				}
				Person person = populationFactory.createPerson(Id.create(String.valueOf(personIdCounter), Person.class));	// ID für aktuellen Passagier
				personIdCounter++;
				population.addPerson(person);

				Plan plan = this.createPlan(populationFactory, fromLink, destinationLink, random, "pt");
				person.addPlan(plan);
				if (this.createAlternativeModePlan) {
					plan = this.createPlan(populationFactory, fromLink, destinationLink, random, "train");
					person.addPlan(plan);
				}
				

			}
		}
		log.info("# Persons created: " + (personIdCounter - 1));
		log.info("# trips removed " + removedTripsCounter);
		log.info("missing origin airports: " + missingOriginAirports);
		log.info("missing destination airports: " + missingDestinationAirports);
		log.info("missing od pairs: " + missingOdPais);
		return population;
	}

	
	private Plan createPlan(PopulationFactory populationFactory, Link fromLink, Link destinationLink, Random random, String legmode) {
		Plan plan = populationFactory.createPlan();

		ActivityImpl activity1 = (ActivityImpl) populationFactory.createActivityFromLinkId("home", fromLink.getId());
		activity1.setCoord(fromLink.getCoord());
		plan.addActivity(activity1); // add the Activity to the Plan
		
		double firstActEndTime = random.nextDouble() * durationAirportOpen + startTimeUtcSeconds;
		activity1.setEndTime(firstActEndTime); // zufällig generierte ActivityStartTime als Endzeit gesetzt

		plan.addLeg(populationFactory.createLeg(legmode));

		ActivityImpl destinationActivity = (ActivityImpl) populationFactory.createActivityFromLinkId("home", destinationLink.getId());
		destinationActivity.setCoord(destinationLink.getCoord());
		plan.addActivity(destinationActivity);
		if (this.writePlanType){
			((PlanImpl) plan).setType(legmode);
		}
		return plan;
	}

	
	public boolean isCreateOtherModePlan() {
		return createAlternativeModePlan;
	}

	
	public void setCreateAlternativeModePlan(boolean createOtherModePlan) {
		this.createAlternativeModePlan = createOtherModePlan;
	}

	public void setWritePlanType(boolean writePlanType) {
		this.writePlanType = writePlanType;
	}

	
	
}		
