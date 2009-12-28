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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;



/**
 * Compares trip statistics of two plans files
 *
 * @author mfeil
 */
public class TravelStatsMZMATSim {

	private final PopulationImpl populationMZ, populationMATSim;
	private static final Logger log = Logger.getLogger(TravelStatsMZMATSim.class);


	public TravelStatsMZMATSim(final PopulationImpl populationMZ, final PopulationImpl populationMATSim) {
		this.populationMZ = populationMZ;
		this.populationMATSim = populationMATSim;
	}	
	
	private void run(String outputFile){
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		stream.println("\taveTripDistance\t\t\t\taveTripTravelTime\t\t\t\taveAgents\t\t\t\t\t\tPopSize");
		stream.println("\tCar\tPT\tWalk\tBike\tCar\tPT\tWalk\tBike\tplanDistance\tplanTime\tnoOfCar\tnoOfPT\tnoOfWalk\tnoOfBike");	
		this.runPopulation("MZ", this.populationMZ, stream);
		this.runPopulation("MATSim", this.populationMATSim, stream);
		
		stream.close();
	}
		
		private void runPopulation (String name, PopulationImpl population, PrintStream stream){
		
		// Initiate output
		double aveTripDistanceCarPop1 = 0;
		double aveTripDistancePTPop1 = 0;
		double aveTripDistanceBikePop1 = 0;
		double aveTripDistanceWalkPop1 = 0;
		double aveTripTimeCarPop1 = 0;
		double aveTripTimePTPop1 = 0;
		double aveTripTimeBikePop1 = 0;
		double aveTripTimeWalkPop1 = 0;
		int counterCar = 0;
		int counterPT = 0;
		int counterWalk = 0;
		int counterBike = 0;
		int size = population.getPersons().size();
		
		stream.print(name+"\t");
		
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			for (int i=1;i<plan.getPlanElements().size();i+=2){
				LegImpl leg = (LegImpl)plan.getPlanElements().get(i);
				if (leg.getMode().equals(TransportMode.car)) {
					aveTripDistanceCarPop1 += leg.getRoute().getDistance();
					aveTripTimeCarPop1 += leg.getTravelTime();
					counterCar++;
				}
				else if (leg.getMode().equals(TransportMode.pt)) {
					aveTripDistancePTPop1 += leg.getRoute().getDistance();
					aveTripTimePTPop1 += leg.getTravelTime();
					counterPT++;
				}
				else if (leg.getMode().equals(TransportMode.walk)) {
					aveTripDistanceWalkPop1 += leg.getRoute().getDistance();
					aveTripTimeWalkPop1 += leg.getTravelTime();
					counterWalk++;
				}
				else if (leg.getMode().equals(TransportMode.bike)) {
					aveTripDistanceBikePop1 += leg.getRoute().getDistance();
					aveTripTimeBikePop1 += leg.getTravelTime();
					counterBike++;
				}
				else log.warn("Undefined transport mode for person "+plan.getPerson().getId());
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
				final String populationFilenameMATSim = "/home/baug/mfeil/data/largeSet/it0/output_plans_mz02.xml";
				final String populationFilenameMZ = "/home/baug/mfeil/data/mz/plans_Zurich10.xml";
				final String outputFile = "/home/baug/mfeil/data/largeSet/it1/trip_stats0216.xls";
	
				ScenarioImpl scenarioMZ = new ScenarioImpl();
				new MatsimNetworkReader(scenarioMZ.getNetwork()).readFile(networkFilename);
				new MatsimFacilitiesReader(scenarioMZ.getActivityFacilities()).readFile(facilitiesFilename);
				new MatsimPopulationReader(scenarioMZ).readFile(populationFilenameMZ);
				
				ScenarioImpl scenarioMATSim = new ScenarioImpl();
				scenarioMATSim.setNetwork(scenarioMZ.getNetwork());
				new MatsimFacilitiesReader(scenarioMATSim.getActivityFacilities()).readFile(facilitiesFilename);
				new MatsimPopulationReader(scenarioMATSim).readFile(populationFilenameMATSim);
								
				TravelStatsMZMATSim ts = new TravelStatsMZMATSim(scenarioMZ.getPopulation(), scenarioMATSim.getPopulation());
				ts.run(outputFile);
				log.info("Process finished.");
			}
}

