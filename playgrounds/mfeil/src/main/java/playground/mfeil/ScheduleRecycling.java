/* *********************************************************************** *
 * project: org.matsim.*
 * ScheduleRecycling.java
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

package playground.mfeil;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.knowledges.Knowledges;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;

import playground.mfeil.config.ScheduleRecyclingConfigGroup;



/**
 * @author Matthias Feil
 * Schedule recycling re-uses plans of individually optimized agents for other non-optimized agents.
 * See dissertation Matthias Feil "Choosing the Daily Schedule: Expanding Activity-Based travel demand modelling".
 */


public class ScheduleRecycling implements PlanStrategyModule{

	private  ArrayList<Plan> []						list;
	private final AbstractMultithreadedModule 		schedulingModule,
													assignmentModule;
	private final LocationMutatorwChoiceSet 		locator;
	private final PlanScorer						scorer;
	private final Controler							controler;
	private OptimizedAgents 						agents;
	private LinkedList<String>						nonassignedAgents;
	private final DepartureDelayAverageCalculator 	tDepDelayCalc;
	private final NetworkImpl 						network;
	public static PrintStream 						assignment;
	private final Knowledges 						knowledges;
	private final ActivityTypeFinder 				finder;
	private final int 								iterationsFirstTime,
													iterationsFurtherTimes,
													noOfIndividualAgents,
													noOfRecycledAgents,
													noOfSoftCoefficients;
	protected final DistanceCoefficients 			coefficients;
	private ArrayList<double[]> 					tabuList;
	private final String 							primActsDistance,
													homeLocationDistance,
													municipality,
													gender,
													age,
													license,
													car_avail,
													employed;
	private final ArrayList<String> 				softCoef,
													allCoef;
	private ArrayList<Integer> 						list1Pointer;
	private static final Logger 					log = Logger.getLogger(ScheduleRecycling.class);


