package playground.toronto;

import java.io.File;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;

import playground.toronto.analysis.handlers.AgentTripChainHandler;
import playground.toronto.analysis.handlers.AggregateBoardingsOverTimePeriodHandler;
import playground.toronto.router.TorontoTransitRouterImplFactory;
import playground.toronto.router.TransitDataCache;
import playground.toronto.router.UpgradedTransitRouterFactory;

public class UpgradedRunToronto {

	private static final Logger log = Logger.getLogger(UpgradedRunToronto.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//-----LOAD THE CONFIG FILE & SCENARIO--------------------------------------------------
		String file;
		if (args.length != 1){
			file = "";
			JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileFilter() {
				@Override
				public String getDescription() {return "MATSim config file in *.xml format";}
				@Override
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".xml" );
				}
			});
			int state = fc.showOpenDialog(null);
			if (state == JFileChooser.APPROVE_OPTION){
				file = fc.getSelectedFile().getAbsolutePath();
			}else if (state == JFileChooser.CANCEL_OPTION) return;
			if (file == "" || file == null) return;	
		}else{
			file = args[0];
		}
		
		Config config = ConfigUtils.loadConfig(file);
		config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
		config.checkConsistency();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controller = new Controler(scenario);
		controller.setOverwriteFiles(true);
		log.info("Scenario loaded.");
		
		//-----PRE-PROCESS RUN-------------------------------------------------
		// Put run-specific code here.
		
		//Set up the custom travel-time calculator
		TransitRouterConfig trConfig = new TransitRouterConfig(config);
		TransitDataCache transitDataCache = new TransitDataCache(scenario.getTransitSchedule());
		TransitRouterFactory trFactory;
		if (config.getModule("boardingcosts") != null){
			double busWeight = Double.parseDouble(config.getParam("boardingcosts", "busPenalty"));
			double tramWeight = Double.parseDouble(config.getParam("boardingcosts", "streetcarPenalty"));
			double subwayWeight = Double.parseDouble(config.getParam("boardingcosts", "subwayPenalty"));
			trFactory = new TorontoTransitRouterImplFactory(scenario.getTransitSchedule(), trConfig, 
					transitDataCache, busWeight, tramWeight, subwayWeight);
		}else{
			trFactory = new UpgradedTransitRouterFactory(trConfig, scenario.getTransitSchedule(), transitDataCache);
		}
		controller.setTransitRouterFactory(trFactory);
		
		//Optional: Load transit schedule data from earlier run
		if (config.getModule("precongest") != null){
			EventsManager em = EventsUtils.createEventsManager();
			em.addHandler(transitDataCache);
			log.info("Pre-loading transit data from earlier run...");
			new MatsimEventsReader(em).readFile(config.getParam("precongest", "eventsFile"));
			//transitDataCache.reset(0); //I think this is not necessary
			log.info("Pre-loading complete.");
		}
		
		//Add custom handlers to the controller. This allows for, among other things, quick export of assignment results.
		controller.getEvents().addHandler(transitDataCache);
		
		AggregateBoardingsOverTimePeriodHandler morningBoardings = 
				new AggregateBoardingsOverTimePeriodHandler(0.0, Time.parseTime("08:59:59"));
		
		controller.getEvents().addHandler(morningBoardings);
		
		AgentTripChainHandler tripsHandler = new AgentTripChainHandler();
		controller.getEvents().addHandler(tripsHandler);
		
		
		//-----RUN-------------------------------------------------
		controller.run();
		
		
		//-----POST-PROCESS--------------------------------------------------
		
		
	}

}
