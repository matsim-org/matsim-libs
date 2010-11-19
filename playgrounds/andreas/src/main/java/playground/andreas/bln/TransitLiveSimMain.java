package playground.andreas.bln;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;
import org.xml.sax.SAXException;

public class TransitLiveSimMain {
	
	public static void main(String[] args) {
		
		String inputDir = "e:/_out/otflivetest/";

		double snapshotPeriod = 60;
		SimulationConfigGroup simulationConfigGroup = new SimulationConfigGroup();
		String configFilename = inputDir + "config.xml";
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(configFilename);
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		ScenarioImpl scenario = new ScenarioImpl(config);
		scenario = (ScenarioImpl) new ScenarioLoaderImpl(scenario).loadScenario();
		
        try {
			new TransitScheduleReaderV1(scenario.getTransitSchedule(), scenario.getNetwork()).readFile(inputDir + "transitSchedule.xml");
			new VehicleReaderV1(scenario.getVehicles()).parse(inputDir + "vehicles.xml");
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
		ReconstructingUmlaufBuilder reconstructingUmlaufBuilder = new ReconstructingUmlaufBuilder(
				scenario.getNetwork(), 
				scenario.getTransitSchedule().getTransitLines().values(),
				scenario.getVehicles(),
				scenario.getConfig().charyparNagelScoring());
		reconstructingUmlaufBuilder.build();

		EventsManager events = new EventsManagerImpl();
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
