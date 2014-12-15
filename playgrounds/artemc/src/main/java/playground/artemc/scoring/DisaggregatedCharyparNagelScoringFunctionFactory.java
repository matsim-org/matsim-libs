/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.artemc.scoring;

import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import playground.artemc.heterogeneity.HeterogeneityConfig;
import playground.artemc.scoring.functions.CharyparNagelActivityScoring;
import playground.artemc.scoring.functions.CharyparNagelAgentStuckScoring;
import playground.artemc.scoring.functions.CharyparNagelLegScoring;
import playground.artemc.scoring.functions.CharyparNagelMoneyScoring;
import playground.artemc.scoring.functions.PersonalScoringParameters;
import playground.artemc.scoring.functions.PersonalScoringParameters.Mode;
/**
 * A factory to create scoring functions as described by D. Charypar and K. Nagel.
 * 
 * <blockquote>
 *  <p>Charypar, D. und K. Nagel (2005) <br>
 *  Generating complete all-day activity plans with genetic algorithms,<br>
 *  Transportation, 32 (4) 369-397.</p>
 * </blockquote>
 * 
 * @author rashid_waraich
 */
public class DisaggregatedCharyparNagelScoringFunctionFactory implements ScoringFunctionFactory {
	

	private static final Logger log = Logger.getLogger(DisaggregatedCharyparNagelScoringFunctionFactory.class);

	protected Network network;
	private final PlanCalcScoreConfigGroup config;
	private HashMap<Id, ScoringFunction> personScoringFunctions;
	private HashMap<Id<Person>, Double> incomeFactors = null;
	private HashMap<Id<Person>, Double> betaFactors = null;
	private String simulationType;


	public DisaggregatedCharyparNagelScoringFunctionFactory(final PlanCalcScoreConfigGroup config, Network network) {
		this.config = config;
		this.network = network;
		this.personScoringFunctions = new HashMap<Id, ScoringFunction>();
	}

	public DisaggregatedCharyparNagelScoringFunctionFactory(final PlanCalcScoreConfigGroup config, Network network, HeterogeneityConfig heterogeneityConfig) {
		this.config = config;
		this.network = network;
		this.personScoringFunctions = new HashMap<Id, ScoringFunction>();
		this.incomeFactors = heterogeneityConfig.getIncomeFactors();
		this.betaFactors = heterogeneityConfig.getBetaFactors();
		this.simulationType = heterogeneityConfig.getSimulationType();
	}

	/**
	 * puts the scoring functions together, which form the
	 * CharyparScoringFunction
	 * <p/>
	 * This creational method gets the plan as an argument.  Since it is possible to get the person from the plan, it is thus
	 * possible to make the scoring function person-specific.
	 * <p/>  
	 * Notes:<ul>
	 * <li>If I understand this correctly, this creational method is 
	 * called in every iteration. kai, apr'11
	 * <li>The fact that you have a person-specific scoring function does not mean that the "creative" modules
	 * (such as route choice) are person-specific.  This is not a bug but a deliberate design concept in order 
	 * to reduce the consistency burden.  Instead, the creative modules should generate a diversity of possible
	 * solutions.  In order to do a better job, they may (or may not) use person-specific info.  kai, apr'11
	 * </ul>
	 * 
	 * @param plan
	 * @return new ScoringFunction
	 */

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		PersonalScoringParameters params = new PersonalScoringParameters(this.config);
		
		//Adjust individuals scoring parameters for heterogeneity simulation. If simulationType is set to "homo" (default setting) no adjustment takes place.
		adjustParametersForHeterogeneity(this.simulationType, person, params);

