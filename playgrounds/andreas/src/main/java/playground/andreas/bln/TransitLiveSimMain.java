package playground.andreas.bln;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;

public class TransitLiveSimMain {

	public static void main(String[] args) {

		String inputDir = "e:/_out/otflivetest/";

		String configFilename = inputDir + "config.xml";
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(configFilename);
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		scenario = (ScenarioImpl) new ScenarioLoaderImpl(scenario).loadScenario();

		new TransitScheduleReaderV1(scenario.getTransitSchedule(), scenario.getNetwork(), scenario).readFile(inputDir + "transitSchedule.xml");
		new VehicleReaderV1(scenario.getVehicles()).parse(inputDir + "vehicles.xml");

		ReconstructingUmlaufBuilder reconstructingUmlaufBuilder = new ReconstructingUmlaufBuilder(
				scenario.getNetwork(),
				scenario.getTransitSchedule().getTransitLines().values(),
				scenario.getVehicles(),
				scenario.getConfig().planCalcScore());
		reconstructingUmlaufBuilder.build();

		EventsManager events = EventsUtils.createEventsManager();
		QSim sim = new QSim(scenario, events);
		sim.getTransitEngine().setUseUmlaeufe(true);
		OTFVisMobsimFeature otfVisQSimFeature = new OTFVisMobsimFeature(sim);
		otfVisQSimFeature.setVisualizeTeleportedAgents(sim.getScenario().getConfig().otfVis().isShowTeleportedAgents());
		sim.addFeature(otfVisQSimFeature);


//		if(useHeadwayControler){
//			sim.getQSimTransitEngine().setAbstractTransitDriverFactory(new FixedHeadwayCycleUmlaufDriverFactory());
//			this.events.addHandler(new FixedHeadwayControler(sim));
//		}
//		this.events.addHandler(new LogOutputEventHandler());


		sim.run();

	}

}
