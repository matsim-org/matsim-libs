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
			while (getSimTime()<SimulationParameters.maxSimulationLength){
				System.out.println("id:" + id + " - " + scheduler.queue.queue1.size());
				message=scheduler.queue.getNextMessage();
				System.out.println("id:" + id + " - " + scheduler.queue.queue1.size());
				
				if (message.firstLock!=null){
					synchronized (message.firstLock){
						System.out.println("id:" + id + " - " + "executeMessage");
						executeMessage();
					}
				} else {
					System.out.println("id:" + id + " - " + "ERROR");
				}
				System.out.println("id:" + id + " - " + "getSimTime()"+getSimTime());
			}
		} catch (java.lang.NullPointerException npe){
			// ignore, because it comes from the fact, that we do not check 'scheduler.queue.hasElement()'
			npe.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		} finally {
			System.out.println("id:" + id + " - " + "sdfasf");
		}
		System.out.println("FFFid:" + id + " - " + scheduler.queue.queue1.size());
	}

	private void executeMessage(){
		MessageExecutor.setSimTime(message.getMessageArrivalTime());
		message.printMessageLogString();
		if (message instanceof SelfhandleMessage){
			System.out.println("id:" + id + " - " + "selfhandleMessage");
			((SelfhandleMessage) message).selfhandleMessage();
		} else {
			System.out.println("id:" + id + " - " + "recevingUnit handle");
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
