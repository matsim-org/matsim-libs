package playground.tschlenther.parkingSearch.memoryBased;

import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.text.html.ParagraphView;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingListener;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingSlotVisualiser;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.scenario.ScenarioUtils;

import playground.tschlenther.parkingSearch.Benenson.BenensonParkingAgentFactory;
import playground.tschlenther.parkingSearch.analysis.TSParkingSearchEvaluation;
import playground.tschlenther.parkingSearch.analysis.ZoneParkingOccupationListenerV2;
import playground.tschlenther.parkingSearch.install.TSSetupParking;
import playground.tschlenther.parkingSearch.utils.GridNetPopulationCreator;
import playground.tschlenther.parkingSearch.utils.ZoneParkingManager;

public class RunMemoryBasedParking {
//	private static final String inputDir = "C:/Users/Work/Bachelor Arbeit/input/GridNet/";
//	private static String outputDir = "C:/Users/Work/Bachelor Arbeit/RUNS/TestVisualiser/Memory/GridNet/";
//	private final static String ZONE1 = "C:/Users/Work/Bachelor Arbeit/input/GridNet/Zonen/Links.txt";
//	private final static String ZONE2 = "C:/Users/Work/Bachelor Arbeit/input/GridNet/Zonen/Rechts.txt";
	
	private static final String inputDir = "C:/Users/Work/Bachelor Arbeit/input/Berlin/";
	private static String outputDir = "C:/Users/Work/Bachelor Arbeit/RUNS/TestVisualiser/Memory/Berlin/";
	private final static String ZONE1 = "C:/Users/Work/Bachelor Arbeit/input/Berlin/Mierendorff.txt";
	private final static String ZONE2 = "C:/Users/Work/Bachelor Arbeit/input/Berlin/Klausener.txt";
	
	public static void main(String[] args) {
		
//		Config config = ConfigUtils.loadConfig(inputDir + "config_links_rechts.xml", new DvrpConfigGroup());
//		config.plans().setInputFile(inputDir + "population_links_rechts_V3.xml");
//		config.facilities().setInputFile(inputDir + "Facilites_links_rechts_V2.xml");
////		config.facilities().setInputFile(inputDir + "parkingFacilities_full_workNames.xml");
//		config.network().setInputFile(inputDir + "grid_network_length200.xml");

		Config config = ConfigUtils.loadConfig(inputDir + "configBCParking.xml", new DvrpConfigGroup());
		config.plans().setInputFile(inputDir + "untersuchungsraum-plans.xml.gz");
//		config.plans().setInputFile(inputDir + "population_Test_60.xml");
		config.facilities().setInputFile(inputDir + "Berlin-facilities-untersuchungsraum-full.xml");
		config.network().setInputFile(inputDir + "network-car.xml.gz");
		config.network().setChangeEventsInputFile(inputDir + "changeEvents.xml.gz");
		
		String runID = new SimpleDateFormat("ddMMyy_HH.mm").format(new Date());
		runID +=  "_MEMORY";
//		String runID = "050417_19.46_MEMORY";
		outputDir += "RUN_" + runID;
		
		
		int maxIter = 0;
		config.controler().setOutputDirectory(outputDir);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setLastIteration(maxIter);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
//		GridNetPopulationCreator.createPopulationForTwoZones(scenario, 60);
		
		Controler controler = new Controler(scenario);
		config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);

		config.controler().setWriteEventsInterval(1);
		TSSetupParking.installParkingModules(controler);
		
		String[] zonen = new String[2];
		zonen[0] = ZONE2;
		zonen[1] = ZONE1;
		ZoneParkingManager manager = new ZoneParkingManager(scenario,zonen);
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				
				addMobsimListenerBinding().to(ZoneParkingOccupationListenerV2.class).asEagerSingleton();
				bind(ParkingSearchManager.class).toInstance(manager);
			}
		});
		
		ParkingSlotVisualiser visualiser = new ParkingSlotVisualiser(scenario);
		controler.getEvents().addHandler(visualiser);
		controler.run();
		
		TSParkingSearchEvaluation eval = new TSParkingSearchEvaluation(zonen);
		eval.analyseRun(outputDir + "/ITERS", config.controler().getLastIteration());
		
		String scriptPath = 	"C:/Users/Work/Bachelor Arbeit/Analysis.vbs";
		
		String zone1 = ZONE1.substring(ZONE1.lastIndexOf("/")+1, ZONE1.lastIndexOf("."));
		String zone2 = ZONE2.substring(ZONE2.lastIndexOf("/")+1, ZONE2.lastIndexOf("."));
		
		
		String[] arguments = new String[] {
				"wscript.exe", scriptPath , outputDir, ""+maxIter, zone1 , zone2, runID
		};

		System.out.println("START");
		runVBScript(arguments);
		System.out.println("STARTED");
		
		visualiser.finishDay();
		visualiser.plotCarPositions(outputDir + "/parkingSlotXY.csv");
	}
	
	private static void runVBScript(String[] cmds){
		try {
	        Runtime.getRuntime().exec(cmds);        
	    } catch (Exception ex) {
	        ex.printStackTrace();
	        throw new RuntimeException("konnte Skript nicht durchlaufen lassen");
	    }
	}
	
}
