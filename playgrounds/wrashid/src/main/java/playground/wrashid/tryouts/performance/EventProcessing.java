package playground.wrashid.tryouts.performance;

import java.util.LinkedList;

import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.events.parallelEventsHandler.ParallelEvents;

// this events processing relies on creating artifical events, intead of reading them
/*
 * use for compiling:
 * make clean ; nice make run "MEMORY=-Xms1g -Xmx1g" "MAINCLASS=playground/wrashid/tryouts/performance/EventProcessing" ARGS="2 4"
 * (first parameter is numberOfThreads and Second parameter is numberOfHandlers)
 * 
 * use for running (without nice):
 * make runonly "MEMORY=-Xms1g -Xmx1g" "MAINCLASS=playground/wrashid/tryouts/performance/EventProcessing" ARGS="2 4"
 * 
 */
public class EventProcessing {
	public static void main(String[] args) {
		
		//System.out.println(args[0]);
		//System.out.println(args[1]);
		//System.out.println(args[2]);
		//System.out.println(args[3]);
		
		// the first two arguments are the config file and the dtd...
		
		int numberOfThreads=Integer.parseInt(args[2]);
		int numberOfHandlers=Integer.parseInt(args[3]);
		
		
		
		double timer=System.currentTimeMillis();
		EventProcessing ep=new EventProcessing();
		
		EventsManagerImpl events = new ParallelEvents(numberOfThreads);
		//Events events = new Events();
		
		// start iteration
		events.initProcessing();

		
		for (int i=0;i<numberOfHandlers;i++){
			events.addHandler(ep.new Handler1());
		}
		
		LinkLeaveEventImpl linkLeaveEvent=new LinkLeaveEventImpl(0, new IdImpl(""), new IdImpl(""));
		
		for (int i=0;i<1000000;i++){
				events.processEvent(linkLeaveEvent);
		}
		
		
		
		// This is very important!!!
		events.finishProcessing();
		
		System.out.println("time needed in [s]:" + (System.currentTimeMillis() -  timer)/1000);
		

	}
	
	private class Handler1 implements LinkLeaveEventHandler {

		public void handleEvent(LinkLeaveEvent event) {
			LinkedList<Double> list=new LinkedList<Double>();
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
