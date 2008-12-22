package playground.wrashid.tryouts.performance;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.EventsReaderTXTv1;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;

import playground.wrashid.DES.ParallelEvents;
import playground.wrashid.PHEV.co2emissions.AllLinkHandler;
import playground.wrashid.PHEV.co2emissions.AllLinkOneIntervalHandler;
import playground.wrashid.PHEV.co2emissions.OneLinkHandler;

public class EventProcessing {
	public static void main(String[] args) {
		double timer=System.currentTimeMillis();
		EventProcessing ep=new EventProcessing();
		String eventsFilePath = "C:\\data\\SandboxCVS\\ivt\\studies\\wrashid\\IAMF2009Paper\\CO2Experiment\\56.events.txt";
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
		
		Events events = new ParallelEvents(4);
		//Events events = new Events();

		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());

		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		reader.readFile(eventsFilePath);
		// This is very important!!!
		if (events instanceof ParallelEvents){
			((ParallelEvents) events).awaitHandlerThreads();
		}
		
		System.out.println("time needed in [s]:" + (System.currentTimeMillis() -  timer)/1000);
		

	}
	
	private class Handler1 implements LinkLeaveEventHandler {

		public void handleEvent(LinkLeaveEvent event) {
			for (int i = 0; i < 1000000; i++) {

			}
		}

		public void reset(int iteration) {
			// TODO Auto-generated method stub

		}
		
		public Handler1(){
			
		}

	}


}
