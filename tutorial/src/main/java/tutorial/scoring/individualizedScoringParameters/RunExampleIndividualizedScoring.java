package tutorial.scoring.individualizedScoringParameters;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

/**
 * @author thibautd
 */
public class RunExampleIndividualizedScoring {
	public static void main(String... args) {
		final Config config = ConfigUtils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.controler().setOutputDirectory( "output/exampleIndividualScores/");

		final Controler controler = new Controler( config );
		controler.addOverridingModule( new ExampleIndividualizedScoringFunctionModule() );
		controler.run();
	}
}
