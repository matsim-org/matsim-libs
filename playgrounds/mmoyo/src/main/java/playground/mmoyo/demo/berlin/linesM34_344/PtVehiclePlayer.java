package playground.mmoyo.demo.berlin.linesM34_344;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.pt.queuesim.TransitQueueSimulation;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.xml.sax.SAXException;

import playground.mrieser.OTFDemo;

/**runs only pt vehicles of a scenario*/
public class PtVehiclePlayer {
	private static final String SERVERNAME = "vechiclePlayer";

	public PtVehiclePlayer(ScenarioImpl scenario){
		//set network
		scenario.getNetwork().setCapacityPeriod(3600.0);
		scenario.getNetwork().getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
		
		//event manager
		EventsManagerImpl eventManager = new EventsManagerImpl();
		EventWriterXML eventWriter = new EventWriterXML("./output/vehPlayerEvents.xml");
		eventManager.addHandler(eventWriter);

		//run simulation with OTFDemo
		final TransitQueueSimulation trSimulation = new TransitQueueSimulation(scenario, eventManager);
		new CreateVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getVehicles()).run();
		trSimulation.startOTFServer(SERVERNAME);
		OTFDemo.ptConnect(SERVERNAME);
		trSimulation.run();
		eventWriter.closeFile();
	}
	
	public static void main(final String[] args) throws SAXException, ParserConfigurationException, IOException {
		String configFile;
		
		if (args.length==1){
			configFile = args[0];} 
		else {
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/config_900s_small.xml";
		}
 
		/**load scenario */
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(configFile);
		ScenarioImpl scenario = scenarioLoader.getScenario();
		scenario.getNetwork().getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
		scenarioLoader.loadScenario();
		new TransitScheduleReaderV1(scenario.getTransitSchedule(), scenario.getNetwork()).parse(scenario.getConfig().getParam("transit", "transitScheduleFile"));
		//new CreateVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getVehicles()).run();
		new PtVehiclePlayer(scenario);
	}
}
