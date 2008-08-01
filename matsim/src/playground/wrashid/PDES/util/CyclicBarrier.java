package playground.wrashid.PDES.util;

import playground.wrashid.PDES.SimulationParameters;

public abstract class CyclicBarrier {
	
	protected int noOfParities;
	protected volatile int counter=0;
	public volatile int round=0;
	private Object lock=new Object();
	private Object lock2=new Object();
	
	synchronized private int informBarrier(){
		counter++;
		if (counter==noOfParities){
			counter=0;
			doWhenAllAtBarrier();
			return round++;
		}
		return round;
	}
	
	public abstract void doWhenAllAtBarrier();
	public abstract void useCPUCycles();

	private void await(int threadRound){
		while (threadRound==round && counter>0){
			useCPUCycles();
		}
	}
	
	public void await(){
		await(informBarrier());
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
