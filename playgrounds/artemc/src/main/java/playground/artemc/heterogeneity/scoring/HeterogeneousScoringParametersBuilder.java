package playground.artemc.heterogeneity.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.pt.PtConstants;
import playground.artemc.heterogeneity.scoring.functions.PersonalScoringParameters;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by artemc on 24/06/16.
 */
public class HeterogeneousScoringParametersBuilder {

	private final Scenario scenario;

	public HeterogeneousScoringParametersBuilder(Scenario scenario){
		this.scenario = scenario;
	}

	public CharyparNagelScoringParameters buildIncomeBasedScoringParameters(String simulationType, Person person){

		final CharyparNagelScoringParameters.Builder builder = new CharyparNagelScoringParameters.Builder(scenario, person.getId());

		final PlanCalcScoreConfigGroup.ScoringParameterSet scoringParameterSet = scenario.getConfig().planCalcScore().getScoringParameters((String) scenario.getPopulation().getPersonAttributes().getAttribute(person.toString(), scenario.getConfig().plans().getSubpopulationAttributeName()));

		double marginalUtilityOfWaiting_s = scoringParameterSet.getMarginalUtlOfWaiting_utils_hr() / 3600.0;
		double marginalUtilityOfLateArrival_s = scoringParameterSet.getLateArrival_utils_hr() / 3600.0;
		double marginalUtilityOfEarlyDeparture_s = scoringParameterSet.getEarlyDeparture_utils_hr() / 3600.0;
		double marginalUtilityOfWaitingPt_s = scoringParameterSet.getMarginalUtlOfWaitingPt_utils_hr() / 3600.0 ;
		double marginalUtilityOfPerforming_s = scoringParameterSet.getPerforming_utils_hr() / 3600.0;

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

				builder.setMarginalUtilityOfPerforming_s(marginalUtilityOfPerforming_s * incomeAlphaFactor);

				Map<String, PlanCalcScoreConfigGroup.ModeParams> modes = scoringParameterSet.getModes();
				for (Map.Entry<String, PlanCalcScoreConfigGroup.ModeParams> mode : modes.entrySet()) {
					String modeName = mode.getKey();
					Double marginalUtilityOfTraveling_s  = mode.getValue().getMarginalUtilityOfTraveling() / 3600.;

					builder.getModeParameters(modeName).setMarginalUtilityOfTraveling_s(marginalUtilityOfTraveling_s * incomeAlphaFactor);

				}

				/* Adjust beta - schedule delay early*/
				builder.setMarginalUtilityOfWaiting_s(marginalUtilityOfWaiting_s * incomeAlphaFactor);

				/* Adjust gamma - schedule delay late*/
				builder.setMarginalUtilityOfLateArrival_s(marginalUtilityOfLateArrival_s * incomeAlphaFactor);
				builder.setMarginalUtilityOfEarlyDeparture_s(marginalUtilityOfEarlyDeparture_s * incomeAlphaFactor);

			}
	/*		else if(simulationType.equals("heteroAlpha") ){

				Double betaFactor = (Double) person.getCustomAttributes().get("betaFactor");

				double performingConst = 	params.marginalUtilityOfPerforming_s;

				*//* Adjust alpha - value of time*//*
				params.marginalUtilityOfPerforming_s =  params.marginalUtilityOfPerforming_s * incomeAlphaFactor;

				for (Map.Entry<String, PersonalScoringParameters.Mode> mode : params.modeParams.entrySet()) {
					mode.getValue().marginalUtilityOfTraveling_s = mode.getValue().marginalUtilityOfTraveling_s  * incomeAlphaFactor;
				}

				*//* Adjust beta - schedule delay early (sdBeta = marginalUtilityOfWaiting - marginalUtilityOfPerforming*//*
				*//*relation beta/alpha  = 0.5 for beta_mean is also used by van den Berg and Verhoef (2011)*//*
				//params.marginalUtilityOfWaiting_s = params.marginalUtilityOfPerforming_s - performingConst * betaFactor;

				// new beta factor transforms distribution from 0.4 - 1.6 with mean=1 and sd=0.3 to 1 - 3 with mean=2, sd=1/2
				//double newbetaFactor = (betaFactor + 0.2 ) * (10.0 / 6.0);
				//params.marginalUtilityOfWaiting_s = params.marginalUtilityOfPerforming_s - params.marginalUtilityOfPerforming_s * 2  / newbetaFactor;

				// new beta factor transforms distribution from 0.4 - 1.6 with mean=1 and sd=0.3 to a standard normal distribution

				//double std = 0.383304098205198;
				double std = 0.25;
				double mean = Math.log(1) - (std * std) / 2;
				double lnBetaFactor = Math.exp(mean + std * betaFactor);

				params.marginalUtilityOfWaiting_s = params.marginalUtilityOfPerforming_s - params.marginalUtilityOfPerforming_s * lnBetaFactor;

				*//* Adjust gamma - schedule delay late*//*
				*//*relation gamma/beta  = 3.9 as in Arnott, de Palma, Lindsey (1990) and later used by van den Berg and Verhoef (2011)*//*
				params.marginalUtilityOfLateArrival_s =  (params.marginalUtilityOfWaiting_s - params.marginalUtilityOfPerforming_s)*3.9;
				params.marginalUtilityOfEarlyDeparture_s =  (params.marginalUtilityOfWaiting_s - params.marginalUtilityOfPerforming_s)*3.9;


			}
			else if(simulationType.equals("heteroAlphaRatio") ){

				Double betaFactor = (Double) person.getCustomAttributes().get("betaFactor");

				double performingConst = 	params.marginalUtilityOfPerforming_s;

				*//* Adjust alpha - value of time*//*
				params.marginalUtilityOfPerforming_s =  params.marginalUtilityOfPerforming_s * incomeAlphaFactor;

				for (Map.Entry<String, PersonalScoringParameters.Mode> mode : params.modeParams.entrySet()) {
					mode.getValue().marginalUtilityOfTraveling_s = mode.getValue().marginalUtilityOfTraveling_s  * incomeAlphaFactor;
				}

			*//* Adjust beta - schedule delay early (sdBeta = marginalUtilityOfWaiting - marginalUtilityOfPerforming*//*
			*//*relation beta/alpha  = 0.5 for beta_mean is also used by van den Berg and Verhoef (2011)*//*
				//params.marginalUtilityOfWaiting_s = params.marginalUtilityOfPerforming_s - performingConst * betaFactor;

				// new beta factor transforms distribution from 0.4 - 1.6 with mean=1 and sd=0.3 to 1 - 3 with mean=2, sd=1/2
				//double newbetaFactor = (betaFactor + 0.2 ) * (10.0 / 6.0);
				//params.marginalUtilityOfWaiting_s = params.marginalUtilityOfPerforming_s - params.marginalUtilityOfPerforming_s * 2  / newbetaFactor;

				// new beta factor transforms distribution from 0.4 - 1.6 with mean=1 and sd=0.3 to a standard normal distribution

				//double std = 0.383304098205198;
				double std = 0.25;
				double mean = Math.log(1) - (std * std) / 2;
				double lnBetaFactor = Math.exp(mean + std * betaFactor);

				double inverseMean = 2.132033;

				params.marginalUtilityOfWaiting_s = params.marginalUtilityOfPerforming_s - params.marginalUtilityOfPerforming_s * lnBetaFactor * (inverseMean / 2.0) ;

			*//* Adjust gamma - schedule delay late*//*
			*//*relation gamma/beta  = 3.9 as in Arnott, de Palma, Lindsey (1990) and later used by van den Berg and Verhoef (2011)*//*
				params.marginalUtilityOfLateArrival_s =  (params.marginalUtilityOfWaiting_s - params.marginalUtilityOfPerforming_s)*3.9;
				params.marginalUtilityOfEarlyDeparture_s =  (params.marginalUtilityOfWaiting_s - params.marginalUtilityOfPerforming_s)*3.9;


			}
			else if(simulationType.equals("heteroGamma") ){

				Double incomeGammaFactor = (Double) person.getCustomAttributes().get("incomeGammaFactor");

				double performingConst = 	params.marginalUtilityOfPerforming_s;
				double waitingConst = 	params.marginalUtilityOfWaiting_s;
			*//* Adjust alpha - value of time*//*
				params.marginalUtilityOfPerforming_s =  params.marginalUtilityOfPerforming_s * incomeAlphaFactor;

				for (Map.Entry<String, PersonalScoringParameters.Mode> mode : params.modeParams.entrySet()) {
					mode.getValue().marginalUtilityOfTraveling_s = mode.getValue().marginalUtilityOfTraveling_s  * incomeAlphaFactor;
				}

			*//* Adjust beta - schedule delay early*//*
				params.marginalUtilityOfWaiting_s =  params.marginalUtilityOfWaiting_s * incomeAlphaFactor;

			*//* Adjust gamma - schedule delay late*//*
				// gamma factor transforms distribution from 0.4 - 1.6 with mean=1 and sd=0.3 to 1 - 6.8 with mean=3.9, sd=1.45
				Double betaFactor = (Double) person.getCustomAttributes().get("betaFactor");
				//double gammaFactor = (betaFactor - 28.0/145.0) * 29.0/6.0;
				double gammaFactor = betaFactor * 0.975 + 3.9;

				params.marginalUtilityOfLateArrival_s =  (params.marginalUtilityOfWaiting_s - params.marginalUtilityOfPerforming_s)  * gammaFactor;
				params.marginalUtilityOfEarlyDeparture_s =  (params.marginalUtilityOfWaiting_s - params.marginalUtilityOfPerforming_s) * gammaFactor;

//			params.marginalUtilityOfLateArrival_s =  (waitingConst - performingConst) * 3.9 * incomeGammaFactor;
//			params.marginalUtilityOfEarlyDeparture_s =  (waitingConst- performingConst) * 3.9 * incomeGammaFactor;


			*//*  OLD VERSION (before April 24, 2015)
				params.marginalUtilityOfPerforming_s =  params.marginalUtilityOfPerforming_s;

				params.marginalUtilityOfLateArrival_s =  params.marginalUtilityOfLateArrival_s * (incomeFactors.get(person.getId())/factorMean);
				params.marginalUtilityOfEarlyDeparture_s =  params.marginalUtilityOfEarlyDeparture_s * (incomeFactors.get(person.getId())/factorMean);

				params.marginalUtilityOfWaiting_s = params.marginalUtilityOfWaiting_s;
			*//*
			}

			else if(simulationType.equals("heteroPropSymmetric") ){

				Double votDeviation = (Double) person.getCustomAttributes().get("votDeviation");

				Double factor = (params.marginalUtilityOfPerforming_s * 2  + votDeviation * params.marginalUtilityOfMoney / 3600.0) / (params.marginalUtilityOfPerforming_s * 2) ;

				params.marginalUtilityOfPerforming_s =  params.marginalUtilityOfPerforming_s  * factor;

				for (Map.Entry<String, PersonalScoringParameters.Mode> mode : params.modeParams.entrySet()) {
					mode.getValue().marginalUtilityOfTraveling_s = mode.getValue().marginalUtilityOfTraveling_s  * factor;
				}

			*//* Adjust beta - schedule delay early*//*
				params.marginalUtilityOfWaiting_s =  params.marginalUtilityOfWaiting_s * factor;

			*//* Adjust gamma - schedule delay late*//*
				params.marginalUtilityOfLateArrival_s =  params.marginalUtilityOfLateArrival_s * factor;
				params.marginalUtilityOfEarlyDeparture_s= params.marginalUtilityOfEarlyDeparture_s * factor;
			}

			else if(simulationType.equals("heteroAlphaOnly") ){

				Double betaFactor = (Double) person.getCustomAttributes().get("betaFactor");

				double std = 0.25;
				double mean = Math.log(1) - (std * std) / 2;
				double lnBetaFactor = Math.exp(mean + std * betaFactor);

				params.marginalUtilityOfWaiting_s = params.marginalUtilityOfPerforming_s - params.marginalUtilityOfPerforming_s * lnBetaFactor;
			*//* Adjust gamma - schedule delay late*//*
			*//*relation gamma/beta  = 3.9 as in Arnott, de Palma, Lindsey (1990) and later used by van den Berg and Verhoef (2011)*//*
				params.marginalUtilityOfLateArrival_s =  (params.marginalUtilityOfWaiting_s - params.marginalUtilityOfPerforming_s)*3.9;
				params.marginalUtilityOfEarlyDeparture_s =  (params.marginalUtilityOfWaiting_s - params.marginalUtilityOfPerforming_s)*3.9;

			}else if(simulationType.equals("heteroGammaOnly") ) {

			*//* Adjust gamma - schedule delay late*//*

				Double betaFactor = (Double) person.getCustomAttributes().get("betaFactor");
				double gammaFactor = betaFactor * 0.975 + 3.9;

				params.marginalUtilityOfLateArrival_s = (params.marginalUtilityOfWaiting_s - params.marginalUtilityOfPerforming_s) * gammaFactor;
				params.marginalUtilityOfEarlyDeparture_s = (params.marginalUtilityOfWaiting_s - params.marginalUtilityOfPerforming_s) * gammaFactor;
			}

		*//*OLD - Appears to be unrealistic
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

		}*//*
*/

		}


		PlanCalcScoreConfigGroup.ActivityParams transitActivityParams = new PlanCalcScoreConfigGroup.ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		transitActivityParams.setOpeningTime(0.) ;
		transitActivityParams.setClosingTime(0.) ;
		ActivityUtilityParameters.Builder modeParamsBuilder = new ActivityUtilityParameters.Builder(transitActivityParams);
		modeParamsBuilder.setScoreAtAll(false);
		builder.setActivityParameters(PtConstants.TRANSIT_ACTIVITY_TYPE, modeParamsBuilder);

		final CharyparNagelScoringParameters individualParameters = builder.build();
		return individualParameters;
	}

}
