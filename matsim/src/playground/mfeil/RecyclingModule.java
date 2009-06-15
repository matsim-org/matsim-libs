/* *********************************************************************** *
 * project: org.matsim.*
 * RecyclingModule.java
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
import java.util.LinkedList;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.replanning.PlanStrategyModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.knowledges.Knowledges;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author Matthias Feil
 * This is a module that individually optimizes a number (=this.testAgentsNumber) of agents
 * and then recycles the derived plans for all further agents to be replanned.
 * The module is doubly parallelized meaning that
 * the individual optimization of the agents is parallel
 * the assigment of one of the optimized plan to all further agents is parallel.
 */


public class RecyclingModule implements PlanStrategyModule {
	
	protected  ArrayList<Plan> []				list;
	protected final AbstractMultithreadedModule 		schedulingModule;
	protected final AbstractMultithreadedModule		assignmentModule;
	protected final LocationMutatorwChoiceSet 	locator;
	protected final PlanScorer					scorer;
	protected final double						minimumTime;
	protected final PlanAlgorithm				timer;
	protected final int							testAgentsNumber;
	protected String[]							criteria;
	protected final Controler					controler;
	protected OptimizedAgents 					agents;
	protected LinkedList<String>				nonassignedAgents;
	protected final DepartureDelayAverageCalculator 	tDepDelayCalc;
	protected final NetworkLayer 						network;
	public static PrintStream 					assignment;
	protected Knowledges knowledges;
	
	
	
	public RecyclingModule (ControlerMFeil controler){
		this.controler=controler;
		this.knowledges = ((ScenarioImpl)controler.getScenarioData()).getKnowledges();
		this.locator 				= new LocationMutatorwChoiceSet(controler.getNetwork(), controler, this.knowledges);
		this.scorer 				= new PlanScorer (controler.getScoringFunctionFactory());
		this.network = controler.getNetwork();
		this.init(network);	
		this.tDepDelayCalc = new DepartureDelayAverageCalculator(this.network,controler.getConfig().travelTimeCalculator().getTraveltimeBinSize());
	
		this.nonassignedAgents 		= new LinkedList<String>();
		this.timer					= new TimeModeChoicer1 (controler, this.tDepDelayCalc);
		//this.timer					= new Planomat (this.estimator, controler.getScoringFunctionFactory());
		this.schedulingModule 		= new PlanomatX12Initialiser(controler);
		this.assignmentModule		= new AgentsAssignmentInitialiser (this.controler, this.tDepDelayCalc, this.locator,
			this.scorer, this, this.minimumTime, this.nonassignedAgents);
		this.minimumTime			= 3600;	
		this.testAgentsNumber		= 5;
		this.criteria				= new String [2];
		this.criteria [0]			= "distance";
		this.criteria [1]			= "primacts";
		
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
		assignment.println("Agent\tScore\tPlan\n");
	}
	
	private void init(final NetworkLayer network) {
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
		
		for (int i=0;i<list[0].size();i++) schedulingModule.handlePlan(list[0].get(i));
		schedulingModule.finishReplanning();
		
		for (int i=0;i<list[0].size();i++){
			assignment.print(list[0].get(i).getPerson().getId()+"\t"+list[0].get(i).getScore()+"\t");
			for (int j=0;j<list[0].get(i).getPlanElements().size();j+=2){
				assignment.print(((Activity)(list[0].get(i).getPlanElements().get(j))).getType()+"\t");
			}
			assignment.println();
		}
		assignment.println();
		
		this.agents = new OptimizedAgents (this.list[0], this.knowledges);		
		this.assignmentModule.prepareReplanning();		
		for (int i=0;i<list[1].size();i++){
			this.assignmentModule.handlePlan(this.list[1].get(i));
		}		
		this.assignmentModule.finishReplanning();
		
		for (int i=0;i<Statistics.list.size();i++){
			for (int j=0;j<Statistics.list.get(i).size();j++){
				assignment.println(Statistics.list.get(i).get(j));
			}
		}
		Statistics.list.clear();
	}
	
	public OptimizedAgents getOptimizedAgents (){
		return this.agents;
	}
		
	
	
