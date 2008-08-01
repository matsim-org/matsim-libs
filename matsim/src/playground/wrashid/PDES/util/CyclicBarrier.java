package playground.wrashid.PDES.util;

public class CyclicBarrier {
	
	protected int noOfParities;
	protected volatile int counter;
	boolean upMode=true;
	private Object lock=new Object();
	private Object lock2=new Object();
	
	synchronized public void informBarrier(){
		if (upMode){
			counter++;
		} else {
			counter--;
		}
		if (counter==noOfParities){
			upMode=false;
		}
	}
	public boolean isOpen(){
		if (upMode){
			return counter==noOfParities;
		} else {
			return counter==0;
		}
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
