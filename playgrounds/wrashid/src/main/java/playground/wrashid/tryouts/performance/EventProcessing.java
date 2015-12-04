package playground.wrashid.tryouts.performance;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.ParallelEventsManagerImpl;
import org.matsim.vehicles.Vehicle;

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
		
		ParallelEventsManagerImpl events = new ParallelEventsManagerImpl(numberOfThreads);
		//Events events = new Events();
		
		// start iteration
		events.initProcessing();

		
		for (int i=0;i<numberOfHandlers;i++){
			events.addHandler(ep.new Handler1());
		}
		
		LinkLeaveEvent linkLeaveEvent=new LinkLeaveEvent(0, Id.create("", Vehicle.class), Id.create("", Link.class) );
		
		for (int i=0;i<1000000;i++){
				events.processEvent(linkLeaveEvent);
		}
		
		
		
		// This is very important!!!
		events.finishProcessing();
		
		System.out.println("time needed in [s]:" + (System.currentTimeMillis() -  timer)/1000);
		

	}
	
	private class Handler1 implements LinkLeaveEventHandler {

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			LinkedList<Double> list=new LinkedList<Double>();
			for (int i = 0; i < 100; i++) {
				list.add(Math.sin(i));
			}
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
		}
		
		public Handler1(){
			
		}

	}


}
