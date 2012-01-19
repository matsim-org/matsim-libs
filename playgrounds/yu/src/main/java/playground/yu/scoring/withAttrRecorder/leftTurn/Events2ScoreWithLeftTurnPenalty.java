package playground.yu.scoring.withAttrRecorder.leftTurn;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.Tuple;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.generalNormal.scoring.Events2Score4PC;

public class Events2ScoreWithLeftTurnPenalty extends Events2Score4PC {

	public Events2ScoreWithLeftTurnPenalty(Config config,
			ScoringFunctionFactory sfFactory, Scenario scenario) {
		super(config, sfFactory, scenario);
		attrNameList.add("constantLeftTurn");
		addLeftTurnCoeffToMNL();
	}

	private void addLeftTurnCoeffToMNL() {
		// turn left
		int attrNameIndex = attrNameList.indexOf("constantLeftTurn");
		getMultinomialLogit()
				.setCoefficient(
						attrNameIndex,
						((CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty) sfFactory)
								.getAdditionalParams().constantLeftTurn);
	}

	@Override
	public void finish() {
		for (Tuple<Plan, ScoringFunction> plansScorFunction : agentScorers
				.values()) {

			Plan plan = plansScorFunction.getFirst();
			Map<String, Object> attrs = plan.getCustomAttributes();

			ScoringFunction sf = plansScorFunction.getSecond();
			// sf.finish();//TEST hier at first do NOT do this
			// **********************codes from {@code EventsToScore}
			// save attributes as custom attritubes.
			// #########################################
			if (sf instanceof ScoringFunctionAccumulatorWithLeftTurnPenalty) {
				// leftTurn
				attrs.put("constantLeftTurn",
						((ScoringFunctionAccumulatorWithLeftTurnPenalty) sf)
								.getNbOfLeftTurnAttrCar());
			}
		}
	}
}
