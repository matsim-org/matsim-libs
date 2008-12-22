package playground.wrashid.PHEV.co2emissions;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.EventsReaderTXTv1;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;

public class SimpleEvaluator {

	public static void main(String[] args) {
		String eventsFilePath = "C:\\data\\SandboxCVS\\ivt\\studies\\wrashid\\IAMF2009Paper\\CO2Experiment\\input events\\56.events.txt";
		args=new String[1];
		args[0]="C:\\data\\SandboxCVS\\ivt\\studies\\triangle\\config\\config.xml";
		
		
		Config config = Gbl.createConfig(args);

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer) Gbl.getWorld().createLayer(
				NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());
		System.out.println("  done.");
		
		
		Events events = new Events();

		AllLinkHandler allLinkHandler = new AllLinkHandler(180.0,network); // co2 emissions in gram per km 
		// get for one link statistics of CO2, for the specified interval
		OneLinkHandler oneLinkHandler = new OneLinkHandler(180.0,network,"107",1800);
		
		// 
		// 28800s => 8:00
		// 30600s => 8:30
		// 32400s => 9:00
		AllLinkOneIntervalHandler allLinkOneIntervalHandler = new AllLinkOneIntervalHandler(180.0,network,0,68400);
		
		events.addHandler(allLinkHandler);
		events.addHandler(oneLinkHandler);
		events.addHandler(allLinkOneIntervalHandler);
		

		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		reader.readFile(eventsFilePath);

		allLinkHandler.printCO2EmissionsWholeDay();
		oneLinkHandler.printHourlyCO2Emissions();
		allLinkOneIntervalHandler.printCO2EmissionsSpecifiedInterval();

	}
}
