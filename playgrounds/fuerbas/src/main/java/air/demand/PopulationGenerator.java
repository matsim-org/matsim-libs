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
import java.util.List;
import java.util.Random;

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

import air.demand.FlightDemandReader.FlightODRelation;



/**
 * 
 * Creates a population based on the Data of DeStatis Passagierzahlen September 2010
 * 
 * @author treczka
 * @author dgrether
 *
 */
public class PopulationGenerator {
	
	private static final Logger log = Logger.getLogger(PopulationGenerator.class);
	
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
	private static final String inputNetworkFile = repos + "shared-svn/studies/countries/de/flight/dg_oag_flight_model_2_runways_60vph/air_network.xml";
	private static final String outputPopulation = repos + "shared-svn/studies/countries/de/flight/ct_demand/population.xml.gz";
	
	public void createPopulation(){
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(inputNetworkFile);
		Scenario sc = ScenarioUtils.loadScenario(config);
		/*
		 * Initialize randomizer
		 */
		Random random = MatsimRandom.getLocalInstance();
		/*
		 * Pick the Network and the Population out of the Scenario for convenience. 
		 */
		Network network = sc.getNetwork();
		Population population = sc.getPopulation();
		/*
		 * Pick the PopulationFactory out of the Population for convenience.
		 * It contains methods to create new Population items.
		 */
		PopulationFactory populationFactory = population.getFactory();
		/*
		 * Eingangsdaten importieren
		 */
		FlightDemandReader eingang;
		eingang = new FlightDemandReader();
		List<FlightODRelation> airportdaten = null;
		try {
			airportdaten = eingang.readFlightODDataPerMonth(inputFlightOdDemand);
		} catch (IOException e) {
			e.printStackTrace();
		}	

		int personid_zaehler = 1;
		for (FlightODRelation od : airportdaten){	//für alle Datensätze
			String fromLinkIdString = od.getFromAirportCode();
			Id fromLinkId = sc.createId(fromLinkIdString);
			Link fromLink = sc.getNetwork().getLinks().get(fromLinkId);
			if (fromLink == null) {
				log.warn("Link id " + fromLinkIdString + " not found in network!");
				continue;
			}
			
			int aktuellepassagierzahl = od.getNumberOfTrips() / 30;
			
			for ( int i=0; i< aktuellepassagierzahl; i++){		// eingangsdatearraynindex+2 ist aktuelle Passagieranzahl
				String toLinkIdString = od.getToAirportCode();
				Id toLinkId = sc.createId(toLinkIdString);
				if (fromLinkIdString.compareTo(toLinkIdString) == 0) {
					continue;
				}
				Link destinationLink = sc.getNetwork().getLinks().get(toLinkId);
				if (destinationLink == null) {	// abfangen von Flughäfen die in den Passagierdaten von DeStatis vorkommen, allerdings nicht im verwendeten Flugnetzwerk vorkommen
					log.warn("Link id " + fromLinkIdString + " not found in network!");
					continue;
				}
				Person person = populationFactory.createPerson(sc.createId(String.valueOf(personid_zaehler)));	// ID für aktuellen Passagier
				Plan plan = populationFactory.createPlan();
				person.addPlan(plan);
				personid_zaehler++;
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
		/*
		 * Write the population  to a file.
		 */
		MatsimWriter popWriter = new org.matsim.api.core.v01.population.PopulationWriter(population, network);
		popWriter.write(outputPopulation);
	}
	
	public static void main(String[] args) {
		new PopulationGenerator().createPopulation();
	}
}		
