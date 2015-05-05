package playground.toronto;

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import playground.toronto.analysis.handlers.AggregateBoardingsOverTimePeriodHandler;
import playground.toronto.exceptions.NetworkFormattingException;
import playground.toronto.router.TorontoTransitRouterImplFactory;
import playground.toronto.router.UpgradedTransitRouterFactory;
import playground.toronto.router.calculators.TransitDataCache;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.Locale;

public class UpgradedRunToronto {

	private static final Logger log = Logger.getLogger(UpgradedRunToronto.class);
	
	/**
	 * @param args
	 * @throws NetworkFormattingException 
	 */
	public static void main(String[] args) throws NetworkFormattingException {
		
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
		final Provider<TransitRouter> trFactory;
		
		//Branch: if mode-specific boarding costs are specified, use a different calculator.
		if (config.getModule("boardingcosts") != null){
			log.info("Boarding costs module detected. Configuring MATSim to allow mode-specific boarding costs");
			double busWeight = Double.parseDouble(config.getParam("boardingcosts", "busPenalty"));
			double tramWeight = Double.parseDouble(config.getParam("boardingcosts", "streetcarPenalty"));
			double subwayWeight = Double.parseDouble(config.getParam("boardingcosts", "subwayPenalty"));
			trFactory = new TorontoTransitRouterImplFactory(scenario.getNetwork() ,scenario.getTransitSchedule(), trConfig, 
					transitDataCache, busWeight, tramWeight, subwayWeight);
		}else{
			log.info("Configuring Toronto Transit Router");
			trFactory = new UpgradedTransitRouterFactory(scenario.getNetwork(), trConfig, scenario.getTransitSchedule(), transitDataCache);
		}
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TransitRouter.class).toProvider(trFactory);
			}
		});

		//Optional: Load transit schedule data from earlier run
		if (config.getModule("precongest") != null){
			EventsManager em = EventsUtils.createEventsManager();
			em.addHandler(transitDataCache);
			log.info("Pre-congestion module detected. Pre-loading transit data from earlier run...");
			new MatsimEventsReader(em).readFile(config.getParam("precongest", "eventsFile"));
			log.info("Pre-loading complete.");
		}
		
		//Add custom handlers to the controller. This allows for, among other things, quick export of assignment results.
		controller.getEvents().addHandler(transitDataCache);
		
		AggregateBoardingsOverTimePeriodHandler morningBoardings = 
				new AggregateBoardingsOverTimePeriodHandler(Time.convertHHMMInteger(600), Time.convertHHMMInteger(859));
		
		AggregateBoardingsOverTimePeriodHandler middayBoardings = 
				new AggregateBoardingsOverTimePeriodHandler(Time.convertHHMMInteger(900), Time.convertHHMMInteger(1459));
		
		AggregateBoardingsOverTimePeriodHandler afternoonBoardings = 
				new AggregateBoardingsOverTimePeriodHandler(Time.convertHHMMInteger(1500), Time.convertHHMMInteger(1759));
		
		AggregateBoardingsOverTimePeriodHandler allDayBoardings = 
				new AggregateBoardingsOverTimePeriodHandler();
		
		controller.getEvents().addHandler(allDayBoardings);
		controller.getEvents().addHandler(morningBoardings);
		controller.getEvents().addHandler(middayBoardings);
		controller.getEvents().addHandler(afternoonBoardings);		
	
		
		//-----RUN-------------------------------------------------
		controller.run();
		
		
		//-----POST-PROCESS--------------------------------------------------
		
		
	}

}
