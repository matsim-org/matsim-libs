/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mmoyo.taste_variations;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;

import playground.mmoyo.io.TextFileWriter;
import playground.mmoyo.utils.DataLoader;

/**
 * Calculates: prior choice probability, 
 * 				cadyts correction based probability, 
 * 				posterior choice probability for each plan.
 */
public class ChoiceProba {
	protected final double beta;
	
	public ChoiceProba (){
		//set beta default value according to config
		Config config = new Config();
		config.addCoreModules();
		this.beta = config.planCalcScore().getBrainExpBeta();
	}

	private void writeChoiceProbability (final Population popWithMatsimScores, final Population popWithCadytsCorrections, final String outputFile){
		//these are just variables for output
		final String NR= "\n";
		final String TB= "\t";
		StringBuffer sBuff = new StringBuffer();
		sBuff.append("AGENT" + TB + "MATSIM SCORE"+	TB + "PROBA" + TB + "CADYTS SCORE" + TB + "PROBA" +  TB + "POST PROBAB\n");

		for (Person matsimPerson: popWithMatsimScores.getPersons().values()){
			//get Matsims-ExpBeta selection probabilities (a priory)
			Map<Plan, Double> plan_ExpBetaSelProb_Map = this.getPlansSelectionProbabilities(matsimPerson);
			
			//get Cadyts' corrections selection probability (conditional)
			Person cadytsPerson = popWithCadytsCorrections.getPersons().get(matsimPerson.getId());
			Map<Plan, Double> plan_CadytsSelProb_Map = this.getPlansSelectionProbabilities(cadytsPerson);
		
			//get sum of ExpBetaSelProb * CadytsSelProb   (joint probability)
			double sumMultip_MatsimProb_Cadytsprob= 0.0;
			Map<Integer, Double> plan_multip_MatsimProb_Cadytsprob_Map = new TreeMap <Integer, Double>();
			for (int i=0;i<matsimPerson.getPlans().size();i++){
				Plan matsimPlan = matsimPerson.getPlans().get(i);
				Plan cadytsPlan = cadytsPerson.getPlans().get(i);
				double multip_MatsimProb_Cadytsprob = plan_ExpBetaSelProb_Map.get(matsimPlan) * plan_CadytsSelProb_Map.get(cadytsPlan);
				plan_multip_MatsimProb_Cadytsprob_Map.put(i, multip_MatsimProb_Cadytsprob);
				sumMultip_MatsimProb_Cadytsprob += multip_MatsimProb_Cadytsprob;
			}
			
			//calculate posterior probability per plan
			Map<Integer, Double> plan_postProbab_Map = new TreeMap <Integer, Double>();
			for (int i=0;i<matsimPerson.getPlans().size();i++){
				double multip_MatsimProb_Cadytsprob = plan_multip_MatsimProb_Cadytsprob_Map.get(i); 
				double postProbab = multip_MatsimProb_Cadytsprob / sumMultip_MatsimProb_Cadytsprob;  
				plan_postProbab_Map.put(i, postProbab);
			}
						
			//store values in string buffer for output
			sBuff.append(matsimPerson.getId() + NR);
			for (int i=0;i<matsimPerson.getPlans().size();i++){
				Plan matsimPlan = matsimPerson.getPlans().get(i);
				Plan cadytsPlan = cadytsPerson.getPlans().get(i);
				double matsimProbab = plan_ExpBetaSelProb_Map.get(matsimPlan);
				double cadytsProbab = plan_CadytsSelProb_Map.get(cadytsPlan);
				double postProbab = plan_postProbab_Map.get(i); 
				sBuff.append(TB + matsimPlan.getScore() + TB + matsimProbab + TB + cadytsPlan.getScore() + TB + cadytsProbab + TB + postProbab + NR);
			}

		}
		
		//write
		new TextFileWriter().write(sBuff.toString(), outputFile, false);
	}
	
	/**
	 * Calculates selection probabilities according to ExpBetaPlanChanger and code
	 */
	protected Map<Plan, Double> getPlansSelectionProbabilities(final Person person) {

		// - first find the max. score of all plans of this person
		//code of method calcExpBetaPlanWeights is used here, manuel
		double maxScore = Double.NEGATIVE_INFINITY;
		for (Plan plan1 : person.getPlans()) {
			if ((plan1.getScore() != null) && (plan1.getScore().doubleValue() > maxScore)) {
				maxScore = plan1.getScore().doubleValue();
			}
		}

		// calculate weights for each plan
		Map<Plan, Double> weights = new LinkedHashMap<Plan, Double>(person.getPlans().size());
		double sumWeights = 0.0;
		for (Plan plan : person.getPlans()) {
			double planWeight = Double.NaN;
			if (plan.getScore() != null) {
				
				//////////calculate plan weight///////////////
				//from ExpBetaPlanChanger code and NewPtBsePlanChanger.selectPlan. 
				double currentScore = plan.getScore();
				double otherScore = maxScore;  //or other random plan??? ((PersonImpl) person).getRandomPlan().getScore()
				planWeight = Math.exp(0.5 * this.beta * (otherScore - currentScore));     
				//or other option? from ExpBetaPlanSelector.calcPlanWeight code
				//planWeight = Math.exp(this.beta * (plan.getScore() - maxScore));
				
				//set min and maximal possible values for planWeight
				planWeight= planWeight<Double.MIN_VALUE?Double.MIN_VALUE:planWeight;
				planWeight= planWeight>Double.MAX_VALUE?Double.MAX_VALUE:planWeight;

				//System.out.println("currentScore:" + currentScore + " otherScore: " + otherScore +  " otherScore-currentScore:" + (otherScore - currentScore) +  " planWeight:" + planWeight + " " + (planWeight > Double.MAX_VALUE)) ;
			}
			weights.put(plan, planWeight);
			sumWeights += planWeight;
		}
		//set sumWeights min and maximal possible values
		sumWeights= sumWeights<Double.MIN_VALUE?Double.MIN_VALUE:sumWeights;
		sumWeights= sumWeights>Double.MAX_VALUE?Double.MAX_VALUE:sumWeights;
		
		//To calculate probability, code of method "ExpBetaPlanSelector.getSelectionProbability" is used here
		Map<Plan, Double> plan_proba_Map = new LinkedHashMap<Plan, Double>(person.getPlans().size());
		for(Entry<Plan, Double> entry: weights.entrySet() ){
			Plan plan = entry.getKey(); 
			double weight = entry.getValue();
			double probability =  weight/sumWeights;
			plan_proba_Map.put(plan, probability);
		}

		return plan_proba_Map;
	}

	public static void main(String[] args) {
		String strPlansWithMatsimScores = "../../input/sep/tasteRouting/500.plans.xml";
		String strPlansWithCadytsCorrection = "../../runs_manuel/CalibLineM44/automCalib10xTimeMutated/10xrun/it.500/500.plans.xml";
		String outputFile = "../../input/sep/tasteRouting/choiceProbabilities.xls";
		
		DataLoader dloader = new DataLoader();
		Population popWithMatsimScores = dloader.readPopulation(strPlansWithMatsimScores);
		Population popWithCadytsCorrection = dloader.readPopulation(strPlansWithCadytsCorrection);
		new ChoiceProba().writeChoiceProbability(popWithMatsimScores, popWithCadytsCorrection, outputFile);
	}
}