	@Deprecated
	private double [] findDistanceMeasure (final ArrayList<Plan> list){
		
		int [][] distanceMatrix = new int [this.testAgentsNumber][this.testAgentsNumber];
		for (int i=0;i<this.testAgentsNumber-1;i++){
			for (int j=i+1;j<this.testAgentsNumber;j++){
				distanceMatrix[i][j]=0;
				if (list.get(i).getPlanElements().size()!=list.get(j).getPlanElements().size()) distanceMatrix[i][j]+=1;
				for (int x=0;x<java.lang.Math.min(list.get(i).getPlanElements().size(), list.get(j).getPlanElements().size());x+=2){
					if (((Activity)(list.get(i).getPlanElements().get(x))).getType()!=((Activity)(list.get(i).getPlanElements().get(x))).getType()) distanceMatrix[i][j]+=1;
				}
				System.out.println(i+""+j+" = "+distanceMatrix[i][j]);
			}
		}
		
		int firstCentroid = (int)java.lang.Math.floor(MatsimRandom.getRandom().nextDouble()*this.testAgentsNumber);
		int secondCentroid;
		do {
			secondCentroid = (int)java.lang.Math.floor(MatsimRandom.getRandom().nextDouble()*this.testAgentsNumber);
		} while (secondCentroid==firstCentroid);
		
		System.out.println("firstCentroid = "+firstCentroid+", secondCentroid = "+secondCentroid);
		
		int one=0;
		int two=0;
		double firstMeasure=0;
		double secondMeasure=0;
		int [] dependents = new int [this.testAgentsNumber];
		for (int i=0;i<this.testAgentsNumber;i++){
			if ((i!=firstCentroid) && (i!=secondCentroid)){
				if ((firstCentroid<i) && (secondCentroid<i)) {
					if (distanceMatrix[firstCentroid][i]<distanceMatrix[secondCentroid][i]) {
						dependents[i]=1;
						firstMeasure+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(0).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(1).getLocation().getCoord());
						System.out.println("firstMeasure = "+firstMeasure);
						one++;
					}
					else {
						dependents[i]=2;
						secondMeasure+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(0).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(1).getLocation().getCoord());
						System.out.println("secondMeasure = "+secondMeasure);
						two++;
					}
				}
				else if ((firstCentroid>i) && (secondCentroid>i)) {
					if (distanceMatrix[i][firstCentroid]<distanceMatrix[i][secondCentroid]) {
						dependents[i]=1;
						firstMeasure+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(0).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(1).getLocation().getCoord());
						System.out.println("firstMeasure = "+firstMeasure);
						one++;
					}
					else{
						dependents[i]=2;
						secondMeasure+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(0).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(1).getLocation().getCoord());
						System.out.println("secondMeasure = "+secondMeasure);
						two++;
					}
				}
				else if ((firstCentroid<i) && (secondCentroid>i)) {
					if (distanceMatrix[firstCentroid][i]<distanceMatrix[i][secondCentroid]) {
						dependents[i]=1;
						firstMeasure+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(0).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(1).getLocation().getCoord());
						System.out.println("firstMeasure = "+firstMeasure);
						one++;
					}
					else {
						dependents[i]=2;
						secondMeasure+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(0).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(1).getLocation().getCoord());
						System.out.println("secondMeasure = "+secondMeasure);
						two++;
					}
				}
				else {
					if (distanceMatrix[i][firstCentroid]<distanceMatrix[secondCentroid][i]) {
						dependents[i]=1;
						firstMeasure+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(0).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(1).getLocation().getCoord());
						System.out.println("firstMeasure = "+firstMeasure);
						one++;
					}
					else {
						dependents[i]=2;
						secondMeasure+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(0).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(list.get(i).getPerson().getId()).getActivities(true).get(1).getLocation().getCoord());
						System.out.println("secondMeasure = "+secondMeasure);
						two++;
					}
				}
			}
		}
		firstMeasure+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(list.get(firstCentroid).getPerson().getId()).getActivities(true).get(0).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(list.get(firstCentroid).getPerson().getId()).getActivities(true).get(1).getLocation().getCoord());
		secondMeasure+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(list.get(secondCentroid).getPerson().getId()).getActivities(true).get(0).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(list.get(secondCentroid).getPerson().getId()).getActivities(true).get(1).getLocation().getCoord());
		
		firstMeasure/=one+1;
		secondMeasure/=two+1;
		
		System.out.println ("First measure = "+firstMeasure+" und second measure = "+secondMeasure);
		int a,b;
		if (firstMeasure<secondMeasure) {
			a = firstCentroid;
			b = secondCentroid;
		}
		else{
			a = secondCentroid;
			b = firstCentroid;
		}
		
		return (new double[]{java.lang.Math.abs((firstMeasure-secondMeasure)/2)+java.lang.Math.min(firstMeasure, secondMeasure),a,b});
	}

}
