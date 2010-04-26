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
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;

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

	public void run(Population populationMZ, Population populationMATSim, PrintStream stream, final String attributesInputFile){

		AgentsAttributesAdder aaa = new AgentsAttributesAdder();
		aaa.runMZ(attributesInputFile);
		Map<Id, Double> personsWeights = aaa.getAgentsWeight();

		this.runAggregateStats("MZ_weighted", populationMZ, stream, personsWeights);
		this.runAggregateStats("MZ_unweighted", populationMZ, stream, null);
		this.runAggregateStats("MATSim", populationMATSim, stream, null);
	}

	public void runAggregateStats (String name, Population population, PrintStream stream, final Map<Id, Double> personsWeights){

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
		double counterAll = 0;

		stream.print(name+"\t");

		for (Person person : population.getPersons().values()) {

			double weight = -1;
			if (personsWeights!=null) weight = personsWeights.get(person.getId());
			else weight = 1;
			counterAll += weight;

			double duration = 0;

			Plan plan = person.getSelectedPlan();
			for (int i=1;i<plan.getPlanElements().size();i+=2){
				LegImpl leg = (LegImpl)plan.getPlanElements().get(i);
				if (leg.getMode().equals(TransportMode.car)) {
					if (leg.getRoute()!=null) aveTripDistanceCarPop1 += weight*leg.getRoute().getDistance();
					else log.warn("A car leg of person "+person.getId()+" has no route!");
					aveTripTimeCarPop1 += weight*leg.getTravelTime();
					duration += leg.getTravelTime();
					counterCar+=weight;
				}
				else if (leg.getMode().equals(TransportMode.pt)) {
					if (leg.getRoute()!=null) aveTripDistancePTPop1 += weight*leg.getRoute().getDistance();
					else log.warn("A pt leg of person "+person.getId()+" has no route!");
					aveTripTimePTPop1 += weight*leg.getTravelTime();
					duration += leg.getTravelTime();
					counterPT+=weight;
				}
				else if (leg.getMode().equals(TransportMode.walk)) {
					if (leg.getRoute()!=null) aveTripDistanceWalkPop1 += weight*leg.getRoute().getDistance();
					else log.warn("A walk leg of person "+person.getId()+" has no route!");
					aveTripTimeWalkPop1 += weight*leg.getTravelTime();
					duration += leg.getTravelTime();
					counterWalk+=weight;
				}
				else if (leg.getMode().equals(TransportMode.bike)) {
					if (leg.getRoute()!=null) aveTripDistanceBikePop1 += weight*leg.getRoute().getDistance();
					else log.warn("A bike leg of person "+person.getId()+" has no route!");
					aveTripTimeBikePop1 += weight*leg.getTravelTime();
					duration += leg.getTravelTime();
					counterBike+=weight;
				}
				else log.warn("Undefined transport mode for person "+plan.getPerson().getId()+": "+leg.getMode());
			}
		//	if (name.equals("MZ_weighted")) stream.println(person.getId()+"\t"+duration);
		}
		stream.print(aveTripDistanceCarPop1/counterCar+"\t");
		stream.print(aveTripDistancePTPop1/counterPT+"\t");
		stream.print(aveTripDistanceWalkPop1/counterWalk+"\t");
		stream.print(aveTripDistanceBikePop1/counterBike+"\t");
		stream.print(aveTripTimeCarPop1/counterCar+"\t");
		stream.print(aveTripTimePTPop1/counterPT+"\t");
		stream.print(aveTripTimeWalkPop1/counterWalk+"\t");
		stream.print(aveTripTimeBikePop1/counterBike+"\t");
		stream.print((aveTripDistanceCarPop1+aveTripDistancePTPop1+aveTripDistanceWalkPop1+aveTripDistanceBikePop1)/counterAll+"\t");
		stream.print((aveTripTimeCarPop1+aveTripTimePTPop1+aveTripTimeWalkPop1+aveTripTimeBikePop1)/counterAll+"\t");
		stream.print(Double.parseDouble(counterCar+"")/counterAll+"\t");
		stream.print(Double.parseDouble(counterPT+"")/counterAll+"\t");
		stream.print(Double.parseDouble(counterWalk+"")/counterAll+"\t");
		stream.print(Double.parseDouble(counterBike+"")/counterAll+"\t");
		stream.println(counterAll);
	}

	public void runDisaggregateStats (String name, Population population, PrintStream stream, final Map<Id, Double> personsWeights){

		double[][]stats = new double[14][5]; // 14 distance classes, 4 modes and 1 count
		double counterAll = 0;

		stream.println("Distance stats");

		for (Person person : population.getPersons().values()) {

			double weight = -1;
			if (personsWeights!=null) weight = personsWeights.get(person.getId());
			else weight = 1;
			counterAll += weight;

			Plan plan = person.getSelectedPlan();
			for (int i=1;i<plan.getPlanElements().size();i+=2){
				LegImpl leg = (LegImpl)plan.getPlanElements().get(i);
				if (leg.getMode().equals(TransportMode.car)) {
					if (leg.getRoute()!=null) {
						double distance = leg.getRoute().getDistance();
						int distanceClass = this.getClass (distance);
						stats[distanceClass][0] += weight;
						stats[distanceClass][4] += weight;
					}
					else log.warn("A car leg of person "+person.getId()+" has no route!");
				}
				else if (leg.getMode().equals(TransportMode.pt)) {
					if (leg.getRoute()!=null) {
						double distance = leg.getRoute().getDistance();
						int distanceClass = this.getClass (distance);
						stats[distanceClass][1] += weight;
						stats[distanceClass][4] += weight;
					}
					else log.warn("A pt leg of person "+person.getId()+" has no route!");
				}
				else if (leg.getMode().equals(TransportMode.walk)) {
					if (leg.getRoute()!=null) {
						double distance = leg.getRoute().getDistance();
						int distanceClass = this.getClass (distance);
						stats[distanceClass][3] += weight;
						stats[distanceClass][4] += weight;
					}
					else log.warn("A walk leg of person "+person.getId()+" has no route!");
				}
				else if (leg.getMode().equals(TransportMode.bike)) {
					if (leg.getRoute()!=null) {
						double distance = leg.getRoute().getDistance();
						int distanceClass = this.getClass (distance);
						stats[distanceClass][2] += weight;
						stats[distanceClass][4] += weight;
					}
					else log.warn("A bike leg of person "+person.getId()+" has no route!");
				}
				else log.warn("Undefined transport mode for person "+plan.getPerson().getId()+": "+leg.getMode());
			}
		}
		for (int i=0;i<14;i++){
			if (i==0) stream.println(name+"\t0\t"+stats[0][0]+"\t"+stats[0][1]+"\t"+stats[0][2]+"\t"+stats[0][3]+"\t"+stats[0][4]);
			else stream.println("\t"+i+"\t"+stats[i][0]+"\t"+stats[i][1]+"\t"+stats[i][2]+"\t"+stats[i][3]+"\t"+stats[i][4]);
		}

	}

	private int getClass (double distance){
		if (distance == 0) return 0;
		if (distance < 100) return 1;
		if (distance < 200) return 2;
		if (distance < 500) return 3;
		if (distance < 1000) return 4;
		if (distance < 2000) return 5;
		if (distance < 5000) return 6;
		if (distance < 10000) return 7;
		if (distance < 20000) return 8;
		if (distance < 50000) return 9;
		if (distance < 100000) return 10;
		if (distance < 200000) return 11;
		if (distance < 500000) return 12;
		else return 13;
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

