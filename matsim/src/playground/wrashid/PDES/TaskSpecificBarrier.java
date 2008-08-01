package playground.wrashid.PDES;

import playground.wrashid.PDES.util.CyclicBarrier;

	


public class TaskSpecificBarrier extends CyclicBarrier {

	private Scheduler scheduler;
	
	TaskSpecificBarrier(int noOfParities,Scheduler scheduler){
		super(noOfParities);
		this.scheduler=scheduler;
	}
	
	
	public void doWhenAllAtBarrier() {
		//for (int i=1;i<SimulationParameters.numberOfMessageExecutorThreads+1;i++){
		//	if (scheduler.threadMessageQueues[i-1].getArrivalTimeOfNextMessage()<scheduler.timeOfNextBarrier)
		//		scheduler.timeOfNextBarrier=scheduler.threadMessageQueues[i-1].getArrivalTimeOfNextMessage();
		//}
		//scheduler.timeOfNextBarrier+=scheduler.barrierDelta;
	}
	
	public void useCPUCycles(int threadId) {
		if (threadId!=-1){
			scheduler.threadMessageQueues[threadId-1].emptyBuffers();
		}
	}
	
	
	// TODO Auto-generated method stub
	
	
}
