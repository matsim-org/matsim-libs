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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.artemc.analysis.AnalysisControlerListener;
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
	private PersonalScoringParameters params = null;
	private HashMap<Id, ScoringFunction> personScoringFunctions;
	private HashMap<Id<Person>, Double> incomeFactors = null;
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
		if (this.params == null) {
			/* lazy initialization of params. not strictly thread safe, as different threads could
			 * end up with different params-object, although all objects will have the same
			 * values in them due to using the same config. Still much better from a memory performance
			 * point of view than giving each ScoringFunction its own copy of the params.
			 */
			this.params = new PersonalScoringParameters(this.config);

			if(this.simulationType.equals("hetero") || this.simulationType.equals("homo")){
				if(incomeFactors!=null){
					
					/*Calculate the mean in order to adjust the utility parameters*/
					Double factorSum=0.0;
					Double factorMean = 0.0;
					for(Double incomeFactor:this.incomeFactors.values()){
						factorSum = factorSum + incomeFactor;
					}
					factorMean = factorSum / (double) incomeFactors.size();
					
					
					this.params.marginalUtilityOfPerforming_s = this.params.marginalUtilityOfPerforming_s * (1.0/(incomeFactors.get(person.getId())/ factorMean));
					
					this.params.marginalUtilityOfLateArrival_s =  this.params.marginalUtilityOfLateArrival_s * this.params.marginalUtilityOfPerforming_s;
					this.params.marginalUtilityOfEarlyDeparture_s= this.params.marginalUtilityOfEarlyDeparture_s * this.params.marginalUtilityOfPerforming_s;

					for (Entry<String, Mode> mode : this.params.modeParams.entrySet()) {
						mode.getValue().marginalUtilityOfTraveling_s = mode.getValue().marginalUtilityOfTraveling_s  * (1.0/(incomeFactors.get(person.getId()) / factorMean));
					}
				}
			}
			else if(this.simulationType.equals("heteroAlpha") ){
				this.params.marginalUtilityOfLateArrival_s = this.params.marginalUtilityOfPerforming_s;
				this.params.marginalUtilityOfEarlyDeparture_s= 0.0;
				
			}
		}

		DisaggregatedSumScoringFunction sumScoringFunction = new DisaggregatedSumScoringFunction();
		sumScoringFunction.setParams(this.params);
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(this.params));		
		for(String mode:config.getModes().keySet()){
			sumScoringFunction.addLegScoringFunction(mode, new CharyparNagelLegScoring(this.params, this.network));
		}
		sumScoringFunction.addLegScoringFunction("transit_walk", new CharyparNagelLegScoring(this.params, this.network));

		//sumScoringFunction.addLegScoringFunction("car", new CharyparNagelLegScoring(this.params, this.network));
		//sumScoringFunction.addLegScoringFunction("pt", new CharyparNagelLegScoring(this.params, this.network));


		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(this.params));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(this.params));


		//		ScoringFunctionAccumulator sumScoringFunction = new ScoringFunctionAccumulator();
		//		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(this.params));
		//		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(this.params, this.network));
		//		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(this.params));
		//		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(this.params));


		personScoringFunctions.put(person.getId(), sumScoringFunction);

		return sumScoringFunction;
	}

	public HashMap<Id, ScoringFunction> getPersonScoringFunctions() {
		return personScoringFunctions;
	}

}
