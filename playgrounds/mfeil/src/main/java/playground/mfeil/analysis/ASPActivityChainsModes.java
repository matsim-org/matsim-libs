/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysisSelectedPlansActivityChains.java
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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.knowledges.Knowledges;

import playground.mfeil.ActChainEqualityCheck;


/**
 * Simple class to analyze the selected plans of a plans (output) file. Extracts the 
 * activity chains per modes and their number of occurrences.
 *
 * @author mfeil
 */
public class ASPActivityChainsModes extends ASPActivityChains{

	private static final Logger log = Logger.getLogger(ASPActivityChainsModes.class);
	


	public ASPActivityChainsModes(final PopulationImpl population, Knowledges knowledges, final String outputDir) {
		super (population, null, knowledges, outputDir);
	}
	
	public ASPActivityChainsModes(final PopulationImpl population) {
		super (population);
	//	this.outputDir = "/home/baug/mfeil/data/largeSet";
		this.outputDir = "./plans";
	}
	
	
	public void run(){
		this.initAnalysis();
		this.analyze();
	}
	
	private void initAnalysis(){
		
		this.activityChains = new ArrayList<List<PlanElement>>();
		this.plans = new ArrayList<ArrayList<Plan>>();
		ActChainEqualityCheck ac = new ActChainEqualityCheck();
		for (Person person : this.populationMATSim.getPersons().values()) {
			boolean alreadyIn = false;
			for (int i=0;i<this.activityChains.size();i++){
				if (ac.checkEqualActChainsModes(person.getSelectedPlan().getPlanElements(), this.activityChains.get(i))){
					plans.get(i).add(person.getSelectedPlan());
					alreadyIn = true;
					break;
				}
			}
			if (!alreadyIn){
				this.activityChains.add(person.getSelectedPlan().getPlanElements());
				this.plans.add(new ArrayList<Plan>());
				this.plans.get(this.plans.size()-1).add(person.getSelectedPlan());
			}
		}
	}
	
	@Override
	protected void analyze(){
	
		PrintStream stream1;
		try {
			stream1 = new PrintStream (new File(this.outputDir + "/analysis_Zurich10_accumulated.xls"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		/* Analysis of activity chains */
		double averageACLength=0;
		stream1.println("Number of occurrences\tActivity chain");
		for (int i=0; i<this.activityChains.size();i++){
			double weight = this.plans.get(i).size();
			stream1.print(weight+"\t");
			double length = this.activityChains.get(i).size();
			averageACLength+=weight*(java.lang.Math.ceil(length/2));
			for (int j=0; j<length;j++){
				if (j%2==0) stream1.print(((ActivityImpl)(this.activityChains.get(i).get(j))).getType()+"\t");
				else stream1.print(((LegImpl)(this.activityChains.get(i).get(j))).getMode()+"\t");
			}
			stream1.println();
		}
		stream1.println((averageACLength/this.populationMATSim.getPersons().size())+"\tAverage number of activities");
		stream1.println();
	}		
	

	public static void main(final String [] args) {
		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		final String populationFilename = "/home/baug/mfeil/data/fullSet/it0/output_plans_mz01.xml";
/*		final String populationFilename = "./plans/output_plans_mz01.xml.gz";
		final String networkFilename = "./plans/network.xml";
		final String facilitiesFilename = "./plans/facilities.xml.gz";
*/
		final String outputDir = "/home/baug/mfeil/data/fullSet/it0";
//		final String outputDir = "./plans";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		ASPActivityChainsModes sp = new ASPActivityChainsModes(scenario.getPopulation(), scenario.getKnowledges(), outputDir);
		sp.run();
		
		log.info("Analysis of plan finished.");
	}

}

