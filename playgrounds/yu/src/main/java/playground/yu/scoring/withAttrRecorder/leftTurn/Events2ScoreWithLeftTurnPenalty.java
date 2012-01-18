package playground.yu.scoring.withAttrRecorder.leftTurn;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.Tuple;

import playground.yu.scoring.withAttrRecorder.Events2Score4AttrRecorder;

public class Events2ScoreWithLeftTurnPenalty extends Events2Score4AttrRecorder {

	public Events2ScoreWithLeftTurnPenalty(Config config,
			ScoringFunctionFactory sfFactory, Scenario scenario) {
		super(config, sfFactory, scenario);
		attrNameList.add("constantLeftTurn");
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
			ScoringFunctionAccumulatorWithLeftTurnPenalty sfa = (ScoringFunctionAccumulatorWithLeftTurnPenalty) sf;

			// leftTurn
			attrs.put("constantLeftTurn", sfa.getNbOfLeftTurnAttrCar());
		}
	}
}
