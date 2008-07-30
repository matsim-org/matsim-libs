package playground.wrashid.PDES.util;

public class QueueObject {

	  private boolean isNotified = false;

	  public synchronized void doWait() throws InterruptedException {
	    while(!isNotified){
	        this.wait();
	    }
	    this.isNotified = false;
	  }

	  public synchronized void doNotify() {
	    this.isNotified = true;
	    this.notify();
	  }

	  public boolean equals(Object o) {
	    return this == o;
	  }
	}