package playground.wrashid.PDES;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.matsim.gbl.Gbl;

public class Scheduler {
	private double simTime=0;
	public MessageQueue queue=new MessageQueue();
	HashMap<Long,SimUnit> simUnits=new HashMap<Long, SimUnit>();
	ConcurrentLinkedQueue<MessageExecutor> messageExecutors= new ConcurrentLinkedQueue<MessageExecutor>();
	public Lock schedulerLock=new ReentrantLock();
	public Condition emtpyMessageExecutorQueue=schedulerLock.newCondition();
	
	
	public void schedule(Message m){		
		if (m.getMessageArrivalTime()>=simTime){	
			queue.putMessage(m);
		} else {
			System.out.println("WARNING: You tried to send a message in the past. Message discarded.");
			//System.out.println("m.getMessageArrivalTime():"+m.getMessageArrivalTime());
			//System.out.println("simTime:"+simTime);
			//System.out.println(m.getClass());
			assert(false); // for backtracing, where a wrong message has been scheduled
		}
	}
	
	public void unschedule(Message m){
		queue.removeMessage(m);
	}
	
	
	public void startSimulation(){
		
		long simulationStart=System.currentTimeMillis();
		double hourlyLogTime=3600;
		
		initializeSimulation();
		
	}
	
	
	public void register(SimUnit su){
		simUnits.put(new Long(su.unitNo), su);
	}
	
	public Object getSimUnit(long unitId){
		return simUnits.get(new Long(unitId));
	}
	
	
	// attention: this procedure only invokes
	// the initialization method of objects, which
	// exist at the beginning of the simulation
	public void initializeSimulation(){
		
		
		
		
		Object[] objects=simUnits.values().toArray();
		SimUnit su;
		
		for (int i=0;i<objects.length;i++){
			su=(SimUnit) objects[i];
			su.initialize();
		}
		
		
		// create message executors and start them
		for (int i=0;i<Runtime.getRuntime().availableProcessors();i++){
			MessageExecutor me= new MessageExecutor (i);
			me.setDaemon(false);
			me.setScheduler(this);
			me.start();
		}
	}


	//public double getSimTime() {
	//	return simTime;
	//}


	public void unregister(SimUnit unit) {
		simUnits.remove(new Long(unit.unitNo));
	}
	
	public void queueMessageExecutor(MessageExecutor me){
		messageExecutors.add(me);
		//schedulerLock.lock();
		//emtpyMessageExecutorQueue.signal();
		//schedulerLock.unlock();
	}
}
