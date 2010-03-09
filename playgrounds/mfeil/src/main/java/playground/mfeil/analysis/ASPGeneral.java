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



import java.io.BufferedReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import org.matsim.core.network.NetworkImpl;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import playground.mfeil.attributes.AgentsAttributesAdder;
import playground.mfeil.ActChainEqualityCheck;


/**
 * This class summarizes the analysis functionalities 
 * act chains,
 * trips,
 * and act timings,
 * and offers simple access.
 *
 * @author mfeil
 */
public class ASPGeneral {
	
	private static final Logger log = Logger.getLogger(ASPGeneral.class);
	private ArrayList<List<PlanElement>> activityChainsMATSim;
	private ArrayList<ArrayList<Plan>> plansMATSim;
	private ArrayList<List<PlanElement>> activityChainsMZ;
	private ArrayList<ArrayList<Plan>> plansMZ;
	private ASPActivityChains sp;
	private ASPActivityChains spMZ;
	private Map<Id, Double> personsWeights;
	private PrintStream stream;
	
	
	public ASPGeneral (final int iter, final int lastIter, final String directory, final NetworkImpl network) {
		// Scenario files
		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
	//	final String facilitiesFilename = "../matsim/test/scenarios/chessboard/facilities.xml";
	//	final String networkFilename = "/home/baug/mfeil/data/Zurich10/network_0.7.xml";
		
		// Special MZ file so that weights of MZ persons can be read
		final String attributesInputFile = "/home/baug/mfeil/data/mz/attributes_MZ2005.txt";
		
		// Population files
		final String populationFilenameMZ = "/home/baug/mfeil/data/choiceSet/it0/output_plans_mzAS0997b.xml";
		
		// Counts file
	//	final String counts = "/home/baug/mfeil/data/runs/0995b_18rec/ITERS/it.70/70.countscompare.txt";
		final String counts = directory+"/ITERS/it."+iter+"/"+iter+".countscompare.txt";
		final String populationFilenameMATSim = directory+"/ITERS/it."+iter+"/"+iter+".plans.xml";
		
		// Output file
	//	final String outputFile = "/home/baug/mfeil/output/plx.analysis.xls";	
		final String outputFile = directory+"/"+iter+".analysis.xls";	
			
		// Settings
		final boolean compareWithMZ = true; 		
		
		
		// Start calculations
		ScenarioImpl scenarioMATSim = new ScenarioImpl();
		scenarioMATSim.setNetwork(network);
		new MatsimFacilitiesReader(scenarioMATSim).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenarioMATSim).readFile(populationFilenameMATSim);
			
		ScenarioImpl scenarioMZ = null;
		if (compareWithMZ){
			scenarioMZ = new ScenarioImpl();
			scenarioMZ.setNetwork(network);
			new MatsimFacilitiesReader(scenarioMZ).readFile(facilitiesFilename);
			new MatsimPopulationReader(scenarioMZ).readFile(populationFilenameMZ);
		}
			
		this.initiatePrinter(outputFile);
		this.runMATSimActivityChains(scenarioMATSim.getPopulation());
			
		if (compareWithMZ) {
			PopulationImpl pop = this.reducePopulation(scenarioMZ.getPopulation(), attributesInputFile);
			this.reducePopulation(scenarioMZ.getPopulation(), attributesInputFile);
			this.runMZActivityChains(pop);
			this.compareMATSimAndMZActivityChains();
			this.runMATSimTrips(scenarioMATSim.getPopulation());
			this.runMZTrips(pop);
			this.runMATSimDistances(scenarioMATSim.getPopulation());
			this.runMZDistances(pop);
			this.runMATSimTimings(scenarioMATSim.getPopulation());
			this.runMZTimings(pop);
		}
		else {
			this.runMATSimTrips(scenarioMATSim.getPopulation());
			this.runMATSimTimings(scenarioMATSim.getPopulation());
		}
		this.analyzeCounts(counts);
		
