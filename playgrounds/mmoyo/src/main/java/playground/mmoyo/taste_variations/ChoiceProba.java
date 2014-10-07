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
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.analysis.tools.PtPlanAnalyzer;
import playground.mmoyo.analysis.tools.PtPlanAnalyzer.PtPlanAnalysisValues;
import playground.mmoyo.io.TextFileWriter;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.calibration.StopNumberPerPassenger;

//import java.util.Map.Entry;

/**
 * Reads two versions of same plans file: 
 * 		1) with matsimscores and choice 
 * 		2) cadyts utl correction and choice 
 * Calculates: prior choice probability, 
 * 				cadyts correction based probability, 
 * 				posterior choice probability for each plan.
 */
public class ChoiceProba {
	protected final double beta;
	private StopNumberPerPassenger stopNumberPerPassenger;
	private PtPlanAnalyzer ptPlanAnalyzer;
	final ExpBetaPlanSelector delegExpBetaPlanSelector;
	
	public ChoiceProba (final Network net, final TransitSchedule schedule, final TransitLine line){
		
		//set beta default value according to config
		Config config = new Config();
		config.addCoreModules();
		config.planCalcScore().setBrainExpBeta(1.0);// 1.0 should be the new default value
		this.beta = config.planCalcScore().getBrainExpBeta();

		//create ExpBetaPlanSelector. It will calculate the plan choice proba
		delegExpBetaPlanSelector = new ExpBetaPlanSelector(config.planCalcScore());
		
		//create pt analyzer to get trWalkTime, inVehTravTime, InVehDist, Transfers_num values after on
		ptPlanAnalyzer = new PtPlanAnalyzer(net, schedule);
		
		//create StopNumberPerPassenger object for analysis of stop per line 
		//stopNumberPerPassenger = new StopNumberPerPassenger(net, schedule, line); //only if one wants stop-use analysis
	
	}

	private void writeChoiceProbability (final Population popWithMatsimScores, final Population popWithCadytsCorrections, final String outputFile){
		//these are just variables for output
		final String NR= "\n";
		final String TB= "\t";
		final String s = "yes";
		final String empty = "";
		StringBuffer sBuff = new StringBuffer();
		
		sBuff.append("AGENT" + TB + "trWalkTime_s" + TB + "InVehTravTime_s" + TB + "tInVehDist_m" + TB + "transfers" + TB + "MATSIM SCORE"+	TB + "MATSim_PROBA" + TB + "MATSim_SELECTED" + TB +  "CADYTS SCORE" + TB + "CADYTS PROBA" + TB +  "CADYTS_SELECTED" + TB + "DIF_.25" + TB + "POST PROBAB" + TB + "M44_STOPS\n");
		
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

			//calculate how many stops of the given line used the person. //only if one wants stop-use analysis
			//stopNumberPerPassenger.get_persId_stopNumRecList_Map().clear();
			//stopNumberPerPassenger.run(cadytsPerson);
			//List<StopNumRecord> stopNumRecordList = stopNumberPerPassenger.get_persId_stopNumRecList_Map().entrySet().iterator().next().getValue();
					
			boolean diffCadytsProbabilities = false;
			StringBuffer sBuffPersonAnalysis = new StringBuffer();
			
			for (int i=0;i<matsimPerson.getPlans().size();i++){
				Plan matsimPlan = matsimPerson.getPlans().get(i);
				Plan cadytsPlan = cadytsPerson.getPlans().get(i);
				double matsimProbab = plan_ExpBetaSelProb_Map.get(matsimPlan);
				double cadytsProbab = plan_CadytsSelProb_Map.get(cadytsPlan);
				double postProbab = plan_postProbab_Map.get(i); 
				
				//pt analysis to find out trWalkTime, inVehTravTime, InVehDist, Transfers_num values
				PtPlanAnalysisValues v = ptPlanAnalyzer.run(matsimPlan);  //matsim and cadyts plans trip values should be the same. The only diffs. should be score and selected plan
				
				//only if one wants stop-use analysis
				//int stops = stopNumRecordList.get(i).getStopsNum();
				
				String matsimSelected = matsimPlan.isSelected()? s : empty ;
				String cadytsSelected = cadytsPlan.isSelected()? s : empty ;
				
				sBuffPersonAnalysis.append(i + TB + v.getTransitWalkTime_secs() + TB + v.trTravelTime_secs() + TB + v.getInVehDist_mts() + TB + v.getTransfers_num() + TB + matsimPlan.getScore() + TB + matsimProbab + TB + matsimSelected + TB + cadytsPlan.getScore() + TB + cadytsProbab + TB + cadytsSelected + TB + TB + postProbab + /* TB + matsimSelected + TB + cadytsSelected +*/  /* TB + stops +*/ NR);
			
				//find persons with proba very different from 0.25 for each plan
				diffCadytsProbabilities = (0.25 - Math.abs(cadytsProbab))> 0.02 || diffCadytsProbabilities ;
			}
	
			//store values in string buffer for output
			sBuff.append(matsimPerson.getId() + TB + TB + TB + TB + TB + TB + TB + TB + TB + TB +  TB + diffCadytsProbabilities + NR + sBuffPersonAnalysis);

		}
		
