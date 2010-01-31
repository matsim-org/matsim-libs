/* *********************************************************************** *
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
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.knowledges.Knowledges;

import playground.mfeil.ActChainEqualityCheck;


/**
 * Simple class to analyze the selected plans of a plans (output) file. Extracts the 
 * activity chains and their number of occurrences.
 *
 * @author mfeil
 */
public class ASPActivityChains {

	protected final PopulationImpl population;
	protected String outputDir;
	protected ArrayList<List<PlanElement>> activityChains;
	protected ArrayList<ArrayList<Plan>> plans;
	protected Map<String,Double> minimumTime;
	protected Knowledges knowledges;
	private static final Logger log = Logger.getLogger(ASPActivityChains.class);
	


	public ASPActivityChains(final PopulationImpl population, Knowledges knowledges, final String outputDir) {
		this.population = population;
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
		this.population = population;
	}
	
	public void run(){
		this.initAnalysis();
	}
	
	public ArrayList<List<PlanElement>> getActivityChains (){
		return this.activityChains;
	}
	
	public ArrayList<ArrayList<Plan>> getPlans (){
		return this.plans;
	}
	
	private void initAnalysis(){
		
		this.activityChains = new ArrayList<List<PlanElement>>();
		this.plans = new ArrayList<ArrayList<Plan>>();
		ActChainEqualityCheck ac = new ActChainEqualityCheck();
		for (Person person : this.population.getPersons().values()) {
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
	
/*
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
*/
	
	
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
		double averageACLength=0;
		stream1.println("Number of occurrences\tActivity chain");
		for (int i=0; i<this.activityChains.size();i++){
			double weight = this.plans.get(i).size();
			stream1.print(weight+"\t");
			double length = this.activityChains.get(i).size();
			averageACLength+=weight*(java.lang.Math.ceil(length/2));
			for (int j=0; j<length;j=j+2){
				stream1.print(((ActivityImpl)(this.activityChains.get(i).get(j))).getType()+"\t");
			}
			stream1.println();
		}
		stream1.println((averageACLength/this.population.getPersons().size())+"\tAverage number of activities");
		stream1.println();
		
		double[] kpis= this.analyzeActTypes(null);
		stream1.println(kpis[0]+"\tAverage number of same consecutive acts per plan");
		stream1.println(kpis[1]+"\tPercentage of same consecutive acts");
		stream1.println(kpis[2]+"\tAverage number of occurrences of same acts per plan");
		stream1.println(kpis[3]+"\tAverage number of same acts per plan");
		stream1.println(kpis[4]+"\tAverage maximum number of same acts per plan");
		stream1.println(kpis[5]+"\tShare of plans in which same acts occur");
	}
	
	public double[] analyzeActTypes (final Map<Id, Double> personsWeights){
		
		double overall = 0;
		for (int i=0; i<this.plans.size();i++){
			for (int j=0;j<this.plans.get(i).size();j++){
				if (personsWeights!=null) overall += personsWeights.get(((Plan)this.plans.get(i).get(j)).getPerson().getId());
				else overall += this.plans.get(i).size();
			}
		}
		
		int sameCon = 0;
		int sumActs = 0;
		int occSame = 0;
		int occSeveral = 0;
		double numSame = 0;
		double maxSame = 0;
		
		ArrayList<String> takenActTypes = new ArrayList<String>();
		
		for (int i=0; i<this.activityChains.size();i++){
			for (int j=0; j<this.plans.get(i).size();j++){
				
				double weight = -1;
				Plan plan = this.plans.get(i).get(j);
				if (personsWeights!=null) weight = personsWeights.get(plan.getPerson().getId());
				else weight = 1;
				if (weight<0) log.warn("An invalid weight of "+weight+" has been assigned to person "+plan.getPerson().getId());
				
				takenActTypes.clear();
						
				boolean occ = false;
				boolean occSev = false;
				double numPlanSame = 0;
				double maxPlanSame = 0;
				for (int k=0;k<plan.getPlanElements().size()-2;k+=2){
					sumActs+=weight;
					if (((ActivityImpl)(plan.getPlanElements().get(k))).getType().equals(((ActivityImpl)(plan.getPlanElements().get(k+2))).getType())){
						sameCon+=weight;
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
				if (occ) occSame+=weight;
				if (occSev) occSeveral+=weight;
				if (Math.floor(plan.getPlanElements().size()/2)>0) {
					numSame += weight*Double.parseDouble(numPlanSame+"")/Math.floor(plan.getPlanElements().size()/2);
					maxSame += weight*Double.parseDouble(maxPlanSame+"")/Math.floor(plan.getPlanElements().size()/2);
				}
			}
		}
		
		double [] kpis = new double[5];
		kpis[0] = Double.parseDouble(sameCon+"")/overall;
		kpis[1] = Double.parseDouble(sameCon+"")/sumActs;
		kpis[2] = Double.parseDouble(occSame+"")/overall;
		kpis[3] = numSame/overall;
		kpis[4] = maxSame/occSeveral;
		kpis[5] = Double.parseDouble(occSeveral+"")/overall;
		return kpis;
		/*
		stream1.println((Double.parseDouble(sameCon+"")/this.population.getPersons().size())+"\tAverage number of same consecutive acts per plan");
		stream1.println((Double.parseDouble(sameCon+"")/Double.parseDouble(sumActs+""))+"\tPercentage of same consecutive acts");
		stream1.println((Double.parseDouble(occSame+"")/this.population.getPersons().size())+"\tAverage number of occurrences of same acts per plan");
		stream1.println((numSame/this.population.getPersons().size())+"\tAverage number of same acts per plan");
		stream1.println((maxSame/occSeveral)+"\tAverage maximum number of same acts per plan");
		stream1.println(Double.parseDouble(occSeveral+"")/this.population.getPersons().size()+"\tShare of plans in which same acts occur");
		*/
	}
	

	public static void main(final String [] args) {
/*		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		final String populationFilename = "/home/baug/mfeil/data/choiceSet/it1/run151/output_plans.xml";
		final String outputDir = "/home/baug/mfeil/data/choiceSet/it1/run151";		
*/
		final String populationFilename = "./plans/output_plans.xml";
		final String networkFilename = "./plans/network.xml";
		final String facilitiesFilename = "./plans/facilities.xml";
		final String outputDir = "./plans";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		ASPActivityChains sp = new ASPActivityChains(scenario.getPopulation(), scenario.getKnowledges(), outputDir);
		sp.run();
		sp.analyze();
		sp.checkCorrectness();
		
		log.info("Analysis of plan finished.");
	}

}

