package playground.wrashid.PDES.util;

import playground.wrashid.PDES.SimulationParameters;

public abstract class CyclicBarrier {
	
	protected int noOfParities;
	protected volatile int counter=0;
	public volatile int round=0;
	
	synchronized private int informBarrier(int threadId){
		counter++;
		if (counter==noOfParities){
			counter=0;
			doWhenAllAtBarrier();
			useCPUCycles(threadId);
			return round++;
		}
		return round;
	}
	
	public abstract void doWhenAllAtBarrier();
	public abstract void useCPUCycles(int threadId);

	private void await(int threadRound){
		while (threadRound==round && counter>0){
			useCPUCycles(-1);
		}
	}
	
	
	
	private void await(int threadRound,int threadId){
		while (threadRound==round && counter>0){
			useCPUCycles(threadId);
		}
	}
	
	public void awaitT(int threadId){
		await(informBarrier(threadId),threadId);
	}

	public CyclicBarrier(int noOfParities){
		this.noOfParities=noOfParities;
	}

	
	
	
	
	
	
	 
	/*
	public void reset() {
	        counter = 0;
	    }
	    
	    public synchronized void await()
	        throws InterruptedException
	    {	System.out.println(counter);
	    	counter++;
	        // The final thread to reach barrier resets barrier and
	        // releases all threads
	        if ( counter==noOfParities ) {
	            // notify blocked threads that threshold has been reached
	            reset(); // perform the requested operation
	            notifyAll();
	        }
	        else while ( counter<noOfParities ) {
	            wait();
	        }
	    }
*/

	
}
