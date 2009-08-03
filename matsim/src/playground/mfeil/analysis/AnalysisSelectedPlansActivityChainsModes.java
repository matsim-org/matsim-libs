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
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.knowledges.Knowledges;


/**
 * Simple class to analyze the selected plans of a plans (output) file. Extracts the 
 * activity chains per modes and their number of occurrences.
 *
 * @author mfeil
 */
public class AnalysisSelectedPlansActivityChainsModes extends AnalysisSelectedPlansActivityChains{

	protected final PopulationImpl population;
	protected String outputDir;
	protected ArrayList<List<PlanElement>> activityChains;
	protected ArrayList<ArrayList<PlanImpl>> plans;
	protected Knowledges knowledges;
	protected static final Logger log = Logger.getLogger(AnalysisSelectedPlansActivityChainsModes.class);
	


	public AnalysisSelectedPlansActivityChainsModes(final PopulationImpl population, Knowledges knowledges, final String outputDir) {
		super (population, knowledges, outputDir);
		this.population = population;
		this.outputDir = outputDir;
		this.knowledges = knowledges;
		initAnalysis();
	}
	
	protected boolean checkForEquality (PlanImpl plan, List<PlanElement> activityChain){
		
		if (plan.getPlanElements().size()!=activityChain.size()){
		
			return false;
		}
		else{
			ArrayList<String> actsmodes1 = new ArrayList<String> ();
			ArrayList<String> actsmodes2 = new ArrayList<String> ();
			for (int i = 0;i<plan.getPlanElements().size();i++){
				if (i%2==0)	actsmodes1.add(((ActivityImpl)(plan.getPlanElements().get(i))).getType().toString());		
				else actsmodes1.add(((LegImpl)(plan.getPlanElements().get(i))).getMode().toString());
			}
			for (int i = 0;i<activityChain.size();i++){
				if (i%2==0) actsmodes2.add(((ActivityImpl)(activityChain.get(i))).getType().toString());	
				else actsmodes1.add(((LegImpl)(plan.getPlanElements().get(i))).getMode().toString());
			}		
			return (actsmodes1.equals(actsmodes2));
		}
	}		
	
	protected void analyze(){
	
		PrintStream stream1;
		try {
			stream1 = new PrintStream (new File(this.outputDir + "/analysis.xls"));
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
		stream1.println((averageACLength/this.population.getPersons().size())+"\tAverage number of activities");
		stream1.println();
	}		
	

	public static void main(final String [] args) {
//		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
//		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
//		final String populationFilename = "/home/baug/mfeil/data/mz/plans.xml";
		final String populationFilename = "./plans/output_plans.xml.gz";
		final String networkFilename = "./plans/network.xml";
		final String facilitiesFilename = "./plans/facilities.xml.gz";

//		final String outputDir = "/home/baug/mfeil/data/Zurich10";
		final String outputDir = "./plans";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario.getActivityFacilities()).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		AnalysisSelectedPlansActivityChainsModes sp = new AnalysisSelectedPlansActivityChainsModes(scenario.getPopulation(), scenario.getKnowledges(), outputDir);
		sp.analyze();
	//	sp.checkCorrectness();
		
		log.info("Analysis of plan finished.");
	}

}

