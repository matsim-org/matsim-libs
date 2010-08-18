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
import org.matsim.pt.qsim.TransitQSimulation;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;
import org.xml.sax.SAXException;

public class TransitLiveSimMain {
	
	public static void main(String[] args) {

//		 String fileName = "../../run749/749.output_network.xml.gz";
//		 String eventsFileName = "../../run749/it.1000/749.1000.events.txt.gz";
//		
		String networkFileName = "../../matsim/output/example5/output_network.xml.gz";
		String eventsFileName = "../../matsim/output/example5/ITERS/it.10/10.events.xml.gz";
		String populationFileName = "../../matsim/output/example5/wurst.xml";
		
//		String networkFileName = "../../network-ivtch/ivtch-osm.xml";
//		String eventsFileName = "../../run657/it.1000/1000.events.txt.gz";
//		String populationFileName = "../../run657/it.1000/1000.plans.xml.gz";
		
//		String networkFileName = "output/brandenburg/output_network.xml.gz";
//		String eventsFileName = "output/brandenburg/ITERS/it.10/10.events.txt.gz";
//		String populationFileName = "output/brandenburg/output_plans.xml.gz";
		
		double snapshotPeriod = 60;
		SimulationConfigGroup simulationConfigGroup = new SimulationConfigGroup();
		String configFilename = "e:\\_out\\test\\config.xml";
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(configFilename);
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		ScenarioImpl scenario = new ScenarioImpl(config);
		scenario = (ScenarioImpl) new ScenarioLoaderImpl(scenario).loadScenario();
		
        try {
			new TransitScheduleReaderV1(scenario.getTransitSchedule(), scenario.getNetwork()).readFile("e:\\_out\\test\\transitSchedule.xml");
			new VehicleReaderV1(scenario.getVehicles()).parse("e:\\_out\\test\\vehicles.xml");
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
		TransitQSimulation sim = new TransitQSimulation(scenario, events);
		sim.getQSimTransitEngine().setUseUmlaeufe(true);
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
