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

import playground.wrashid.DES.utils.Timer;

public class Scheduler {
	private double simTime=0;
	public MessageQueue queue=new MessageQueue();
	LinkedList<SimUnit> simUnits=new LinkedList<SimUnit>();
	Timer timer=new Timer();
	Lock lock=new ReentrantLock();
	private volatile int noOfAliveThreads=0;
	public MessageQueue[] threadMessageQueues=new MessageQueue[SimulationParameters.numberOfMessageExecutorThreads];
	
	// actually this is not the right one, because it could be, that the min outflow cap is on the same
	// thread (adjacent links)
	public double[] minInverseOutflowCapacities=new double[SimulationParameters.numberOfMessageExecutorThreads];
	
	public void schedule(Message m){		
		if (m.getMessageArrivalTime()>=simTime){	
			threadMessageQueues[((Road)m.receivingUnit).getBelongsToMessageExecutorThreadId()-1].putMessage(m);
		} else {
			System.out.println("WARNING: You tried to send a message in the past. Message discarded.");
			//System.out.println("m.getMessageArrivalTime():"+m.getMessageArrivalTime());
			//System.out.println("simTime:"+simTime);
			//System.out.println(m.getClass());
			assert(false); // for backtracing, where a wrong message has been scheduled
		}
		
		
		
		
	}
	
	public void unschedule(Message m){
		threadMessageQueues[((Road)m.receivingUnit).getBelongsToMessageExecutorThreadId()-1].removeMessage(m);
	}
	
	public Message getNextMessage(int threadId){
		return threadMessageQueues[threadId-1].getNextMessage();
	}
	
	
	public void startSimulation(){
		timer.startTimer();
		long simulationStart=System.currentTimeMillis();
		double hourlyLogTime=3600;
		
		initializeSimulation();
		
		try {
			while (noOfAliveThreads>0){
				Thread.currentThread().sleep(10000);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void register(SimUnit su){
		simUnits.add(su);
	}
		
	
	// attention: this procedure only invokes
	// the initialization method of objects, which
	// exist at the beginning of the simulation
	public void initializeSimulation(){
		
		// initialize MessageQueue array
		for (int i=1;i<SimulationParameters.numberOfMessageExecutorThreads+1;i++){
			threadMessageQueues[i-1]=new MessageQueue();
		}
		
		
		Object[] objects=simUnits.toArray();
		SimUnit su;
		
		for (int i=0;i<objects.length;i++){
			su=(SimUnit) objects[i];
			su.initialize();
		}
		
		
		
		// create message executors and start them (precondition: all sim units need to be initialized at this point)
		for (int i=1;i<SimulationParameters.numberOfMessageExecutorThreads+1;i++){
			MessageExecutor me= new MessageExecutor (i);
			me.setDaemon(false);
			me.setScheduler(this);
			me.start();
		}
		noOfAliveThreads=SimulationParameters.numberOfMessageExecutorThreads;
		
		
		
		
	}


	//public double getSimTime() {
	//	return simTime;
	//}


	public void unregister(SimUnit unit) {
		simUnits.remove(unit.unitNo);
	}
	
	synchronized void decrementNoOfAliveThreads(){
		noOfAliveThreads--;
	}
	
}
