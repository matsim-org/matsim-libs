package playground.wrashid.PDES;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MessageExecutor extends Thread {
	int id=0;
	private Message message;
	private Scheduler scheduler;
	//public volatile boolean hasAqiredLocks=false;
	public Lock lock1=new ReentrantLock();
	public Lock lock2=new ReentrantLock();
	public Condition hasAcquiredLock=lock1.newCondition();
	public Condition mayStart=lock2.newCondition();

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
		this.id=id;
	}

	public void run() {
		MessageExecutor.setSimTime(0);
		
		// of course here is a problem:
		// we first check if empty and then try to get the element. For this reason
		// several threads may try to get the same message
		// TODO: in order to improve this, we could leave out this check and instead make a try/catch
		// This would also improve performance.
		
		try{
			while (true || getSimTime()<SimulationParameters.maxSimulationLength){
				/*
				while (scheduler.queue.getCounter()%(this.id+1)!=0 && !scheduler.queue.isEmpty()){
					try {
						Thread.currentThread().sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				*/
				
				message=scheduler.queue.getNextMessage();
				if (message==null){
					break;
				}
				synchronized (scheduler){
					//synchronized (message.firstLock){
					//	synchronized (message.secondLock){
							executeMessage();
					//	}
					//}
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
		scheduler.timer.printMeasuredTime("ThreadId-"+id + ": ");
	}

	private void executeMessage(){
		MessageExecutor.setSimTime(message.getMessageArrivalTime());
		message.printMessageLogString();
		if (message instanceof SelfhandleMessage){
			((SelfhandleMessage) message).selfhandleMessage();
		} else {
			message.receivingUnit.handleMessage(message);
		}
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

}
