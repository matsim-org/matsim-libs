/* *********************************************************************** *
 * project: org.matsim.*
 * PlansEvaluator.java
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



import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationImpl;
import java.util.Iterator;

import org.apache.log4j.Logger;


import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.PlanStrategyModule;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.FixedRouteLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;




/**
 * @author Matthias Feil
 * Class that reads a file of plans (with or without varied plans) and evaluates them.
 */


public class PlansEvaluator implements PlanStrategyModule{
		
	private final Controler controler;
	private final String inputFile, outputFile;
	private PopulationImpl population;
	private final DepartureDelayAverageCalculator tDepDelayCalc;
	private final NetworkLayer network;
	private final PlansCalcRoute router;
	private final LegTravelTimeEstimator estimator;
	private static final Logger log = Logger.getLogger(PlansEvaluator.class);
	
	                      
	public PlansEvaluator (Controler controler) {
		this.controler = controler;
		this.inputFile = "./plans/input_plans.xml.gz";	
		this.outputFile = "./plans/output_plans.dat";	
		this.population = new PopulationImpl();
		this.network = controler.getNetwork();
		this.init(network);	
		this.router = new PlansCalcRoute (controler.getConfig().plansCalcRoute(), controler.getNetwork(), controler.getTravelCostCalculator(), controler.getTravelTimeCalculator(), controler.getLeastCostPathCalculatorFactory());
		this.tDepDelayCalc = new DepartureDelayAverageCalculator(this.network,controler.getConfig().travelTimeCalculator().getTraveltimeBinSize());
		this.controler.getEvents().addHandler(tDepDelayCalc);
		LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory = new LegTravelTimeEstimatorFactory(controler.getTravelTimeCalculator(), this.tDepDelayCalc);
		this.estimator = (FixedRouteLegTravelTimeEstimator) legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
				PlanomatConfigGroup.SimLegInterpretation.CetinCompatible, 
				PlanomatConfigGroup.RoutingCapability.fixedRoute,
				this.router);
		
	}
	
	private void init(final NetworkLayer network) {
		this.network.connect();
	}
	
	public void prepareReplanning() {
		// Read the external plans file.
		new MatsimPopulationReader(this.population, this.controler.getNetwork()).readFile(this.inputFile);		
	}

	public void handlePlan(final PlanImpl plan) {			
		// Do nothing here. We work only on the external plans.
	}

	public void finishReplanning(){
		for (Iterator<PersonImpl> iterator1 = this.population.getPersons().values().iterator(); iterator1.hasNext();){
			PersonImpl person = iterator1.next();
			for (Iterator<PlanImpl> iterator2 = person.getPlans().iterator(); iterator2.hasNext();){
				PlanImpl plan = iterator2.next();
				this.router.run(plan);
				this.estimator.initPlanSpecificInformation(plan);
				// Start from first leg
				for (int i=1;i<plan.getPlanElements().size();i++){
					if (i%2==1){
						LegImpl leg = ((LegImpl)(plan.getPlanElements().get(i)));
						leg.setDepartureTime(plan.getPreviousActivity(leg).getEndTime());
						double travelTime = this.estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), plan.getPreviousActivity(leg).getEndTime(), plan.getPreviousActivity(leg), plan.getNextActivity(leg), leg, false);
						leg.setTravelTime(travelTime);
						leg.setArrivalTime(leg.getDepartureTime()+leg.getTravelTime());
					}
					else{
						ActivityImpl act = ((ActivityImpl)(plan.getPlanElements().get(i)));
						act.setStartTime(plan.getPreviousLeg(act).getArrivalTime());
						try {
							act.setDuration(act.getEndTime()-act.getStartTime());
						}catch(Exception e){
							log.info("Activity has no end time. No activity duration written.");
						}
					}
				}
			}
		}
		this.writePlansForBiogeme();
	}
	
	private void writePlansForBiogeme(){
		// to be written yet
	}
		
}
