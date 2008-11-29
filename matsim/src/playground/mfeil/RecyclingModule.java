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

import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.controler.Controler;
import org.matsim.population.Act;
import org.matsim.population.Plan;
import org.matsim.replanning.modules.StrategyModule;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.scoring.PlanScorer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author Matthias Feil
 * This is a module that individually optimizes a number (=this.testAgentsNumber) of agents
 * and then recycles the derived plans for all further agents to be replanned.
 * The module is doubly parallelized meaning that
 * the individual optimization of the agents is parallel
 * the assigment of one of the optimized plan to all further agents is parallel.
 */


public class RecyclingModule implements StrategyModule {
	
	protected  ArrayList<Plan> []				list;
	protected final MultithreadedModuleA 		schedulingModule;
	protected final MultithreadedModuleA		assignmentModule;
	protected final PreProcessLandmarks			preProcessRoutingData;
	protected final LocationMutatorwChoiceSet 	locator;
	//private final PlansCalcRouteLandmarks 	router;
	protected final LegTravelTimeEstimator		estimator;
	protected final double						minimumTime;
	protected final ScheduleCleaner				cleaner;
	protected final PlanAlgorithm				timer;
	protected final int							testAgentsNumber;
	protected String[]							criteria;
	protected final Controler					controler;
	protected OptimizedAgents 					agents;
	
	public static PrintStream 					assignment;
	
	
	
