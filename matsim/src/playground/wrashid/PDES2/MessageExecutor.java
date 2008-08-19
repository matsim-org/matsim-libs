package playground.wrashid.PDES2;

import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MessageExecutor extends Thread {
	private int threadId=0;
	private Message message;
	private Scheduler scheduler;
	//public volatile boolean hasAqiredLocks=false;
	public Lock lock1=new ReentrantLock();
	public Lock lock2=new ReentrantLock();
	public Condition hasAcquiredLock=lock1.newCondition();
	public Condition mayStart=lock2.newCondition();
	public volatile boolean isAlive=true;
	
	private static ThreadLocal tId = new ThreadLocal();

    public static int getThreadId(){
        return  ((Integer) (tId.get())).intValue();
        
    }

    public static void setThreadId(int obj) {
    	tId.set(obj);
    }

    private static ThreadLocal simTime = new ThreadLocal();

    public static double getSimTime() {
        return ((Double) (simTime.get())).doubleValue();
    }

    public static void setSimTime(double obj) {
    	simTime.set(obj);
    }

   

	public MessageExecutor(Message message){
		this.message=message;
	}
	
	public MessageExecutor(int id){
		threadId=id;
	}

	public void run() {
		MessageExecutor.setSimTime(0);
		MessageExecutor.setThreadId(threadId);
		// of course here is a problem:
		// we first check if empty and then try to get the element. For this reason
		// several threads may try to get the same message
		// TODO: in order to improve this, we could leave out this check and instead make a try/catch
		// This would also improve performance.
		
		
		double arrivalTimeOfLastProcessedMessage=0;
		Random r=new Random();
		int i=threadId;
		//Message nullMessage=new NullMessage();
		//nullMessage.firstLock=nullMessage; // TODO: remove this nonsense (just needed for assertion problem)
		
		//nullMessage.messageArrivalTime=scheduler.timeOfNextBarrier;
		//scheduler.threadMessageQueues[threadId-1].putMessage(nullMessage);
		int barrierRound=0;
		int unsuccessful=0;
		try{
			while (getSimTime()<SimulationParameters.maxSimulationLength){
				
				
				
				if (scheduler.simulationTerminated){
					break;
				}
				/*
				//for (int i=0;i<SimulationParameters.numberOfZones;i++){
				int i=r.nextInt(SimulationParameters.numberOfZones);
					if (scheduler.zoneMessageQueues[i].lock.tryLock()){
						message=scheduler.getNextMessage(i);
						while (message!=null){
							executeMessage();
							message=scheduler.getNextMessage(i);
						}
						scheduler.zoneMessageQueues[i].lock.unlock();
					}
					//scheduler.zoneMessageQueues[i].printSize();
					//System.out.println(threadId);
				//}
					*/
				
					message=scheduler.getNextMessage(i);
					while (message!=null){
						executeMessage();
						message=scheduler.getNextMessage(i);
						//System.out.println(".");
						unsuccessful=0;
					}
					//System.out.println("-");
					unsuccessful++;
					if (unsuccessful==100000){
						System.out.println(unsuccessful);
						//Thread.sleep(100);
					}
					
				
			}
		} catch (java.lang.NullPointerException npe){
			// ignore this exception, because it comes from the fact, that we do not check 'scheduler.queue.isEmpty()'
			npe.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		} finally {
		}
		
		scheduler.timer.endTimer();
		scheduler.timer.printMeasuredTime("ThreadId-"+tId + ": ");
		scheduler.decrementNoOfAliveThreads();
		isAlive=false;
	}

	private void executeMessage(){
		MessageExecutor.setSimTime(message.getMessageArrivalTime());
		message.printMessageLogString();
		if (message instanceof SelfhandleMessage){
			((SelfhandleMessage) message).selfhandleMessage();
		} else {
			message.receivingUnit.handleMessage(message);
		}
		message.recycleMessage();
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
	
	

}
