/* *********************************************************************** *
 * project: org.matsim.*
 * PlansConstructor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.mfeil.MDSAM;



import java.io.File;
import java.util.TreeMap;
import java.util.Map;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.locationchoice.constrained.ManageSubchains;
import org.matsim.locationchoice.constrained.SubChain;
import org.matsim.population.algorithms.XY2Links;

import playground.mfeil.ActChainEqualityCheck;
import playground.mfeil.analysis.ASPActivityChainsModes;
import playground.mfeil.analysis.ASPActivityChainsModesAccumulated;
import playground.mfeil.attributes.AgentsAttributesAdder;



/**
 * @author Matthias Feil
 * Class that reads a file of plans and constructs an estimation choice set from that.
 */


public class PlansConstructor implements PlanStrategyModule{

	protected Controler controler;
	protected String inputFile, outputFile, outputFileBiogeme, attributesInputFile, outputFileMod, outputFileSimsOverview, outputFileSimsDetailLog;
	protected PopulationImpl population;
	protected ArrayList<List<PlanElement>> actChains;
	protected NetworkImpl network;
	protected PlansCalcRoute router;
	protected LocationMutatorwChoiceSet locator;
	protected MDSAM mdsam;
	protected XY2Links linker;
	protected Map<Id, List<Double>> sims; // indicates the similarity of a person's plans with all other plans
	protected Map<Id, int[]> simsPosition; // indicates which activity chain alternative the plan/similarity value belongs to
	protected static final Logger log = Logger.getLogger(PlansConstructor.class);
	protected int noOfAlternatives;
	protected String similarity, incomeConstant, incomeDivided, incomeDividedLN, incomeBoxCox, gender, age, license, 
	carAvail, income, seasonTicket, travelDistance, travelCost, travelConstant, bikeIn, beta, gamma, beta_travel, munType, innerHome; 
	protected double travelCostCar, costPtNothing, costPtHalbtax, costPtGA;


	public PlansConstructor (Controler controler) {
		this.controler = controler;
		String version = "0997b";
		this.inputFile = "/home/baug/mfeil/data/choiceSet/it0/output_plans_mz05.xml"; // sonst "/home/baug/mfeil/data/mz/plans_Zurich10.xml";
		this.outputFile = "/home/baug/mfeil/data/choiceSet/it0/output_plans_mzASb.xml.gz";
		this.outputFileBiogeme = "/home/baug/mfeil/data/choiceSet/it0/output_plans"+version+".dat";
		this.attributesInputFile = "/home/baug/mfeil/data/mz/attributes_MZ2005.txt";
		this.outputFileMod = "/home/baug/mfeil/data/choiceSet/it0/model"+version+".mod";
		this.outputFileSimsOverview = "/home/baug/mfeil/data/choiceSet/it0/simsOverview"+version+".xls";
		this.outputFileSimsDetailLog = "/home/baug/mfeil/data/choiceSet/it0/simsDetails"+version+".xls";
		this.population = new PopulationImpl(this.controler.getScenario());
		this.sims = null;
		this.network = controler.getNetwork();
		this.init(network);
		this.router = new PlansCalcRoute (controler.getConfig().plansCalcRoute(), controler.getNetwork(), controler.getTravelCostCalculator(), controler.getTravelTimeCalculator(), controler.getLeastCostPathCalculatorFactory());
		this.locator = new LocationMutatorwChoiceSet(controler.getNetwork(), controler, controler.getScenario().getKnowledges());
		this.linker = new XY2Links (this.controler.getNetwork());
		this.beta				= "yes";
		this.gamma				= "no";
		this.similarity 		= "yes";
		this.incomeConstant 	= "no";
		this.incomeDivided 		= "no";
		this.incomeDividedLN	= "no";
		this.incomeBoxCox 		= "yes";
		this.age 				= "yes";
		this.gender 			= "yes";
		this.income 			= "no";
		this.license 			= "yes";
		this.carAvail 			= "no";
		this.seasonTicket 		= "no";
		this.travelDistance		= "no";
		this.travelCost			= "no";
		this.travelConstant 	= "yes";
		this.beta_travel		= "no";
		this.bikeIn				= "yes";
		this.munType			= "no";
		this.innerHome			= "yes";
		// if incomeBoxCox is set to yes, travelCost must be yes, too.
		if (this.incomeBoxCox.equals("yes")) this.travelCost = "yes";
		
		this.noOfAlternatives 	= 20;
		
		this.travelCostCar		= 0.5;	// CHF/km
		this.costPtNothing		= 0.28;	// CHF/km
		this.costPtHalbtax		= 0.15;	// CHF/km
		this.costPtGA			= 0.08;	// CHF/km
	}

	public PlansConstructor (PopulationImpl population, String simsOverviewLog, String simsDetailLog) {
		this.population = population;
		this.outputFileSimsOverview = simsOverviewLog;
		this.outputFileSimsDetailLog = simsDetailLog;
		this.noOfAlternatives 	= 20;
		this.travelCostCar		= 0.5;	// CHF/km
		this.costPtNothing		= 0.28;	// CHF/km
		this.costPtHalbtax		= 0.15;	// CHF/km
		this.costPtGA			= 0.08;	// CHF/km
	}

	private void init(final NetworkImpl network) {
		this.network.connect();
	}

	public void prepareReplanning() {
		// Read the external plans file.
		this.controler.getScenario().setPopulation(this.population);
		new MatsimPopulationReader(this.controler.getScenario()).readFile(this.inputFile);
		log.info("Reading population done.");
	}

	public void handlePlan(final Plan plan) {
		// Do nothing here. We work only on the external plans.
	}

	public void finishReplanning(){

	//	Once-off task to filter Zurich people from overall census people
		//	this.selectZurich10MZPlans();

	// Can be always switch on
		this.rectifyActTypes();

	// Select type of population you want to work with
		this.keepPersons();
		//	this.reducePersonsMostFrequentStructures();
		//	this.reducePersonsRandomly();
		//	this.reducePersonsIntelligently();

	// Needs to always run
	//	this.linkRouteOrigPlans(); // sonst standard

	// Type of enlarging plans set
		//	this.enlargePlansSet();
		//this.enlargePlansSetWithRandomSelection("PlanomatX"); //sonst standard
		this.routeAlternativePlans();

	// Needs to always run
		this.writePlans(this.outputFile);

	// Only if similarity attribute is desired
		//	this.mdsam = new MDSAM(this.population);
		//	this.sims = this.mdsam.runPopulation();
		//	this.writeSims(this.outputFileSims);

	// Type of writing the Biogeme dat file
		//	this.writePlansForBiogeme(this.outputFileBiogeme);
		//this.writePlansForBiogemeWithRandomSelection(this.outputFileBiogeme, this.attributesInputFile, 
		//		this.similarity, this.incomeConstant, this.incomeDivided, this.incomeDividedLN, this.incomeBoxCox, this.age, this.gender, this.employed, this.license, this.carAvail, this.seasonTicket, this.travelDistance, this.travelCost, this.travelConstant, this.bikeIn);	
		this.writePlansForBiogemeWithRandomSelectionAccumulated(this.outputFileBiogeme, this.attributesInputFile, 
				this.beta, this.gamma, this.similarity, this.incomeConstant, this.incomeDivided, this.incomeDividedLN, 
				this.incomeBoxCox, this.age, this.gender, this.income, this.license, this.carAvail, this.seasonTicket, 
				this.travelDistance, this.travelCost, this.travelConstant, this.beta_travel, this.bikeIn, this.munType, this.innerHome);	
		
	// Type of writing the mod file
		//	this.writeModFile(this.outputFileMod);
		//	this.writeModFileWithSequence(this.outputFileMod);
		this.writeModFileWithRandomSelection(this.outputFileMod);
	}


	//////////////////////////////////////////////////////////////////////
	// Methods running always
	//////////////////////////////////////////////////////////////////////


	protected void rectifyActTypes (){
		log.info("Rectifying act types...");
		for (Person person : this.population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			for (int i=0;i<plan.getPlanElements().size();i+=2){
				ActivityImpl act = (ActivityImpl) plan.getPlanElements().get(i);
				/*if (act.getType().equalsIgnoreCase("h")) act.setType("home");
				else if (act.getType().equalsIgnoreCase("w")) act.setType("work");
				else if (act.getType().equalsIgnoreCase("e")) act.setType("education");
				else*/ if (act.getType().equalsIgnoreCase("s")) act.setType("shop");
				else if (act.getType().equalsIgnoreCase("l")) act.setType("leisure");
	//			else if (act.getType().equalsIgnoreCase("h") && i!=0 && i!=plan.getPlanElements().size()-1) act.setType("h_inner");
				//else log.warn("Unknown act detected: "+act.getType());
			}
		}
		log.info("done... ");
	}