	public ScheduleRecycling (Controler controler, ActivityTypeFinder finder) {

		this.controler=controler;
		this.knowledges 			= controler.getScenario().getKnowledges();
		this.locator 				= new LocationMutatorwChoiceSet(controler.getNetwork(), controler, this.knowledges, new Random(4711));
		this.scorer 				= new PlanScorer (controler.getScoringFunctionFactory());
		this.network 				= controler.getNetwork();
		this.init(network);
		this.tDepDelayCalc 			= new DepartureDelayAverageCalculator(this.network,controler.getConfig().travelTimeCalculator().getTraveltimeBinSize());
		this.controler.getEvents().addHandler(tDepDelayCalc);
		this.nonassignedAgents 		= new LinkedList<String>();
		this.noOfIndividualAgents	= Integer.parseInt(ScheduleRecyclingConfigGroup.getNoOfIndividualAgents());
		this.noOfRecycledAgents		= Integer.parseInt(ScheduleRecyclingConfigGroup.getNoOfRecycledAgents());
		this.finder					= finder;
		this.iterationsFirstTime 	= Integer.parseInt(ScheduleRecyclingConfigGroup.getIterationsFirstTime());
		this.iterationsFurtherTimes = Integer.parseInt(ScheduleRecyclingConfigGroup.getIterationsFurtherTimes());
		this.primActsDistance 		= ScheduleRecyclingConfigGroup.getPrimActsDistance();
		this.homeLocationDistance 	= ScheduleRecyclingConfigGroup.getHomeLocationDistance();
		this.municipality			= ScheduleRecyclingConfigGroup.getMunicipalityType();
		this.gender 				= ScheduleRecyclingConfigGroup.getGender();
		this.age 					= ScheduleRecyclingConfigGroup.getAge();
		this.license 				= ScheduleRecyclingConfigGroup.getLicenseOwnership();
		this.car_avail 				= ScheduleRecyclingConfigGroup.getCarAvailability();
		this.employed 				= ScheduleRecyclingConfigGroup.getEmployment();
		this.softCoef 				= this.detectSoftCoefficients();
		this.allCoef 				= this.detectAllCoefficients();
		this.noOfSoftCoefficients	= this.softCoef.size();
		double [] startCoefficients = new double [this.noOfSoftCoefficients];
		for (int i=0;i<startCoefficients.length;i++) startCoefficients[i]=1;
		this.coefficients 			= new DistanceCoefficients (startCoefficients, this.softCoef, this.allCoef);
		this.list1Pointer 			= new ArrayList<Integer>();

		this.schedulingModule 		= new PlanomatXInitialiser(controler, finder);
		this.assignmentModule		= new AgentsAssignmentInitialiser (this.controler, this.tDepDelayCalc, this.locator, this.scorer, this.finder, this, this.coefficients, this.nonassignedAgents);


		new Statistics();
		String outputfileOverview = controler.getControlerIO().getOutputFilename("assignment_log.txt");
		FileOutputStream fileOverview;
		try {
			fileOverview = new FileOutputStream(new File(outputfileOverview), true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		ScheduleRecycling.assignment = new PrintStream (fileOverview);
		ScheduleRecycling.assignment.println("Agent\tScore\tPlan\n");
	}

	private void init(final NetworkImpl network) {
		this.network.connect();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void prepareReplanning() {

		this.list = new ArrayList[2];
		for (int i=0;i<2;i++){
			list[i] = new ArrayList<Plan>();
		}
		this.schedulingModule.prepareReplanning();
	}

	@Override
	public void handlePlan(final Plan plan) {

		this.list[1].add(plan);
	}

	@Override
	public void finishReplanning(){

		Statistics.noSexAssignment=false;
		Statistics.noCarAvailAssignment=false;
		Statistics.noEmploymentAssignment=false;
		Statistics.noLicenseAssignment=false;
		Statistics.noMunicipalityAssignment=false;

		/* Individual optimization of agents */
		for (int i=0;i<this.noOfIndividualAgents;i++) {
			int pos = (int)(MatsimRandom.getRandom().nextDouble()*this.list[1].size());
			list[0].add(list[1].get(pos));
			schedulingModule.handlePlan(list[1].get(pos));
			list[1].remove(pos);
		}
		schedulingModule.finishReplanning();

		/* Fill Optimized Agents object */
		if (this.controler.getIterationNumber()== null) {
			log.warn("This if-loop to be run only for test case! Check code if run during operational simulation!");
			list[0].clear();
			list[0].add(this.controler.getPopulation().getPersons().get(new IdImpl(1)).getSelectedPlan());
			list[0].add(this.controler.getPopulation().getPersons().get(new IdImpl(2)).getSelectedPlan());
			list[0].add(this.controler.getPopulation().getPersons().get(new IdImpl(3)).getSelectedPlan());
			list[0].add(this.controler.getPopulation().getPersons().get(new IdImpl(4)).getSelectedPlan());
			list[0].add(this.controler.getPopulation().getPersons().get(new IdImpl(5)).getSelectedPlan());
		}
		agents = new OptimizedAgents (list[0], this.knowledges);

		/* Detect optimal coefficients metric */
		if ((this.controler.getIterationNumber()== null || this.controler.getIterationNumber()%10==1)	&&	this.noOfSoftCoefficients>1) {
			Statistics.prt=false;
			this.detectCoefficients();
			Statistics.prt=true;
			this.calculate();
			if (!this.nonassignedAgents.isEmpty()) this.rescheduleNonassigedAgents();
		}
		else {
			this.calculate();
			if (!this.nonassignedAgents.isEmpty()) this.rescheduleNonassigedAgents();
		}

		/* Print statistics of individual optimization */
		assignment.println("Iteration "+this.controler.getIterationNumber());
		assignment.println("Individual optimization");
		for (int i=0;i<list[0].size();i++){
			assignment.print(list[0].get(i).getPerson().getId()+"\t\t"+list[0].get(i).getScore()+"\t"+this.getDistance(list[0].get(i))+"\t");
			for (int j=0;j<list[0].get(i).getPlanElements().size();j+=2){
				assignment.print(((ActivityImpl)(list[0].get(i).getPlanElements().get(j))).getType()+"\t");
			}
			assignment.println();
			if (i==this.noOfIndividualAgents-1) {
				assignment.println();
				assignment.println("Individual optimization of non-assigend agents in metric detection phase");
			}
		}
		assignment.println();

		/* Assign remaining agents */
		assignmentModule.prepareReplanning();
		for (int i=this.noOfRecycledAgents;i<list[1].size();i++){
			assignmentModule.handlePlan(list[1].get(i));
		}

		assignmentModule.finishReplanning();

		/* Individually optimize all agents that couldn't be assigned */
		if (this.nonassignedAgents.size()>0){
			schedulingModule.prepareReplanning();
			Iterator<String> naa = this.nonassignedAgents.iterator();
			while (naa.hasNext()) {
				String st = naa.next();
				for (int x=0;x<this.list[1].size();x++){
					if (this.list[1].get(x).getPerson().getId().toString().equals(st)){
						schedulingModule.handlePlan(list[1].get(x));
						break;
					}
				}
			}
			schedulingModule.finishReplanning();
		}

		/* Print statistics of assignment */
		assignment.println("Assignment");
		for (int i=0;i<this.allCoef.size();i++){
			assignment.print(this.allCoef.get(i)+"\t");
			if (this.coefficients.getSingleCoef(this.allCoef.get(i))!=-1){
				assignment.print(this.coefficients.getSingleCoef(this.allCoef.get(i)));
			}
			else assignment.print("Hard constraint");
			assignment.println();
		}
		for (int i=0;i<Statistics.list.size();i++){
			if (Statistics.list.get(i)!=null) {
				for (int j=0;j<Statistics.list.get(i).size();j++){
				assignment.print(Statistics.list.get(i).get(j)+"\t");
				}
				assignment.println();
			}
			else {
				log.warn("Statistics.list.get("+i+") == null");
			}
		}
		assignment.println();
		if (this.nonassignedAgents.size()>0){
			assignment.println("Individual optimization of non-assigend agents in assignment phase");
			Iterator<String> naa = this.nonassignedAgents.iterator();
			while (naa.hasNext()) {
				String st = naa.next();
				for (int x=0;x<this.list[1].size();x++){
					if (this.list[1].get(x).getPerson().getId().toString().equals(st)){
						assignment.print(this.list[1].get(x).getPerson().getId()+"\t\t"+this.list[1].get(x).getScore()+"\t");
						for (int j=0;j<this.list[1].get(x).getPlanElements().size();j+=2){
							assignment.print(((ActivityImpl)(this.list[1].get(x).getPlanElements().get(j))).getType()+"\t");
						}
						assignment.println();
						break;
					}
				}
			}
			assignment.println();
			this.nonassignedAgents.clear();
		}

		if (Statistics.noSexAssignment) log.warn("There are agents that have no gender information.");
		if (Statistics.noCarAvailAssignment) log.warn("There are agents that have no car availabiity information.");
		if (Statistics.noEmploymentAssignment) log.warn("There are agents that have no employment information.");
		if (Statistics.noLicenseAssignment) log.warn("There are agents that have no license information.");
		if (Statistics.noMunicipalityAssignment) log.warn("There are agents that have no municipality information.");
		if (Statistics.noSexAssignment ||
				Statistics.noCarAvailAssignment ||
				Statistics.noEmploymentAssignment ||
				Statistics.noMunicipalityAssignment ||
				Statistics.noLicenseAssignment) log.warn("For these agents, recycling was conducted without the relevant attribute.");
		Statistics.list.clear();
	}

	private void detectCoefficients (){

		/* Initialization */
		this.tabuList = new ArrayList<double[]>();
		double offset = 0.5;
		double coefSet [] = new double [this.noOfSoftCoefficients];	// set for calculation input
		double coefIterBase [] = new double [this.noOfSoftCoefficients];	// set that is static in iteration
		double coefIterBest [] = new double [this.noOfSoftCoefficients];	// set with iteration's best coefficients
		double coefBest [] = new double [this.noOfSoftCoefficients];	// set with best coefficients
		double scoreIter;
		double scoreBest;
		double scoreAux, coefAux;
		int [] modified = new int [this.noOfSoftCoefficients]; // last coefficient fixed
		for (int z=0;z<modified.length;z++){
			modified[z]=0;
		}
		int modifiedAux;

		/* Iteration 0 */
		scoreIter = this.calculate();		// calculate score of initial set
		if (!this.nonassignedAgents.isEmpty()) scoreIter = this.rescheduleNonassigedAgents();		// in case some agents were not assignable
		scoreBest = scoreIter;
		for (int z=0;z<coefSet.length;z++){
			coefAux = this.coefficients.getSingleCoef(z);  // set base coef
			coefIterBase[z] = coefAux;
			coefSet[z] = coefAux;
			coefBest [z] = coefAux;
		}

		/* Further iterations */
		int iter;
		if (this.controler.getIterationNumber()== null || this.controler.getIterationNumber()==1) iter=this.iterationsFirstTime;
		else iter=this.iterationsFurtherTimes;
		for (int i=1;i<iter+1;i++){
			log.info("Metric detection: iteration "+i);
			scoreIter = -Double.MAX_VALUE;

			for (int z=0;z<coefSet.length;z++){
				coefAux = coefIterBase[z];
				coefSet[z] = coefAux;
			}

			for (int j=0;j<this.noOfSoftCoefficients-1;j++){ // last coef fixed
				modifiedAux = modified[j];
				modified[j]=0;

				if (modifiedAux!=-1){ // if not decreased by offset in previous iteration
					coefAux = coefIterBase[j] + offset; // increase coef by offset
					coefSet[j] = coefAux;
					this.checkTabuList(coefSet, j, offset, false);
					this.coefficients.setSingleCoef(coefSet[j], j);
					scoreAux = this.calculate();
					if (scoreAux>scoreIter) {
						scoreIter = scoreAux;
						for (int x=0;x<j;x++){
							modified [x]=0;	// set back the former index;
						}
						modified [j] = 1; // 1 means "increased by offset";
						for (int x=0;x<modified.length;x++){
							coefAux = coefSet[x];// set back the coef itself;
							coefIterBest[x] = coefAux;
						}
					}
					coefAux = coefIterBase[j];
					coefSet[j] = coefAux;
				}

				if (modifiedAux!=1){
					if (coefIterBase[j] - offset>=0){
						coefSet[j] = coefIterBase[j] - offset;
						this.checkTabuList(coefSet, j, offset, true);
						this.coefficients.setSingleCoef(coefSet[j], j);
						scoreAux = this.calculate();
						if (scoreAux>scoreIter) {
							scoreIter = scoreAux;

							// check whether coef has been really decreased, or eventually increased by method checkTabuList()
							int movement = 0;
							if (coefSet[j]>coefIterBase[j]) movement = 1;
							else if (coefSet[j]<coefIterBase[j]) movement = -1;
							else log.warn("coef chosen but neither bigger or smaller than in previous iterations. This may not happen!");
							for (int x=0;x<j;x++){
								modified [x]=0;	// set back the former index;
							}
							modified [j] = movement; // 1 means "increased by offset";
							for (int x=0;x<modified.length;x++){
								coefAux = coefSet[x];// set back the coef itself;
								coefIterBest[x] = coefAux;
							}
						}
						coefAux = coefIterBase[j];
						coefSet[j] = coefAux;
					}
					else {
						coefSet[j] = coefIterBase[j] + 2*offset;
						this.checkTabuList(coefSet, j, offset, false);
						this.coefficients.setSingleCoef(coefSet[j], j);
						scoreAux = this.calculate();
						if (scoreAux>scoreIter) {
							scoreIter = scoreAux;
							for (int x=0;x<j;x++){
								modified [x]=0;	// set back the former index;
							}
							modified [j] = 1; // 1 means "increased by offset";
							for (int x=0;x<modified.length;x++){
								coefAux = coefSet[x];// set back the coef itself;
								coefIterBest[x] = coefAux;
							}
						}
					}
				}
			}
			if (scoreIter>scoreBest){
				scoreBest = scoreIter;
				for (int x=0;x<coefIterBest.length;x++){
					coefAux = coefIterBest[x];// set back the coef itself;
					coefBest[x] = coefAux;
				}
			}
			for (int s=0;s<coefIterBest.length;s++){
				log.info("Chosen coef set "+s+" = "+coefIterBest[s]);
				coefAux = coefIterBest[s];  // set previous iteration's coef
				coefIterBase[s] = coefAux;
			}
			log.info("Iteration's score = "+scoreIter);
		}

		this.coefficients.setCoef(coefBest);
		log.info("");
		log.info("Final coefficients:");
		for (int j=0;j<this.noOfSoftCoefficients;j++) log.info(softCoef.get(j)+" = "+this.coefficients.getCoef()[j]);
		log.info("Score = "+scoreBest);
		log.info("");
	}

	private double calculate (){
		double score = 0;
		this.assignmentModule.prepareReplanning();
		for (int j=0;j<java.lang.Math.min(this.noOfRecycledAgents, list[1].size());j++){
			assignmentModule.handlePlan(list[1].get(j));
		}
		assignmentModule.finishReplanning();
		for (int j=0;j<java.lang.Math.min(this.noOfRecycledAgents, list[1].size());j++){
			score += this.list[1].get(j).getScore().doubleValue();
		}
		return score;
	}

	private double rescheduleNonassigedAgents (){
		list1Pointer.clear();
		this.schedulingModule.prepareReplanning();
		Iterator<String> naa = this.nonassignedAgents.iterator();
		while (naa.hasNext()) {
			String st = naa.next();
			for (int x=0;x<this.list[1].size();x++){
				if (this.list[1].get(x).getPerson().getId().toString().equals(st)){
					schedulingModule.handlePlan(list[1].get(x));
					this.list1Pointer.add(x);
					break;
				}
			}
		}
		schedulingModule.finishReplanning();
		java.util.Collections.sort(this.list1Pointer);
		for (int x=list1Pointer.size()-1;x>=0;x--){
			this.list[0].add(this.list[1].get(list1Pointer.get(x)));
			this.agents.addAgent((this.list[0].get(this.list[0].size()-1)));

		}
		double score=0;
		if (this.nonassignedAgents.size()>0){
			this.nonassignedAgents.clear();
			score = this.calculate();// Do this again, now all agents must be assignable.
			if (this.nonassignedAgents.size()>0) {
				log.warn("Something went wrong when optimizing the non-assigned agents of the metric detection phase!");
			}
		}
		log.info("Agents to be optimized individually:");
		for (int i=0;i<this.list[0].size();i++){
			log.info("Agent "+(i+1)+": "+list[0].get(i).getPerson().getId());
		}

		return score;
	}

	private ArrayList<String> detectSoftCoefficients (){
		ArrayList<String> softCoef = new ArrayList<String>();
		if (this.primActsDistance.equals("yes")) {
			softCoef.add("primActsDistance");
		}
		if (this.municipality.equals("yes")) {
			softCoef.add("municipality");
		}
		if (this.homeLocationDistance.equals("yes")) {
			softCoef.add("homeLocationDistance");
		}
		if (this.age.equals("yes")) {
			softCoef.add("age");
		}
		return softCoef;
	}

	private ArrayList<String> detectAllCoefficients (){
		ArrayList<String> allCoef = new ArrayList<String>();
		if (this.primActsDistance.equals("yes")) {
			allCoef.add("primActsDistance");
		}
		if (this.homeLocationDistance.equals("yes")) {
			allCoef.add("homeLocationDistance");
		}
		if (this.municipality.equals("yes")) {
			allCoef.add("municipality");
		}
		if (this.gender.equals("yes")) {
			allCoef.add("sex");
		}
		if (this.age.equals("yes")) {
			allCoef.add("age");
		}
		if (this.license.equals("yes")) {
			allCoef.add("license");
		}
		if (this.car_avail.equals("yes")) {
			allCoef.add("car_avail");
		}
		if (this.employed.equals("yes")) {
			allCoef.add("employed");
		}
		return allCoef;
	}


	private void checkTabuList (double [] coefMatrix, int j, double offset, boolean decrease){
		boolean modified = false;
		ArrayList<Double> takenNumbers = new ArrayList<Double>();
		do {
			modified = false;
			OuterLoop:
			for (int x=0;x<this.tabuList.size();x++){
				for (int y=0;y<coefMatrix.length-1;y++){
					if (this.tabuList.get(x)[y]!=coefMatrix[y]) continue OuterLoop;
				}
				modified = true;
				if (decrease) {
					// decrease as long as >=0
					if (coefMatrix[j]-offset>=0) {
						double aux = coefMatrix[j];
						takenNumbers.add(aux);
						coefMatrix[j]=coefMatrix[j]-offset;
					}
					// reaching <0 increase beyond start value
					else {
						if (takenNumbers.isEmpty()){
							coefMatrix[j]=coefMatrix[j]+offset;
						}
						else {
							java.util.Collections.sort(takenNumbers);
							coefMatrix[j]=takenNumbers.get(takenNumbers.size()-1)+offset;
						}
					}
				}
				// increase is straight forward
				else coefMatrix[j]=coefMatrix[j]+offset;
			}
		} while (modified);
		// write chosen coef set into tabuList
		double[] tabu = new double[coefMatrix.length];
		for (int y=0;y<coefMatrix.length;y++){
			double aux = coefMatrix[y];
		tabu[y] = aux;
		}
		this.tabuList.add(tabu);
	}

	public OptimizedAgents getOptimizedAgents (){
		return this.agents;
	}

	private double getDistance(Plan plan){
		double distance = 0;
		for (int i=1;i<plan.getPlanElements().size();i+=2){
			LegImpl leg = ((LegImpl)(plan.getPlanElements().get(i)));
			if (!leg.getMode().equals(TransportMode.car)) distance += leg.getRoute().getDistance(); // distance in kilometers
			else distance += RouteUtils.calcDistance((NetworkRoute) leg.getRoute(), this.network);
		}
		return distance;
	}

}
