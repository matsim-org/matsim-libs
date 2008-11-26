/* *********************************************************************** *
 * project: org.matsim.*
 * ClusterModule.java
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
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSetSimultan;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.controler.Controler;
import org.matsim.population.Act;
import org.matsim.population.Plan;
import org.matsim.replanning.modules.StrategyModule;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.scoring.PlanScorer;

import java.util.ArrayList;

public class ClusterModule implements StrategyModule {
	
	private  ArrayList<Plan> []				list;
	private final MultithreadedModuleA 		module;
	private final PreProcessLandmarks		preProcessRoutingData;
	private final LocationMutatorwChoiceSet locator;
	private final PlansCalcRouteLandmarks 	router;
	private final LegTravelTimeEstimator	estimator;
	private final double					minimumTime;
	private final ScheduleCleaner			cleaner;
	private final String					mode;
	private final PlanAlgorithm				timer;
	private final int						clusterNumber, testAgentsNumber;
	private String[]						criteria;
	private final Controler					controler;
	private double							distanceMeasure;
	
	
	
	public ClusterModule (ControlerMFeil controler){
		this.controler=controler;
		this.preProcessRoutingData 	= new PreProcessLandmarks(new FreespeedTravelTimeCost());
		this.preProcessRoutingData.run(controler.getNetwork());
		this.router 				= new PlansCalcRouteLandmarks (controler.getNetwork(), this.preProcessRoutingData, controler.getTravelCostCalculator(), controler.getTravelTimeCalculator());
		this.locator 				= new LocationMutatorwChoiceSetSimultan(controler.getNetwork(), controler);
		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(
				controler.getNetwork(), 
				controler.getTraveltimeBinSize());
		this.estimator = Gbl.getConfig().planomat().getLegTravelTimeEstimator(
				controler.getTravelTimeCalculator(), 
				controler.getTravelCostCalculator(), 
				tDepDelayCalc, 
				controler.getNetwork());
		this.timer					= new TimeOptimizer14 (this.estimator, new PlanScorer(controler.getScoringFunctionFactory()));
		this.module 				= new PlanomatX12Initialiser(controler, this.preProcessRoutingData, this.estimator, this.locator, this.timer);
		this.minimumTime			= 1800;
		this.cleaner				= new ScheduleCleaner (this.estimator, this.minimumTime);
		this.mode					= "timer";		
		this.clusterNumber			= 2;
		this.testAgentsNumber		= 5;
		this.criteria				= new String [2];
		this.criteria [0]			= "distance";
		this.criteria [1]			= "primacts";
	}
	
	public void init() {
		
		this.list = new ArrayList [this.clusterNumber];
		for (int i=0;i<this.clusterNumber;i++){
			list[i] = new ArrayList<Plan>();
		}
		this.module.init();
	}

	public void handlePlan(final Plan plan) {	
		
		if (this.list[0].size()<this.testAgentsNumber) this.list[0].add(plan);
		else this.list[1].add(plan);
	}

	public void finish(){
		
		for (int i=0;i<list[0].size();i++) module.handlePlan(list[0].get(i));
		module.finish();
		
		double [] distancesTestAgents = new double [list[0].size()];
		for (int i=0;i<distancesTestAgents.length;i++){
			distancesTestAgents[i] = list[0].get(i).getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(list[0].get(i).getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
		}
		int [] allocations = new int [list[1].size()];
		for (int i=0;i<list[1].size();i++){
			double distance = Double.MAX_VALUE;
			double distanceAgent = list[1].get(i).getPerson().getKnowledge().getActivities(true).get(0).getLocation().getCenter().calcDistance(list[1].get(i).getPerson().getKnowledge().getActivities(true).get(1).getLocation().getCenter());
			for (int j=0;j<list[0].size();j++){
				if (java.lang.Math.abs(distanceAgent-distancesTestAgents[j])<distance){
					allocations[i]=j;
					distance = java.lang.Math.abs(distanceAgent-distancesTestAgents[j]);
				}
			}
			this.writePlan(list[0].get(allocations[i]), list[1].get(i));
			this.locator.handlePlan(list[1].get(i));
			this.router.run(list[1].get(i));
			if (mode.equals("timer")){
				this.timer.run(list[1].get(i));
			}
			else this.cleanUpPlan(list[1].get(i));
		}
	}
	
	private void cleanUpPlan (Plan plan){
		double move = this.cleaner.run(((Act)(plan.getActsLegs().get(0))).getEndTime(), plan);
		int loops=1;
		while (move!=0.0){
			loops++;
			move = this.cleaner.run(java.lang.Math.max(((Act)(plan.getActsLegs().get(0))).getEndTime()-move,0), plan);
			if (loops>3) {
				for (int i=2;i< plan.getActsLegs().size()-4;i+=2){
					((Act)(plan.getActsLegs().get(i))).setDuration(this.minimumTime);
				}
				move = this.cleaner.run(this.minimumTime, plan);
				if (move!=0.0){
					throw new IllegalArgumentException("No valid plan possible for person "+plan.getPerson().getId());
				}
			}
		}
	}
	
	
	private void writePlan (Plan in, Plan out){
		Plan bestPlan = new Plan (in.getPerson());
		bestPlan.copyPlan(in);
		ArrayList<Object> al = out.getActsLegs();
		if(al.size()>bestPlan.getActsLegs().size()){ 
			int i;
			for (i = 2; i<bestPlan.getActsLegs().size()-2;i++){
				al.remove(i);
				al.add(i, bestPlan.getActsLegs().get(i));	
			}
			for (int j = i; j<al.size()-2;j=j+0){
				al.remove(j);
			}
		}
		else if(al.size()<bestPlan.getActsLegs().size()){
			int i;
			for (i = 2; i<al.size()-2;i++){
				al.remove(i);
				al.add(i, bestPlan.getActsLegs().get(i));	
			}
			for (int j = i; j<bestPlan.getActsLegs().size()-2;j++){			
				al.add(j, bestPlan.getActsLegs().get(j));
			}
		}
		else {
			for (int i = 2; i<al.size()-2;i++){
			al.remove(i);
			al.add(i, bestPlan.getActsLegs().get(i));	
			}
		}
	}
	
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
