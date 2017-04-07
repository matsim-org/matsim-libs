/**
 * 
 */
package playground.tschlenther.parkingSearch.Benenson;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.parking.parkingsearch.RunParkingSearchExample;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingSlotVisualiser;
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

import playground.tschlenther.parkingSearch.analysis.TSParkingSearchEvaluation;
import playground.tschlenther.parkingSearch.analysis.ZoneParkingOccupationListenerV2;
import playground.tschlenther.parkingSearch.install.TSSetupParking;
import playground.tschlenther.parkingSearch.memoryBased.MemoryBasedParkingAgentFactory;
import playground.tschlenther.parkingSearch.utils.ZoneParkingManager;

/**
 * @author schlenther
 *
 */
public class RunBenensonParking {

//	private static final String inputDir = "C:/Users/Work/Bachelor Arbeit/input/GridNet/";
//	private final static String outputDir = "C:/Users/Work/Bachelor Arbeit/RUNS/SERIOUS/GridNet/Run_0304";
//	private final static String ZONE1 = "C:/Users/Work/Bachelor Arbeit/input/GridNet/Zonen/Zielzone.txt";
//	private final static String ZONE2 = "C:/Users/Work/Bachelor Arbeit/input/GridNet/Zonen/Homezone.txt";
	
	private static final String inputDir = "C:/Users/Work/Bachelor Arbeit/input/Berlin/";
	private static String outputDir = "C:/Users/Work/Bachelor Arbeit/RUNS/SERIOUS/Benenson/Berlin/";
//	private final static String outputDir = "C:/Users/Work/Bachelor Arbeit/RUNS/LastTest";
	private final static String ZONE1 = "C:/Users/Work/Bachelor Arbeit/input/Berlin/Klausener.txt";
	private final static String ZONE2 = "C:/Users/Work/Bachelor Arbeit/input/Berlin/Mierendorff.txt";
	
	
	
	
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
		
		
//		Config config = ConfigUtils.loadConfig(inputDir + "config.xml", new DvrpConfigGroup());
//		config.plans().setInputFile(inputDir + "population10.xml");		
//		config.facilities().setInputFile(inputDir + "parkingFacilities_full_workNames.xml");		
//		config.network().setInputFile(inputDir + "grid_network_length200.xml");
		
		Config config = ConfigUtils.loadConfig(inputDir + "configBCParking.xml", new DvrpConfigGroup());
		config.plans().setInputFile(inputDir + "untersuchungsraum-plans.xml.gz");
		config.facilities().setInputFile(inputDir + "Berlin-facilities-untersuchungsraum-full.xml");
		config.network().setInputFile(inputDir + "network-car.xml.gz");
		config.network().setChangeEventsInputFile(inputDir + "changeEvents.xml.gz");
		
		String runID = new SimpleDateFormat("ddMMyy_HH.mm").format(new Date());
		runID +=  "_MIN1_MAX3_PH250_PH100_MAXDIST400";
		outputDir += "changeEvents/RUN_" + runID;
		
		
		
		config.controler().setOutputDirectory(outputDir);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		config.controler().setCreateGraphs(true);
		config.controler().setWriteEventsInterval(1);
		int maxIter = 10;
		if (otfvis) {
			config.controler().setLastIteration(0);
		} else {
			config.controler().setLastIteration(maxIter);
		}
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		TSSetupParking.installParkingModules(controler);
		
		String[] zonen = new String[2];
		zonen[0] = ZONE2;
		zonen[1] = ZONE1;
		ZoneParkingManager manager = new ZoneParkingManager(scenario,zonen);
		
		controler.addOverridingModule(new AbstractModule() {
			
			
			@Override
			public void install() {
//				bind(AgentFactory.class).to(BenensonParkingAgentFactory.class).asEagerSingleton(); // (**)
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
