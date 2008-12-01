package playground.wrashid.DES;

public abstract class SimUnit {

	protected Scheduler scheduler = null;

	public SimUnit(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public void sendMessage(Message m, SimUnit targetUnit,
			double messageArrivalTime) {
		m.setSendingUnit(this);
		m.setReceivingUnit(targetUnit);
		m.setMessageArrivalTime(messageArrivalTime);
		scheduler.schedule(m);
	}

	// this procedure is invoked at the end of the simulation
	public abstract void finalize();

	public Scheduler getScheduler() {
		return scheduler;
	}
}