	public RecyclingModule (ControlerMFeil controler){
		this.controler=controler;
		this.preProcessRoutingData 	= new PreProcessLandmarks(new FreespeedTravelTimeCost());
		this.preProcessRoutingData.run(controler.getNetwork());
		//this.router 				= new PlansCalcRouteLandmarks (controler.getNetwork(), this.preProcessRoutingData, controler.getTravelCostCalculator(), controler.getTravelTimeCalculator());
		this.locator 				= new LocationMutatorwChoiceSet(controler.getNetwork(), controler);
		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(
				controler.getNetwork(), 
				controler.getTraveltimeBinSize());
		this.estimator = Gbl.getConfig().planomat().getLegTravelTimeEstimator(
				controler.getTravelTimeCalculator(), 
				controler.getTravelCostCalculator(), 
				tDepDelayCalc, 
				controler.getNetwork());
		this.timer					= new TimeOptimizer14 (this.estimator, new PlanScorer(controler.getScoringFunctionFactory()));
		this.schedulingModule 		= new PlanomatX12Initialiser(controler, this.preProcessRoutingData, this.estimator, this.locator, this.timer);
		this.assignmentModule		= new AgentsAssignmentInitialiser (this.controler, this.preProcessRoutingData, this.estimator, this.locator,
			this.timer, this.cleaner, this, this.minimumTime);
		this.minimumTime			= 1800;
		this.cleaner				= new ScheduleCleaner (this.estimator, this.minimumTime);		
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
	
	public void init() {
		
		this.list = new ArrayList [2];
		for (int i=0;i<2;i++){
			list[i] = new ArrayList<Plan>();
		}
		this.schedulingModule.init();
	}

	public void handlePlan(final Plan plan) {	
		
		if (this.list[0].size()<this.testAgentsNumber) this.list[0].add(plan);
		else this.list[1].add(plan);
	}

	public void finish(){
		
		for (int i=0;i<list[0].size();i++) schedulingModule.handlePlan(list[0].get(i));
		schedulingModule.finish();
		
		for (int i=0;i<list[0].size();i++){
			assignment.print(list[0].get(i).getPerson().getId()+"\t"+list[0].get(i).getScore()+"\t");
			for (int j=0;j<list[0].get(i).getActsLegs().size();j+=2){
				assignment.print(((Act)(list[0].get(i).getActsLegs().get(j))).getType()+"\t");
			}
			assignment.println();
		}
		assignment.println();
		
		this.agents = new OptimizedAgents (this.list[0]);		
		this.assignmentModule.init();		
		for (int i=0;i<list[1].size();i++){
			this.assignmentModule.handlePlan(this.list[1].get(i));
		}		
		this.assignmentModule.finish();
		
		for (int i=0;i<Statistics.list.size();i++){
			assignment.println(Statistics.list.get(i)[0]+"\t"+Statistics.list.get(i)[1]+"\t"+Statistics.list.get(i)[2]);
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
				if (list.get(i).getActsLegs().size()!=list.get(j).getActsLegs().size()) distanceMatrix[i][j]+=1;
				for (int x=0;x<java.lang.Math.min(list.get(i).getActsLegs().size(), list.get(j).getActsLegs().size());x+=2){
					if (((Act)(list.get(i).getActsLegs().get(x))).getType()!=((Act)(list.get(i).getActsLegs().get(x))).getType()) distanceMatrix[i][j]+=1;
				}
				System.out.println(i+""+j+" = "+distanceMatrix[i][j]);
			}
		}
		
		int firstCentroid = (int)java.lang.Math.floor(MatsimRandom.random.nextDouble()*this.testAgentsNumber);
		int secondCentroid;
		do {
			secondCentroid = (int)java.lang.Math.floor(MatsimRandom.random.nextDouble()*this.testAgentsNumber);
		} while (secondCentroid==firstCentroid);
		
		System.out.println("firstCentroid = "+firstCentroid+", secondCentroid = "+secondCentroid);
		
		int one=0;
		int two=0;
		double firstMeasure=0;
		double secondMeasure=0;
		int [] dependents = new int [this.testAgentsNumber];
		for (int i=0;i<this.testAgentsNumber;i++){
			if (i!=firstCentroid && i!=secondCentroid){
				if (firstCentroid<i && secondCentroid<i) {
					if (distanceMatrix[firstCentroid][i]<distanceMatrix[secondCentroid][i]) {
						dependents[i]=1;
						firstMeasure+=list.get(i).getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(list.get(i).getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
						System.out.println("firstMeasure = "+firstMeasure);
						one++;
					}
					else {
						dependents[i]=2;
						secondMeasure+=list.get(i).getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(list.get(i).getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
						System.out.println("secondMeasure = "+secondMeasure);
						two++;
					}
				}
				else if (firstCentroid>i && secondCentroid>i) {
					if (distanceMatrix[i][firstCentroid]<distanceMatrix[i][secondCentroid]) {
						dependents[i]=1;
						firstMeasure+=list.get(i).getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(list.get(i).getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
						System.out.println("firstMeasure = "+firstMeasure);
						one++;
					}
					else{
						dependents[i]=2;
						secondMeasure+=list.get(i).getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(list.get(i).getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
						System.out.println("secondMeasure = "+secondMeasure);
						two++;
					}
				}
				else if (firstCentroid<i && secondCentroid>i) {
					if (distanceMatrix[firstCentroid][i]<distanceMatrix[i][secondCentroid]) {
						dependents[i]=1;
						firstMeasure+=list.get(i).getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(list.get(i).getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
						System.out.println("firstMeasure = "+firstMeasure);
						one++;
					}
					else {
						dependents[i]=2;
						secondMeasure+=list.get(i).getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(list.get(i).getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
						System.out.println("secondMeasure = "+secondMeasure);
						two++;
					}
				}
				else {
					if (distanceMatrix[i][firstCentroid]<distanceMatrix[secondCentroid][i]) {
						dependents[i]=1;
						firstMeasure+=list.get(i).getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(list.get(i).getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
						System.out.println("firstMeasure = "+firstMeasure);
						one++;
					}
					else {
						dependents[i]=2;
						secondMeasure+=list.get(i).getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(list.get(i).getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
						System.out.println("secondMeasure = "+secondMeasure);
						two++;
					}
				}
			}
		}
		firstMeasure+=list.get(firstCentroid).getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(list.get(firstCentroid).getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
		secondMeasure+=list.get(secondCentroid).getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(list.get(secondCentroid).getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
		
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
