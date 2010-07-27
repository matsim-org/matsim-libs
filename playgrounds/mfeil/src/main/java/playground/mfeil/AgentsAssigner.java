/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsAssigner.java
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



import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.knowledges.Knowledges;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mfeil.MDSAM.ActivityTypeFinder;


/**
 * @author Matthias Feil
 * Parallel PlanAlgorithm to assign the non-optimized agents to an optimized agent
 * (= non-optimized agent copies the plan of the most similar optimized agent).
 */

public class AgentsAssigner implements PlanAlgorithm{


	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////

	protected final Controler					controler;
	protected final PlanAlgorithm 				timer;
	protected final LocationMutatorwChoiceSet 	locator;
	protected final PlansCalcRoute				router;
	protected final Network						network;
	protected final RecyclingModule				module;
	protected LinkedList<String>				nonassignedAgents;
	protected Knowledges 						knowledges;
	private final ActivityTypeFinder 			finder;
	protected static final Logger 				log = Logger.getLogger(AgentsAssigner.class);
	private static final int					trialsLCTimings = 1;
	private static final double					LC_minimum_time_AA = 300.0;

	private final DistanceCoefficients coefficients;
	private String primActsDistance, homeLocation, municipality, age, sex, license, car_avail, employed;


	public AgentsAssigner (Controler controler, DepartureDelayAverageCalculator 	tDepDelayCalc,
			LocationMutatorwChoiceSet locator, PlanScorer scorer, ActivityTypeFinder finder, RecyclingModule recyclingModule,
			DistanceCoefficients coefficients, LinkedList<String> nonassignedAgents){

		this.controler				= controler;
		this.router 				= new PlansCalcRoute (controler.getConfig().plansCalcRoute(), controler.getNetwork(), controler.createTravelCostCalculator(), controler.getTravelTimeCalculator(), controler.getLeastCostPathCalculatorFactory());
		this.network				= controler.getNetwork();
		LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory = new LegTravelTimeEstimatorFactory(controler.getTravelTimeCalculator(), tDepDelayCalc);
		this.timer					= new TimeModeChoicer2(controler, legTravelTimeEstimatorFactory, scorer);
		this.locator 				= locator;
		this.finder					= finder;
		this.module					= recyclingModule;
		this.nonassignedAgents		= nonassignedAgents;
		this.knowledges = (controler.getScenario()).getKnowledges();

		this.coefficients = coefficients;
		this.primActsDistance	="no";
		this.homeLocation		="no";
		this.age				="no";
		this.sex				="no";
		this.license			="no";
		this.car_avail			="no";
		this.employed			="no";
		for (int i=0; i<this.coefficients.getNamesOfCoef().size();i++){
			if (this.coefficients.getNamesOfCoef().get(i).equals("primActsDistance")) this.primActsDistance="yes";
			if (this.coefficients.getNamesOfCoef().get(i).equals("homeLocation")) this.homeLocation="yes";
			if (this.coefficients.getNamesOfCoef().get(i).equals("municipality")) this.municipality="yes";
			if (this.coefficients.getNamesOfCoef().get(i).equals("age")) this.age="yes";
			if (this.coefficients.getNamesOfCoef().get(i).equals("sex")) this.sex="yes";
			if (this.coefficients.getNamesOfCoef().get(i).equals("license")) this.license="yes";
			if (this.coefficients.getNamesOfCoef().get(i).equals("car_avail")) this.car_avail="yes";
			if (this.coefficients.getNamesOfCoef().get(i).equals("employed")) this.employed="yes";
		}

	}


	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run (Plan plan){
		
		OptimizedAgents agents = this.module.getOptimizedAgents();

		double distance = Double.MAX_VALUE;
		double distanceAgent;
		int assignedAgent = -1;

		optimizedAgentsLoop:
		for (int j=0;j<agents.getNumberOfAgents();j++){

			/* Hard constraints */
			// All prim acts in potential agent's plan
			for (int i=0;i<this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).size();i++){
				boolean in = false;
				for (int x=0;x<agents.getAgentPlan(j).getPlanElements().size();x=x+2){
					// try statement with print block to analyze some strange exceptions in Zurich scenario
					try {
						if (((ActivityImpl)(agents.getAgentPlan(j).getPlanElements().get(x))).getType().equals(this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(i).getType())){
							in = true;
							break;
						}
					} catch (Exception e){
						log.warn(e);
						log.warn("Acts im Plan des schon optimierten Agenten "+agents.getAgentPlan(j).getPerson().getId()+":");
						for (int k=0;k<agents.getAgentPlan(j).getPlanElements().size();k++) {
							log.warn(agents.getAgentPlan(j).getPlanElements().get(k));
						}
						System.out.println();
						log.warn("Primacts im Knowledge des zuzuordnenden Agenten "+plan.getPerson().getId()+":");
						for (int k=0;k< this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).size();k++){
							this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(k);
						}
						System.out.println();
						continue optimizedAgentsLoop;	// if exception occurs go to next agent whatsoever the exception is.
					}
				}
				if (!in) {
					continue optimizedAgentsLoop;
				}
			}

