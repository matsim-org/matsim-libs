package playground.wrashid.tryouts.performance;

import java.util.LinkedList;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.Events;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.parallelEventsHandler.ParallelEvents;

// this events processing relies on creating artifical events, intead of reading them
/*
 * make clean ; nice make run "MEMORY=-Xms1g -Xmx1g" "MAINCLASS=playground/wrashid/tryouts/performance/EventProcessing"
 * 
 * => wichtig, dass man nice raus nimmt...
 * make run "MEMORY=-Xms1g -Xmx1g" "MAINCLASS=playground/wrashid/tryouts/performance/EventProcessing"
 * 
 * First compile with the above command (in the matsim folder)
 * Then go to folder classes
 * Then start:
 * java playground/wrashid/tryouts/performance/EventProcessing <numberOfThreads> <numberOfHandlers>
 */
public class EventProcessing {
	public static void main(String[] args) {
		
		System.out.println(args[0]);
		System.out.println(args[1]);
		System.out.println(args[2]);
		System.out.println(args[3]);
		
		int numberOfThreads=Integer.parseInt(args[0]);
		int numberOfHandlers=Integer.parseInt(args[1]);
		
		
		
		double timer=System.currentTimeMillis();
		EventProcessing ep=new EventProcessing();
		
		Events events = new ParallelEvents(numberOfThreads);
		//Events events = new Events();
		
		// start iteration
		events.initProcessing();

		
		for (int i=0;i<numberOfHandlers;i++){
			events.addHandler(ep.new Handler1());
		}
		
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
