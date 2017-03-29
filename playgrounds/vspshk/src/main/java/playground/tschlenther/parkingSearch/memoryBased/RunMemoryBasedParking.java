package playground.tschlenther.parkingSearch.memoryBased;

import java.time.ZoneOffset;

import javax.swing.text.html.ParagraphView;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingListener;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.scenario.ScenarioUtils;

import playground.tschlenther.parkingSearch.Benenson.BenensonParkingAgentFactory;
import playground.tschlenther.parkingSearch.analysis.TSParkingSearchEvaluation;
import playground.tschlenther.parkingSearch.analysis.ZoneParkingOccupationListener;
import playground.tschlenther.parkingSearch.analysis.ZoneParkingOccupationListenerV2;
import playground.tschlenther.parkingSearch.install.TSSetupParking;
import playground.tschlenther.parkingSearch.utils.ZoneParkingManager;

public class RunMemoryBasedParking {
	private static final String inputDir = "C:/Users/Work/Bachelor Arbeit/input/GridNet/";
	private final static String outputDir = "C:/Users/Work/Bachelor Arbeit/RUNS/MemoryBased/Distance/testStrategyAgain";
	
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
		config.plans().setInputFile(inputDir + "population10.xml");
		config.facilities().setInputFile(inputDir + "parkingFacilities_full_memoryBased.xml");
		config.controler().setOutputDirectory(outputDir);
		config.network().setInputFile(inputDir + "grid_network_length200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		if (otfvis) {
			config.controler().setLastIteration(0);
		} else {
			config.controler().setLastIteration(2);
		}
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);

		config.controler().setWriteEventsInterval(1);
	
		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		TSSetupParking.installParkingModules(controler);
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				
//				addMobsimListenerBinding().to(ZoneParkingOccupationListener.class).asEagerSingleton();
				addMobsimListenerBinding().to(ZoneParkingOccupationListenerV2.class).asEagerSingleton();
				bind(ParkingSearchManager.class).to(ZoneParkingManager.class).asEagerSingleton();
//				bind(AgentFactory.class).to(MemoryBasedParkingAgentFactory.class).asEagerSingleton(); // (**)
			}
		});
		
		controler.run();
		
		
//		TSParkingSearchEvaluation.analyseRun(outputDir + "/ITERS", config.controler().getLastIteration());
	}
	
}
