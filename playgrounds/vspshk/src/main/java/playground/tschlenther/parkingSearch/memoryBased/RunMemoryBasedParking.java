package playground.tschlenther.parkingSearch.memoryBased;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.scenario.ScenarioUtils;

import playground.tschlenther.parkingSearch.Benenson.BenensonParkingAgentFactory;
import playground.tschlenther.parkingSearch.install.TSSetupParking;

public class RunMemoryBasedParking {
	private static final String inputDir = "C:/Users/Work/Bachelor Arbeit/input/GridNet/";

	public static void main(String[] args) {
		RunMemoryBasedParking.run(false);
	}

	/**
	 * 
	 * @param otfvis
	 *            turns otfvis visualisation on or off
	 */
	public static void run(boolean otfvis) {
		Config config = ConfigUtils.loadConfig(inputDir + "config.xml", new DvrpConfigGroup());
		config.plans().setInputFile(inputDir + "population10_neuesVideo.xml");
		config.facilities().setInputFile(inputDir + "parkingFacilities_full_workNames.xml");
		config.controler().setOutputDirectory("C:/Users/Work/Bachelor Arbeit/RUNS/MemoryBased/FirstTest/");
		config.network().setInputFile(inputDir + "grid_network_length200.xml");
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