			// All act types complying with those eligible to the agent (see ActivityTypeFinder)
			List<String> actTypes = this.finder.getActTypes(plan.getPerson());
			for (int i=2;i<agents.getAgentPlan(j).getPlanElements().size()-2;i+=2){ // "home" does not need to be checked
				if (!actTypes.contains(((ActivityImpl)(agents.getAgentPlan(j).getPlanElements().get(i))).getType())) {
					continue optimizedAgentsLoop;
				}
			}

			// Further hard constraints

			// Gender
			if (this.sex=="yes"){
				try{
					if (!((PersonImpl) plan.getPerson()).getSex().equals(((PersonImpl) agents.getAgentPerson(j)).getSex())) continue optimizedAgentsLoop;
				}
				catch (Exception e) {
					Statistics.noSexAssignment = true;
				}
			}

			// License
			if (this.license=="yes"){
				try{
					if (!((PersonImpl) plan.getPerson()).getLicense().equals(((PersonImpl) agents.getAgentPerson(j)).getLicense())) continue optimizedAgentsLoop;
				}
				catch (Exception e){
					Statistics.noLicenseAssignment = true;
				}
			}

			// Car availability
			if (this.car_avail=="yes"){
				try{
					if (!((PersonImpl) plan.getPerson()).getCarAvail().equals(((PersonImpl) agents.getAgentPerson(j)).getCarAvail())) continue optimizedAgentsLoop;
				}
				catch (Exception e){
					Statistics.noCarAvailAssignment = true;
				}
			}

			// Employment status
			if (this.employed=="yes"){
				try{
					if (!((PersonImpl) plan.getPerson()).isEmployed().equals(((PersonImpl) agents.getAgentPerson(j)).isEmployed())) continue optimizedAgentsLoop;
				}
				catch (Exception e){
					Statistics.noEmploymentAssignment = true;
				}
			}


			/* Distance (=soft) fitness */

