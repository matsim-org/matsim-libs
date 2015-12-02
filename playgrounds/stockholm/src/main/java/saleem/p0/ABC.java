package saleem.p0;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.VariableIntervalTimeVariantLinkFactory;
import org.matsim.core.scenario.ScenarioUtils;

public class ABC {
	static NetworkImpl network;
	public static void main(String[] args) {
		
		
		
		
		Config config = ConfigUtils.loadConfig("H:\\Mike Work\\input\\config.xml");
		config.network().setTimeVariantNetwork(true);
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		NetworkFactoryImpl nf = (NetworkFactoryImpl) scenario.getNetwork().getFactory();
		nf.setLinkFactory(new VariableIntervalTimeVariantLinkFactory());
		network = (NetworkImpl) scenario.getNetwork();
		Controler controler = new Controler( scenario ) ;
		controler.addControlerListener(new P0ControlListener(network));
		controler.run() ;
		//controler.setOverwriteFiles(true);
	      // Create an EventsManager instance. This is MATSim infrastructure.
	     // EventsManager eventsManager = EventsUtils.createEventsManager();
	      // Create an instance of the custom EventHandler which you just wrote. Add it to the EventsManager.
	     // P0QueueDelayControl handler = new P0QueueDelayControl(network);
	      //eventsManager.addHandler(handler);
	   
	      // Connect a file reader to the EventsManager and read in the event file.
	      //String inputFile = "H:\\Mike Work\\output\\ITERS\\it.0\\0.events.xml.gz";
	     // MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
	     // reader.readFile(inputFile);

	     // System.out.println("Events file read!");

	      // Print the counts from the TripCounter
	     // handler.adjustCapacityP0();
		// controler.setOverwriteFiles(true);

	}
}
