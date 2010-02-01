/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysisSelectedPlansGeneral.java
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
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import playground.mfeil.attributes.AgentsAttributesAdder;
import playground.mfeil.ActChainEqualityCheck;


/**
 * This is a class that facilitates calling various other analysis classes. 
 * It summarizes the analysis functionalities and offers simple access.
 *
 * @author mfeil
 */
public class ASPGeneral {
	
	private static final Logger log = Logger.getLogger(ASPActivityChains.class);
	private ArrayList<List<PlanElement>> activityChainsMATSim;
	private ArrayList<ArrayList<Plan>> plansMATSim;
	private ArrayList<List<PlanElement>> activityChainsMZ;
	private ArrayList<ArrayList<Plan>> plansMZ;
	private ASPActivityChains sp;
	private ASPActivityChains spMZ;
	private Map<Id, Double> personsWeights;
	private PrintStream stream;
	
	private void runMATSimActivityChains (final PopulationImpl population){
		log.info("Analyzing MATSim population...");
		this.sp = new ASPActivityChains(population);
		this.sp.run();
		this.activityChainsMATSim = sp.getActivityChains();
		this.plansMATSim = sp.getPlans();
		log.info("done. "+this.activityChainsMATSim.size()+" act chains.");
	}
	
	private PopulationImpl reducePopulation (PopulationImpl pop, final String attributesInputFile){
		log.info("Reading weights of persons...");
		// Get persons' weights
		AgentsAttributesAdder aaa = new AgentsAttributesAdder();
		aaa.runMZ(attributesInputFile);
		this.personsWeights = aaa.getAgentsWeight();
		log.info("done.");
		
		log.info("Reducing population...");
		// Drop persons for whom no weight info is available
		// Quite strange coding but throws ConcurrentModificationException otherwise...
		Object [] a = pop.getPersons().values().toArray();
		for (int i=a.length-1;i>=0;i--){
			PersonImpl person = (PersonImpl) a[i];
			if (!this.personsWeights.containsKey(person.getId())) pop.getPersons().remove(person.getId());
		}
		log.info("done... Size of population is "+pop.getPersons().size()+".");
		return pop;
	}
	
	private void runMZActivityChains (final PopulationImpl population){
		log.info("Analyzing MZ population...");
		// Analyze the activity chains
		this.spMZ = new ASPActivityChains(population);
		this.spMZ.run();
		this.activityChainsMZ = spMZ.getActivityChains();
		this.activityChainsMZ = this.normalizeActTypes(this.activityChainsMZ);
		this.plansMZ = spMZ.getPlans();
		log.info("done. "+this.activityChainsMZ.size()+" act chains.");
	}
	
