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

package playground.artemc.heterogeneity.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import playground.artemc.heterogeneity.IncomeHeterogeneity;
import playground.artemc.heterogeneity.scoring.functions.*;
import playground.artemc.heterogeneity.scoring.functions.PersonalScoringParameters.Mode;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map.Entry;

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
public class HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory implements ScoringFunctionFactory, PersonalScoringFunctionFactory {
	

	private static final Logger log = Logger.getLogger(HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory.class);

	protected Network network;
	private final PlanCalcScoreConfigGroup config;
	private HashMap<Id, ScoringFunction> personScoringFunctions;
	private HashMap<Id<Person>, Double> incomeFactors = null;
	private HashMap<Id<Person>, Double> betaFactors = null;

	private String simulationType;
	private IncomeHeterogeneity incomeHeterogeneity;


	public HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory(final PlanCalcScoreConfigGroup config, Network network) {
		this.config = config;
		this.network = network;
		this.personScoringFunctions = new HashMap<Id, ScoringFunction>();
		this.simulationType = "homo";
	}

	//setter method injector
	@Inject
	public void setIncomeHeterogeneity(IncomeHeterogeneity incomeHeterogeneity){
		this.incomeHeterogeneity = incomeHeterogeneity;
	}

	private void init(){
		this.incomeFactors = incomeHeterogeneity.getIncomeFactors();
		this.betaFactors = incomeHeterogeneity.getBetaFactors();
		this.simulationType = incomeHeterogeneity.getType();
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {

		//this.init();
		PersonalScoringParameters params = new PersonalScoringParameters(this.config);
		
		//Adjust individuals scoring parameters for heterogeneity simulation. If simulationType is set to "homo" (default setting) no adjustment takes place.
		adjustParametersForHeterogeneity(this.simulationType, person, params);

		DisaggregatedSumScoringFunction sumScoringFunction = new DisaggregatedSumScoringFunction();
		sumScoringFunction.setParams(params);

		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));		
		for(String mode:params.modeParams.keySet()){
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

		if(person.getCustomAttributes().containsKey("incomeAlphaFactor") && !simulationType.equals("homo")){

			Double incomeAlphaFactor = (Double) person.getCustomAttributes().get("incomeAlphaFactor");

//			/*Calculate the mean in order to adjust the utility parameters*/
//			Double factorSum=0.0;
//			Double factorMean = 0.0;
//			Double inverseFactorSum=0.0;
//			Double inverseFactorMean=0.0;
//			for(Double incomeFactor:this.incomeFactors.values()){
//				factorSum = factorSum + incomeFactor;
//				inverseFactorSum = inverseFactorSum + (1.0/incomeFactor);
//			}
//			factorMean = factorSum / (double) incomeFactors.size();
//			inverseFactorMean = inverseFactorSum / (double) incomeFactors.size();
			

		if(simulationType.equals("hetero")){

			    /* Adjust alpha - value of time*/
				params.marginalUtilityOfPerforming_s =  params.marginalUtilityOfPerforming_s * incomeAlphaFactor;

				for (Entry<String, Mode> mode : params.modeParams.entrySet()) {
					mode.getValue().marginalUtilityOfTraveling_s = mode.getValue().marginalUtilityOfTraveling_s  * incomeAlphaFactor;
				}

				/* Adjust beta - schedule delay early*/
				params.marginalUtilityOfWaiting_s =  params.marginalUtilityOfWaiting_s * incomeAlphaFactor;

				/* Adjust gamma - schedule delay late*/
				params.marginalUtilityOfLateArrival_s =  params.marginalUtilityOfLateArrival_s * incomeAlphaFactor;
				params.marginalUtilityOfEarlyDeparture_s= params.marginalUtilityOfEarlyDeparture_s * incomeAlphaFactor;

		}
		else if(simulationType.equals("heteroAlpha") ){

				Double sdBetaFactor = (Double) person.getCustomAttributes().get("sdBetaFactor");

				double performingConst = 	params.marginalUtilityOfPerforming_s;

				/* Adjust alpha - value of time*/
				params.marginalUtilityOfPerforming_s =  params.marginalUtilityOfPerforming_s * incomeAlphaFactor;

				for (Entry<String, Mode> mode : params.modeParams.entrySet()) {
					mode.getValue().marginalUtilityOfTraveling_s = mode.getValue().marginalUtilityOfTraveling_s  * incomeAlphaFactor;
				}

				/* Adjust beta - schedule delay early (sdBeta = marginalUtilityOfWaiting - marginalUtilityOfPerforming*/
				/*relation beta/alpha  = 0.5 for beta_mean is also used by van den Berg and Verhoef (2011)*/
				params.marginalUtilityOfWaiting_s = params.marginalUtilityOfPerforming_s - performingConst * 0.5 * sdBetaFactor;

				/* Adjust gamma - schedule delay late*/
				/*relation gamma/beta  = 3.9 as in Arnott, de Palma, Lindsey (1990) and later used by van den Berg and Verhoef (2011)*/
				params.marginalUtilityOfLateArrival_s =  (params.marginalUtilityOfWaiting_s - params.marginalUtilityOfPerforming_s)*3.9;
				params.marginalUtilityOfEarlyDeparture_s =  (params.marginalUtilityOfWaiting_s - params.marginalUtilityOfPerforming_s)*3.9;


		}
		else if(simulationType.equals("heteroGamma") ){

			Double incomeGammaFactor = (Double) person.getCustomAttributes().get("incomeAlphaFactor");

			double performingConst = 	params.marginalUtilityOfPerforming_s;
			double waitingConst = 	params.marginalUtilityOfWaiting_s;
			/* Adjust alpha - value of time*/
			params.marginalUtilityOfPerforming_s =  params.marginalUtilityOfPerforming_s * incomeAlphaFactor;

			for (Entry<String, Mode> mode : params.modeParams.entrySet()) {
				mode.getValue().marginalUtilityOfTraveling_s = mode.getValue().marginalUtilityOfTraveling_s  * incomeAlphaFactor;
			}

			/* Adjust beta - schedule delay early*/
			params.marginalUtilityOfWaiting_s =  params.marginalUtilityOfWaiting_s * incomeAlphaFactor;

			/* Adjust gamma - schedule delay late*/
			params.marginalUtilityOfLateArrival_s =  (waitingConst - performingConst) * 3.9 * incomeGammaFactor;
			params.marginalUtilityOfEarlyDeparture_s =  (waitingConst- performingConst) * 3.9 * incomeGammaFactor;


			/*  OLD VERSION (before April 24, 2015)
				params.marginalUtilityOfPerforming_s =  params.marginalUtilityOfPerforming_s;
				
				params.marginalUtilityOfLateArrival_s =  params.marginalUtilityOfLateArrival_s * (incomeFactors.get(person.getId())/factorMean);
				params.marginalUtilityOfEarlyDeparture_s =  params.marginalUtilityOfEarlyDeparture_s * (incomeFactors.get(person.getId())/factorMean);

				params.marginalUtilityOfWaiting_s = params.marginalUtilityOfWaiting_s;
			*/
			}

		/*OLD - Appears to be unrealistic
		else if(simulationType.equals("heteroGammaProp") ){
			
			params.marginalUtilityOfLateArrival_s =  params.marginalUtilityOfLateArrival_s * (1.0/incomeFactors.get(person.getId())) / inverseFactorMean;
			params.marginalUtilityOfEarlyDeparture_s= params.marginalUtilityOfEarlyDeparture_s * (1.0/incomeFactors.get(person.getId())) / inverseFactorMean;

		}
		
		else if(simulationType.equals("heteroAlphaProp") ){
			
			double performingConst = 	params.marginalUtilityOfPerforming_s;
				
			params.marginalUtilityOfPerforming_s =  params.marginalUtilityOfPerforming_s * (1.0/incomeFactors.get(person.getId())) / inverseFactorMean;
			
			params.marginalUtilityOfLateArrival_s =  params.marginalUtilityOfLateArrival_s * betaFactors.get(person.getId());
			params.marginalUtilityOfEarlyDeparture_s =  params.marginalUtilityOfLateArrival_s * betaFactors.get(person.getId());
			
			params.marginalUtilityOfWaiting_s = params.marginalUtilityOfPerforming_s - betaFactors.get(person.getId()) * performingConst;
			
			for (Entry<String, Mode> mode : params.modeParams.entrySet()) {
				mode.getValue().marginalUtilityOfTraveling_s = mode.getValue().marginalUtilityOfTraveling_s  * (1.0/incomeFactors.get(person.getId())) / inverseFactorMean;
			}

		}*/
			

		}
		return params;
	}

	@Override
	public HashMap<Id, ScoringFunction> getPersonScoringFunctions() {
		return personScoringFunctions;
	}

	public void setSimulationType(String simulationType) {
		this.simulationType = simulationType;
	}

}
