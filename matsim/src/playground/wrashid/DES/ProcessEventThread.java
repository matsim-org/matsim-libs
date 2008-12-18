package playground.wrashid.DES;

import java.util.LinkedList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.events.BasicEvent;
import org.matsim.events.Events;

import playground.wrashid.PDES2.util.ConcurrentListSPSC;

public class ProcessEventThread implements Runnable {
	//LinkedList<BasicEvent> preInputBuffer=null;
	ConcurrentListSPSC<BasicEvent> eventQueue = null;
	Events events;
	CyclicBarrier cb=new CyclicBarrier(2);
	private int preInputBufferMaxLength;

	public ProcessEventThread(Events events, int preInputBufferMaxLength) {
		this.events = events;
		this.preInputBufferMaxLength= preInputBufferMaxLength;
		eventQueue = new ConcurrentListSPSC<BasicEvent>();

		
		Thread t = new Thread(this);
		t.start();
	}
	
	public ProcessEventThread(Events events) {
		this.events = events;
		eventQueue = new ConcurrentListSPSC<BasicEvent>();
		
		Thread t = new Thread(this);
		t.start();
	}

	
	// a different approach was tried, but it was not as efficient:
	// buffer the elements locally and then write them to eventQueue at once.
	// the problem with this is, that actually the current thread is allowed to 
	// be slow. But this second approach makes the processEventThread slow instead
	// of the main thread.
	
	// This second proposed approach would again make sense, if we use the main thread
	// after its completion as a worker thread.
	public void processEvent(BasicEvent event) {
		eventQueue.add(event);
		//preInputBuffer.add(event);
		//if (preInputBuffer.size()>preInputBufferMaxLength){
		//	eventQueue.add(preInputBuffer);
		//	preInputBuffer.clear();
		//}
	}

	public void run() {
		// process events, until DummyEvent arrives
		BasicEvent nextEvent = null;
		while (true) {
			nextEvent = eventQueue.remove();
			if (nextEvent != null) {
				if (nextEvent instanceof DummyEvent) {
					break;
				} else {
					events.processEvent(nextEvent);
				}
			}
		}
		// inform main thread, that processing finished
		try {
			cb.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	// When the main method is finish, it must await this barrier
	public CyclicBarrier getBarrier(){
		return cb;
	}

	// call to flush buffer
	public void close() {
		eventQueue.add(new DummyEvent(0.0));
		//eventQueue.add(preInputBuffer);
		//preInputBuffer.clear();
	}
	
}
