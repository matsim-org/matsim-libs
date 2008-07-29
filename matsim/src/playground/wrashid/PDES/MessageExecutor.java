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
		while (true){
			//System.out.println("start me" + id);
			synchronized (this){
				try {
					lock2.lock();
					mayStart.await();
					//lock.unlock();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//System.out.println("continue me" + id);
			
			if (message.firstLock!=null){
				synchronized (message.firstLock){
					lock1.lock();
					hasAcquiredLock.signal();
					lock1.unlock();
					//hasAqiredLocks=true;
					executeMessage();
					message.firstLock.notify(); // TODO: tidy up this code logic...
				}
			} else {
				lock1.lock();
				hasAcquiredLock.signal();
				lock1.unlock();
				executeMessage();
			}
			scheduler.queueMessageExecutor(this);
			
			//System.out.println("end me" + id);
		}
		
		/*
		if (message.firstLock!=null){
			synchronized (message.firstLock){
				hasAqiredLocks=true;
				executeMessage();
			}
		} else {
			hasAqiredLocks=true;
			executeMessage();
		}
		*/
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
