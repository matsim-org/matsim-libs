package playground.wrashid.tryouts.performance;

import java.util.LinkedList;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.Events;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.parallelEventsHandler.ParallelEvents;

// this events processing relies on creating artifical events, intead of reading them
public class EventProcessing {
	public static void main(String[] args) {
		double timer=System.currentTimeMillis();
		EventProcessing ep=new EventProcessing();
		
		Events events = new ParallelEvents(8);
		//Events events = new Events();
		
		// start iteration
		events.initProcessing();

		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());
		events.addHandler(ep.new Handler1());

		
		LinkLeaveEvent linkLeaveEvent=new LinkLeaveEvent(0, new IdImpl(""), new IdImpl(""));
		
		for (int i=0;i<1000000;i++){
				events.processEvent(linkLeaveEvent);
		}
		
		
		
		// This is very important!!!
		events.finishProcessing();
		
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