		log.info("Analysis of plan finished.");
	}
	
	private void runMATSimActivityChains (PopulationImpl population){
		log.info("Analyzing MATSim population...");
		population = this.normalizeMATSimActTypes(population);
		this.sp = new ASPActivityChains(population);
		this.sp.run();
		this.activityChainsMATSim = sp.getActivityChains();
		this.plansMATSim = sp.getPlans();
		log.info("done. "+this.activityChainsMATSim.size()+" act chains.");
	}
	
	// Drop the MZ persons for whom no weight information is available
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
		this.activityChainsMZ = this.normalizeMZActTypes(this.activityChainsMZ);
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
			averageACLengthMZWeighted+=weight*(this.activityChainsMZ.get(i).size()/2+1);
			this.stream.print(weight+"\t"+weight/overallWeight+"\t");
			
			// MZ unweighted
			this.stream.print(this.plansMZ.get(i).size()+"\t"+this.plansMZ.get(i).size()/overallUnweighted+"\t");
			averageACLengthMZUnweighted+=this.plansMZ.get(i).size()*(this.activityChainsMZ.get(i).size()/2+1);
			
			// MATSim
			boolean found = false;
			for (int k=0;k<this.activityChainsMATSim.size();k++){
				if (check.checkEqualActChains(this.activityChainsMATSim.get(k), this.activityChainsMZ.get(i))){
					this.stream.print(this.plansMATSim.get(k).size()+"\t"+this.plansMATSim.get(k).size()/overallMATSim+"\t");
					averageACLengthMATSim+=this.plansMATSim.get(k).size()*(this.activityChainsMATSim.get(k).size()/2+1);
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
				averageACLengthMATSim+=this.plansMATSim.get(i).size()*(this.activityChainsMATSim.get(i).size()/2+1);
				for (int j=0; j<this.activityChainsMATSim.get(i).size();j=j+2){
					this.stream.print(((ActivityImpl)(this.activityChainsMATSim.get(i).get(j))).getType()+"\t");
				}
				this.stream.println();
			}
		}
		
		// Average lenghts of act chains
		this.stream.println((averageACLengthMZWeighted/overallWeight)+"\t\t"+(averageACLengthMZUnweighted/overallUnweighted)+"\t\t"+(averageACLengthMATSim/overallMATSim)+"\t\tAverage number of activities");
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
		this.stream.println();
	}
	
	private void runMATSimTrips (PopulationImpl pop){
		TravelStatsMZMATSim ts = new TravelStatsMZMATSim();
		ts.printHeader(this.stream);
		ts.runAggregateStats("MATSim", pop, this.stream, null);
	}
	
	private void runMZTrips (PopulationImpl pop){
		TravelStatsMZMATSim ts = new TravelStatsMZMATSim();
		ts.runAggregateStats("MZ_weighted", pop, this.stream, this.personsWeights);
		ts.runAggregateStats("MZ_unweighted", pop, this.stream, null);
		this.stream.println();
	}
	
	private void runMATSimDistances (PopulationImpl pop){
		TravelStatsMZMATSim ts = new TravelStatsMZMATSim();
		ts.printHeader(this.stream);
		ts.runDisaggregateStats("MATSim", pop, this.stream, null);
	}
	
	private void runMZDistances (PopulationImpl pop){
		TravelStatsMZMATSim ts = new TravelStatsMZMATSim();
		ts.runDisaggregateStats("MZ_weighted", pop, this.stream, this.personsWeights);
		ts.runDisaggregateStats("MZ_unweighted", pop, this.stream, null);
		this.stream.println();
	}
	
	private void runMATSimTimings (PopulationImpl pop){
		ActTimingsMZMATSim at = new ActTimingsMZMATSim();
		at.printHeader(this.stream);
		at.runPopulation("MATSim", pop, this.stream, null);
	}
	
	private void runMZTimings (PopulationImpl pop){
		ActTimingsMZMATSim at = new ActTimingsMZMATSim();
		at.runPopulation("MZ_weighted", pop, this.stream, this.personsWeights);
		at.runPopulation("MZ_unweighted", pop, this.stream, null);
		this.stream.println();
	}
	
	// MZ act chains have only letters, MATSim chain full types. Therefore, MZ chains need to adapted.
	private ArrayList<List<PlanElement>> normalizeMZActTypes (ArrayList<List<PlanElement>> actChains){
		log.info("Normalizing act types ...");
		for (int i=0;i<actChains.size();i++){
			for (int j=0;j<actChains.get(i).size();j+=2){
				ActivityImpl act = (ActivityImpl) actChains.get(i).get(j);
				if (act.getType().startsWith("h")) act.setType("home");
				else if (act.getType().startsWith("w")) act.setType("work");
				else if (act.getType().startsWith("e")) act.setType("education");
				else if (act.getType().startsWith("l")) act.setType("leisure");
				else if (act.getType().startsWith("s")) act.setType("shop");
				else log.warn("Unknown act type in MZ actChain "+i+" at position "+j);
			}
		}
		log.info("done.");
		return actChains;
	}
	
	// MATSim act chains have several work and education types, MZ not. Therefore, MATSim chains need to adapted.
	private PopulationImpl normalizeMATSimActTypes (PopulationImpl pop){
		log.info("Normalizing act types ...");
		for (Person person : pop.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			for (int j=0;j<plan.getPlanElements().size();j+=2){
				ActivityImpl act = (ActivityImpl) plan.getPlanElements().get(j);
				if (act.getType().startsWith("w")) act.setType("work");
				else if (act.getType().startsWith("e")) act.setType("education");
				else if (!act.getType().startsWith("l") && !act.getType().startsWith("h") && !act.getType().startsWith("s")) log.warn("Unknown act type in MATSim actChain of person "+person.getId()+" at position "+j);
			}
		}
		log.info("done.");
		return pop;
	}
	
	
	// Analyzes a counts file
	private void analyzeCounts (String countsFile){
		log.info("Analyzing counts file ...");
		
		TreeMap<Integer, ArrayList<double[]>> data = new TreeMap<Integer, ArrayList<double[]>>();
		
		try {

			FileReader fr = new FileReader(countsFile);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just
									// contains column headers
			line = br.readLine();
			int tokenId;
			double tokenHour;
			double tokenMATSim;
			double tokenCounts;
			double tokenDiff;
			double counter = 1;
			ArrayList<double[]> list = new ArrayList<double[]>();;
			while (line != null) {		
				
				if (counter==1) list = new ArrayList<double[]>();
				
				tokenizer = new StringTokenizer(line);
				tokenId = Integer.parseInt(tokenizer.nextToken());
				
				tokenHour = Double.parseDouble(tokenizer.nextToken());		
				
				String token = tokenizer.nextToken();
				token = token.trim();
	            token = token.replace(",",""); //filter out the commas
	            tokenMATSim = Double.parseDouble(token);		
	            
	            token = tokenizer.nextToken();
				token = token.trim();
	            token = token.replace(",",""); //filter out the commas
	            tokenCounts = Double.parseDouble(token);		
	            
	            token = tokenizer.nextToken();
				token = token.trim();
	            token = token.replace(",",""); //filter out the commas
	            tokenDiff = Double.parseDouble(token);		
				
				list.add(new double[] {tokenHour,tokenMATSim,tokenCounts,tokenDiff});
				if (counter==24) {
					data.put(tokenId, list);
					counter=0;
				}
				counter++;
				
				line = br.readLine();
			}		
		} catch (Exception ex) {
			System.out.println(ex);
		}
		
		this.stream.println();
		this.stream.println("Counts");
		this.stream.println();
		
		
		// by counting point
		for (int key : data.keySet()) {
			double volumeMatsim=0;
			double volumeCounts=0;
			
			this.stream.print(key+"\tHour\t");
			for (int i=1;i<25;i++){
				this.stream.print(i+"\t");
			}
			this.stream.println("Sum");
			
			this.stream.print("\tMATSim\t");
			for (int i=1;i<25;i++){
				if (data.get(key).get(i-1)[0]!=i) {
					log.warn("Hour "+i+" and data field "+data.get(key).get(i-1)[0]+" do not match for entry "+key);
				}
				else {
					volumeMatsim += data.get(key).get(i-1)[1];
					this.stream.print(data.get(key).get(i-1)[1]+"\t");
				}
			}
			this.stream.println(volumeMatsim);
			
			this.stream.print("\tCounts\t");
			for (int i=1;i<25;i++){
				if (data.get(key).get(i-1)[0]!=i) {
					log.warn("Hour "+i+" and data field "+data.get(key).get(i-1)[0]+" do not match for entry "+key);
				}
				else {
					volumeCounts += data.get(key).get(i-1)[2];
					this.stream.print(data.get(key).get(i-1)[2]+"\t");
				}
			}
			this.stream.println(volumeCounts);
			
			this.stream.print("\tDiff\t");
			for (int i=1;i<25;i++){
				if (data.get(key).get(i-1)[0]!=i) {
					log.warn("Hour "+i+" and data field "+data.get(key).get(i-1)[0]+" do not match for entry "+key);
				}
				else {
					this.stream.print(data.get(key).get(i-1)[3]+"\t");
				}
			}
			this.stream.println((volumeMatsim-volumeCounts)/volumeCounts*100);
		}
		this.stream.println();
		
		
		// by time
		double totalMatsim = 0;
		double totalCounts = 0;
		double[] totalHoursMatsim = new double[24];
		double[] totalHoursCounts = new double[24];
		this.stream.print("Hour\t");
		for (int i=1;i<25;i++){
			this.stream.print(i+"\t");
		}
		this.stream.println("Sum");
		
		this.stream.print("Matsim\t");
		for (int i=1;i<25;i++){
			double volumeMatsim=0;
			for (int key : data.keySet()) {
				if (data.get(key).get(i-1)[0]!=i) {
					log.warn("Hour "+i+" and data field "+data.get(key).get(i-1)[0]+" do not match for entry "+key);
				}
				else {
					volumeMatsim += data.get(key).get(i-1)[1];
				}
			}
			totalMatsim += volumeMatsim;
			this.stream.print(volumeMatsim+"\t");
			totalHoursMatsim[i-1] = volumeMatsim;
		}
		this.stream.println(totalMatsim);
		
		this.stream.print("Counts\t");
		for (int i=1;i<25;i++){
			double volumeCounts=0;
			for (int key : data.keySet()) {
				if (data.get(key).get(i-1)[0]!=i) {
					log.warn("Hour "+i+" and data field "+data.get(key).get(i-1)[0]+" do not match for entry "+key);
				}
				else {
					volumeCounts += data.get(key).get(i-1)[2];
				}
			}
			totalCounts += volumeCounts;
			this.stream.print(volumeCounts+"\t");
			totalHoursCounts[i-1] = volumeCounts;
		}
		this.stream.println(totalCounts);
		
		this.stream.print("Diff\t");
		for (int i=1;i<25;i++){
			this.stream.print((totalHoursMatsim[i-1]-totalHoursCounts[i-1])/totalHoursCounts[i-1]*100+"\t");
		}
		this.stream.println((totalMatsim-totalCounts)/totalCounts*100);
		
		this.stream.println();
		
		// for scatter charts
		this.stream.println("\t7-8\t\t8-9\t\t17-18\t\t18-19");
		this.stream.println("Id\tMATSim\tCounts\tMATSim\tCounts\tMATSim\tCounts\tMATSim\tCounts");
		for (int key : data.keySet()) {
			this.stream.print(key+"\t");
			if (data.get(key).get(7)[0]!=8){
				log.warn("Wrong 7-8 timings for scatter charts!");
			}
			else {
				this.stream.print(data.get(key).get(7)[1]+"\t"+data.get(key).get(7)[2]+"\t");
			}
			if (data.get(key).get(8)[0]!=9){
				log.warn("Wrong 8-9 timings for scatter charts!");
			}
			else {
				this.stream.print(data.get(key).get(8)[1]+"\t"+data.get(key).get(8)[2]+"\t");
			}
			if (data.get(key).get(17)[0]!=18){
				log.warn("Wrong 17-18 timings for scatter charts!");
			}
			else {
				this.stream.print(data.get(key).get(17)[1]+"\t"+data.get(key).get(17)[2]+"\t");
			}
			if (data.get(key).get(18)[0]!=19){
				log.warn("Wrong 18-19 timings for scatter charts!");
			}
			else {
				this.stream.print(data.get(key).get(18)[1]+"\t"+data.get(key).get(18)[2]+"\t");
			}
			this.stream.println();
		}
		
		log.info("done.");
	}
	
	public static void main(final String [] args) {	
		int iter = 100;
		int lastIter = iter;
		String directory = "Test1";
		/*	// Scenario files
		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network_0.7.xml";
		
		// Special MZ file so that weights of MZ persons can be read
		final String attributesInputFile = "/home/baug/mfeil/data/mz/attributes_MZ2005.txt";
		
		// Population files
		final String populationFilenameMATSim;
		if (iter.equals("50")) populationFilenameMATSim = "/home/baug/mfeil/output/output_plans.xml";
		else populationFilenameMATSim = "/home/baug/mfeil/output/output_plans.xml";
		final String populationFilenameMZ = "/home/baug/mfeil/data/choiceSet/it0/output_plans_mzAS0997b.xml";
		
		// Counts file
		final String counts = "/home/baug/mfeil/data/runs/0995b_18rec/ITERS/it.70/70.countscompare.txt";
		
		// Output file
		final String outputFile = "/home/baug/mfeil/output/plx.analysis.xls";	
	*/	
		
		final String networkFilename = "../matsim/test/scenarios/chessboard/network.xml";
		
		// Start calculations
		ScenarioImpl scenarioMATSim = new ScenarioImpl();
		new MatsimNetworkReader(scenarioMATSim).readFile(networkFilename);
		
		new ASPGeneral(iter, lastIter, directory, scenarioMATSim.getNetwork()) ;
	}

}

