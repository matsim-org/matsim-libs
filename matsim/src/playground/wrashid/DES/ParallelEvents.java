package playground.wrashid.DES;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.events.BasicEvent;
import org.matsim.events.Events;
import org.matsim.events.handler.EventHandler;

public class ParallelEvents extends Events {

	private int numberOfThreads;
	private Events[] events = null;
	private ProcessEventThread[] eventsProcessThread = null;
	private int numberOfAddedEventsHandler = 0;
	private CyclicBarrier barrier=null;

	public void processEvent(final BasicEvent event) {
		for (int i=0;i<eventsProcessThread.length;i++){
			eventsProcessThread[i].processEvent(event);
		}
	}

	// TODO: this is not thread safe yet:
	// even though, we set the handler, it might be, that the processing
	// thread does not see this
	// probably this is ok: because the synchronization happens, when 
	// processEvent is invoked by the main thread
	public void addHandler(final EventHandler handler) {
		events[numberOfAddedEventsHandler].addHandler(handler);
		numberOfAddedEventsHandler = (numberOfAddedEventsHandler + 1)
				% numberOfThreads;
	}

	/**
	 * @param numberOfThreads - specify the number of threads used for the events handler
	 */
	public ParallelEvents(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
		this.events = new Events[numberOfThreads];
		this.eventsProcessThread = new ProcessEventThread[numberOfThreads];
		// the additional 1 is for the simulation barrier
		barrier = new CyclicBarrier(numberOfThreads+1);
		for (int i = 0; i < numberOfThreads; i++) {
			events[i] = new Events();
			eventsProcessThread[i] = new ProcessEventThread(events[i],100000,barrier);
		}
	}
	
	// When the main method is finish, it must await this barrier
	public void awaitHandlerThreads(){
		for (int i=0;i<eventsProcessThread.length;i++){
			eventsProcessThread[i].close();
		}
		
		
		try {
			barrier.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} 
}
