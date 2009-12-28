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



import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.FixedRouteLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;


/**
 * @author Matthias Feil
 * Class that reads a file of plans (with or without varied plans) and evaluates them.
 */
public class PlansEvaluator extends PlansConstructor implements PlanStrategyModule {
		
	private final LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory;
	private static final Logger log = Logger.getLogger(PlansEvaluator.class);	
	                      
	public PlansEvaluator (Controler controler) {
		super (controler);
		this.inputFile = "/home/baug/mfeil/data/fullSet/it0/output_plans_mz01.xml";	
		this.outputFile = "/home/baug/mfeil/data/fullSet/it1/output_plans_mz11.xml.gz";	
		this.outputFileBiogeme = "/home/baug/mfeil/data/fullSet/it1/output_plans11.dat";	
		this.attributesInputFile = "/home/baug/mfeil/data/mz/attributes_MZ2005.txt";
		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(this.network,controler.getConfig().travelTimeCalculator().getTraveltimeBinSize());
		this.controler.getEvents().addHandler(tDepDelayCalc);
		this.legTravelTimeEstimatorFactory = new LegTravelTimeEstimatorFactory(controler.getTravelTimeCalculator(), tDepDelayCalc);
		
	}
	
	@Override
	public void prepareReplanning() {
		// Read the external plans file.
		new MatsimPopulationReader(this.controler.getScenario()).readFile(this.inputFile);		
		log.info("Reading population done.");
	}

	@Override
	public void finishReplanning(){
		
	// 	Needs to always run		
		this.evaluatePlans();
		this.writePlans(this.outputFile);
		
		
	// 	Only if similarity attribute is desired
		//this.sims = new MDSAM(this.population).runPopulation();
		
		
	// 	Type of writing the Biogeme file		
		//this.writePlansForBiogeme(this.outputFileBiogeme);
		//this.writePlansForBiogemeWithRandomSelection(this.outputFileBiogeme, this.attributesInputFile, 
		//		super.similarity, super.incomeConstant, super.incomeDivided, super.incomeDividedLN, super.incomeBoxCox, super.age, super.gender, super.employed, super.license, super.carAvail, super.seasonTicket, super.travelDistance, super.travelCost, super.travelConstant, super.bikeIn);
		this.writePlansForBiogemeWithRandomSelectionAccumulated(this.outputFileBiogeme, this.attributesInputFile, 
				super.beta, super.gamma, super.similarity, super.incomeConstant, super.incomeDivided, super.incomeDividedLN, super.incomeBoxCox, super.age, super.gender, super.employed, super.license, super.carAvail, super.seasonTicket, super.travelDistance, super.travelCost, super.travelConstant, super.beta_travel, super.bikeIn);
	}
	
	
	private void evaluatePlans (){
		log.info("Evaluating plans...");
		int counter=0;
		for (Person person : this.population.getPersons().values()) {
			counter++;
			if (counter%10==0) {
				log.info("Handled "+counter+" persons");
				Gbl.printMemoryUsage();
			}
			for (Plan plan : person.getPlans()) {
				
				LegTravelTimeEstimator estimator = (FixedRouteLegTravelTimeEstimator) legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
						plan,
						PlanomatConfigGroup.SimLegInterpretation.CetinCompatible, 
						PlanomatConfigGroup.RoutingCapability.fixedRoute,
						this.router);
				
				// Start from first leg
				for (int i=1;i<plan.getPlanElements().size();i++){
					if (i%2==1){
						LegImpl leg = ((LegImpl)(plan.getPlanElements().get(i)));
						leg.setDepartureTime(((PlanImpl) plan).getPreviousActivity(leg).getEndTime());
						double travelTime = estimator.getLegTravelTimeEstimation(plan.getPerson().getId(), ((PlanImpl) plan).getPreviousActivity(leg).getEndTime(), ((PlanImpl) plan).getPreviousActivity(leg), ((PlanImpl) plan).getNextActivity(leg), leg, false);
						leg.setTravelTime(travelTime);
						leg.setArrivalTime(leg.getDepartureTime()+leg.getTravelTime());
					}
					else{
						ActivityImpl act = ((ActivityImpl)(plan.getPlanElements().get(i)));
						act.setStartTime(((PlanImpl) plan).getPreviousLeg(act).getArrivalTime());
						try {
							act.setDuration(act.getEndTime()-act.getStartTime());
							if (act.getDuration()<=0) {
								act.setDuration(1);
								act.setEndTime(act.getStartTime()+1);
							}
						}catch(Exception e){
							log.info("Activity has no end time. No activity duration written.");
						}
					}
				}
				// if plan too long make it invalid (set score to -100000)
				if (((PlanImpl) plan).getLastActivity().getStartTime()-86400>((PlanImpl) plan).getFirstActivity().getEndTime()){
					plan.setScore(-100000.0);
				}
			}
		}
		log.info("done.");
	}
}
