/**
 * 
 */
package playground.tschlenther.parkingSearch.Benenson;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.parking.parkingsearch.RunParkingSearchExample;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.sim.SetupParking;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Binder;

import playground.tschlenther.parkingSearch.analysis.ZoneParkingOccupationListener;
import playground.tschlenther.parkingSearch.install.TSSetupParking;
import playground.tschlenther.parkingSearch.memoryBased.MemoryBasedParkingAgentFactory;
import playground.tschlenther.parkingSearch.utils.ZoneParkingManager;

/**
 * @author schlenther
 *
 */
public class RunBenensonParking {

	private static final String inputDir = "C:/Users/Work/Bachelor Arbeit/input/GridNet/";
	
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
		
		Config config = ConfigUtils.loadConfig(inputDir + "config.xml", new DvrpConfigGroup());

		//		config.plans().setInputFile(inputDir + "untersuchungsraum-plans.xml.gz");
		config.plans().setInputFile(inputDir + "population10.xml");

		//		config.facilities().setInputFile(inputDir + "Berlin-facilities-untersuchungsraum-full.xml");
		config.facilities().setInputFile(inputDir + "parkingFacilities_full_workNames.xml");
		
//		config.network().setInputFile(inputDir + "network-car.xml.gz");
		config.network().setInputFile(inputDir + "grid_network_length200.xml");
		
		config.controler().setOutputDirectory("C:/Users/Work/Bachelor Arbeit/RUNS/BenensonParking/GridNet/TestControlerListener/");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		config.controler().setCreateGraphs(true);
		config.controler().setWriteEventsInterval(0);
		if (otfvis) {
			config.controler().setLastIteration(0);
		} else {
			config.controler().setLastIteration(1);
		}
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		TSSetupParking.installParkingModules(controler);
		
		controler.addOverridingModule(new AbstractModule() {
			
			
			@Override
			public void install() {
//				bind(AgentFactory.class).to(BenensonParkingAgentFactory.class).asEagerSingleton(); // (**)
				addMobsimListenerBinding().to(ZoneParkingOccupationListener.class).asEagerSingleton();
				bind(ParkingSearchManager.class).to(ZoneParkingManager.class).asEagerSingleton();
			}
		});
		
		controler.run();
	}
	
}
