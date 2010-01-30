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
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.knowledges.Knowledges;

import playground.mfeil.ActChainEqualityCheck;
import playground.mfeil.attributes.AgentsAttributesAdder;

/**
 * Simple class to analyze the selected plans of a plans (output) file. Extracts the 
 * activity chains and their number of occurrences.
 *
 * @author mfeil
 */
public class ASPActivityChains {

	protected final PopulationImpl populationMATSim;
	protected PopulationImpl populationMZ;
	protected String outputDir;
	protected ArrayList<List<PlanElement>> activityChains;
	protected ArrayList<ArrayList<Plan>> plans;
	protected ArrayList<ArrayList<Plan>> plansMZ;
	protected Map<String,Double> minimumTime;
	protected Knowledges knowledges;
	private Map<Id, Double> agentsWeight;
	private static final Logger log = Logger.getLogger(ASPActivityChains.class);
	


	public ASPActivityChains(final PopulationImpl populationMATSim, final PopulationImpl populationMZ, Knowledges knowledges, final String outputDir) {
		this.populationMATSim = populationMATSim;
		this.populationMZ = populationMZ;
		this.outputDir = outputDir;
		this.knowledges = knowledges;
		this.minimumTime = new TreeMap<String, Double>();
		this.minimumTime.put("home", 7200.0);
		this.minimumTime.put("work", 3600.0);
		this.minimumTime.put("shopping", 1800.0);
		this.minimumTime.put("leisure", 3600.0);
		this.minimumTime.put("education_higher", 3600.0);
		this.minimumTime.put("education_kindergarten", 3600.0);
		this.minimumTime.put("education_other", 3600.0);
		this.minimumTime.put("education_primary", 3600.0);
		this.minimumTime.put("education_secondary", 3600.0);
		this.minimumTime.put("shop", 3600.0);
		this.minimumTime.put("work_sector2", 3600.0);
		this.minimumTime.put("work_sector3", 3600.0);
		this.minimumTime.put("tta", 3600.0);
		this.minimumTime.put("w", 3600.0);
		this.minimumTime.put("h", 7200.0);
	}
	
	public ASPActivityChains(final PopulationImpl population) {
		this.populationMATSim = population;
	}
	
	public ArrayList<List<PlanElement>> getActivityChains (){
		return this.activityChains;
	}
	
	public ArrayList<ArrayList<Plan>> getPlans (){
		return this.plans;
	}
	
