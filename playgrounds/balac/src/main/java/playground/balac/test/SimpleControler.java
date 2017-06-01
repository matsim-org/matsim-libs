package playground.balac.test;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParameters;


public class SimpleControler {

	public static void main(String[] args) {
		final String configFile = args[ 0 ];
		
		final Config config = ConfigUtils.loadConfig(
				configFile,
				new BJActivityScoringConfigGroup());
		
		Controler controler = new Controler(config);		

		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				SumScoringFunction sumScoringFunction = new SumScoringFunction();

				
				double slopeHome1 = ((BJActivityScoringConfigGroup)controler.getConfig().getModule("BJactivityscoring")).getSlopeHome1();
				double slopeHome2 = ((BJActivityScoringConfigGroup)controler.getConfig().getModule("BJactivityscoring")).getSlopeHome2();
				
				Map<String, Double> slopes = new HashMap<>();
				slopes.put("home_1", slopeHome1);
				slopes.put("home_2", slopeHome2);
				slopes.put("work", 0.0);
				slopes.put("secondary", 0.0);
				// Score activities, legs, payments and being stuck
				// with the default MATSim scoring based on utility parameters in the config file.
				final ScoringParameters params =
						new ScoringParameters.Builder(controler.getScenario(), person.getId()).build();
				sumScoringFunction.addScoringFunction(new BJActivityScoring(params, slopes));
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				return sumScoringFunction;
			}

		});
		
		
		controler.run();
	}

}
