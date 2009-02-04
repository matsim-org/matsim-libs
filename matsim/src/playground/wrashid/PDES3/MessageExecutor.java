package playground.wrashid.PDES3;

import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.matsim.mobsim.deqsim.Message;
import org.matsim.mobsim.deqsim.SimulationParameters;

import playground.wrashid.PDES2.SelfhandleMessage;

public class MessageExecutor extends Thread {
	private int threadId = 0;
	private PScheduler scheduler;
	private Message message = null;

	private static ThreadLocal tId = new ThreadLocal();

	public static int getThreadId() {
		return ((Integer) (tId.get())).intValue();

	}

	public static void setThreadId(int obj) {
		tId.set(obj);
	}

	public MessageExecutor(int id) {
		threadId = id;
	}

	public void run() {
		// MessageExecutor.setSimTime(0);
		MessageExecutor.setThreadId(threadId);

		double arrivalTimeOfLastProcessedMessage = 0;

		int i = threadId;
		double simTime = 0;
		try {
			while (true) {

				message = scheduler.getNextMessage(i);
				message.processEvent();
				message.handleMessage();

				if (message.getMessageArrivalTime() > SimulationParameters.maxSimulationLength) {
					break;
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			
		}

	}


}