	private void linkRouteOrigPlans (){
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
	}
	
	private void routeAlternativePlans (){
		log.info("Adding routes and travel times to alternative plans...");
		for (Person person : this.population.getPersons().values()) {
			int counter = 0;
			for (int i=0;i<person.getPlans().size();i++){
				PlanImpl plan = (PlanImpl) person.getPlans().get(i);
				counter++;
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
			if (counter!=20)log.warn("Something went wrong when routing the alternative plans. Counter is "+counter);
		}
		log.info("done.");
	}


	protected void writePlans(String outputFile){
		log.info("Writing plans...");
		new PopulationWriter(this.population, this.network).writeFile(outputFile);
		log.info("done.");
	}


	//////////////////////////////////////////////////////////////////////
	// Methods reducing/refactoring the population
	//////////////////////////////////////////////////////////////////////

	protected void reducePersonsMostFrequentStructures (){
		// Drop those persons whose plans do not belong to x most frequent activity chains.
		log.info("Analyzing activitiy chains...");
		ASPActivityChainsModes analyzer = new ASPActivityChainsModes(this.population);
		analyzer.run();
		ArrayList<List<PlanElement>> ac = analyzer.getActivityChains();
		ArrayList<ArrayList<Plan>> pl = analyzer.getPlans();
		log.info("done.");
		List<Integer> ranking = new ArrayList<Integer>();
		for (int i=0;i<pl.size();i++){
			ranking.add(pl.get(i).size());
		}
		java.util.Collections.sort(ranking);
		this.actChains = new ArrayList<List<PlanElement>>();
		List<Id> agents = new LinkedList<Id>();
		for (int i=0;i<pl.size();i++){
			if (pl.get(i).size()>=ranking.get(java.lang.Math.max(ranking.size()-51,0))){ //51
//			if (pl.get(i).size()>=ranking.get(java.lang.Math.max(ranking.size()-2,0))){
				this.actChains.add(ac.get(i));
				for (Iterator<Plan> iterator = pl.get(i).iterator(); iterator.hasNext();){
					Plan plan = iterator.next();
					agents.add(plan.getPerson().getId());
				}
			}
		}
		log.info("Dropping persons from population...");
		// Quite strange coding but throws ConcurrentModificationException otherwise...
		Object [] a = this.population.getPersons().values().toArray();
		for (int i=a.length-1;i>=0;i--){
			PersonImpl person = (PersonImpl) a[i];
			if (!agents.contains(person.getId())) this.population.getPersons().remove(person.getId());
		}
		log.info("done... Size of population is "+this.population.getPersons().size()+".");
	}

	protected void reducePersonsRandomly (){
		// Select randomly actchainsmodes accumulated.
		log.info("Analyzing activitiy chains...");
		ASPActivityChainsModesAccumulated analyzer = new ASPActivityChainsModesAccumulated(this.population);
		analyzer.run();
		ArrayList<List<PlanElement>> ac = analyzer.getActivityChains();
		ArrayList<ArrayList<Plan>> pl = analyzer.getPlans();
		log.info("done.");

		this.actChains = new ArrayList<List<PlanElement>>();
		List<Id> agents = new LinkedList<Id>();
		ArrayList<Integer> randomField = new ArrayList<Integer>();

		for (int i=0;i<this.noOfAlternatives;i++){
			int random = ((int) (MatsimRandom.getRandom().nextDouble()*ac.size()));
			while (randomField.contains(random)){
				random = ((int) (MatsimRandom.getRandom().nextDouble()*ac.size()));
			}
			randomField.add(random);
			this.actChains.add(ac.get(random));
			for (Iterator<Plan> iterator = pl.get(random).iterator(); iterator.hasNext();){
				Plan plan = iterator.next();
				agents.add(plan.getPerson().getId());
			}
		}

		log.info("Dropping persons from population...");
		// Quite strange coding but throws ConcurrentModificationException otherwise...
		Object [] a = this.population.getPersons().values().toArray();
		for (int i=a.length-1;i>=0;i--){
			PersonImpl person = (PersonImpl) a[i];
			if (!agents.contains(person.getId())) this.population.getPersons().remove(person.getId());
		}
		log.info("done... Size of population is "+this.population.getPersons().size()+".");
	}

	protected void reducePersonsIntelligently (){
		// Take 20 most frequent actchainmodes accumulated plus 40 longest ones.
		log.info("Analyzing activitiy chains...");
		ASPActivityChainsModesAccumulated analyzer = new ASPActivityChainsModesAccumulated(this.population);
		analyzer.run();
		ArrayList<List<PlanElement>> ac = analyzer.getActivityChains();
		ArrayList<ArrayList<Plan>> pl = analyzer.getPlans();
		log.info("done.");

		List<Integer> ranking = new ArrayList<Integer>();

		// most frequent chains
		for (int i=0;i<pl.size();i++){
			ranking.add(pl.get(i).size());
		}
		java.util.Collections.sort(ranking);
		this.actChains = new ArrayList<List<PlanElement>>();
		List<Id> agents = new LinkedList<Id>();
		for (int i=0;i<pl.size();i++){
			if (pl.get(i).size()>=ranking.get(java.lang.Math.max(ranking.size()-20,0))){
				this.actChains.add(ac.get(i));
				for (Iterator<Plan> iterator = pl.get(i).iterator(); iterator.hasNext();){
					Plan plan = iterator.next();
					agents.add(plan.getPerson().getId());
				}
			}
		}
		ranking.clear();

		// longest chains
		for (int i=0;i<ac.size();i++){
			ranking.add(ac.get(i).size());
		}
		java.util.Collections.sort(ranking);
		for (int i=0;i<ac.size();i++){
			if ((ac.get(i).size()>=ranking.get(java.lang.Math.max(ranking.size()-40,0)))  &&
				!this.actChains.contains(ac.get(i))	){
				this.actChains.add(ac.get(i));
				for (Iterator<Plan> iterator = pl.get(i).iterator(); iterator.hasNext();){
					Plan plan = iterator.next();
					agents.add(plan.getPerson().getId());
				}
			}
		}


		log.info("Dropping persons from population...");
		// Quite strange coding but throws ConcurrentModificationException otherwise...
		Object [] a = this.population.getPersons().values().toArray();
		for (int i=a.length-1;i>=0;i--){
			PersonImpl person = (PersonImpl) a[i];
			if (!agents.contains(person.getId())) this.population.getPersons().remove(person.getId());
		}
		log.info("done... Size of population is "+this.population.getPersons().size()+".");
	}


	protected void keepPersons (){
		// Keep all persons of Zurich10, reduce choice set later on.
		log.info("Analyzing activitiy chains...");
		ASPActivityChainsModesAccumulated analyzer = new ASPActivityChainsModesAccumulated(this.population);
		analyzer.run();
		this.actChains = analyzer.getActivityChains();
		log.info("done... Size of population is "+this.population.getPersons().size()+".");
	}



	//////////////////////////////////////////////////////////////////////
	// Methods enlarging the choice set
	//////////////////////////////////////////////////////////////////////

	protected void enlargePlansSet (){
		log.info("Adding alternative plans...");
		int counter=0;
		ActChainEqualityCheck acCheck = new ActChainEqualityCheck();
		for (Person person : this.population.getPersons().values()) {
			counter++;
			if (counter%10==0) {
				log.info("Handled "+counter+" persons");
				Gbl.printMemoryUsage();
			}
			for (int i=0;i<this.actChains.size();i++){

				// Add all plans with activity chains different to the one of person's current plan
				if (!acCheck.checkEqualActChainsModesAccumulated(person.getSelectedPlan().getPlanElements(), this.actChains.get(i))){
				//if (!acCheck.checkEqualActChainsModes(person.getSelectedPlan().getPlanElements(), this.actChains.get(i))){
					PlanImpl plan = new PlanImpl (person);

					for (int j=0;j<this.actChains.get(i).size();j++){
						if (j%2==0) {
							ActivityImpl act = new ActivityImpl((ActivityImpl)this.actChains.get(i).get(j));
							if (/*j!=0 && */j!=this.actChains.get(i).size()-1) {
								act.setEndTime(MatsimRandom.getRandom().nextDouble()*act.getDuration()*2+act.getStartTime());
								if ((j!=0) && !act.getType().equalsIgnoreCase("h")) {
									this.modifyLocationCoord(act);
								}
								else if (act.getType().equalsIgnoreCase("h")) {
									act.setCoord(((PlanImpl) person.getSelectedPlan()).getFirstActivity().getCoord());
								}
							}
							plan.addActivity(act);
						}
						else {
							LegImpl leg = new LegImpl((LegImpl)this.actChains.get(i).get(j));
							plan.addLeg(leg);
						}
					}

					plan.getFirstActivity().setCoord(((PlanImpl) person.getSelectedPlan()).getFirstActivity().getCoord());
					plan.getLastActivity().setCoord(((PlanImpl) person.getSelectedPlan()).getLastActivity().getCoord());

					this.linker.run(plan);

					/* Analysis of subtours and random allocation of modes to subtours
					PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours();
					planAnalyzeSubtours.run(plan);
					for (int j=0;j<planAnalyzeSubtours.getNumSubtours();j++){
						TransportMode[]	modes = TimeModeChoicerConfigGroup.getPossibleModes();
						TransportMode mode = modes[(int)(MatsimRandom.getRandom().nextDouble()*modes.length)];
						for (int k=1;k<plan.getPlanElements().size();k+=2){
							if (planAnalyzeSubtours.getSubtourIndexation()[(k-1)/2]==j){
								((LegImpl)plan.getPlanElements().get(k)).setMode(mode);
							}
						}
					}*/

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
					// if plan too long make it invalid (set score to -100000)
					if (plan.getLastActivity().getStartTime()-86400>plan.getFirstActivity().getEndTime()){
						plan.setScore(-100000.0);
					}
					person.addPlan(plan);
				}
			}
		}
		log.info("done.");
	}

	protected void enlargePlansSetWithRandomSelection (String locationChoice){

		log.info("Adding alternative plans...");
		int counter=0;
		ActChainEqualityCheck acCheck = new ActChainEqualityCheck();

		for (Person person : this.population.getPersons().values()) {
			counter++;
			if (counter%10==0) {
				log.info("Handling person "+counter);
				Gbl.printMemoryUsage();
			}
			ArrayList<Integer> taken = new ArrayList<Integer>();

			for (int i=0;i<this.noOfAlternatives-1;i++){

				// Randomly select an act/mode chain different to the chosen one.
				int position = 0;
				boolean isValidPlan = true;
				PlanImpl plan = new PlanImpl (person);
				do {
					isValidPlan = true;
					do {
						position = (int)(MatsimRandom.getRandom().nextDouble()*this.actChains.size());
					} while(taken.contains(position) || acCheck.checkEqualActChainsModesAccumulated(person.getSelectedPlan().getPlanElements(), this.actChains.get(position)));
					taken.add(position);

					plan = new PlanImpl (person);
					for (int j=0;j<this.actChains.get(position).size();j++){
						if (j%2==0) {
							ActivityImpl act = new ActivityImpl((ActivityImpl)this.actChains.get(position).get(j));
							//Timing
							if (j!=this.actChains.get(position).size()-1) {
								act.setEndTime(MatsimRandom.getRandom().nextDouble()*act.getDuration()*2+act.getStartTime());
							}
							//Location if "primary"
							if (act.getType().equalsIgnoreCase("w") || act.getType().equalsIgnoreCase("e") || act.getType().equalsIgnoreCase("h") || act.getType().equalsIgnoreCase("h_inner")){
								for (int k=0;k<person.getSelectedPlan().getPlanElements().size();k+=2){
									if (act.getType().equalsIgnoreCase(((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(k))).getType())){
										act.setCoord(((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(k))).getCoord());
										break;
									}
									else if (act.getType().equalsIgnoreCase("h_inner") && ((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(k))).getType().equalsIgnoreCase("h")){
										act.setCoord(((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(k))).getCoord());
										break;
									}
									// If primary act cannot be found in selectedPlan
									this.modifyLocationCoord(act);
								}
							}
							plan.addActivity(act);
						}
						else {
							LegImpl leg = new LegImpl((LegImpl)this.actChains.get(position).get(j));
							plan.addLeg(leg);
						}
					}

					// Adopting PlanomatX for secondary location choice
					if (locationChoice.equalsIgnoreCase("PlanomatX")){
						this.locator.handleSubChains(plan, this.getSubChains(plan));
					}

					// Adopting random location choice
					else {
						List<SubChain> subChains = this.getSubChains(plan);
						for (Iterator<SubChain> iteratorSubChain = subChains.iterator(); iteratorSubChain.hasNext();){
							for (Iterator<ActivityImpl> iteratorActs = iteratorSubChain.next().getSlActs().iterator(); iteratorActs.hasNext();){
								this.modifyLocationCoord(iteratorActs.next());
							}
						}
					}

					this.linker.run(plan);

					for (int j=0;j<plan.getPlanElements().size();j++){
						if (j%2==1){
							this.router.handleLeg((LegImpl)plan.getPlanElements().get(j), (ActivityImpl)plan.getPlanElements().get(j-1), (ActivityImpl)plan.getPlanElements().get(j+1), ((ActivityImpl)plan.getPlanElements().get(j-1)).getEndTime());
						}
						else {
							if (j!=0)((ActivityImpl)(plan.getPlanElements().get(j))).setStartTime(((LegImpl)(plan.getPlanElements().get(j-1))).getArrivalTime());
							if (j!=plan.getPlanElements().size()-1){
								((ActivityImpl)(plan.getPlanElements().get(j))).setEndTime(java.lang.Math.max(((ActivityImpl)(plan.getPlanElements().get(j))).getStartTime()+1, ((ActivityImpl)(plan.getPlanElements().get(j))).getEndTime()));
								((ActivityImpl)(plan.getPlanElements().get(j))).setDuration(((ActivityImpl)(plan.getPlanElements().get(j))).getEndTime()-((ActivityImpl)(plan.getPlanElements().get(j))).getStartTime());
							}
						}
					}
					// if plan too long make it invalid (set score to -100000)
					if (plan.getLastActivity().getStartTime()-86400>plan.getFirstActivity().getEndTime()){
						plan.setScore(-100000.0);
						isValidPlan = false;
					}
				} while (!isValidPlan);
				person.addPlan(plan);

			}
		}
		log.info("done.");
	}


	//////////////////////////////////////////////////////////////////////
	// Location help methods
	//////////////////////////////////////////////////////////////////////

	protected void modifyLocation (ActivityImpl act){
		log.info("Start modify.");
		ActivityFacilitiesImpl afImpl = (ActivityFacilitiesImpl) this.controler.getFacilities();

		String actType = null;
		if (act.getType().equalsIgnoreCase("w")) actType = "work_sector2";
		else if (act.getType().equalsIgnoreCase("e")) actType = "education_higher";
		else if (act.getType().equalsIgnoreCase("s")) actType = "shop";
		else if (act.getType().equalsIgnoreCase("l")) actType = "leisure";
		else log.warn("Unerkannter act type: "+act.getType());

		List <ActivityFacilityImpl> facs = new ArrayList<ActivityFacilityImpl>(afImpl.getFacilitiesForActivityType(actType).values());
		ActivityFacilityImpl fac;
		do {
			int position = (int) (MatsimRandom.getRandom().nextDouble()*facs.size());
			fac = facs.get(position);
		} while (CoordUtils.calcDistance(fac.getCoord(), new CoordImpl(683518.0,246836.0))>30000);
		act.setCoord(fac.getCoord());
	}

	protected void modifyLocationCoord (ActivityImpl act){
		// circle around Zurich centre
		double X = 683518.0 - 30000 + java.lang.Math.floor(MatsimRandom.getRandom().nextDouble()*60000);
		double Y = 246836.0 - Math.sqrt(Math.abs(30000*30000-(683518-X)*(683518-X))) + java.lang.Math.floor(MatsimRandom.getRandom().nextDouble()*Math.sqrt(Math.abs(30000*30000-(683518-X)*(683518-X)))*2);
		act.setCoord(new CoordImpl(X, Y));
	}

	protected List<SubChain> getSubChains (PlanImpl plan){
		ManageSubchains manager = new ManageSubchains();
		for (int i=0;i<plan.getPlanElements().size()-4;i+=2){
			ActivityImpl act = (ActivityImpl)(plan.getPlanElements().get(i));
			ActivityImpl actFollowing = (ActivityImpl)(plan.getPlanElements().get(i+2));
			if ((act.getType().equalsIgnoreCase("w") || act.getType().equalsIgnoreCase("e") || act.getType().equalsIgnoreCase("h") || act.getType().equalsIgnoreCase("h_inner")) &&
					!(actFollowing.getType().equalsIgnoreCase("w") || actFollowing.getType().equalsIgnoreCase("e") || actFollowing.getType().equalsIgnoreCase("h") || actFollowing.getType().equalsIgnoreCase("h_inner"))){
				manager.primaryActivityFound(act, (LegImpl)(plan.getPlanElements()).get(i+1));
				while (!(actFollowing.getType().equalsIgnoreCase("w") || actFollowing.getType().equalsIgnoreCase("e") || actFollowing.getType().equalsIgnoreCase("h") || actFollowing.getType().equalsIgnoreCase("h_inner"))){
					i+=2;
					act = (ActivityImpl)(plan.getPlanElements().get(i));
					actFollowing = (ActivityImpl)(plan.getPlanElements().get(i+2));
					manager.secondaryActivityFound(act, (LegImpl)(plan.getPlanElements()).get(i+1));
				}
				manager.primaryActivityFound(actFollowing, null);
			}
		}
		return manager.getSubChains();
	}


	//////////////////////////////////////////////////////////////////////
	// Writing output plans methods
	//////////////////////////////////////////////////////////////////////

	public void writePlansForBiogeme(String outputFile){
		log.info("Writing plans for Biogeme...");
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		// First row
		stream.print("Id\tChoice\t");
		Person p = this.population.getPersons().values().iterator().next();
		for (int i = 0;i<p.getPlans().size();i++){
			int j=0;
			for (j =0;j<java.lang.Math.max(p.getPlans().get(i).getPlanElements().size()-1,1);j++){
				stream.print("x"+(i+1)+""+(j+1)+"\t");
			}
			stream.print("x"+(i+1)+""+(j+1)+"\t");
		}
		for (int i = 0;i<p.getPlans().size();i++){
			stream.print("av"+(i+1)+"\t");
		}
		stream.println();

		// Filling plans
		int counterPerson = -1;
		boolean firstPersonFound = true;
		Id firstPersonId = new IdImpl (1);
		for (Person person : this.population.getPersons().values()){
			if (firstPersonFound){
				firstPersonId = person.getId();
				firstPersonFound = false;
			}
			counterPerson++;
			stream.print(person.getId()+"\t");
			int position = -1;
			for (int i=0;i<person.getPlans().size();i++){
				if (person.getPlans().get(i).equals(person.getSelectedPlan())) {
					position = i+1;
					break;
				}
			}
			stream.print(position+"\t");
			int counterPlan = -1;
			for (Plan plan : person.getPlans()){
				counterPlan++;
				Plan planFirstPerson = this.population.getPersons().get(firstPersonId).getPlans().get(counterPlan);

				// Plan has only one act
				if (plan.getPlanElements().size()==1) stream.print("24\t");

				else {
					// First and last home act
					stream.print((((((PlanImpl) plan).getFirstActivity())).getEndTime()+86400-((((PlanImpl) plan).getLastActivity())).getStartTime())/3600+"\t");

					// All inner acts
					/*// Old version with fixed act/mode chain
					for (int i=1;i<plan.getPlanElements().size()-1;i++){
						if (i%2==0) stream.print(((ActivityImpl)(plan.getPlanElements().get(i))).calculateDuration()/3600+"\t");
						else stream.print(((LegImpl)(plan.getPlanElements().get(i))).getTravelTime()/3600+"\t");
					}*/
					// New version with just identical number of acts and modes but no regard to position
					LinkedList<Integer> takenPositions = new LinkedList<Integer>();
					for (int i=1;i<planFirstPerson.getPlanElements().size()-1;i++){
						if (i%2==0){
							for (int j=2;j<plan.getPlanElements().size()-2;j+=2){
								if (((ActivityImpl)(planFirstPerson.getPlanElements().get(i))).getType().equals(((ActivityImpl)(plan.getPlanElements().get(j))).getType()) &&
										!takenPositions.contains(j)){
									stream.print(((ActivityImpl)(plan.getPlanElements().get(j))).calculateDuration()/3600+"\t");
									takenPositions.add(j);
									break;
								}
							}
						}
						else {
							for (int j=1;j<plan.getPlanElements().size()-1;j+=2){
								if (((LegImpl)(planFirstPerson.getPlanElements().get(i))).getMode().equals(((LegImpl)(plan.getPlanElements().get(j))).getMode()) &&
										!takenPositions.contains(j)){
									stream.print(((LegImpl)(plan.getPlanElements().get(j))).getTravelTime()/3600+"\t");
									takenPositions.add(j);
									break;
								}
							}
						}
					}
				}

				// Similarity attribute
				stream.print(this.sims.get(counterPerson).get(counterPlan)+"\t");
			}
			for (Plan plan : person.getPlans()){

				// Plan not executable, drop from choice set
				if ((plan.getScore()!=null) && (plan.getScore()==-100000.0))	stream.print(0+"\t");

				// Plan executable
				else stream.print(1+"\t");
			}
			stream.println();
		}
		stream.close();
		log.info("done.");
	}



	//****************************************************************************************
	// Writes a Biogeme dat file that fits "protected void enlargePlansSetWithRandomSelection ()"
	//****************************************************************************************
	
	
	public void writePlansForBiogemeWithRandomSelectionAccumulated (String outputFile, String attributesInputFile,
			String beta,
			String gamma,
			String similarity,
			String incomeConstant,
			String incomeDivided,
			String incomeDividedLN,
			String incomeBoxCox,
			String age,
			String gender,
			String income,
			String license,
			String carAvail,
			String seasonTicket,
			String travelDistance,
			String travelCost,
			String travelConstant,
			String beta_travel,
			String bikeIn,
			String munType,
			String innerHome){

		log.info("Writing plans for Biogeme...");

		// Writing the variables back to head of class due to MDSAM call possibility.
		// Like this, they are also available for modMaker class.
		this.beta=beta;
		this.gamma=gamma;
		this.similarity=similarity;
		this.incomeConstant=incomeConstant;
		this.incomeDivided=incomeDivided;
		this.incomeDividedLN=incomeDividedLN;
		this.incomeBoxCox=incomeBoxCox;
		this.age=age;
		this.gender=gender;
		this.income=income;
		this.license=license;
		this.carAvail=carAvail;
		this.seasonTicket=seasonTicket;
		this.travelDistance=travelDistance;
		this.travelCost=travelCost;
		this.travelConstant=travelConstant;
		this.beta_travel=beta_travel;
		this.bikeIn=bikeIn;
		this.munType=munType;
		this.innerHome=innerHome;

		ActChainEqualityCheck acCheck = new ActChainEqualityCheck();
		AgentsAttributesAdder aaa = new AgentsAttributesAdder ();
		int incomeAverage=0;
		int noOfGA = 0;
		int noOfHalbtax = 0;
		int noOfNothing = 0;

		// Run external classes if required
		if (incomeConstant.equals("yes") || incomeDivided.equals("yes") || incomeDividedLN.equals("yes") || incomeBoxCox.equals("yes") || carAvail.equals("yes") || seasonTicket.equals("yes") || travelCost.equals("yes") || income.equals("yes")){
			aaa.runMZ(attributesInputFile);
		}
		if (similarity.equals("yes")){
			this.mdsam = new MDSAM(this.population, this.outputFileSimsDetailLog);
			this.sims = this.mdsam.runPopulation();
			this.simsPosition = new TreeMap<Id, int[]>();
		}

		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		//**************************************************************************
		// First row
		//**************************************************************************
		
		log.info("Writing dat file...");
		
		int counterFirst=0;
		stream.print("Id\tChoice\t");
		counterFirst+=2;

		if (incomeConstant.equals("yes") || incomeDivided.equals("yes") || incomeDividedLN.equals("yes") || income.equals("yes")) {
			stream.print("Income\t");
			counterFirst++;
		}
		if (incomeBoxCox.equals("yes")) {
			stream.print("Income_IncomeAverage\t");
			counterFirst++;

			// Calculate average income
			int counterIncome=0;
			for (Person person : this.population.getPersons().values()) {
				if (!aaa.getIncome().containsKey(person.getId())){
					continue;
				}
				counterIncome++;
				incomeAverage+=aaa.getIncome().get(person.getId());
			}
			incomeAverage=incomeAverage/counterIncome;
		}
		/*if (age.equals("yes")) {
			stream.print("Age_0_15\t");
			stream.print("Age_16_30\t");
			stream.print("Age_31_60\t");
			stream.print("Age_61\t");
			counterFirst+=4;
		}*/
		if (age.equals("yes")) {
			stream.print("Age\t"); 
			counterFirst++;
		}
		if (gender.equals("yes")) {
			stream.print("Female\t");
			counterFirst++;
		}
		if (license.equals("yes")) {
			stream.print("License\t");
			counterFirst++;
		}
		if (carAvail.equals("yes")) {
			stream.print("CarAlways\tCarSometimes\tCarNever\t");
			counterFirst+=3;
		}
	/*	if (seasonTicket.equals("yes")) {
			stream.print("SeasonTicket\t");
			counterFirst++;
		}*/
		if (seasonTicket.equals("yes")) {
			stream.print("SeasonTicket_1\t");
			stream.print("SeasonTicket_2\t");
			stream.print("SeasonTicket_3\t");
			counterFirst+=3;
		}
	/*	if (munType.equals("yes")) {
			stream.print("MunType\t"); 
			counterFirst++;
		}*/
		if (munType.equals("yes")) {
			stream.print("MunType_1\t"); 
			stream.print("MunType_2\t"); 
			stream.print("MunType_3\t"); 
			stream.print("MunType_4\t"); 
			stream.print("MunType_5\t"); 
			counterFirst+=5;
		}

		for (int i = 0;i<this.actChains.size();i++){
			boolean car = false;
			boolean pt = false;
			boolean bike = false;
			boolean walk = false;
			for (int j=0;j<java.lang.Math.max(this.actChains.get(i).size()-1,1);j+=2){
				stream.print("x"+(i+1)+"_"+(j+1)+"\t");
				counterFirst++;
			}
			for (int j=1;j<this.actChains.get(i).size()-1;j+=2){
				LegImpl leg = ((LegImpl)(this.actChains.get(i).get(j)));
				if (leg.getMode().equals(TransportMode.car)) car = true;
				else if (leg.getMode().equals(TransportMode.pt)) pt = true;
				else if (leg.getMode().equals(TransportMode.bike)) bike = true;
				else if (leg.getMode().equals(TransportMode.walk)) walk = true;
				else log.warn("Leg mode "+leg.getMode()+" in act chain "+i+" could not be identified!");
			}
			if (car) {
				stream.print("x"+(i+1)+"_car_time\t");
				counterFirst++;
				if (travelCost.equals("yes")|| incomeDivided.equals("yes") || incomeDividedLN.equals("yes")) {
					stream.print("x"+(i+1)+"_car_cost\t");
					counterFirst++;
				}
				if (travelDistance.equals("yes")) {
					stream.print("x"+(i+1)+"_car_distance\t");
					counterFirst++;
				}
		//		stream.print("x"+(i+1)+"_car_legs\t");
		//		counterFirst++;
			}
			if (pt) {
				stream.print("x"+(i+1)+"_pt_time\t");
				counterFirst++;
				if (travelCost.equals("yes")|| incomeDivided.equals("yes") || incomeDividedLN.equals("yes")) {
					stream.print("x"+(i+1)+"_pt_cost\t");
					counterFirst++;
				}
				if (travelDistance.equals("yes")) {
					stream.print("x"+(i+1)+"_pt_distance\t");
					counterFirst++;
				}
				stream.print("x"+(i+1)+"_pt_legs\t");
				counterFirst++;
			}
			if (bike && this.bikeIn.equals("yes")) {
				stream.print("x"+(i+1)+"_bike_time\t");
				counterFirst++;
				if (travelDistance.equals("yes")) {
					stream.print("x"+(i+1)+"_bike_distance\t");
					counterFirst++;
				}
				stream.print("x"+(i+1)+"_bike_legs\t");
				counterFirst++;
			}
			if (walk) {
				stream.print("x"+(i+1)+"_walk_time\t");
				counterFirst++;
				if (travelDistance.equals("yes")) {
					stream.print("x"+(i+1)+"_walk_distance\t");
					counterFirst++;
				}
				stream.print("x"+(i+1)+"_walk_legs\t");
				counterFirst++;
			}
			if (similarity.equals("yes")) {
				stream.print("x"+(i+1)+"_sim\t");
				counterFirst++;
			}
		}
		for (int i = 0;i<this.actChains.size();i++){
			stream.print("av"+(i+1)+"\t");
			counterFirst++;
		}
		stream.println();

		
		
		//**************************************************************************
		// Filling plans
		//**************************************************************************
		
		int counter=0;
		int valid=0;
		int invalid=0;
		for (Person p : this.population.getPersons().values()) {
			PersonImpl person = (PersonImpl) p;
			counter++;
			if (counter%1000==0) {
				log.info("Handling person "+counter);
				Gbl.printMemoryUsage();
			}
			
			// Initializing similarity position array (always, even if not needed)
			int[] personSimsPos = new int[this.noOfAlternatives];
		
			// Check whether all info is available for this person. Drop the person, otherwise
			if ((incomeConstant.equals("yes") || incomeDivided.equals("yes") || incomeDividedLN.equals("yes") || incomeBoxCox.equals("yes")) && !aaa.getIncome().containsKey(person.getId())){
				log.warn("No income available for person "+person.getId()+". Dropping the person.");
				continue;
			}
			if (carAvail.equals("yes") && !aaa.getCarAvail().containsKey(person.getId())){
				log.warn("No car availability info available for person "+person.getId()+". Dropping the person.");
				continue;
			}
			if ((seasonTicket.equals("yes") || travelCost.equals("yes")) && !aaa.getSeasonTicket().containsKey(person.getId())){
				log.warn("No season ticket info available for person "+person.getId()+". Dropping the person.");
				continue;
			}

			// Check whether the selected plan of the person is valid. Drop the person, otherwise
			if ((person.getSelectedPlan().getScore()!=null) && (person.getSelectedPlan().getScore()==-100000.0)){
				log.warn("Person's "+person.getId()+" selected plan is not valid. Dropping the person.");
				continue;
			}

			// Calculate travelCostPt for the person
			double travelCostPt = 0;
			if (this.travelCost.equals("yes") || this.incomeDivided.equals("yes") || this.incomeDividedLN.equals("yes")){
				if (aaa.getSeasonTicket().get(person.getId())==11) { // No season ticket
					travelCostPt = this.costPtNothing;
					noOfNothing++;
				}
				else if ((aaa.getSeasonTicket().get(person.getId())==2) || (aaa.getSeasonTicket().get(person.getId())==3)) { // GA
					travelCostPt = this.costPtGA;
					noOfGA++;
				}
				else { // all other cases
					travelCostPt = this.costPtHalbtax;
					noOfHalbtax++;
				}
			}

			int counterRow=0;

			// Person ID
			stream.print(person.getId()+"\t");
			counterRow++;

			// Choice
			int position = -1;
			for (int i=0;i<this.actChains.size();i++){
				if (acCheck.checkEqualActChainsModesAccumulated(person.getSelectedPlan().getPlanElements(), this.actChains.get(i))) {
					position = i+1;
					break;
				}
			}
			stream.print(position+"\t");
			counterRow++;

			if (incomeConstant.equals("yes") || incomeDivided.equals("yes") || incomeDividedLN.equals("yes") || income.equals("yes")) {
				stream.print((aaa.getIncome().get(person.getId())/30)+"\t");
				counterRow++;
			}
			if (incomeBoxCox.equals("yes")) {
				stream.print(((double)(aaa.getIncome().get(person.getId()))/(double)(incomeAverage))+"\t");
				counterRow++;
			}
	/*		if (age.equals("yes")) {
				if (person.getAge()<=15) stream.print(1+"\t"+0+"\t"+0+"\t"+0+"\t");
				else if (person.getAge()<=30) stream.print(0+"\t"+1+"\t"+0+"\t"+0+"\t");
				else if (person.getAge()<=60) stream.print(0+"\t"+0+"\t"+1+"\t"+0+"\t");
				else stream.print(0+"\t"+0+"\t"+0+"\t"+1+"\t");
				counterRow+=4;
			}*/
			if (age.equals("yes")) {
				stream.print(person.getAge()+"\t"); 
				counterRow++;
			}
			if (gender.equals("yes")) {
				if (person.getSex().equals("f")) stream.print(1+"\t");
				else if (person.getSex().equals("m")) stream.print(0+"\t");
				else log.warn("Person "+person.getId()+" has no valid gender.");
				counterRow++;
			}
			if (license.equals("yes")) {
				if (person.getLicense().equals("no")) stream.print(0+"\t");
				else stream.print(1+"\t");
				counterRow++;
			}
			if (carAvail.equals("yes")) {
				int car = aaa.getCarAvail().get(person.getId());
				if (car==1) stream.print(1+"\t"+0+"\t"+0+"\t");
				else if (car==2) stream.print(0+"\t"+1+"\t"+0+"\t");
				else if (car==3) stream.print(0+"\t"+0+"\t"+1+"\t");
				else log.warn("Unidentified car availability "+car+" for person "+person.getId());
				counterRow+=3;
			}
	/*		if (seasonTicket.equals("yes")) {
				int st = aaa.getSeasonTicket().get(person.getId());		
				int ticket = 0;
				if (st==2 || st==3) ticket = 3;
				else if (st==11) ticket = 1;
				else ticket = 2;
				stream.print(ticket+"\t");
				counterRow++;
			}*/
			if (seasonTicket.equals("yes")) {
				int st = aaa.getSeasonTicket().get(person.getId());		
				int ticket = 0;
				if (st==2 || st==3) ticket = 3;
				else if (st==11) ticket = 1;
				else ticket = 2;
				if (ticket==1) stream.print(1+"\t"+0+"\t"+0+"\t");
				else if (ticket==2) stream.print(0+"\t"+1+"\t"+0+"\t");
				else if (ticket==3) stream.print(0+"\t"+0+"\t"+1+"\t");
				else log.warn("Unidentified seasonTicket "+st+" for person "+person.getId());
				counterRow+=3;
			}
		/*	if (munType.equals("yes")) {
				int mt = aaa.getMunType().get(person.getId());				
				stream.print(mt+"\t");
				counterRow++;
			}*/
			if (munType.equals("yes")) {
				int mt = aaa.getMunType().get(person.getId());				
				if (mt==1) stream.print(1+"\t"+0+"\t"+0+"\t"+0+"\t"+0+"\t");
				else if (mt==2) stream.print(0+"\t"+1+"\t"+0+"\t"+0+"\t"+0+"\t");
				else if (mt==3) stream.print(0+"\t"+0+"\t"+1+"\t"+0+"\t"+0+"\t");
				else if (mt==4) stream.print(0+"\t"+0+"\t"+0+"\t"+1+"\t"+0+"\t");
				else if (mt==5) stream.print(0+"\t"+0+"\t"+0+"\t"+0+"\t"+1+"\t");
				else log.warn("Unidentified munType "+mt+" for person "+person.getId());
				counterRow+=5;
			}
			
			//***********************************************************************************************************
			// Go through all act chains: if act chain == a plan of the person -> write it into file; write 0 otherwise 
			//***********************************************************************************************************
			
			int counterFound = 0;
			for (int i=0;i<this.actChains.size();i++){
				boolean found = false;
				for (int j=0;j<person.getPlans().size();j++){
					if (acCheck.checkEqualActChainsModesAccumulated(person.getPlans().get(j).getPlanElements(), this.actChains.get(i))) {
						counterRow += this.writeAccumulatedPlanIntoFileAccumulated(stream, person.getPlans().get(j).getPlanElements(), this.actChains.get(i), travelCostPt);
						found = true;
						counterFound++;
						// Similarity attribute
						if (similarity.equals("yes")) {
							stream.print(this.sims.get(person.getId()).get(j)+"\t");
							counterRow++;
							personSimsPos[j]=i;
						}
						break;
					}
				}
				if (!found){
					boolean car = false;
					boolean pt = false;
					boolean bike = false;
					boolean walk = false;
					for (int j=0;j<Math.max(this.actChains.get(i).size()-1, 1);j++){
						if (j%2==0){
							stream.print(0+"\t");
							counterRow++;
						/*	if (j%2==1 && this.travelCost.equals("yes") && (((LegImpl)(this.actChains.get(i).get(j))).getMode().equals(TransportMode.car) || ((LegImpl)(this.actChains.get(i).get(j))).getMode().equals(TransportMode.pt))){
								stream.print(0+"\t");
								counterRow++;
							}
							if (j%2==1 && (this.travelDistance.equals("yes"))){
								stream.print(0+"\t");
								counterRow++;
							}*/
						}
						else { // Check whether mode is in
							LegImpl leg = ((LegImpl)(this.actChains.get(i).get(j)));
							if (leg.getMode().equals(TransportMode.car)) car = true;
							else if (leg.getMode().equals(TransportMode.pt)) pt = true;
							else if (leg.getMode().equals(TransportMode.bike)) bike = true;
							else if (leg.getMode().equals(TransportMode.walk)) walk = true;
							else log.warn("Leg mode "+leg.getMode()+" in act chain "+i+" could not be identified!");
						}
					}
					if (car) {
						stream.print("0\t");
						counterRow++;
						if (this.travelCost.equals("yes") || this.incomeDivided.equals("yes") || this.incomeDividedLN.equals("yes")){
							stream.print("0\t");
							counterRow++;
						}
						if (travelDistance.equals("yes")){
							stream.print("0\t");
							counterRow++;
						}
				//		stream.print("0\t");
				//		counterRow++;
					}
					if (pt) {
						stream.print("0\t");
						counterRow++;
						if (this.travelCost.equals("yes") || this.incomeDivided.equals("yes") || this.incomeDividedLN.equals("yes")){
							stream.print("0\t");
							counterRow++;
						}
						if (travelDistance.equals("yes")){
							stream.print("0\t");
							counterRow++;
						}
						stream.print("0\t");
						counterRow++;
					}
					if (bike && bikeIn.equals("yes")) {
						stream.print("0\t");
						counterRow++;
						if (travelDistance.equals("yes")){
							stream.print("0\t");
							counterRow++;
						}
						stream.print("0\t");
						counterRow++;
					}
					if (walk) {
						stream.print("0\t");
						counterRow++;
						if (travelDistance.equals("yes")){
							stream.print("0\t");
							counterRow++;
						}
						stream.print("0\t");
						counterRow++;
					}
					if (similarity.equals("yes")) {
						stream.print(0+"\t");
						counterRow++;
					}
				}
			}
			if (similarity.equals("yes")) this.simsPosition.put(person.getId(), personSimsPos);
			if (counterFound!=this.noOfAlternatives) log.warn("For person "+person+", size of choice set is not "+this.noOfAlternatives+" but only "+counterFound);

			for (int i=0;i<this.actChains.size();i++){
				boolean found = false;
				for (int j=0;j<person.getPlans().size();j++){
					if (acCheck.checkEqualActChainsModesAccumulated(person.getPlans().get(j).getPlanElements(), this.actChains.get(i))) {
						if ((person.getPlans().get(j).getScore()!=null) && (person.getPlans().get(j).getScore()==-100000.0)) {
							stream.print(0+"\t");
							counterRow++;
							invalid++;
						}
						else {
							stream.print(1+"\t");
							counterRow++;
							valid++;
						}
						found = true;
						break;
					}
				}
				if (!found){
					stream.print(0+"\t");
					counterRow++;
				}
			}
			stream.println();
			if (counterFirst!=counterRow) log.warn("For person "+person.getId()+", the row length of "+counterRow+" does not fit the expected length of "+counterFirst+"!");
		}
		stream.close();
		log.info("Number of valid plans is "+valid+"; number of invalid plans is "+invalid);
		log.info("Number of GA = "+noOfGA+", number of Halbtax = "+noOfHalbtax+", and number of nothing = "+noOfNothing);
		log.info("done.");
		
		// Write out the similarity values to a separate file for cross-check
		if (similarity.equals("yes")) this.writeSims();
	}




	// for "repeat" attribute
	public void writePlansForBiogemeWithSequence(String outputFile){
		log.info("Writing plans for Biogeme...");
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		// First row
		stream.print("Id\tChoice\t");
		Person p = this.population.getPersons().values().iterator().next();
		for (int i = 0;i<p.getPlans().size();i++){
			int j=0;
			for (j =0;j<java.lang.Math.max(p.getPlans().get(i).getPlanElements().size()-1,1);j++){//act/mode attributes
				stream.print("x"+(i+1)+""+(j+1)+"\t");
				if ((j!=0) && (j%2==0))stream.print("x"+(i+1)+""+(j+1)+"_1\t");
			}
			stream.print("x"+(i+1)+""+(j+1)+"\t");//Sim
		}
		for (int i = 0;i<p.getPlans().size();i++){//Availability
			stream.print("av"+(i+1)+"\t");
		}
		stream.println();

		// Filling plans
		int counterPerson = -1;
		boolean firstPersonFound = true;
		Id firstPersonId = new IdImpl (1);
		for (Person person : this.population.getPersons().values()) {
			if (firstPersonFound){
				firstPersonId = person.getId();
				firstPersonFound = false;
			}
			counterPerson++;
			stream.print(person.getId()+"\t");
			int position = -1;
			for (int i=0;i<person.getPlans().size();i++){
				if (person.getPlans().get(i).equals(person.getSelectedPlan())) {
					position = i+1;
					break;
				}
			}
			stream.print(position+"\t");
			int counterPlan = -1;
			for (Plan plan : person.getPlans()) {
				counterPlan++;
				Plan planFirstPerson = this.population.getPersons().get(firstPersonId).getPlans().get(counterPlan);

				// Plan has only one act
				if (plan.getPlanElements().size()==1) stream.print("24\t");

				else {
					// First and last home act
					stream.print((((((PlanImpl) plan).getFirstActivity())).getEndTime()+86400-((((PlanImpl) plan).getLastActivity())).getStartTime())/3600+"\t");

					// All inner acts
					// Old version with fixed act/mode chain
					for (int i=1;i<plan.getPlanElements().size()-1;i++){
						if (i%2==0) {
							stream.print(((ActivityImpl)(plan.getPlanElements().get(i))).calculateDuration()/3600+"\t");
							if (((ActivityImpl)(plan.getPlanElements().get(i))).getType().equals(((ActivityImpl)(plan.getPlanElements().get(i-2))).getType())) stream.print("1\t");
							else stream.print("0\t");
						}
						else stream.print(((LegImpl)(plan.getPlanElements().get(i))).getTravelTime()/3600+"\t");
					}
					/*
					// New version with just identical number of acts and modes but no regard to position
					LinkedList<Integer> takenPositions = new LinkedList<Integer>();
					for (int i=1;i<planFirstPerson.getPlanElements().size()-1;i++){
						if (i%2==0){
							for (int j=2;j<plan.getPlanElements().size()-2;j+=2){
								if (((ActivityImpl)(planFirstPerson.getPlanElements().get(i))).getType().equals(((ActivityImpl)(plan.getPlanElements().get(j))).getType()) &&
										!takenPositions.contains(j)){
									stream.print(((ActivityImpl)(plan.getPlanElements().get(j))).calculateDuration()/3600+"\t");
									takenPositions.add(j);
									break;
								}
							}
						}
						else {
							for (int j=1;j<plan.getPlanElements().size()-1;j+=2){
								if (((LegImpl)(planFirstPerson.getPlanElements().get(i))).getMode().equals(((LegImpl)(plan.getPlanElements().get(j))).getMode()) &&
										!takenPositions.contains(j)){
									stream.print(((LegImpl)(plan.getPlanElements().get(j))).getTravelTime()/3600+"\t");
									takenPositions.add(j);
									break;
								}
							}
						}
					}*/
				}

				// Similarity attribute
				stream.print(this.sims.get(counterPerson).get(counterPlan)+"\t");
			}
			for (Plan plan : person.getPlans()) {

				// Plan not executable, drop from choice set
				if ((plan.getScore()!=null) && (plan.getScore()==-100000.0))	stream.print(0+"\t");

				// Plan executable
				else stream.print(1+"\t");
			}
			stream.println();
		}
		stream.close();
		log.info("done.");
	}
	/*
	private int writeAccumulatedPlanIntoFile (PrintStream stream, List<PlanElement> planToBeWritten, List<PlanElement> referencePlan, double travelCostPt){

		if (planToBeWritten.size()!=referencePlan.size()){
			log.warn("Plans do not have same size; planToBeWritten: "+planToBeWritten+", referencePlan: "+referencePlan);
		}

		int counter=0;

		// Plan has only one act
		if (planToBeWritten.size()==1) {
			stream.print(24+"\t");
			counter++;
		}
		else {
			// First and last home act
			stream.print((((ActivityImpl)(planToBeWritten.get(0))).getEndTime()+86400-((ActivityImpl)(planToBeWritten.get(planToBeWritten.size()-1))).getStartTime())/3600+"\t");
			counter++;
			LinkedList<Integer> takenPositions = new LinkedList<Integer>();
			for (int i=1;i<referencePlan.size()-1;i++){
				if (i%2==0){ // Activities
					boolean found = false;
					for (int j=2;j<planToBeWritten.size()-2;j+=2){
						if (((ActivityImpl)(referencePlan.get(i))).getType().equals(((ActivityImpl)(planToBeWritten.get(j))).getType()) &&
								!takenPositions.contains(j)){
							stream.print(((ActivityImpl)(planToBeWritten.get(j))).calculateDuration()/3600+"\t");
							counter++;
							takenPositions.add(j);
							found = true;
							break;
						}
					}
					if (!found) log.warn("Activity "+referencePlan.get(i)+" could not be found!");
				}
				else { // Legs
					if ((!((LegImpl)(referencePlan.get(i))).getMode().equals(TransportMode.bike)) || this.bikeIn.equals("yes")){
						boolean found = false;
						for (int j=1;j<planToBeWritten.size()-1;j+=2){
							if (((LegImpl)(referencePlan.get(i))).getMode().equals(((LegImpl)(planToBeWritten.get(j))).getMode()) &&
									!takenPositions.contains(j)){
								stream.print(((LegImpl)(planToBeWritten.get(j))).getTravelTime()/3600+"\t");
								counter++;
								takenPositions.add(j);
								if (this.travelCost.equals("yes")){
									if (((LegImpl)(planToBeWritten.get(j))).getMode().equals(TransportMode.car)){
										stream.print((((LegImpl)(planToBeWritten.get(j))).getRoute().getDistance()/1000*this.travelCostCar)+"\t");
										counter++;
									}
									else if (((LegImpl)(planToBeWritten.get(j))).getMode().equals(TransportMode.pt)){
										stream.print((((LegImpl)(planToBeWritten.get(j))).getRoute().getDistance()/1000*travelCostPt)+"\t");
										counter++;
									}
								}
								if (this.incomeBoxCox.equals("yes") || this.travelDistance.equals("yes")){
									stream.print((((LegImpl)(planToBeWritten.get(j))).getRoute().getDistance()/1000)+"\t");
									counter++;
								}
								found = true;
								break;
							}
						}
						if (!found) log.warn("Leg "+referencePlan.get(i)+" could not be found!");
					}
				}
			}
		}
		return counter;
	}
	*/


	private int writeAccumulatedPlanIntoFileAccumulated (PrintStream stream, List<PlanElement> planToBeWritten, List<PlanElement> referencePlan, double travelCostPt){

		if (planToBeWritten.size()!=referencePlan.size()){
			log.warn("Plans do not have same size; planToBeWritten: "+planToBeWritten+", referencePlan: "+referencePlan);
		}

		int counter=0;

		// Plan has only one act
		if (planToBeWritten.size()==1) {
			stream.print(24+"\t");
			counter++;
		}
		else {
			// First and last home act
			stream.print((((ActivityImpl)(planToBeWritten.get(0))).getEndTime()+86400-((ActivityImpl)(planToBeWritten.get(planToBeWritten.size()-1))).getStartTime())/3600+"\t");
			counter++;

			LinkedList<Integer> takenPositions = new LinkedList<Integer>();
			int car = 0;
			int pt = 0;
			int bike = 0;
			int walk = 0;
			double car_time = 0;
			double car_cost = 0;
			double car_distance = 0;
			double pt_time = 0;
			double pt_cost = 0;
			double pt_distance = 0;
			double bike_time = 0;
			double bike_distance = 0;
			double walk_time = 0;
			double walk_distance = 0;
			for (int i=1;i<referencePlan.size()-1;i++){
				if (i%2==0){ // Activities
					boolean found = false;
					for (int j=2;j<planToBeWritten.size()-2;j+=2){
						if (((ActivityImpl)(referencePlan.get(i))).getType().equals(((ActivityImpl)(planToBeWritten.get(j))).getType()) &&
								!takenPositions.contains(j)){
							stream.print(((ActivityImpl)(planToBeWritten.get(j))).calculateDuration()/3600+"\t");
							counter++;
							takenPositions.add(j);
							found = true;
							break;
						}
					}
					if (!found) log.warn("Activity "+referencePlan.get(i)+" could not be found!");
				}
				else { // Check whether mode is in
					LegImpl leg = ((LegImpl)(referencePlan.get(i)));
					if (leg.getMode().equals(TransportMode.car)) car++;
					else if (leg.getMode().equals(TransportMode.pt)) pt++;
					else if (leg.getMode().equals(TransportMode.bike)) bike++;
					else if (leg.getMode().equals(TransportMode.walk)) walk++;
					else log.warn("Leg mode "+leg.getMode()+" in act chain "+i+" could not be identified!");
				}
			}

			// Now go through planToBeWritten, check whether everything correct and write legs into file
			int carCheck = 0;
			int ptCheck = 0;
			int bikeCheck = 0;
			int walkCheck = 0;
			for (int i=1;i<planToBeWritten.size()-1;i+=2){
				LegImpl leg = ((LegImpl)(planToBeWritten.get(i)));
				if (leg.getMode().equals(TransportMode.car)) {
					carCheck++;
					car_time += leg.getTravelTime()/3600;
					car_cost += leg.getRoute().getDistance()/1000*this.travelCostCar;
					car_distance += leg.getRoute().getDistance()/1000;
				}
				else if (leg.getMode().equals(TransportMode.pt)) {
					ptCheck++;
					pt_time += leg.getTravelTime()/3600;
					pt_cost += leg.getRoute().getDistance()/1000*travelCostPt;
					pt_distance += leg.getRoute().getDistance()/1000;
				}
				else if (leg.getMode().equals(TransportMode.bike)) {
					bikeCheck++;
					bike_time += leg.getTravelTime()/3600;
					bike_distance += leg.getRoute().getDistance()/1000;
				}
				else if (leg.getMode().equals(TransportMode.walk)) {
					walkCheck++;
					walk_time += leg.getTravelTime()/3600;
					walk_distance += leg.getRoute().getDistance()/1000;
				}
				else log.warn("Leg mode "+leg.getMode()+" in act chain "+i+" could not be identified!");
			}
			if (car!=carCheck) log.warn("Number of "+car+" car legs in referencePlan is different from number of "+carCheck+" car legs in planToBeWritten!");
			if (pt!=ptCheck) log.warn("Number of "+pt+" pt legs in referencePlan is different from number of "+ptCheck+" pt legs in planToBeWritten!");
			if (bike!=bikeCheck) log.warn("Number of "+bike+" bike legs in referencePlan is different from number of "+bikeCheck+" bike legs in planToBeWritten!");
			if (walk!=walkCheck) log.warn("Number of "+walk+" walk legs in referencePlan is different from number of "+walkCheck+" walk legs in planToBeWritten!");

			if (car>0){
				stream.print(car_time+"\t");
				counter++;
				if (this.travelCost.equals("yes") || this.incomeDivided.equals("yes") || this.incomeDividedLN.equals("yes")){
					stream.print(car_cost+"\t");
					counter++;
				}
				if (this.travelDistance.equals("yes")){
					stream.print(car_distance+"\t");
					counter++;
				}
			//	stream.print(car+"\t");
			//	counter++;
			}
			if (pt>0){
				stream.print(pt_time+"\t");
				counter++;
				if (this.travelCost.equals("yes") || this.incomeDivided.equals("yes") || this.incomeDividedLN.equals("yes")){
					stream.print(pt_cost+"\t");
					counter++;
				}
				if (this.travelDistance.equals("yes")){
					stream.print(pt_distance+"\t");
					counter++;
				}
				stream.print(pt+"\t");
				counter++;

			}
			if ((bike>0) && this.bikeIn.equals("yes")){
				stream.print(bike_time+"\t");
				counter++;
				if (this.travelDistance.equals("yes")){
					stream.print(bike_distance+"\t");
					counter++;
				}
				stream.print(bike+"\t");
				counter++;
			}
			if (walk>0){
				stream.print(walk_time+"\t");
				counter++;
				if (this.travelDistance.equals("yes")){
					stream.print(walk_distance+"\t");
					counter++;
				}
				stream.print(walk+"\t");
				counter++;
			}
		}
		return counter;
	}


	//////////////////////////////////////////////////////////////////////
	// Writing mod file methods
	//////////////////////////////////////////////////////////////////////

	/*public void writeModFile(String outputFile){
		new ModFileMaker (this.population, this.actChains).write(outputFile);
	}

	public void writeModFileWithSequence(String outputFile){
		new ModFileMaker (this.population, this.actChains).writeWithSequence(outputFile);
	}*/

	public void writeModFileWithRandomSelection (String outputFile){
		//new ModFileMaker (this.population, this.actChains).writeWithRandomSelection(outputFile,
		new ModFileMaker (this.population, this.actChains).writeWithRandomSelectionAccumulated(outputFile,
				this.beta,
				this.gamma,
				this.similarity,
				this.incomeConstant,
				this.incomeDivided,
				this.incomeDividedLN,
				this.incomeBoxCox,
				this.age,
				this.gender,
				this.income,
				this.license,
				this.carAvail,
				this.seasonTicket,
				this.travelDistance,
				this.travelCost,
				this.travelConstant,
				this.beta_travel,
				this.bikeIn,
				this.munType,
				this.innerHome);
	}


	//////////////////////////////////////////////////////////////////////
	// Writing sim file
	//////////////////////////////////////////////////////////////////////

	public void writeSims (){
		
		log.info("Writing sims file...");

		// Statistics
		int [] stats = new int [50];
		for (int i=0;i<stats.length;i++){
			stats[i]=0;
		}

		PrintStream stream;
		try {
			stream = new PrintStream (new File(this.outputFileSimsOverview));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		for (int i=0;i<this.actChains.size();i++){
			stream.print("alt"+(i+1)+"\t");
		}
		stream.println();

		for (Person p : this.population.getPersons().values()) {
			if (this.simsPosition.containsKey(p.getId())){
				stream.print(p.getId()+"\t");
				for (int i=0;i<this.actChains.size();i++){
					boolean found = false;
					for (int j=0;j<this.simsPosition.get(p.getId()).length;j++){
						if (this.simsPosition.get(p.getId())[j]==i){
							double value = this.sims.get(p.getId()).get(j);
							stream.print(value+"\t");
							stats[(int)(value)]++;
							found=true;
							break;
						}
					}
					if (!found)stream.print("\t");
				}
				stream.println();
			}
			else {
				log.info("No similarity info for person "+p.getId()+"!");
			}
		}
		
		stream.println();

		for (int i=0;i<stats.length;i++){
			stream.println(i+"\t"+stats[i]);
		}

		stream.close();
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// Once-off method
	//////////////////////////////////////////////////////////////////////


	// Method that filters only Zurich10% plans
	protected void selectZurich10MZPlans (){
		log.info("Creating Zurich10% population...");
		// Quite strange coding but throws ConcurrentModificationException otherwise...
		Object [] a = this.population.getPersons().values().toArray();
		for (int i=a.length-1;i>=0;i--){
			PersonImpl person = (PersonImpl) a[i];
			boolean isIn = false;
			for (int j=0;j<person.getSelectedPlan().getPlanElements().size();j+=2){
				//30km circle around Zurich city centre (Bellevue)
				if (CoordUtils.calcDistance(((ActivityImpl)(person.getSelectedPlan().getPlanElements().get(j))).getCoord(), new CoordImpl(683518.0,246836.0))<=30000){
					isIn = true;
					break;
				}
			}
			if (!isIn){
				this.population.getPersons().remove(person.getId());
			}
		}
		log.info("done... Size of population is "+this.population.getPersons().size()+".");
	}
}
