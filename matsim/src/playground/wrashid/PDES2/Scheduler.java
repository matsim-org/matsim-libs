package playground.wrashid.PDES2;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import playground.wrashid.PDES.util.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.matsim.network.Link;

import org.matsim.gbl.Gbl;

import playground.wrashid.DES.utils.Timer;

public class Scheduler {
	private double simTime=0;
	//public MessageQueue queue=new MessageQueue();
	LinkedList<SimUnit> simUnits=new LinkedList<SimUnit>();
	Timer timer=new Timer();
	Lock lock=new ReentrantLock();
	private volatile int noOfAliveThreads=0;
	public ZoneMessageQueue[] zoneMessageQueues=new ZoneMessageQueue[SimulationParameters.numberOfZones];
	public MessageExecutor[] messageExecutors=new MessageExecutor[SimulationParameters.numberOfMessageExecutorThreads];
	//public TaskSpecificBarrier barrier=new TaskSpecificBarrier(SimulationParameters.numberOfMessageExecutorThreads,this);
	volatile public boolean simulationTerminated=false;
	// actually this is not the right one, because it could be, that the min outflow cap is on the same
	// thread (adjacent links)
	//public double minInverseInOutflowCapacity=Double.MAX_VALUE;
	//public double barrierDelta=0;
	//volatile public double timeOfNextBarrier=0;
	
	public void schedule(Message m){
			// TODO: find out the magic behind this number:
			// if the +xy value is small, then it produces an error, else not.
			// Problem: I need to keep this value small, because else there is no use
			// of the buffer
			// ERRRRRROR: It produces also an error for large +xy. => find out why
			// the error will disappear, if we do not use the buffer. Find out why...
			//if (timeOfNextBarrier>=m.messageArrivalTime){
			//	threadMessageQueues[((Road)m.receivingUnit).getBelongsToMessageExecutorThreadId()-1].putMessage(m);
			//} else {
			//	threadMessageQueues[((Road)m.receivingUnit).getBelongsToMessageExecutorThreadId()-1].addBuffer(m);
			//}
		
		/*
		if (Thread.currentThread() instanceof MessageExecutor){
			threadMessageQueues[((Road)m.receivingUnit).getBelongsToMessageExecutorThreadId()-1].addBuffer(m, MessageExecutor.getThreadId());
		} else {
			threadMessageQueues[((Road)m.receivingUnit).getBelongsToMessageExecutorThreadId()-1].addBuffer(m,1);
		}
		*/
		zoneMessageQueues[((Road)m.receivingUnit).getZoneId()].putMessage(m);
		
	}
	
	public void unschedule(Message m){
		//if (timeOfNextBarrier>=m.messageArrivalTime){
		//	threadMessageQueues[((Road)m.receivingUnit).getBelongsToMessageExecutorThreadId()-1].removeMessage(m);
		//} else {
		//	threadMessageQueues[((Road)m.receivingUnit).getBelongsToMessageExecutorThreadId()-1].deleteBuffer(m);
		//}
		
		
		//threadMessageQueues[((Road)m.receivingUnit).getBelongsToMessageExecutorThreadId()-1].removeMessage(m);
		zoneMessageQueues[((Road)m.receivingUnit).getZoneId()].deleteBuffer(m,MessageExecutor.getThreadId());
	}
	
	public Message getNextMessage(int threadId){
		return zoneMessageQueues[threadId-1].getNextMessage();
	}
	
	
	public void startSimulation(){
		timer.startTimer();
		long simulationStart=System.currentTimeMillis();
		double hourlyLogTime=3600;
		
		initializeSimulation();
		
		//System.out.println("minInverseOutflowCapacity: "+minInverseInOutflowCapacity);
		
		
		try {
			//Thread.currentThread().sleep(20000);
			
			
			
			
			while (true){
				boolean allEmpty=true;
				for (int i=0;i<SimulationParameters.numberOfMessageExecutorThreads;i++){
					if (!zoneMessageQueues[i].isEmpty() && messageExecutors[i].isAlive){
						allEmpty=false;
					}
				}
				if (!allEmpty){
	
					//Gbl.printMemoryUsage();
				
					SimulationParameters.processEventBuffer();
					Thread.currentThread().sleep(1000);
				} else {
					simulationTerminated=true;
					SimulationParameters.processEventBuffer();
					Thread.currentThread().sleep(1000);
					for (int i=0;i<SimulationParameters.numberOfMessageExecutorThreads;i++){
						messageExecutors[i].stop();
					}
					break;
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		System.out.println("XMedian:"+SimulationParameters.sumXCoordinate/SimulationParameters.noOfCars);
		System.out.println("XMedianLeft:"+SimulationParameters.sumXCoordinateLeft/SimulationParameters.noOfCarsLeft);
		System.out.println("XMedianRight:"+SimulationParameters.sumXCoordinateRight/SimulationParameters.noOfCarsRight);
		System.out.println("SimulationParameters.test_timer: "+ SimulationParameters.test_timer);
	}
	
	
	
	public void register(SimUnit su){
		simUnits.add(su);
	}
		
	
	// attention: this procedure only invokes
	// the initialization method of objects, which
	// exist at the beginning of the simulation
	public void initializeSimulation(){
		
		
		
		
		
		// initialize objects (and schedule start leg messages)
		Object[] objects=simUnits.toArray();
		SimUnit su;
		
		for (int i=0;i<objects.length;i++){
			su=(SimUnit) objects[i];
			su.initialize();
		}
		
		
		
		
		
		
		
		
		
		// schedule a null message on all roads
		// this initialization must be done before starting the messageExecutors,
		// because else a race condition occurs between the initial messages and their processing
		// TODO: but perhaps it creates some problem, because there are no message executors at this time
		// to schedule message into the right scheduler => or perpaps yes, because each road has an executor id assigned
		for (Road road:Road.allRoads.values()){
			ZoneBorderMessage.initialNullMessage(road, 0);
		}
		
		// empty all buffer queues
		for (int i=0;i<SimulationParameters.numberOfMessageExecutorThreads;i++){
			zoneMessageQueues[i].emptyBuffers();
		}
		
		
		
		
		
		
	
		
		// create message executors and start them (precondition: all sim units need to be initialized at this point)
		for (int i=0;i<SimulationParameters.numberOfMessageExecutorThreads;i++){
			MessageExecutor me= new MessageExecutor (i);
			messageExecutors[i]=me;
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
	
	// precondition: all roads initialized
	public void inititZoneMessageQueues(){
		// initialize MessageQueue array
		for (int i=0;i<SimulationParameters.numberOfZones;i++){
			zoneMessageQueues[i]=new ZoneMessageQueue(i);	
		}
		
		// init numberOfIncomingLinks
		for (Road road:Road.allRoads.values()){
			for (Link inLink: road.getLink().getFromNode().getInLinks().values()){
				Road inRoad=Road.allRoads.get(inLink.getId().toString());
				if (inRoad.getZoneId()!=road.getZoneId()){
					if (!zoneMessageQueues[road.getZoneId()].tempIncomingLinks.contains(inLink)){
						zoneMessageQueues[road.getZoneId()].tempIncomingLinks.add(inLink);
					}
				}
			}
		}
		
		for (int i=0;i<SimulationParameters.numberOfZones;i++){
			zoneMessageQueues[i].numberOfIncomingLinks=zoneMessageQueues[i].tempIncomingLinks.size();
			zoneMessageQueues[i].tempIncomingLinks=null;
		}
		
	}
	
}