			distanceAgent=0;
			// Distance between primary activities
			if (this.primActsDistance=="yes"){
				double tmpDistance=0;
				if (this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).size()>1){
					for (int k=0;k<this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).size()-1;k++){
						tmpDistance+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(k).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(k+1).getLocation().getCoord());
					}
					tmpDistance+=CoordUtils.calcDistance(this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).size()-1).getLocation().getCoord(), this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(true).get(0).getLocation().getCoord());
				}
				distanceAgent+=	this.coefficients.getSingleCoef("primActsDistance")*(java.lang.Math.abs(tmpDistance-this.module.getOptimizedAgents().getAgentDistance(j)));
			}

			// Distance between home location of potential agent to copy from and home location of agent in question
			if (this.homeLocation=="yes"){
				double homelocationAgentX = this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities("home", true).get(0).getFacility().getCoord().getX();
				double homelocationAgentY = this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities("home", true).get(0).getFacility().getCoord().getY();

				distanceAgent += this.coefficients.getSingleCoef("homeLocationDistance")*java.lang.Math.sqrt(java.lang.Math.pow((this.knowledges.getKnowledgesByPersonId().get(agents.getAgentPerson(j).getId()).getActivities("home", true).get(0).getFacility().getCoord().getX()-homelocationAgentX),2)+
						java.lang.Math.pow((this.knowledges.getKnowledgesByPersonId().get(agents.getAgentPerson(j).getId()).getActivities("home", true).get(0).getFacility().getCoord().getY()-homelocationAgentY),2));
			}

			// Municipality type
			if (this.municipality=="yes"){
				if (plan.getPerson().getCustomAttributes()!=null && plan.getPerson().getCustomAttributes().containsKey("municipality")){
					distanceAgent += this.coefficients.getSingleCoef("municipality") * java.lang.Math.abs(Integer.parseInt(plan.getPerson().getCustomAttributes().get("municipality").toString())-Integer.parseInt(agents.getAgentPerson(j).getCustomAttributes().get("municipality").toString()));
				}
				else Statistics.noMunicipalityAssignment = true;
			}

			// TODO @mfeil: exception handling missing
			if (this.age=="yes"){
				distanceAgent+= this.coefficients.getSingleCoef("age")* java.lang.Math.abs(((PersonImpl)(plan.getPerson())).getAge()-((PersonImpl)(agents.getAgentPerson(j))).getAge());
			}

			if (distanceAgent<distance){
				/*if (Statistics.prt==true){
					if (agents.filling[j]>0){
						assignedAgent=j;
						distance = distanceAgent;
						agents.filling[j]--;
					}
				}
				else {*/
					assignedAgent=j;
					distance = distanceAgent;
				//}
			}
		}
		if (distance==Double.MAX_VALUE){
			log.info("No agent to assign from found for agent "+plan.getPerson().getId()+"!");
			this.nonassignedAgents.add(plan.getPerson().getId().toString());
			return;
		}
		this.writePlan(agents.getAgentPlan(assignedAgent), plan);
	//	do {
	//		this.locator.handlePlan(plan);
	//		this.timer.run(plan);  // includes to write the new score to the plan
	//		counterLCTimings++;
	//	} while (plan.getScore()==-100000 && counterLCTimings <= AgentsAssigner.trialsLCTimings);

		ArrayList<PlanImpl> setOfLCplans = new ArrayList<PlanImpl>();
		double bestDis = Double.MAX_VALUE;
		int pointerToBestDis = -1;
		for (int z=0;z<trialsLCTimings;z++) {
			PlanImpl LCplan = new PlanImpl(plan.getPerson());
			LCplan.copyPlan(plan);
			this.locator.handlePlan(LCplan);
			this.router.run(LCplan);
			double dis = 0;
			for (int y = 1;y<LCplan.getPlanElements().size();y+=2) {
				LegImpl leg = ((LegImpl)(LCplan.getPlanElements().get(y)));
				if (!leg.getMode().equals(TransportMode.car)) dis += leg.getRoute().getDistance(); // distance in kilometers
				else dis += RouteUtils.calcDistance((NetworkRoute) leg.getRoute(), this.network);
			}
			if (dis<bestDis) {
				bestDis = dis;
				pointerToBestDis = z;
			}
			setOfLCplans.add(LCplan);
		}
	//	log.info("beste Distanz fuer Person "+plan.getPerson().getId()+" = "+bestDis+", von Person "+agents.getAgentPlan(assignedAgent).getPerson().getId());
		plan.getPlanElements().clear();
		for (int y=0;y<setOfLCplans.get(pointerToBestDis).getPlanElements().size();y++){
			if (y%2==0) plan.addActivity(((ActivityImpl)(setOfLCplans.get(pointerToBestDis).getPlanElements().get(y))));
			else plan.addLeg(((LegImpl)(setOfLCplans.get(pointerToBestDis).getPlanElements().get(y))));
		}
		this.timer.run(plan);  // includes to write the new score to the plan

		// ... nothing helps, call PlanomatX again
		if (plan.getScore()==-100000){
			log.info("No valid plan assignment possible for person "+plan.getPerson().getId()+" having received person's "+agents.getAgentPlan(assignedAgent).getPerson().getId());
			if (!this.nonassignedAgents.contains(plan.getPerson().getId().toString())){
				this.nonassignedAgents.add(plan.getPerson().getId().toString());
				log.info("Adding person "+plan.getPerson().getId()+" to list of non-assigned agents.");
			}
			return;
		}

		if (Statistics.prt==true) {
			ArrayList<String> prt = new ArrayList<String>();
			prt.add(plan.getPerson().getId().toString());
			prt.add(agents.getAgentPerson(assignedAgent).getId().toString());
			prt.add(plan.getScore().toString());
			prt.add(bestDis+"");
			for (int y=0;y<plan.getPlanElements().size();y+=2){
				prt.add(((ActivityImpl)(plan.getPlanElements().get(y))).getType());
			}
			Statistics.list.add(prt);
		//	log.info("added person "+plan.getPerson().getId()+" to Statistics.");
		}
	}


	protected void writePlan (Plan in, Plan out){
		PlanImpl bestPlan = new org.matsim.core.population.PlanImpl (in.getPerson());
		bestPlan.copyPlan(in);
		List<PlanElement> al = out.getPlanElements();

		ArrayList<ActivityOptionImpl> primActs = new ArrayList<ActivityOptionImpl>(this.knowledges.getKnowledgesByPersonId().get(out.getPerson().getId()).getActivities(true));
		for (int i=2;i<bestPlan.getPlanElements().size()-2;i+=2){
			if (!primActs.isEmpty()){
				for (int j=0;j<primActs.size();j++){
					if (((ActivityImpl)(bestPlan.getPlanElements().get(i))).getType().equals(primActs.get(j).getType())){
						ActivityFacility fac = this.controler.getFacilities().getFacilities().get(primActs.get(j).getFacility().getId());
						((ActivityImpl)(bestPlan.getPlanElements().get(i))).setFacilityId(fac.getId());
						// not only update of fac required but also coord and link; data inconsistencies otherwise
						((ActivityImpl)(bestPlan.getPlanElements().get(i))).setCoord(fac.getCoord());
						((ActivityImpl)(bestPlan.getPlanElements().get(i))).setLinkId(((ActivityFacilityImpl) fac).getLinkId());
						if (!primActs.get(j).getType().equals("home")) primActs.remove(j);
						break;
					}
				}
			}
		}
	//	for (int i=1;i<bestPlan.getPlanElements().size()-1;i+=2){
	//		((LegImpl)(bestPlan.getPlanElements().get(i))).setTravelTime(1);
	//	}

		if(bestPlan.getPlanElements().size()!=1 && al.size()>bestPlan.getPlanElements().size()){
			int i;
			for (i = 2; i<bestPlan.getPlanElements().size()-2;i++){
				al.remove(i);
				al.add(i, bestPlan.getPlanElements().get(i));
			}
			for (int j = i; j<al.size()-2;j=j+0){
				al.remove(j);
			}
		}
		// bestPlan.getPlanElements().size() == 1
		else if(al.size()>bestPlan.getPlanElements().size()){
			for (int j = 1; j<al.size();j=j+0){
				al.remove(j);
			}
			((ActivityImpl)al.get(0)).setEndTime(86400);
			((ActivityImpl)al.get(0)).setDuration(86400);
		}
		else if(al.size()!=1 && al.size()<bestPlan.getPlanElements().size()){
			int i;
			for (i = 2; i<al.size()-2;i++){
				al.remove(i);
				al.add(i, bestPlan.getPlanElements().get(i));
			}
			for (int j = i; j<bestPlan.getPlanElements().size()-2;j++){
				al.add(j, bestPlan.getPlanElements().get(j));
			}
		}
		// al.size() == 0
		else if(al.size()<bestPlan.getPlanElements().size()){
			ActivityImpl actHelp = new ActivityImpl((ActivityImpl)al.get(0));
			al.add(actHelp);
			for (int j = 1; j<bestPlan.getPlanElements().size()-1;j++){
				al.add(j, bestPlan.getPlanElements().get(j));
			}
		}
		else {
			for (int i = 2; i<al.size()-2;i++){
			al.remove(i);
			al.add(i, bestPlan.getPlanElements().get(i));
			}
		}
		/*
		// adjust first home duration if al.size()!=1
		if (al.size()>1){
			((ActivityImpl)al.get(0)).setEndTime(6*3600);
			((ActivityImpl)al.get(0)).setDuration(6*3600);
		}*/

		// adjust first home duration if al.size()!=1: if start time later than 18h move it to midday. Plans get endless, otherwise
		if (al.size()>1 && ((ActivityImpl)(al.get(0))).getEndTime()>18*3600){
			((ActivityImpl)al.get(0)).setEndTime(9*3600);
			((ActivityImpl)al.get(0)).setDuration(9*3600);
		}

		// adjust travel time budget for location choice:
		// travel times need to be translated back to car travel time since LC is based on car travel time
		// not translating results in too long distances traveled and a bias of the schedule recycling (compared to PlanomatX)
		for (int i = 1;i<al.size();i+=2) {
			LegImpl leg = ((LegImpl)(al.get(i)));

			// if travel time = 0 (=same facility) set to 1 sec because LC does nothing, otherwise
			if (leg.getTravelTime()==0) leg.setTravelTime(LC_minimum_time_AA);
			// divide by ptSpeedFactor
			else if (leg.getMode().toString().equals(TransportMode.pt.toString())) {
				leg.setTravelTime(leg.getTravelTime()/this.controler.getConfig().plansCalcRoute().getPtSpeedFactor());
			}
			// bikeSpeed * 1.5 [see strange distance calculation in PlansCalcRoute] / ( 25.3km/h * 1.2) [see LC]
			else if (leg.getMode().toString().equals(TransportMode.bike.toString())) {
				leg.setTravelTime(leg.getTravelTime()*this.controler.getConfig().plansCalcRoute().getBikeSpeed()*1.5/(25.3/3.6*1.2));
			}
			// walkSpeed * 1.5 [see strange distance calculation in PlansCalcRoute] / ( 25.3km/h * 1.2) [see LC]
			else if (leg.getMode().toString().equals(TransportMode.walk.toString())) {
				leg.setTravelTime(leg.getTravelTime()*this.controler.getConfig().plansCalcRoute().getWalkSpeed()*1.5/(25.3/3.6*1.2));
			}
		}
	}
}