	/**
	 * Method that reads all plans and extracts all existing activity chains. Parallel to the activity chains, it writes the plans in a list for
	 * later examination
	 */
	private void initAnalysisForMatsim(){
		
		this.activityChains = new ArrayList<List<PlanElement>>();
		this.plans = new ArrayList<ArrayList<Plan>>();
		ActChainEqualityCheck ac = new ActChainEqualityCheck();
		for (Person person : this.populationMATSim.getPersons().values()) {
			boolean alreadyIn = false;
			for (int i=0;i<this.activityChains.size();i++){
				if (ac.checkEqualActChains(person.getSelectedPlan().getPlanElements(), this.activityChains.get(i))){
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
	
	private void initAnalysisForMZ(String attributesInputFile){
		
		AgentsAttributesAdder aaa = new AgentsAttributesAdder();
		aaa.runMZ(attributesInputFile);
		this.agentsWeight = aaa.getAgentsWeight(); 
		
		log.info("Dropping persons from population...");
		// Quite strange coding but throws ConcurrentModificationException otherwise...
		Object [] a = this.populationMZ.getPersons().values().toArray();
		for (int i=a.length-1;i>=0;i--){
			PersonImpl person = (PersonImpl) a[i];
			if (!this.agentsWeight.containsKey(person.getId())) this.populationMZ.getPersons().remove(person.getId());
		}
		log.info("done... Size of population is "+this.populationMZ.getPersons().size()+".");
		
		this.plansMZ = new ArrayList<ArrayList<Plan>>();
		ActChainEqualityCheck ac = new ActChainEqualityCheck();
		for (Person person : this.populationMATSim.getPersons().values()) {
			boolean alreadyIn = false;
			for (int i=0;i<this.activityChains.size();i++){
				if (ac.checkEqualActChains(person.getSelectedPlan().getPlanElements(), this.activityChains.get(i))){
					plansMZ.get(i).add(person.getSelectedPlan());
					alreadyIn = true;
					break;
				}
			}
			if (!alreadyIn){
				this.activityChains.add(person.getSelectedPlan().getPlanElements());
				this.plansMZ.add(new ArrayList<Plan>());
				this.plansMZ.get(this.plansMZ.size()-1).add(person.getSelectedPlan());
			}
		}
	}
	
	/*private void linkRouteOrigPlans (){
		log.info("Adding links and routes to original plans...");
		for (Person person : this.population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			this.linker.run(plan);
			for (int j=1;j<plan.getPlanElements().size();j++){
				if (j%2==1){
					this.router.handleLeg((LegImpl)plan.getPlanElements().get(j), (ActivityImpl)plan.getPlanElements().get(j-1), (ActivityImpl)plan.getPlanElements().get(j+1), ((ActivityImpl)plan.getPlanElements().get(j-1)).getEndTime());
				}
				else {
					((ActivityImpl)(plan.getPlanElements().get(j))).setStartTime(((LegImpl)(plan.getPlanElements().get(j-1))).getArrivalTime());
					if (j!=plan.getPlanElements().size()-1){
						((ActivityImpl)(plan.getPlanElements().get(j))).setEndTime(java.lang.Math.max(((ActivityImpl)(plan.getPlanElements().get(j))).getStartTime()+1, ((ActivityImpl)(plan.getPlanElements().get(j))).getEndTime()));
						((ActivityImpl)(plan.getPlanElements().get(j))).setDuration(((ActivityImpl)(plan.getPlanElements().get(j))).getEndTime()-((ActivityImpl)(plan.getPlanElements().get(j))).getStartTime());
					}
				}
			}
		}
		log.info("done.");
	}*/
	
	protected void checkCorrectness(){
		for (int i=0;i<this.plans.size();i++){
			for (int j=0;j<this.plans.get(i).size();j++){
				for (int k=0;k<this.plans.get(i).get(j).getPlanElements().size()-2;k+=2){
					if (k==0){
						if ((((ActivityImpl)(this.plans.get(i).get(j).getPlanElements().get(k))).getDuration()+((ActivityImpl)(this.plans.get(i).get(j).getPlanElements().get(this.plans.get(i).get(j).getPlanElements().size()-1))).getDuration())<this.minimumTime.get(((ActivityImpl)(this.plans.get(i).get(j).getPlanElements().get(k))).getType())-1) { //ignoring rounding errors
							log.warn("Duration error in plan of person "+this.plans.get(i).get(j).getPerson().getId()+" at act position "+(k/2)+" with duration "+(((ActivityImpl)(this.plans.get(i).get(j).getPlanElements().get(k))).getDuration()+((ActivityImpl)(this.plans.get(i).get(j).getPlanElements().get(this.plans.get(i).get(j).getPlanElements().size()-1))).getDuration()));
							break;
						}
					}
					else {
						if (((ActivityImpl)(this.plans.get(i).get(j).getPlanElements().get(k))).getDuration()<this.minimumTime.get(((ActivityImpl)(this.plans.get(i).get(j).getPlanElements().get(k))).getType())-1) { //ignoring rounding errors
							log.warn("Duration error in plan of person "+this.plans.get(i).get(j).getPerson().getId()+" at act position "+(k/2)+" with duration "+(((ActivityImpl)(this.plans.get(i).get(j).getPlanElements().get(k))).getDuration()));
							break;
						}
						if (((ActivityImpl)(this.plans.get(i).get(j).getPlanElements().get(k))).getType().equalsIgnoreCase("home") &&
								(((ActivityImpl)(this.plans.get(i).get(j).getPlanElements().get(k-2))).getType().equalsIgnoreCase("home") ||
								((ActivityImpl)(this.plans.get(i).get(j).getPlanElements().get(k+2))).getType().equalsIgnoreCase("home"))) { 
							log.warn("Consecutive home acts found in plan of "+this.plans.get(i).get(j).getPerson().getId()+" at act position "+(k/2));
							break;
						}
					}
				}
				for (int k=0;k<this.knowledges.getKnowledgesByPersonId().get(this.plans.get(i).get(j).getPerson().getId()).getActivities(true).size();k++){
					boolean notIn = true;
					for (int l=0;l<this.plans.get(i).get(j).getPlanElements().size()-2;l+=2){
						if (((ActivityImpl)(this.plans.get(i).get(j).getPlanElements().get(l))).getType().equalsIgnoreCase(this.knowledges.getKnowledgesByPersonId().get(this.plans.get(i).get(j).getPerson().getId()).getActivities(true).get(k).getType()) /*&&
								((ActivityImpl)(this.plans.get(i).get(j).getPlanElements().get(l))).getFacilityId().toString().equalsIgnoreCase(this.knowledges.getKnowledgesByPersonId().get(this.plans.get(i).get(j).getPerson().getId()).getActivities(true).get(k).getFacility().getId().toString())*/){
							notIn = false;
							break;
						}
					}
					if (notIn) log.warn("Prim act error in plan of person "+this.plans.get(i).get(j).getPerson().getId()+" for prim act "+this.knowledges.getKnowledgesByPersonId().get(this.plans.get(i).get(j).getPerson().getId()).getActivities(true).get(k));
				}
				
				IdImpl homeId = new IdImpl(0);
				for (int z=0;z<this.knowledges.getKnowledgesByPersonId().get(this.plans.get(i).get(j).getPerson().getId()).getActivities(true).size();z++){
					if (this.knowledges.getKnowledgesByPersonId().get(this.plans.get(i).get(j).getPerson().getId()).getActivities(true).get(z).getType().equalsIgnoreCase("home")){
						homeId = (IdImpl) this.knowledges.getKnowledgesByPersonId().get(this.plans.get(i).get(j).getPerson().getId()).getActivities(true).get(z).getFacility().getId();
					}
				}
				for (int k=0;k<this.plans.get(i).get(j).getPlanElements().size()-2;k+=2){
					if (((ActivityImpl)(this.plans.get(i).get(j).getPlanElements().get(k))).getType().equalsIgnoreCase("home")){
						if (!((ActivityImpl)(this.plans.get(i).get(j).getPlanElements().get(k))).getFacilityId().equals(homeId)){
							log.warn("Non-primary home act found in plan of person "+this.plans.get(i).get(j).getPerson().getId());
							break;
						}
					}
				}
			}
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
		double averageACLengthMZ=0;
		double averageACLengthMatsim=0;
		stream1.println("MZ\t\tMATSim\t\tActivity chain");
		stream1.println("Number of occurrences\tRelative weight\tNumber of occurrences\tRelative weight");
		for (int i=0; i<this.activityChains.size();i++){
			double numberMZ = 0;
			for (int j=0;j<this.plansMZ.get(i).size();j++){
				numberMZ+=this.agentsWeight.get(((Plan)(this.plansMZ.get(i).get(j)).getPerson()));
			}
			stream1.print(numberMZ+"\t"+(numberMZ/4372)+"\t"); // 4372 plans in MZ
			double numberMatsim = this.plans.get(i).size();
			stream1.print(numberMatsim+"\t"+(numberMatsim/this.populationMATSim.getPersons().size())+"\t"); // 172598 plans in MATSim scenario
			double length = this.activityChains.get(i).size();
			averageACLengthMZ+=numberMZ*(java.lang.Math.ceil(length/2));
			averageACLengthMatsim+=numberMatsim*(java.lang.Math.ceil(length/2));
			for (int j=0; j<length;j=j+2){
				stream1.print(((ActivityImpl)(this.activityChains.get(i).get(j))).getType()+"\t");
			}
			stream1.println();
		}
		stream1.println((averageACLengthMZ/4372)+"\t\t"+(averageACLengthMatsim/this.populationMATSim.getPersons().size())+"\tAverage number of activities");
		stream1.println();
		
		int sameCon = 0;
		int sumActs = 0;
		int occSame = 0;
		int occSeveral = 0;
		double numSame = 0;
		double maxSame = 0;
		ArrayList<String> takenActTypes = new ArrayList<String>();
		for (int i=0; i<this.activityChains.size();i++){
			for (int j=0; j<this.plans.get(i).size();j++){
				Plan plan = this.plans.get(i).get(j);
				takenActTypes.clear();
					
				boolean occ = false;
				boolean occSev = false;
				double numPlanSame = 0;
				double maxPlanSame = 0;
				for (int k=0;k<plan.getPlanElements().size()-2;k+=2){
					sumActs++;
					if (((ActivityImpl)(plan.getPlanElements().get(k))).getType().equals(((ActivityImpl)(plan.getPlanElements().get(k+2))).getType())){
						sameCon++;
						occ = true;
					}
					if (!takenActTypes.contains(((ActivityImpl)(plan.getPlanElements().get(k))).getType())){
						takenActTypes.add(((ActivityImpl)(plan.getPlanElements().get(k))).getType());
						boolean foundSth = false;
						int max = 0;
						for (int l=k+2;l<plan.getPlanElements().size()-2;l+=2){
							if (((ActivityImpl)(plan.getPlanElements().get(k))).getType().equals(((ActivityImpl)(plan.getPlanElements().get(l))).getType())){
								if (!foundSth) {
									max += 2;
									numPlanSame += 2;
								}
								else {
									max++;
									numPlanSame++;
								}
								foundSth = true;
							}							
						}
						if (foundSth) occSev = true;
						if (max>maxPlanSame) maxPlanSame = max;
					}
				}
				if (occ) occSame++;
				if (occSev) occSeveral++;
				if (Math.floor(plan.getPlanElements().size()/2)>0) {
					numSame += Double.parseDouble(numPlanSame+"")/Math.floor(plan.getPlanElements().size()/2);
					maxSame += Double.parseDouble(maxPlanSame+"")/Math.floor(plan.getPlanElements().size()/2);
				}
			}
		}
		stream1.println((Double.parseDouble(sameCon+"")/this.populationMATSim.getPersons().size())+"\tAverage number of same consecutive acts per plan");
		stream1.println((Double.parseDouble(sameCon+"")/Double.parseDouble(sumActs+""))+"\tPercentage of same consecutive acts");
		stream1.println((Double.parseDouble(occSame+"")/this.populationMATSim.getPersons().size())+"\tAverage number of occurrences of same acts per plan");
		stream1.println((numSame/this.populationMATSim.getPersons().size())+"\tAverage number of same acts per plan");
		stream1.println((maxSame/occSeveral)+"\tAverage maximum number of same acts per plan");
		stream1.println(Double.parseDouble(occSeveral+"")/this.populationMATSim.getPersons().size()+"\tShare of plans in which same acts occur");
	}
	

	public static void main(final String [] args) {
		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		final String populationFilenameMATSim = "/home/baug/mfeil/data/runs/run0922_initialdemand_20/output_plans.xml";
		final String populationFilenameMZ = "/home/baug/mfeil/data/mz/plans_Zurich10.xml";
		final String outputDir = "/home/baug/mfeil/data/runs/run0922_initialdemand_20";	
		final String attributesInputFile = "/home/baug/mfeil/data/mz/attributes_MZ2005.txt";
		
/*
		final String populationFilename = "./plans/output_plans.xml";
		final String networkFilename = "./plans/network.xml";
		final String facilitiesFilename = "./plans/facilities.xml";
		final String outputDir = "./plans";

*/

/*		final String populationFilename = "C:/Workspace/matsim/output/Test1/output_plans.xml.gz";
		final String networkFilename = "C:/Workspace/matsim/test/scenarios/chessboard/network.xml";
		final String facilitiesFilename = "C:/Workspace/matsim/test/scenarios/chessboard/facilities.xml";
		final String outputDir = "C:/Workspace/matsim/output/Test1";
		*/
		ScenarioImpl scenarioMZ = new ScenarioImpl();
		new MatsimNetworkReader(scenarioMZ).readFile(networkFilename);
		new MatsimFacilitiesReader(scenarioMZ).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenarioMZ).readFile(populationFilenameMZ);


		ScenarioImpl scenarioMATSim = new ScenarioImpl();
		new MatsimNetworkReader(scenarioMATSim).readFile(networkFilename);
		new MatsimFacilitiesReader(scenarioMATSim).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenarioMATSim).readFile(populationFilenameMATSim);

		ASPActivityChains sp = new ASPActivityChains(scenarioMATSim.getPopulation(), scenarioMZ.getPopulation(), scenarioMATSim.getKnowledges(), outputDir);
		sp.initAnalysisForMZ(attributesInputFile);
		sp.initAnalysisForMatsim();
		sp.analyze();
		sp.checkCorrectness();
		
		log.info("Analysis of plan finished.");
	}

}

