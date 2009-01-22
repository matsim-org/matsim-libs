package playground.wrashid.tryouts.performance;

import java.util.LinkedList;

import org.matsim.basic.v01.BasicNodeImpl;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.EventsReaderTXTv1;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicNode;
import org.matsim.network.Link;
import org.matsim.network.LinkImpl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

import playground.wrashid.DES.ParallelEvents;
import playground.wrashid.PHEV.co2emissions.AllLinkHandler;
import playground.wrashid.PHEV.co2emissions.AllLinkOneIntervalHandler;
import playground.wrashid.PHEV.co2emissions.OneLinkHandler;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.utils.geometry.CoordImpl;

// this events processing relies on creating artifical events, intead of reading them
public class EventProcessing {
	public static void main(String[] args) {
		double timer=System.currentTimeMillis();
		EventProcessing ep=new EventProcessing();
		
		//Events events = new ParallelEvents(2);
		Events events = new Events();

		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());

		
		LinkLeaveEvent linkLeaveEvent=new LinkLeaveEvent(0, "", "", 0);
		
		for (int i=0;i<1000000;i++){
				events.processEvent(linkLeaveEvent);
		}
		
		
		
		// This is very important!!!
		if (events instanceof ParallelEvents){
			((ParallelEvents) events).awaitHandlerThreads();
		}
		
		System.out.println("time needed in [s]:" + (System.currentTimeMillis() -  timer)/1000);
		

	}
	
	private class Handler1 implements LinkLeaveEventHandler {

		public void handleEvent(LinkLeaveEvent event) {
			LinkedList list=new LinkedList();
			for (int i = 0; i < 100; i++) {
				list.add(Math.sin(i));
			}
		}

		public void reset(int iteration) {
			// TODO Auto-generated method stub

		}
		
		public Handler1(){
			
		}

	}


}
