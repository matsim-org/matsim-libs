package playground.wrashid.DES;

import java.util.HashMap;

import org.matsim.gbl.Gbl;

public class Scheduler {
	private double simTime = 0;
	private MessageQueue queue = new MessageQueue();

	public void schedule(Message m) {
		queue.putMessage(m);
	}

	public void unschedule(Message m) {
		queue.removeMessage(m);
	}

	public void startSimulation() {
		long simulationStart = System.currentTimeMillis();
		double hourlyLogTime = 3600;

		Message m;

		while (!queue.isEmpty()
				&& simTime < SimulationParameters.maxSimulationLength) {
			m = queue.getNextMessage();
			simTime = m.getMessageArrivalTime();
			m.processEvent();
			m.handleMessage();

			// print output each hour
			if (simTime / hourlyLogTime > 1) {
				hourlyLogTime = simTime + 3600;
				System.out.print("Simulation at " + simTime / 3600 + "[h]; ");
				System.out
						.println("s/r:"
								+ simTime
								/ (System.currentTimeMillis() - simulationStart)
								* 1000);
				Gbl.printMemoryUsage();
			}
		}
	}

	public double getSimTime() {
		return simTime;
	}

}