		//write
		new TextFileWriter().write(sBuff.toString(), outputFile, false);
	}
	
	class choiceProba{
		
	}
	
	
	/**
	 * Calculates selection probabilities with a ExpBetaPlanSelector instance
	 */
	protected Map<Plan, Double> getPlansSelectionProbabilities(final Person person) {
		Map<Plan, Double> plan_proba_Map = new LinkedHashMap<Plan, Double>(person.getPlans().size());
		for(Plan plan : person.getPlans()  ){
			double probability = ExpBetaPlanSelector.getSelectionProbability(delegExpBetaPlanSelector, plan.getPerson(), plan); /////// using a deleg ExpBetaPlanSelector
			plan_proba_Map.put(plan, probability);
		}
		return plan_proba_Map;
		/* ALL THIS CODE IS REPLACED BY DELEGATION (Using a ExpBetaPlanSelector) 
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
				planWeight = Math.exp(0.5 * this.beta * (currentScore -otherScore));  // 0.5 is not used in ExpBetaPlanSelector!!!     
				
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
		*/
	}
	
	public static void main(String[] args) {
		String strPlansWithMatsimScores ; // input sep routing almost the same 10x autom timeMutated. Differences: here are matsim scores and choice probab used  
		String strPlansWithCadytsCorrection;
		String outputFile ;
		String scheduleFile ;
		String netFile ;
		String strLineId ;
		
		if (args.length>0){
			strPlansWithMatsimScores = args[0];   
			strPlansWithCadytsCorrection =args[1];
			outputFile =args[2];
			scheduleFile =args[3];
			netFile =args[4];
			strLineId =args[5];			
		}else{
			strPlansWithMatsimScores = "../../input/choiceM44/500.plansMatsim.xml.gz";										//"../../input/newDemand/ptLinecountsScenario/OutputPlansfromCluster/500.plansMATSIM.xml.gz"; // input sep routing almost the same 10x autom timeMutated. Differences: here are matsim scores and choice probab used  
			strPlansWithCadytsCorrection = "../../input/choiceM44/poponlyM44.xml.gzWithUtilCorrections.xml.gz";  	//"../../input/newDemand/ptLinecountsScenario/OutputPlansfromCluster/500.plansCadyts.xml.gz";
			outputFile = "../../input/choiceM44/m44choiceProbabilities.xls"; //"../../input/newDemand/ptLinecountsScenario/choiceProbabilities.xls";
			scheduleFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
			netFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
			strLineId = "B-M44";
		}
		
		//load data
		DataLoader dataLoader = new DataLoader();
		Population popWithMatsimScores = dataLoader.readPopulation(strPlansWithMatsimScores);
		Population popWithCadytsCorrection = dataLoader.readPopulation(strPlansWithCadytsCorrection);
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario(); 
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scn);
		matsimNetReader.readFile(netFile);
		TransitSchedule schedule = dataLoader.readTransitSchedule(scheduleFile) ;
		TransitLine line = schedule.getTransitLines().get(Id.create(strLineId, TransitLine.class));
		
		//invoke the calculation of probabilities
		ChoiceProba choiceProba = new ChoiceProba(scn.getNetwork(), schedule, line);
		choiceProba.writeChoiceProbability(popWithMatsimScores, popWithCadytsCorrection, outputFile);
	}
}