package playground.balac.strc2016.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.roadpricing.RoadPricingConfigGroup;

import playground.balac.strc2016.scoring.STRC2016ScoringFunctionFactory;

public class STRC2016Controler {

	public static void main(String[] args) {

		final String configFile = args[ 0 ];
		final Config config = ConfigUtils.loadConfig(
				configFile,
				// this adds a new config group, used by the specific scoring function
				// we use
				new DestinationChoiceConfigGroup(), new RoadPricingConfigGroup());
		final Scenario sc = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler( sc );
        controler.setModules(new ControlerDefaultsWithRoadPricingModule());

		initializeLocationChoice( controler );

		STRC2016ScoringFunctionFactory strcSoringFactory = new STRC2016ScoringFunctionFactory(
				      sc);
		controler.setScoringFunctionFactory(strcSoringFactory); 
		controler.run();
		
	}
	
	private static void initializeLocationChoice( final MatsimServices controler ) {
		final Scenario scenario = controler.getScenario();
		final DestinationChoiceBestResponseContext lcContext =
			new DestinationChoiceBestResponseContext( scenario );
		lcContext.init();

		controler.addControlerListener(
				new DestinationChoiceInitializer(
					lcContext));
	}

}
