package playground.wrashid.PDES2;

import playground.wrashid.PDES2.util.CyclicBarrier;

	


public class TaskSpecificBarrier extends CyclicBarrier {

	private Scheduler scheduler;
	
	TaskSpecificBarrier(int noOfParities,Scheduler scheduler){
		super(noOfParities);
		this.scheduler=scheduler;
	}
	
	
	public void doWhenAllAtBarrier() {

	}
	
	public void useCPUCycles(int threadId) {
		if (threadId!=-1){
			
		}
	}
	
	
	// TODO Auto-generated method stub
	
	
}
