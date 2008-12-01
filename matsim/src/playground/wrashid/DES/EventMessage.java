package playground.wrashid.DES;

public abstract class EventMessage extends Message {
	protected Vehicle vehicle;
	protected Scheduler scheduler;

	public EventMessage(Scheduler scheduler, Vehicle vehicle) {
		super();
		this.vehicle = vehicle;
		this.scheduler = scheduler;
	}

	public void resetMessage(Scheduler scheduler, Vehicle vehicle) {
		this.scheduler = scheduler;
		this.vehicle = vehicle;
	}

}
