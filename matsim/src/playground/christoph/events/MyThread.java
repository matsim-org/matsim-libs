package playground.christoph.events;

public class MyThread extends Thread { 

	private volatile boolean isWaiting = false; 
	private volatile boolean simulationRunning = true;
	
	@Override
	public void run() {
		while (simulationRunning)
		{	
			try
			{
				isWaiting = false;
				// do replanning
				
				isWaiting = true; 
				wait();
			}
			catch (InterruptedException ie)
			{
				System.out.println("Something is going wrong here...");
			}
			// running...
		}
	} 
/*
	public void suspendThread()
	{ 
		interrupt(); 
	} 
*/
	public synchronized void resumeThread()
	{ 
		notify(); 
	} 

	public boolean isSuspended()
	{ 
		return isWaiting; 
	} 
}