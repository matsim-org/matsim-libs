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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioUtils;




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
	
	/*
	 * Setze startzeit (3 Uhr UTC)
		 * airportoffen (15 Stunden) 
		 * 5-20 Uhr => Ausgabewert 15:00:00 (15 Stunden) 
		 * bzw. auf UTC umgerechnet Zeitfenster 03-18 Uhr UTC
		 
	 */
	private static final double startzeit = 3.0 * 3600.0;	
	private static final double airportoffen = 15.0 * 3600.0;
	private static final String repos = "/media/data/work/repos/";
	private static final String inputFlightOdDemand = repos + "lehre-svn/abschlussarbeiten/2011/christopher_treczka/treczka/Modellierung_DE/input/Eingangsdaten_September2010.txt";
	private static final String inputNetworkFile = repos + "shared-svn/studies/countries/de/flight/dg_oag_tuesday_flight_model_2_runways_60vph/air_network.xml";
//	private static final String outputPopulation = repos + "shared-svn/studies/countries/de/flight/ct_demand/population.xml.gz";
	
	private static final String odDemand = "/media/data/work/repos/shared-svn/studies/countries/de/flight/demand/destatis/2011_september/demand_september_2011_tabelle_2.2.2.csv";
	private static final String outputPopulation = repos + "shared-svn/studies/countries/de/flight/demand/destatis/2011_september/population_september_2011_tabelle_2.2.2_new.xml.gz";
	
	public static void ctmain(String[] args) {
		
		CtPopulationGenerator ctPopGen = new CtPopulationGenerator();
		List<FlightODRelation> demandList = ctPopGen.readCtEingangsdaten(inputFlightOdDemand);
		ctPopGen.createPopulation(demandList);
	}
	
	public static void main(String[] args) throws IOException {
		List<FlightODRelation> demandList = new DgDemandReader().readFile(odDemand);
		CtPopulationGenerator ctPopGen = new CtPopulationGenerator();
		DgDemandUtils.convertToDailyDemand(demandList);
		ctPopGen.createPopulation(demandList);
	}

	public List<FlightODRelation> readCtEingangsdaten(String inputFile){
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
	
	

	
	public void createPopulation(List<FlightODRelation> airportdaten){
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(inputNetworkFile);
		Scenario sc = ScenarioUtils.loadScenario(config);
		Random random = MatsimRandom.getLocalInstance();
		Network network = sc.getNetwork();
		Population population = sc.getPopulation();
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
			Id fromLinkId = sc.createId(fromLinkIdString);
			Link fromLink = sc.getNetwork().getLinks().get(fromLinkId);
			if (fromLink == null) {
				log.warn("Link id " + fromLinkIdString + " not found in network!");
				missingOriginAirports.add(fromLinkIdString);
				removedTripsCounter += od.getNumberOfTrips();
				continue;
			}
			
			for ( int i=0; i< od.getNumberOfTrips(); i++){		// eingangsdatearraynindex+2 ist aktuelle Passagieranzahl
				String toLinkIdString = od.getToAirportCode();
				Id toLinkId = sc.createId(toLinkIdString);
				if (fromLinkIdString.compareTo(toLinkIdString) == 0) {
					removedTripsCounter += od.getNumberOfTrips();
					continue;
				}
				Link destinationLink = sc.getNetwork().getLinks().get(toLinkId);
				if (destinationLink == null) {	// abfangen von Flughäfen die in den Passagierdaten von DeStatis vorkommen, allerdings nicht im verwendeten Flugnetzwerk vorkommen
					log.warn("Link id " + toLinkIdString + " not found in network!");
					missingOdPais = missingOdPais + "; " + fromLinkIdString + " -> " + toLinkIdString; 
					missingDestinationAirports.add(toLinkIdString);
					continue;
				}
				Person person = populationFactory.createPerson(sc.createId(String.valueOf(personIdCounter)));	// ID für aktuellen Passagier
				Plan plan = populationFactory.createPlan();
				person.addPlan(plan);
				personIdCounter++;
				population.addPerson(person);

				ActivityImpl activity1 = (ActivityImpl) populationFactory.createActivityFromLinkId("home", fromLink.getId());
				activity1.setCoord(fromLink.getCoord());
				plan.addActivity(activity1); // add the Activity to the Plan
				
				double firstActEndTime = random.nextDouble() * airportoffen + startzeit;
				activity1.setEndTime(firstActEndTime); // zufällig generierte ActivityStartTime als Endzeit gesetzt

				plan.addLeg(populationFactory.createLeg("pt"));

				ActivityImpl destinationActivity = (ActivityImpl) populationFactory.createActivityFromLinkId("home", destinationLink.getId());
				destinationActivity.setCoord(destinationLink.getCoord());
				plan.addActivity(destinationActivity);
			}
		}
		MatsimWriter popWriter = new org.matsim.api.core.v01.population.PopulationWriter(population, network);
		popWriter.write(outputPopulation);
		log.info("# Persons created: " + (personIdCounter - 1));
		log.info("# trips removed " + removedTripsCounter);
		log.info("missing origin airports: " + missingOriginAirports);
		log.info("missing destination airports: " + missingDestinationAirports);
		log.info("missing od pairs: " + missingOdPais);
	}
	
	
}		
