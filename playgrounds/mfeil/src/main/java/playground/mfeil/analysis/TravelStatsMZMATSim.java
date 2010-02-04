/* *********************************************************************** *
 * project: org.matsim.*
 * TravelStatsMZMATSim.java
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

package playground.mfeil.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;

import playground.mfeil.attributes.AgentsAttributesAdder;



/**
 * Compares trip statistics of two plans files
 *
 * @author mfeil
 */
public class TravelStatsMZMATSim {
	
	private static final Logger log = Logger.getLogger(TravelStatsMZMATSim.class);

	private PrintStream initiatePrinter(String outputFile){
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		return stream;
	}
	
	public void printHeader(PrintStream stream){
		stream.println("Trips");
		stream.println("\taveTripDistance\t\t\t\taveTripTravelTime\t\t\t\taveAgents\t\t\t\t\t\tPopSize");
		stream.println("\tCar\tPT\tWalk\tBike\tCar\tPT\tWalk\tBike\tplanDistance\tplanTime\tnoOfCar\tnoOfPT\tnoOfWalk\tnoOfBike");	
	}
	
	public void run(PopulationImpl populationMZ, PopulationImpl populationMATSim, PrintStream stream, final String attributesInputFile){
				
		AgentsAttributesAdder aaa = new AgentsAttributesAdder();
		aaa.runMZ(attributesInputFile);
		Map<Id, Double> personsWeights = aaa.getAgentsWeight();
		
		this.runPopulation("MZ_weighted", populationMZ, stream, personsWeights);
		this.runPopulation("MZ_unweighted", populationMZ, stream, null);
		this.runPopulation("MATSim", populationMATSim, stream, null);
	}
		
	public void runPopulation (String name, PopulationImpl population, PrintStream stream, final Map<Id, Double> personsWeights){
		
		// Initiate output
		double aveTripDistanceCarPop1 = 0;
		double aveTripDistancePTPop1 = 0;
		double aveTripDistanceBikePop1 = 0;
		double aveTripDistanceWalkPop1 = 0;
		double aveTripTimeCarPop1 = 0;
		double aveTripTimePTPop1 = 0;
		double aveTripTimeBikePop1 = 0;
		double aveTripTimeWalkPop1 = 0;
		double counterCar = 0;
		double counterPT = 0;
		double counterWalk = 0;
		double counterBike = 0;
		double size = population.getPersons().size();
		
		stream.print(name+"\t");
		
		for (Person person : population.getPersons().values()) {
			
			double weight = -1;
			if (personsWeights!=null) weight = personsWeights.get(person.getId());
			else weight = 1;
			
			Plan plan = person.getSelectedPlan();
			for (int i=1;i<plan.getPlanElements().size();i+=2){
				LegImpl leg = (LegImpl)plan.getPlanElements().get(i);
				if (leg.getMode().equals(TransportMode.car)) {
					if (leg.getRoute()!=null) aveTripDistanceCarPop1 += weight*leg.getRoute().getDistance();
					else log.warn("A car leg of person "+person.getId()+" has no route!");
					aveTripTimeCarPop1 += weight*leg.getTravelTime();
					counterCar+=weight;
				}
				else if (leg.getMode().equals(TransportMode.pt)) {
					if (leg.getRoute()!=null) aveTripDistancePTPop1 += weight*leg.getRoute().getDistance();
					else log.warn("A pt leg of person "+person.getId()+" has no route!");
					aveTripTimePTPop1 += weight*leg.getTravelTime();
					counterPT+=weight;
				}
				else if (leg.getMode().equals(TransportMode.walk)) {
					if (leg.getRoute()!=null) aveTripDistanceWalkPop1 += weight*leg.getRoute().getDistance();
					else log.warn("A walk leg of person "+person.getId()+" has no route!");
					aveTripTimeWalkPop1 += weight*leg.getTravelTime();
					counterWalk+=weight;
				}
				else if (leg.getMode().equals(TransportMode.bike)) {
					if (leg.getRoute()!=null) aveTripDistanceBikePop1 += weight*leg.getRoute().getDistance();
					else log.warn("A bike leg of person "+person.getId()+" has no route!");
					aveTripTimeBikePop1 += weight*leg.getTravelTime();
					counterBike+=weight;
				}
				else log.warn("Undefined transport mode for person "+plan.getPerson().getId()+": "+leg.getMode());
			}		
		}
		stream.print(aveTripDistanceCarPop1/counterCar+"\t");
		stream.print(aveTripDistancePTPop1/counterPT+"\t");
		stream.print(aveTripDistanceWalkPop1/counterWalk+"\t");
		stream.print(aveTripDistanceBikePop1/counterBike+"\t");
		stream.print(aveTripTimeCarPop1/counterCar+"\t");
		stream.print(aveTripTimePTPop1/counterPT+"\t");
		stream.print(aveTripTimeWalkPop1/counterWalk+"\t");
		stream.print(aveTripTimeBikePop1/counterBike+"\t");
		stream.print((aveTripDistanceCarPop1+aveTripDistancePTPop1+aveTripDistanceWalkPop1+aveTripDistanceBikePop1)/size+"\t");
		stream.print((aveTripTimeCarPop1+aveTripTimePTPop1+aveTripTimeWalkPop1+aveTripTimeBikePop1)/size+"\t");
		stream.print(Double.parseDouble(counterCar+"")/Double.parseDouble(size+"")+"\t");
		stream.print(Double.parseDouble(counterPT+"")/Double.parseDouble(size+"")+"\t");
		stream.print(Double.parseDouble(counterWalk+"")/Double.parseDouble(size+"")+"\t");
		stream.print(Double.parseDouble(counterBike+"")/Double.parseDouble(size+"")+"\t");
		stream.println(size);
	}		
		
	
	public static void main(final String [] args) {
				final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
				final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
				final String populationFilenameMATSim = "/home/baug/mfeil/data/choiceSet/it0/output_plans_mz05.xml";
				final String populationFilenameMZ = "/home/baug/mfeil/data/mz/plans_Zurich10.xml";
				final String outputFile = "/home/baug/mfeil/data/choiceSet/trip_stats_mz05.xls";
				
				// Special MZ file so that weights of MZ persons can be read
				final String attributesInputFile = "/home/baug/mfeil/data/mz/attributes_MZ2005.txt";
	
				ScenarioImpl scenarioMZ = new ScenarioImpl();
				new MatsimNetworkReader(scenarioMZ).readFile(networkFilename);
				new MatsimFacilitiesReader(scenarioMZ).readFile(facilitiesFilename);
				new MatsimPopulationReader(scenarioMZ).readFile(populationFilenameMZ);
				
				ScenarioImpl scenarioMATSim = new ScenarioImpl();
				scenarioMATSim.setNetwork(scenarioMZ.getNetwork());
				new MatsimFacilitiesReader(scenarioMATSim).readFile(facilitiesFilename);
				new MatsimPopulationReader(scenarioMATSim).readFile(populationFilenameMATSim);
								
				TravelStatsMZMATSim ts = new TravelStatsMZMATSim();
				PrintStream stream = ts.initiatePrinter(outputFile);
				ts.printHeader(stream);
				ts.run(scenarioMZ.getPopulation(), scenarioMATSim.getPopulation(), stream, attributesInputFile);
				log.info("Process finished.");
			}
}

