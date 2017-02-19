/**
 * 
 */
package playground.tschlenther.parkingSearch.Benenson;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.parking.parkingsearch.RunParkingSearchExample;
import org.matsim.contrib.parking.parkingsearch.sim.SetupParking;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import playground.tschlenther.parkingSearch.install.TSSetupParking;

/**
 * @author schlenther
 *
 */
public class RunBenensonParking {

	private static final String inputDir = "C:/Users/Work/Bachelor Arbeit/input/";
	
	public static void main(String[] args) {
		// set to false, if you don't require visualisation, the the example will run for 10 iterations
		new RunBenensonParking().run(false);

	}

	/**
	 * 
	 * @param otfvis
	 *            turns otfvis visualisation on or off
	 */
	public void run(boolean otfvis) {
		Config config = ConfigUtils.loadConfig(inputDir + "config.xml");
		config.plans().setInputFile(inputDir + "population10_verteilteActivities_workNames_1bis4.xml");
		config.facilities().setInputFile(inputDir + "parkingFacilities_full_workNames.xml");
		config.controler().setOutputDirectory("C:/Users/Work/Bachelor Arbeit/RUNS/BenensonParking/ZwischenPräsi/KontrolleVonVideo1/");
		config.network().setInputFile("C:/Users/Work/Bachelor Arbeit/input/grid_network_length200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		if (otfvis) {
			config.controler().setLastIteration(0);
		} else {
			config.controler().setLastIteration(0);
		}
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		TSSetupParking.installParkingModules(controler);
		
		controler.run();
	}
	
}