	private void initiatePrinter(final String compareOutput){
		try {
			this.stream = new PrintStream (new File(compareOutput));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
	}
	
	private void compareMATSimAndMZActivityChains (){
		
		/* Analysis of activity chains */
		double averageACLengthMZWeighted=0;
		double averageACLengthMZUnweighted=0;
		double averageACLengthMATSim=0;
		this.stream.println("Activity chains");
		this.stream.println("MZ_weighted\t\tMZ_not_weighted\t\tMATSim");
		this.stream.println("Number of occurrences\tRelative\tNumber of occurrences\tRelative\tNumber of occurrences\tRelative\tActivity chain");
		
		// Overall weighted and unweighted number of persons in MZ	
		double overallWeight = 0;
		double overallUnweighted = 0;
		double overallMATSim = 0;
		for (int i=0; i<this.plansMZ.size();i++){
			overallUnweighted += this.plansMZ.get(i).size();
			for (int j=0;j<this.plansMZ.get(i).size();j++){
				overallWeight += this.personsWeights.get(((Plan)this.plansMZ.get(i).get(j)).getPerson().getId());
			}
		}
		// Overall number of persons in MATSim
		for (int i=0; i<this.plansMATSim.size();i++){
			overallMATSim += this.plansMATSim.get(i).size();
		}
		
		// Calculate MZ chains
		ActChainEqualityCheck check = new ActChainEqualityCheck();
		for (int i=0;i<this.activityChainsMZ.size();i++){
			// MZ weighted
			double weight = 0;
			for (int j=0;j<this.plansMZ.get(i).size();j++){
				weight += this.personsWeights.get(((Plan) this.plansMZ.get(i).get(j)).getPerson().getId());
			}
			this.stream.print(weight+"\t"+weight/overallWeight+"\t");
			
			// MZ unweighted
			this.stream.print(this.plansMZ.get(i).size()+"\t"+this.plansMZ.get(i).size()/overallUnweighted+"\t");
			
			// MATSim
			boolean found = false;
			for (int k=0;k<this.activityChainsMATSim.size();k++){
				if (check.checkEqualActChains(this.activityChainsMATSim.get(k), this.activityChainsMZ.get(i))){
					this.stream.print(this.plansMATSim.get(k).size()+"\t"+this.plansMATSim.get(k).size()/overallMATSim+"\t");
					found = true;
					break;
				}
			}
			if (!found) this.stream.print("0\t0\t");
			
			// Activity chain
			for (int j=0; j<this.activityChainsMZ.get(i).size();j=j+2){
				this.stream.print(((ActivityImpl)(this.activityChainsMZ.get(i).get(j))).getType()+"\t");
			}
			this.stream.println();
		}
		
		// Calculate missing MATSim chains
		for (int i=0;i<this.activityChainsMATSim.size();i++){
			boolean found = false;
			for (int k=0;k<this.activityChainsMZ.size();k++){
				if (check.checkEqualActChains(this.activityChainsMATSim.get(i), this.activityChainsMZ.get(k))){
					found = true;
					break;
				}
			}
			if (!found){
				this.stream.print("0\t0\t0\t0\t"+this.plansMATSim.get(i).size()+"\t"+this.plansMATSim.get(i).size()/overallMATSim+"\t");
				for (int j=0; j<this.activityChainsMATSim.get(i).size();j=j+2){
					this.stream.print(((ActivityImpl)(this.activityChainsMATSim.get(i).get(j))).getType()+"\t");
				}
				this.stream.println();
			}
		}
		
		// Average lenghts of act chains
		this.stream.println((averageACLengthMZWeighted/overallWeight)+"\t\t"+(averageACLengthMZUnweighted/overallUnweighted)+"\t\t"+(averageACLengthMATSim/overallMATSim)+"\tAverage number of activities");
		this.stream.println();
		
		double[] kpisMZWeighted = this.spMZ.analyzeActTypes(this.personsWeights);
		double[] kpisMZUnweighted = this.spMZ.analyzeActTypes(null);
		double[] kpisMATSim = this.sp.analyzeActTypes(null);
		this.stream.println(kpisMZWeighted[0]+"\t\t"+kpisMZUnweighted[0]+"\t\t"+kpisMATSim[0]+"\tAverage number of same consecutive acts per plan");
		this.stream.println(kpisMZWeighted[1]+"\t\t"+kpisMZUnweighted[1]+"\t\t"+kpisMATSim[1]+"\tPercentage of same consecutive acts");
		this.stream.println(kpisMZWeighted[2]+"\t\t"+kpisMZUnweighted[2]+"\t\t"+kpisMATSim[2]+"\tAverage number of occurrences of same acts per plan");
		this.stream.println(kpisMZWeighted[3]+"\t\t"+kpisMZUnweighted[3]+"\t\t"+kpisMATSim[3]+"\tAverage number of same acts per plan");
		this.stream.println(kpisMZWeighted[4]+"\t\t"+kpisMZUnweighted[4]+"\t\t"+kpisMATSim[4]+"\tAverage maximum number of same acts per plan");
		this.stream.println(kpisMZWeighted[5]+"\t\t"+kpisMZUnweighted[5]+"\t\t"+kpisMATSim[5]+"\tShare of plans in which same acts occur");
	}
	
	private void runMATSimTrips (PopulationImpl pop){
		TravelStatsMZMATSim ts = new TravelStatsMZMATSim();
		ts.printHeader(this.stream);
		ts.runPopulation("MATSim", pop, this.stream);
	}
	
	private void runMZTrips (PopulationImpl pop){
		new TravelStatsMZMATSim().runPopulation("MZ", pop, this.stream);
	}
	
	private void runMATSimTimings (PopulationImpl pop){
		ActTimingsMZMATSim at = new ActTimingsMZMATSim();
		at.printHeader(this.stream);
		at.runPopulation("MATSim", pop, this.stream);
	}
	
	private void runMZTimings (PopulationImpl pop){
		new ActTimingsMZMATSim().runPopulation("MZ", pop, this.stream);
	}
	
	private ArrayList<List<PlanElement>> normalizeActTypes (ArrayList<List<PlanElement>> actChains){
		log.info("Normalizing act types ...");
		for (int i=0;i<actChains.size();i++){
			for (int j=0;j<actChains.get(i).size();j+=2){
				ActivityImpl act = (ActivityImpl) actChains.get(i).get(j);
				if (act.getType().startsWith("h")) act.setType("home");
				else if (act.getType().startsWith("w")) act.setType("work");
				else if (act.getType().startsWith("e")) act.setType("education");
				else if (act.getType().startsWith("l")) act.setType("leisure");
				else if (act.getType().startsWith("s")) act.setType("shop");
				else log.warn("Unknown act type in actChain "+i+" at position "+j);
			}
		}
		log.info("done.");
		return actChains;
	}
	
	public static void main(final String [] args) {
		// Scenario files
		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		
		// Special MZ file so that weights of MZ persons can be read
		final String attributesInputFile = "/home/baug/mfeil/data/mz/attributes_MZ2005.txt";
		
		// Population files
		final String populationFilenameMATSim = "/home/baug/mfeil/data/runs/run0922_initialdemand_20/output_plans.xml";
		final String populationFilenameMZ = "/home/baug/mfeil/data/mz/plans_Zurich10.xml";
		
		// Output file
		final String outputFile = "/home/baug/mfeil/data/runs/run0922_initialdemand_20/analysis.xls";	
		
		// Settings
		final boolean compareWithMZ = true; 
		
	
		
		// Start calculations
		ScenarioImpl scenarioMATSim = new ScenarioImpl();
		new MatsimNetworkReader(scenarioMATSim).readFile(networkFilename);
		new MatsimFacilitiesReader(scenarioMATSim).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenarioMATSim).readFile(populationFilenameMATSim);
		
		ScenarioImpl scenarioMZ = null;
		if (compareWithMZ){
			scenarioMZ = new ScenarioImpl();
			scenarioMZ.setNetwork(scenarioMATSim.getNetwork());
			new MatsimFacilitiesReader(scenarioMZ).readFile(facilitiesFilename);
			new MatsimPopulationReader(scenarioMZ).readFile(populationFilenameMZ);
		}
		
		ASPGeneral asp = new ASPGeneral();
		asp.initiatePrinter(outputFile);
		asp.runMATSimActivityChains(scenarioMATSim.getPopulation());
		
		if (compareWithMZ) {
			PopulationImpl pop = asp.reducePopulation(scenarioMZ.getPopulation(), attributesInputFile);
			asp.runMZActivityChains(pop);
			asp.compareMATSimAndMZActivityChains();
		}
		
		asp.runMATSimTrips(scenarioMATSim.getPopulation());
		if (compareWithMZ) {
			asp.runMZTrips(scenarioMZ.getPopulation());
		}
		
		asp.runMATSimTimings(scenarioMATSim.getPopulation());
		if (compareWithMZ) {
			asp.runMZTimings(scenarioMZ.getPopulation());
		}

		
		log.info("Analysis of plan finished.");
	}

}

