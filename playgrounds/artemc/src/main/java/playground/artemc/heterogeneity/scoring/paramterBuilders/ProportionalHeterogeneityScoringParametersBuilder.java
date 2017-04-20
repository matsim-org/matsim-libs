package playground.artemc.heterogeneity.scoring.paramterBuilders;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.pt.PtConstants;

import java.util.Map;

/**
 * Created by artemc on 24/06/16.
 */
public class ProportionalHeterogeneityScoringParametersBuilder {

	private final Scenario scenario;

	public ProportionalHeterogeneityScoringParametersBuilder(Scenario scenario){
		this.scenario = scenario;
	}

	public ScoringParameters buildIncomeBasedScoringParameters(Person person){

		final ScoringParameters.Builder builder = new ScoringParameters.Builder(scenario, person.getId());

		final PlanCalcScoreConfigGroup.ScoringParameterSet scoringParameterSet = scenario.getConfig().planCalcScore().getScoringParameters((String) scenario.getPopulation().getPersonAttributes().getAttribute(person.toString(), scenario.getConfig().plans().getSubpopulationAttributeName()));

		double marginalUtilityOfWaiting_s = scoringParameterSet.getMarginalUtlOfWaiting_utils_hr() / 3600.0;
		double marginalUtilityOfLateArrival_s = scoringParameterSet.getLateArrival_utils_hr() / 3600.0;
		double marginalUtilityOfEarlyDeparture_s = scoringParameterSet.getEarlyDeparture_utils_hr() / 3600.0;
		double marginalUtilityOfWaitingPt_s = scoringParameterSet.getMarginalUtlOfWaitingPt_utils_hr() / 3600.0 ;
		double marginalUtilityOfPerforming_s = scoringParameterSet.getPerforming_utils_hr() / 3600.0;


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


		PlanCalcScoreConfigGroup.ActivityParams transitActivityParams = new PlanCalcScoreConfigGroup.ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		transitActivityParams.setOpeningTime(0.) ;
		transitActivityParams.setClosingTime(0.) ;
		ActivityUtilityParameters.Builder modeParamsBuilder = new ActivityUtilityParameters.Builder(transitActivityParams);
		modeParamsBuilder.setScoreAtAll(false);
		builder.setActivityParameters(PtConstants.TRANSIT_ACTIVITY_TYPE, modeParamsBuilder);

		final ScoringParameters individualParameters = builder.build();
		return individualParameters;
	}
}
