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
		timer.startTimer();
		long simulationStart=System.currentTimeMillis();
		double hourlyLogTime=3600;
		
		initializeSimulation();
		
		try {
			Thread.currentThread().sleep(100000);
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
		
		
		
		
		Object[] objects=simUnits.toArray();
		SimUnit su;
		
		for (int i=0;i<objects.length;i++){
			su=(SimUnit) objects[i];
			su.initialize();
		}
		
		
		// create message executors and start them
		for (int i=0;i<Runtime.getRuntime().availableProcessors()+10;i++){
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
		simUnits.remove(unit.unitNo);
	}
	
}
