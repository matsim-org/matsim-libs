/* *********************************************************************** *
 * project: org.matsim.*
 * RecyclingModule1.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.knowledges.Knowledges;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;

import playground.mfeil.MDSAM.ActivityTypeFinder;


/**
 * @author Matthias Feil
 * Includes also the optimization of the distance coefficients
 */


public class RecyclingModule implements PlanStrategyModule{
		
	private  ArrayList<Plan> []						list;
	private final AbstractMultithreadedModule 		schedulingModule;
	private final AbstractMultithreadedModule		assignmentModule;
	private final LocationMutatorwChoiceSet 		locator;
	private final PlanScorer						scorer;
	private final int								noOfSchedulingAgents;
	private final Controler							controler;
	private OptimizedAgents 						agents;
	private LinkedList<String>						nonassignedAgents;
	private final DepartureDelayAverageCalculator 	tDepDelayCalc;
	private final NetworkImpl 						network;
	public static PrintStream 						assignment;
	private final Knowledges 						knowledges;
	private final ActivityTypeFinder 				finder;
	
	private final int iterations, noOfAssignmentAgents, noOfSoftCoefficients;
	private final DistanceCoefficients 				coefficients;
	private final String primActsDistance, homeLocationDistance, sex, age, license, car_avail, employed; 
	private final ArrayList<String> 				softCoef; 
	private final ArrayList<String> 				allCoef; 
	private ArrayList<Integer> 						list1Pointer; 
	private static final Logger 					log = Logger.getLogger(RecyclingModule.class);
	
	                      
	public RecyclingModule (ControlerMFeil controler, ActivityTypeFinder finder) {
		
		this.controler=controler;
		this.knowledges 			= controler.getScenario().getKnowledges();
		this.locator 				= new LocationMutatorwChoiceSet(controler.getNetwork(), controler, this.knowledges);
		this.scorer 				= new PlanScorer (controler.getScoringFunctionFactory());
		this.network 				= controler.getNetwork();
		this.init(network);	
		this.tDepDelayCalc 			= new DepartureDelayAverageCalculator(this.network,controler.getConfig().travelTimeCalculator().getTraveltimeBinSize());
		this.controler.getEvents().addHandler(tDepDelayCalc);
		this.nonassignedAgents 		= new LinkedList<String>();
		this.noOfSchedulingAgents	= 5;
		
		this.finder					= finder;
		
		this.iterations 			= 20;
		this.noOfAssignmentAgents	= 10;
		this.primActsDistance 		= "yes";
		this.homeLocationDistance 	= "yes";
		this.sex 					= "no";
		this.age 					= "yes";
		this.license 				= "no";
		this.car_avail 				= "no";
		this.employed 				= "no";
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
		String outputfileOverview = Controler.getOutputFilename("assignment_log.xls");
		FileOutputStream fileOverview;
		try {
			fileOverview = new FileOutputStream(new File(outputfileOverview), true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		RecyclingModule.assignment = new PrintStream (fileOverview);
		RecyclingModule.assignment.println("Agent\tScore\tPlan\n");
	}
	
	private void init(final NetworkImpl network) {
		this.network.connect();
	}
	
	@SuppressWarnings("unchecked")
	public void prepareReplanning() {
		
		this.list = new ArrayList[2];
		for (int i=0;i<2;i++){
			list[i] = new ArrayList<Plan>();
		}
		this.schedulingModule.prepareReplanning();
	}

	public void handlePlan(final Plan plan) {	
		
		this.list[1].add(plan);
	}

	public void finishReplanning(){
		
		Statistics.noSexAssignment=false;
		Statistics.noCarAvailAssignment=false;
		Statistics.noEmploymentAssignment=false;
		Statistics.noLicenseAssignment=false;
		
		/* Individual optimization of agents */
		for (int i=0;i<this.noOfSchedulingAgents;i++) {
			int pos = (int)(MatsimRandom.getRandom().nextDouble()*this.list[1].size());
			list[0].add(list[1].get(pos));
			schedulingModule.handlePlan(list[1].get(pos));
			list[1].remove(pos);
		}
		schedulingModule.finishReplanning();
		
		/* Fill Optimized Agents object */
		agents = new OptimizedAgents (list[0], this.knowledges);
		
		/* Detect optimal coefficients metric */
		Statistics.prt=false;
		if ((Controler.getIteration()==1)	&&	(this.noOfSoftCoefficients>1)) this.detectCoefficients();
		else {
			this.calculate();
			this.rescheduleNonassigedAgents();
		}
		Statistics.prt=true;
		
		/* Print statistics of individual optimization */
		assignment.println("Iteration "+Controler.getIteration());
		assignment.println("Individual optimization");
		for (int i=0;i<list[0].size();i++){
			assignment.print(list[0].get(i).getPerson().getId()+"\t\t"+list[0].get(i).getScore()+"\t");
			for (int j=0;j<list[0].get(i).getPlanElements().size();j+=2){
				assignment.print(((ActivityImpl)(list[0].get(i).getPlanElements().get(j))).getType()+"\t");
			}
			assignment.println();
			if (i==this.noOfSchedulingAgents-1) {
				assignment.println();
				assignment.println("Individual optimization of non-assigend agents in metric detection phase");
			}
		}
		assignment.println();
		
		/* Assign remaining agents */
		assignmentModule.prepareReplanning();
	/*	if (this.list1Pointer.size()>0){
			int pointer = 0;
			for (int i=this.noOfAssignmentAgents;i<list[1].size();i++){
				if (i!=this.list1Pointer.get(pointer)){
					assignmentModule.handlePlan(list[1].get(i));
				}
				else pointer=java.lang.Math.min(pointer+1, this.list1Pointer.size()-1);
			}
		}
		else {*/
			for (int i=this.noOfAssignmentAgents;i<list[1].size();i++){
				assignmentModule.handlePlan(list[1].get(i));
			}
		//}
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
			for (int j=0;j<Statistics.list.get(i).size();j++){
				assignment.print(Statistics.list.get(i).get(j)+"\t");
			}
			assignment.println();
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
		if (Statistics.noSexAssignment ||
				Statistics.noCarAvailAssignment ||
				Statistics.noEmploymentAssignment ||
				Statistics.noLicenseAssignment) log.warn("For these agents, recycling was conducted without the relevant attribute.");
		Statistics.list.clear();
	}
	
	private void detectCoefficients (){
		
		/* Initialization */
		double offset = 0.5;
		double coefMatrix [][] = new double [this.iterations+1][this.noOfSoftCoefficients];	// first solution is base solution, then 'this.iterations' iterations
		for (int i=0;i<coefMatrix[0].length;i++){
			double aux = this.coefficients.getSingleCoef(i);
			coefMatrix[0][i] = aux;
		}
		double score [] = new double [this.iterations+1];
		double scoreAux, coefAux;
		int [] modified = new int [this.noOfSoftCoefficients-1]; // last coefficient fixed
		int modifiedAux;
		
		/* Iteration 0 */
		score [0]= this.calculate();		// calculate score for initial vector
		
		score[0]=this.rescheduleNonassigedAgents();		
		/*this.schedulingModule.prepareReplanning();
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
			//this.list[1].remove(list1Pointer.get(x));
			this.agents.addAgent((this.list[0].get(this.list[0].size()-1)));
			
		}
		if (this.nonassignedAgents.size()>0){
			this.nonassignedAgents.clear();
			score [0]= this.calculate();// Do this again, now all agents must be assignable.
			if (this.nonassignedAgents.size()>0) log.warn("Something went wrong when optimizing the non-assigned agents of the metric detection phase!");
		}*/
		
		for (int i=1;i<this.iterations+1;i++) score[i]=-100000;	// set all other iterations' scores to minimum value
	
		/* Further iterations */
		for (int i=1;i<this.iterations+1;i++){
			log.info("Metric detection: iteration "+i);
			
			for (int z=1;z<coefMatrix[0].length;z++){
				coefMatrix[i][z]=coefMatrix[i-1][z];
			}
			for (int j=0;j<this.noOfSoftCoefficients-1;j++){
				modifiedAux = modified[j];
				
				modified[j]=0;
				coefMatrix [i][j] = coefMatrix [i-1][j];
				if (modifiedAux!=-1){
					coefMatrix [i][j] = coefMatrix [i-1][j]+offset;
					coefMatrix [i][j] = this.checkTabuList(coefMatrix, i, j, offset);					
					this.coefficients.setSingleCoef(coefMatrix[i][j], j);
					scoreAux = this.calculate();
					if (scoreAux>score[i]) {
						score[i] = scoreAux;
						modified [j] = 1; // 1 means "increased by offset";
						for (int x=0;x<j;x++){
							modified [x]=0;	// set back the former index;
							coefMatrix[i][x]=coefMatrix[i-1][x]; // set back the coef itself;
						}
					}
					else {
						coefMatrix [i][j] = coefMatrix [i-1][j];
					}
				}
				
				if (modifiedAux!=1){
					coefAux = coefMatrix [i][j];	// keep coef solution temporarily;
					
					if (coefMatrix [i-1][j]-offset>=0){
						coefMatrix [i][j] = coefMatrix [i-1][j]-offset;
						coefMatrix [i][j] = this.checkTabuList(coefMatrix, i, j, offset);	
						this.coefficients.setSingleCoef(coefMatrix[i][j], j);
						scoreAux = this.calculate();
						if (scoreAux>score[i]) {
							score[i] = scoreAux;
							if (modified [j] == 1) modified [j] = -1; // -1 means "decreased by offset";
							else {
								for (int x=0;x<j;x++){
									modified [x]=0;	// set back to former index;
									coefMatrix[i][x]=coefMatrix[i-1][x]; // set back the coef itself;
								}
								modified [j] = -1;
							}
						}
						else {
							coefMatrix [i][j] = coefAux; // set back to former value;
						}
					}
					else {
						coefMatrix [i][j] = coefMatrix [i-1][j]+2*offset;
						coefMatrix [i][j] = this.checkTabuList(coefMatrix, i, j, offset);	
						this.coefficients.setSingleCoef(coefMatrix[i][j], j);
						scoreAux = this.calculate();
						if (scoreAux>score[i]) {
							score[i] = scoreAux;
							if (modified [j] != 1) {
								for (int x=0;x<j;x++){
									modified [x]=0;	// set back to former index;
									coefMatrix[i][x]=coefMatrix[i-1][x]; // set back the coef itself;
								}
								modified [j] = 1;
							}
						}
						else {
							coefMatrix [i][j] = coefAux; // set back to former value;
						}
					}
				}
			}
		}
		
		for (int i=0;i<score.length;i++){
			for (int j=0;j<coefMatrix[0].length-1;j++){
				log.info(coefMatrix[i][j]+" ");
			}
			log.info(this.coefficients.getSingleCoef(coefMatrix[0].length-1)+" "+score[i]);
		}
		
		
		double tmpScoreFinal = -100000;
		for (int i=0;i<this.iterations+1;i++){
			if (score[i]>tmpScoreFinal){
				tmpScoreFinal = score[i];
				this.coefficients.setCoef(coefMatrix[i]);
				for (int j=0;j<this.noOfSoftCoefficients;j++) log.info(softCoef.get(j)+" = "+this.coefficients.getCoef()[j]);
				log.info("Score = "+tmpScoreFinal);
				log.info("");
			}
		}
	}
	
	private double calculate (){
		double score = 0;
		this.assignmentModule.prepareReplanning();
		for (int j=0;j<java.lang.Math.min(this.noOfAssignmentAgents, list[1].size());j++){
			assignmentModule.handlePlan(list[1].get(j));
			log.info("Calculating agent "+list[1].get(j).getPerson().getId());
		}
		assignmentModule.finishReplanning();
		for (int j=0;j<java.lang.Math.min(this.noOfAssignmentAgents, list[1].size());j++){
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
			//this.list[1].remove(list1Pointer.get(x));
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
		log.info("list[0]:");
		for (int i=0;i<this.list[0].size();i++){
			log.info("i: "+i+": "+list[0].get(i).getPerson().getId());
		}
		
		return score;
	}
	
	private ArrayList<String> detectSoftCoefficients (){
		ArrayList<String> softCoef = new ArrayList<String>();
		if (this.primActsDistance.equals("yes")) {
			softCoef.add("primActsDistance");
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
		if (this.sex.equals("yes")) {
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
	
	
	private double checkTabuList (double [][] coefMatrix, int i, int j, double offset){
		OuterLoop:
		for (int x=0;x<i;x++){
			for (int y=0;y<coefMatrix[i].length-1;y++){
				if (coefMatrix[x][y]!=coefMatrix[i][y]) continue OuterLoop;
			}
			double coefAux = -1;
			for (int z=0;z<i;z++){
				if (coefMatrix[z][j]>coefAux)coefAux=coefMatrix[z][j];
			}
			return coefAux+offset;
		}
		return coefMatrix[i][j];
	}
	
	public OptimizedAgents getOptimizedAgents (){
		return this.agents;
	}

}
