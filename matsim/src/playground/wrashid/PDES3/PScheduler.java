package playground.wrashid.PDES3;

import playground.wrashid.DES.Message;
import playground.wrashid.DES.Scheduler;
import playground.wrashid.PDES2.MessageExecutor;


public class PScheduler extends Scheduler {

	public ZoneMessageQueue[] zoneMessageQueues=new ZoneMessageQueue[SimParametersParallel.numberOfZones];
	public MessageExecutor[] messageExecutors=new MessageExecutor[SimParametersParallel.numberOfMessageExecutorThreads];
	
	public void schedule(Message m, int zoneId, boolean ownsZone) {
		// if current thread owns zone, then
		if (ownsZone){
			zoneMessageQueues[zoneId].putMessage(m);
		} else {
			//only messages are buffered, such as legEnd for mode!=car
			zoneMessageQueues[zoneId].bufferMessage(m);
		}
	}

	public void unschedule(Message m, int zoneId, boolean ownsZone) {
		if (ownsZone){
			zoneMessageQueues[zoneId].removeMessage(m);
		} else {
			//only messages are buffered, such as legEnd for mode!=car
			zoneMessageQueues[zoneId].addDeleteBufferMessage(m);
		}
	}

	// only makes sense at the moment, when the owner thread of the zone invokes this
	// especially, when one thread owns several zones
	public Message getNextMessage(int zoneId) {
		return zoneMessageQueues[zoneId].getNextMessage();
	}
	
	// TODO: make start scheduler and init scheduler method here
	public void startSimulation(){
		initializeSimulation();
	}

	private void initializeSimulation() {
		// TODO Auto-generated method stub
		
	}	

}