		DisaggregatedSumScoringFunction sumScoringFunction = new DisaggregatedSumScoringFunction();
		sumScoringFunction.setParams(params);
		
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));		
		for(String mode:config.getModes().keySet()){
			sumScoringFunction.addLegScoringFunction(mode, new CharyparNagelLegScoring(params, this.network));
		}
		sumScoringFunction.addLegScoringFunction("transit_walk", new CharyparNagelLegScoring(params, this.network));
		
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

		//		ScoringFunctionAccumulator sumScoringFunction = new ScoringFunctionAccumulator();
		//		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(this.params));
		//		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(this.params, this.network));
		//		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(this.params));
		//		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(this.params));

		personScoringFunctions.put(person.getId(), sumScoringFunction);
		return sumScoringFunction;
	}
	
	public PersonalScoringParameters adjustParametersForHeterogeneity(String simulationType, Person person, PersonalScoringParameters params){
		if(incomeFactors!=null){
			
			/*Calculate the mean in order to adjust the utility parameters*/
			Double factorSum=0.0;
			Double factorMean = 0.0;
			for(Double incomeFactor:this.incomeFactors.values()){
				factorSum = factorSum + incomeFactor;
			}
			factorMean = factorSum / (double) incomeFactors.size();
			

		if(simulationType.equals("hetero")){
			
				params.marginalUtilityOfPerforming_s =  params.marginalUtilityOfPerforming_s * (1.0/(incomeFactors.get(person.getId())/factorMean));
				
				params.marginalUtilityOfLateArrival_s =  params.marginalUtilityOfLateArrival_s * (1.0/(incomeFactors.get(person.getId())/factorMean));
				params.marginalUtilityOfEarlyDeparture_s= params.marginalUtilityOfEarlyDeparture_s * (1.0/(incomeFactors.get(person.getId())/factorMean));
				
				for (Entry<String, Mode> mode : params.modeParams.entrySet()) {
					mode.getValue().marginalUtilityOfTraveling_s = mode.getValue().marginalUtilityOfTraveling_s  * (1.0/(incomeFactors.get(person.getId()) / factorMean));
				}
			

		}
		else if(simulationType.equals("heteroAlpha") ){
			
				double performingConst = 	params.marginalUtilityOfPerforming_s;
			
				params.marginalUtilityOfPerforming_s =  params.marginalUtilityOfPerforming_s * (1.0/(incomeFactors.get(person.getId())/factorMean));
				
				params.marginalUtilityOfLateArrival_s =  params.marginalUtilityOfLateArrival_s;
				params.marginalUtilityOfEarlyDeparture_s =  params.marginalUtilityOfLateArrival_s;
				
				params.marginalUtilityOfWaiting_s = params.marginalUtilityOfPerforming_s - performingConst;
				
				for (Entry<String, Mode> mode : params.modeParams.entrySet()) {
					mode.getValue().marginalUtilityOfTraveling_s = mode.getValue().marginalUtilityOfTraveling_s  * (1.0/(incomeFactors.get(person.getId()) / factorMean));
				}
			

		}
		else if(simulationType.equals("heteroGamma") ){
				
				params.marginalUtilityOfPerforming_s =  params.marginalUtilityOfPerforming_s;
				
				params.marginalUtilityOfLateArrival_s =  params.marginalUtilityOfLateArrival_s * (incomeFactors.get(person.getId())/factorMean);
				params.marginalUtilityOfEarlyDeparture_s =  params.marginalUtilityOfEarlyDeparture_s * (incomeFactors.get(person.getId())/factorMean);
		
				params.marginalUtilityOfWaiting_s = params.marginalUtilityOfWaiting_s;
			}
		else if(simulationType.equals("heteroGammaProp") ){
			
			params.marginalUtilityOfPerforming_s =  params.marginalUtilityOfPerforming_s;
			
			params.marginalUtilityOfLateArrival_s =  params.marginalUtilityOfLateArrival_s * (1.0/(incomeFactors.get(person.getId())/factorMean));
			params.marginalUtilityOfEarlyDeparture_s= params.marginalUtilityOfEarlyDeparture_s * (1.0/(incomeFactors.get(person.getId())/factorMean));
	
			params.marginalUtilityOfWaiting_s = params.marginalUtilityOfWaiting_s;
		}
		
		else if(simulationType.equals("heteroAlphaProp") ){
			
			double performingConst = 	params.marginalUtilityOfPerforming_s;
				
			params.marginalUtilityOfPerforming_s =  params.marginalUtilityOfPerforming_s * (1.0/(incomeFactors.get(person.getId())/factorMean));
			
			params.marginalUtilityOfLateArrival_s =  params.marginalUtilityOfLateArrival_s;
			params.marginalUtilityOfEarlyDeparture_s =  params.marginalUtilityOfLateArrival_s;
			
			params.marginalUtilityOfWaiting_s = params.marginalUtilityOfPerforming_s - betaFactors.get(person.getId()) * performingConst;
			
			for (Entry<String, Mode> mode : params.modeParams.entrySet()) {
				mode.getValue().marginalUtilityOfTraveling_s = mode.getValue().marginalUtilityOfTraveling_s  * (1.0/(incomeFactors.get(person.getId()) / factorMean));
			}
		}
			

		}
		return params;
	}

	public HashMap<Id, ScoringFunction> getPersonScoringFunctions() {
		return personScoringFunctions;
	}

}
