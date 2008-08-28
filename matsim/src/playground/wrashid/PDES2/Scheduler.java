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
		zoneMessageQueues[((Road)m.receivingUnit).getZoneId()].putMessage(m);
	}
	
	public void unschedule(Message m){
		zoneMessageQueues[((Road)m.receivingUnit).getZoneId()].removeMessage(m);
	}
	
	public Message getNextMessage(int threadId){
		
		return zoneMessageQueues[threadId].getNextMessage();
	}
	
	
	public void startSimulation(){
		timer.startTimer();
		long simulationStart=System.currentTimeMillis();
		double hourlyLogTime=3600;
		
		initializeSimulation();
		
		//System.out.println("minInverseOutflowCapacity: "+minInverseInOutflowCapacity);
		
		
		try {
			Thread.currentThread().sleep(2000);
			
			
			
			
			while (true){
				boolean allDead=true;
				for (int i=0;i<SimulationParameters.numberOfMessageExecutorThreads;i++){
					if (!zoneMessageQueues[i].isEmpty()){
						allDead=false;
						break;
					}
				}
				if (!allDead){
	
					//Gbl.printMemoryUsage();
				
					SimulationParameters.processEventBuffer();
					Thread.currentThread().sleep(1000);
				} else {
					simulationTerminated=true;
					SimulationParameters.eventBuffer.flushAllInputBuffers();
					SimulationParameters.processEventBuffer();
					Thread.currentThread().sleep(2000);
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
		
		
		
		//System.out.println("XMedian:"+SimulationParameters.sumXCoordinate/SimulationParameters.noOfCars);
		//System.out.println("XMedianLeft:"+SimulationParameters.sumXCoordinateLeft/SimulationParameters.noOfCarsLeft);
		//System.out.println("XMedianRight:"+SimulationParameters.sumXCoordinateRight/SimulationParameters.noOfCarsRight);
		//System.out.println("SimulationParameters.test_timer: "+ SimulationParameters.test_timer);
	
		for (int i=0;i<SimulationParameters.numberOfZones;i++){
			System.out.println(i+"-th zone incounter: "+zoneMessageQueues[i].incounter);
			System.out.println(i+"-th zone temp_waitingOnOtherZone: "+zoneMessageQueues[i].temp_waitingOnOtherZone);
			System.out.println(i+"-test_CompareCounter: "+MessageFactory.test_CompareCounter[i]);		
		}
		
		SimulationParameters.events.printEventsCount();
	
	
	
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
		
		
		
		
		
		
		
		
		
		// schedule a message in future eternity on all roads
		for (Road road:Road.allRoads.values()){
			if (road.isOutBorderRoad){
				road.lookahead.add(road.getTimerMessage(Double.MAX_VALUE));
				road.scheduleZoneBorderMessage(road.lookahead.peek());
			}
			//road.scheduleInitialZoneBorderMessage();
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
		
		// initialize numberOfIncomingLinks and numberOfQueuedMessages
		for (Road road:Road.allRoads.values()){
			for (Link inLink: road.getLink().getFromNode().getInLinks().values()){
				Road inRoad=Road.allRoads.get(inLink.getId().toString());
				if (inRoad.getZoneId()!=road.getZoneId()){
					if (!zoneMessageQueues[road.getZoneId()].tempIncomingLinks.contains(inLink)){
						zoneMessageQueues[road.getZoneId()].tempIncomingLinks.add(inLink);
						inRoad.isOutBorderRoad=true;
						zoneMessageQueues[road.getZoneId()].numberOfQueuedMessages[inRoad.getZoneId()].put(inRoad, 0);
						//System.out.println(inRoad.getLink().getLength());
					}
				}
			}
		}
		
		for (int i=0;i<SimulationParameters.numberOfZones;i++){
			zoneMessageQueues[i].numberOfIncomingLinks=zoneMessageQueues[i].tempIncomingLinks.size();
			zoneMessageQueues[i].tempIncomingLinks=null;
			System.out.println(i+"-zoneMessageQueues.numberOfIncomingLinks:" + zoneMessageQueues[i].numberOfIncomingLinks);
		}
		
		
		
	}
	
}
