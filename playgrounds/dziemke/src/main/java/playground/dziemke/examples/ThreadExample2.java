package playground.dziemke.examples;

/**
 * example taken from http://www.tutorialspoint.com/java/java_multithreading.htm
 */
public class ThreadExample2 {
	public static void main(String args[]) {
//		RunnableDemo R1 = new RunnableDemo( "Thread-1");
		new RunnableDemo( "Thread-1");
//		R1.start();

//		RunnableDemo R2 = new RunnableDemo( "Thread-2");
		new RunnableDemo( "Thread-2");
//		R2.start();
	}   
}


class RunnableDemo implements Runnable {
	private Thread t;
	private String threadName;
	
	
	
	RunnableDemo( String threadName){
		this.threadName = threadName;
		System.out.println("Creating " +  threadName );
		
		System.out.println("Starting " +  threadName );
//		if (t == null) {
			t = new Thread (this, threadName);
			t.start ();
//		}
	}
//	}

	public void run() {
		System.out.println("Running " +  threadName );
		try {
			for(int i = 4; i > 0; i--) {
				System.out.println("Thread: " + threadName + ", " + i);
	            // Let the thread sleep for a while.
//	            Thread.sleep(50);
	            Thread.sleep(1000);
	        }
	    } catch (InterruptedException e) {
	    	System.out.println("Thread " +  threadName + " interrupted.");
	    }
	    System.out.println("Thread " +  threadName + " exiting.");
	}
	
//	public void start () {
//		System.out.println("Starting " +  threadName );
////		if (t == null) {
//			t = new Thread (this, threadName);
//			t.start ();
////		}
//	}
}