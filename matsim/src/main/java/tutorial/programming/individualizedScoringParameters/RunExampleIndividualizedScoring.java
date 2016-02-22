package tutorial.programming.individualizedScoringParameters;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

/**
 * @author thibautd
 */
public class RunExampleIndividualizedScoring {
	public static void main(String... args) {
		final String configFile = "examples/equil/config.xml";
		final Config config = ConfigUtils.loadConfig( configFile );
		config.controler().setOutputDirectory( "output/exampleIndividualScores/");

		final Controler controler = new Controler( config );
		controler.addOverridingModule( new ExampleIndividualizedScoringFunctionModule() );
		controler.run();
	}
}
