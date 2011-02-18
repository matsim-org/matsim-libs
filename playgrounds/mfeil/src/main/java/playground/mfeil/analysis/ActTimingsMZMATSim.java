/* *********************************************************************** *
 * project: org.matsim.*
 * ActTimingsMZMATSim.java
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import static org.matsim.core.population.ActivityImpl.*;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.deprecated.DeprecatedStaticMethod;

import playground.mfeil.AgentsAttributesAdder;



/**
 * Reads a plans file and summarizes average activity durations
 *
 * @author mfeil
 */
public class ActTimingsMZMATSim {

	private static final Logger log = Logger.getLogger(ActTimingsMZMATSim.class);

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
		stream.println("Activity timings and frequencies");
		stream.println("\tHome\t\t\tInnerHome\t\tWork\t\tEducations\t\tLeisure\t\tShop\t\tPopSize");
		stream.println("\tStart\tEnd\tFrequency\tDuration\tFrequency\tDuration\tFrequency\tDuration\tFrequency\tDuration\tFrequency\tDuration\tFrequency");
	}

	public void run(Population populationMZ, Population populationMATSim, PrintStream stream, final String attributesInputFile){

		AgentsAttributesAdder aaa = new AgentsAttributesAdder();
		aaa.runMZ(attributesInputFile);
		Map<Id, Double> personsWeights = aaa.getAgentsWeight();

		this.runPopulation("MZ_weighted", populationMZ, stream, personsWeights);
		this.runPopulation("MZ_unweighted", populationMZ, stream, null);
		this.runPopulation("MATSim", populationMATSim, stream, null);
	}

	public void runPopulation (String name, Population population, PrintStream stream, final Map<Id, Double> personsWeights){

		// Initiate output
		double startHome = 0;
		double endHome = 0;
		double durationInnerHome = 0;
		double durationWork = 0;
		double durationEducation = 0;
		double durationLeisure = 0;
		double durationShop = 0;

		double counterHome = 0;
		double counterInnerHome = 0;
		double counterWork = 0;
		double counterEducation = 0;
		double counterLeisure = 0;
		double counterShop = 0;
		double size = population.getPersons().size();

		for (Person person : population.getPersons().values()) {

			double weight = -1;
			if (personsWeights!=null) weight = personsWeights.get(person.getId());
			else weight = 1;

			double duration = 0;

			Plan plan = person.getSelectedPlan();
			for (int i=0;i<plan.getPlanElements().size();i+=2){
				ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(i);
				if (plan.getPlanElements().size()==1){
				//	log.info("Person "+person.getId()+" has only one home act.");
					endHome += weight*86400.0/2.0; // assume one home activity, starting and ending at noon
					startHome += weight*86400.0/2.0;
					duration += 86400;
					counterHome+=weight;
				}
				else {
					if (i==0) { // first home act
						if (act.getEndTime()!=Time.UNDEFINED_TIME){
							endHome += weight*act.getEndTime();
							duration += act.getEndTime();
							counterHome+=weight;
						}
						else log.warn("The end time of person's "+person.getId()+" fist home act is undefined!");
					}
					else if (i==plan.getPlanElements().size()-1) { // last home act
						if (act.getStartTime()!=Time.UNDEFINED_TIME){
							startHome += weight*act.getStartTime();
							duration += (86400-act.getStartTime());
						}
						else log.warn("The start time of person's "+person.getId()+" last home act is undefined!");
					}
					else {
						if (act.getType().startsWith("h")) {
							durationInnerHome+=weight*DeprecatedStaticMethod.calculateSomeDuration(act);
							duration += DeprecatedStaticMethod.calculateSomeDuration(act);
							counterInnerHome+=weight;
						}
						else if (act.getType().startsWith("w")) {
							durationWork+=weight*DeprecatedStaticMethod.calculateSomeDuration(act);
							duration += DeprecatedStaticMethod.calculateSomeDuration(act);
							counterWork+=weight;
						}
						else if (act.getType().startsWith("e")) {
							durationEducation+=weight*DeprecatedStaticMethod.calculateSomeDuration(act);
							duration += DeprecatedStaticMethod.calculateSomeDuration(act);
							counterEducation+=weight;
						}
						else if (act.getType().startsWith("l")) {
							durationLeisure+=weight*DeprecatedStaticMethod.calculateSomeDuration(act);
							duration += DeprecatedStaticMethod.calculateSomeDuration(act);
							counterLeisure+=weight;
						}
						else if (act.getType().startsWith("s")) {
							durationShop+=weight*DeprecatedStaticMethod.calculateSomeDuration(act);
							duration += DeprecatedStaticMethod.calculateSomeDuration(act);
							counterShop+=weight;
						}
						else log.warn("Unknown act type in person's "+person.getId()+" plan at position "+i+"!");
					}
				}
			}
		//	if (name.equals("MZ_weighted")) stream.println(person.getId()+"\t"+duration);
		}
		stream.print(name+"\t");
		stream.print(Time.writeTime(endHome/counterHome)+"\t"+Time.writeTime(startHome/counterHome)+"\t"+1+"\t");
		stream.print(Time.writeTime(durationInnerHome/counterInnerHome)+"\t"+counterInnerHome/counterHome+"\t"+Time.writeTime(durationWork/counterWork)+"\t"+counterWork/counterHome+"\t"+Time.writeTime(durationEducation/counterEducation)+"\t"+counterEducation/counterHome+"\t"+Time.writeTime(durationLeisure/counterLeisure)+"\t"+counterLeisure/counterHome+"\t"+Time.writeTime(durationShop/counterShop)+"\t"+counterShop/counterHome+"\t");
		stream.println(counterHome);
		// write again in seconds
		stream.print(name+"_seconds\t");
		stream.print((endHome/counterHome)+"\t"+(startHome/counterHome)+"\t"+1+"\t");
		stream.print((durationInnerHome/counterInnerHome)+"\t"+counterInnerHome/counterHome+"\t"+(durationWork/counterWork)+"\t"+counterWork/counterHome+"\t"+(durationEducation/counterEducation)+"\t"+counterEducation/counterHome+"\t"+(durationLeisure/counterLeisure)+"\t"+counterLeisure/counterHome+"\t"+(durationShop/counterShop)+"\t"+counterShop/counterHome+"\t");
		stream.println(counterHome);
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

				ActTimingsMZMATSim ts = new ActTimingsMZMATSim();
				PrintStream stream = ts.initiatePrinter(outputFile);
				ts.printHeader(stream);
				ts.run(scenarioMZ.getPopulation(), scenarioMATSim.getPopulation(), stream, attributesInputFile);
				log.info("Process finished.");
			}
}

