package playground.wrashid.DES;




public abstract class SimUnit {
	
	static int unitCounter=0;
	Scheduler scheduler=null;
	String label;
	
	long unitNo;

	int messageType=0;
	
	// every sim unit should invoke this method, before doing anything else
	public SimUnit(Scheduler scheduler){
		this.scheduler=scheduler;
		initializeObject();
	}
	
	public abstract void initialize();
	
	public abstract void handleMessage(Message m);
	
	public void sendMessage(Message m, long targetUnitId, double messageArrivalTime){
		m.sendingUnit=this;
		
		SimUnit targetUnit=(SimUnit) scheduler.getSimUnit(targetUnitId);
		m.receivingUnit=targetUnit;
		
		m.setMessageArrivalTime(messageArrivalTime);
		scheduler.schedule(m);
	}
	
	
	// Message m can be a new message, 
	// and its fields do not need to be initialized.
	public void setTimer(double timeOut, Message m){
		
		m.setMessageArrivalTime(scheduler.simTime+timeOut);
		m.sendingUnit=this;
		m.receivingUnit=this;
		scheduler.schedule(m);
	}
	
	
	// initializationMethod
	private void initializeObject(){
		label="["+ this.getClass().getSimpleName() + " " + unitCounter + "]";
		unitNo=unitCounter;
		scheduler.register(this);
		unitCounter++;
	}
	
	// this procedure is invoked at the end of the simulation
	public abstract void finalize();
	
	public void unregisterFromEventHeap(){
		scheduler.unregister(this);
	}

	public long getUnitNo() {
		return unitNo;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
}
