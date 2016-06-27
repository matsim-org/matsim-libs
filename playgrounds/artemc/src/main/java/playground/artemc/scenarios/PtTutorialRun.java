package playground.artemc.scenarios;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

/**
 * Created by artemc on 24/06/16.
 */
public class PtTutorialRun {

	private static String output;

	public static void main(String[] args) {

		output = args[0];

		Config config = ConfigUtils.loadConfig("matsim/src/test/resources/test/scenarios/pt-tutorial/config.xml");
		config.planCalcScore().setWriteExperiencedPlans(true);
		config.controler().setLastIteration(2);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.plans().setInputFile("matsim/src/test/resources/test/scenarios/pt-tutorial/population2.xml");
		config.network().setInputFile("matsim/src/test/resources/test/scenarios/pt-tutorial/multimodalnetwork.xml");
		config.transit().setVehiclesFile("matsim/src/test/resources/test/scenarios/pt-tutorial/transitVehicles.xml");
		config.transit().setTransitScheduleFile("matsim/src/test/resources/test/scenarios/pt-tutorial/transitschedule.xml");
		config.controler().setOutputDirectory(output);
		Controler controler = new Controler(config);
		controler.run();
	}

}
