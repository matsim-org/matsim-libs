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
	private MessageQueue queue=new MessageQueue();
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
		
		Message m;
		//Executor executor = Executors.newFixedThreadPool(3);
		
		while(queue.hasElement() && simTime<SimulationParameters.maxSimulationLength){
			//System.out.println("hereS");
			m=queue.getNextMessage();
			
			while (messageExecutors.isEmpty()){
				//System.out.println("alarm...");
				/*
				try {
					schedulerLock.lock();
					System.out.println("here...");
					emtpyMessageExecutorQueue.awaitNanos(10); //a deadline must be set here, because a deadlock can occur
					schedulerLock.unlock();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
				*/
			}
			
			//System.out.println("kurz vor dem poll");
			MessageExecutor me=messageExecutors.poll();
			me.setMessage(m);
			me.setScheduler(this);
			
			
			me.lock1.lock();
			me.lock2.lock();
			me.mayStart.signal();
			
			try {
				//System.out.println("ich schlafe ein...");
				me.lock2.unlock();
				me.hasAcquiredLock.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println("ich wurde aufgeweckt");
			//me.lock.unlock();
			
			//System.out.println("restarting loop...");
			
			
			/*
			System.out.println("just before going to sleep");
			synchronized (this){
				try {
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			*/
			
			/*
			simTime=m.getMessageArrivalTime();
			m.printMessageLogString();
			if (m instanceof SelfhandleMessage){
				((SelfhandleMessage) m).selfhandleMessage();
			} else {
				m.receivingUnit.handleMessage(m);
			}
			*/
			
			// print output each hour
			/*
			if (simTime / hourlyLogTime > 1){
				hourlyLogTime = simTime + 3600;
				System.out.print("Simulation at " + simTime/3600 + "[h]; ");
				System.out.println("s/r:"+simTime/(System.currentTimeMillis()-simulationStart)*1000);
				Gbl.printMemoryUsage();
			}
			*/
			
			
			// debug - don't needed anymore remove after some time...
			/*
			if ((queue.counter % 100000 == 0)){
				System.out.println("s/r:"+simTime/(System.currentTimeMillis()-simulationStart)*1000);
				Gbl.printMemoryUsage();
			}
			*/
			
			
		}
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
		
		
		// create message executors and start them
		for (int i=0;i<Runtime.getRuntime().availableProcessors()-1;i++){
			MessageExecutor me= new MessageExecutor (i);
			me.start();
			messageExecutors.add(me);
		}
		
		Object[] objects=simUnits.values().toArray();
		SimUnit su;
		
		for (int i=0;i<objects.length;i++){
			su=(SimUnit) objects[i];
			su.initialize();
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
