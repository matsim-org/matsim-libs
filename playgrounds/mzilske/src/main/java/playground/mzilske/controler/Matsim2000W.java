package playground.mzilske.controler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.pt.PtConstants;
import org.matsim.pt.config.TransitConfigGroup;

public class Matsim2000W {
	
	private static final String configFilename = null;

	private static Logger logger = Logger.getLogger(Matsim2000W.class);
	
	public static void main(String[] args) {
		
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(configFilename);
		config.scenario().setUseTransit(false);
		config.scenario().setUseVehicles(false);
		
		if (config.scenario().isUseTransit()) {
			setupTransitConfig(config);
		}
	
		config.checkConsistency();
		logger.info("Complete config dump:");
		StringWriter writer = new StringWriter();
		new ConfigWriter(config).writeStream(new PrintWriter(writer));
		logger.info("\n\n" + writer.getBuffer().toString());
		logger.info("Complete config dump done.");
		
		
		ScenarioImpl scenario = new ScenarioImpl(config);
		scenario = (ScenarioImpl) new ScenarioLoaderImpl(scenario).loadScenario();
		MZControler controler = new MZControler(scenario);
		controler.run();
		
		
	}
	
	private static void setupTransitConfig(Config config) {
		TransitConfigGroup transitConfig = new TransitConfigGroup();
		if (config.getModule(TransitConfigGroup.GROUP_NAME) == null) {
			config.addModule(TransitConfigGroup.GROUP_NAME, transitConfig);
		} else {
			// this would not be necessary if TransitConfigGroup is part of core config
			Module oldModule = config.getModule(TransitConfigGroup.GROUP_NAME);
			config.removeModule(TransitConfigGroup.GROUP_NAME);
			transitConfig.addParam("transitScheduleFile", oldModule.getValue("transitScheduleFile"));
			transitConfig.addParam("vehiclesFile", oldModule.getValue("vehiclesFile"));
			transitConfig.addParam("transitModes", oldModule.getValue("transitModes"));
		}
		if (!config.scenario().isUseVehicles()) {
			logger.warn("Your are using Transit but not Vehicles. This most likely won't work.");
		}
		Set<EventsFileFormat> formats = EnumSet.copyOf(config.controler().getEventsFileFormats());
		formats.add(EventsFileFormat.xml);
		config.controler().setEventsFileFormats(formats);
		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		config.charyparNagelScoring().addActivityParams(transitActivityParams);
	}

}
