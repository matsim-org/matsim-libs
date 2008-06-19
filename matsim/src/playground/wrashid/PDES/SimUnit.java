package playground.wrashid.PDES;




public abstract class SimUnit {
	
	static int unitCounter=0;
	String label;
	
	long unitNo;

	int messageType=0;
	
	public SimUnit(){
		initializeObject();
	}
	
	public abstract void initialize();
	
	public abstract void handleMessage(Message m);
	
	public void sendMessage(Message m, long targetUnitId, long messageArrivalTime){
		m.sendingUnit=this;
		
		SimUnit targetUnit=(SimUnit) Scheduler.getSimUnit(targetUnitId);
		m.receivingUnit=targetUnit;
		
		m.setMessageArrivalTime(messageArrivalTime);
		Scheduler.schedule(m);
	}
	
	
	// Message m can be a new message, 
	// and its fields do not need to be initialized.
	public void setTimer(long timeOut, Message m){
		
		m.setMessageArrivalTime(Scheduler.simTime+timeOut);
		m.sendingUnit=this;
		m.receivingUnit=this;
		Scheduler.schedule(m);
	}
	
	
	// initializationMethod
	private void initializeObject(){
		label="["+ this.getClass().getSimpleName() + " " + unitCounter + "]";
		unitNo=unitCounter;
		Scheduler.register(this);
		unitCounter++;
	}
	
	// this procedure is invoked at the end of the simulation
	public abstract void finalize();
	
	public void unregisterFromEventHeap(){
		Scheduler.unregister(this);
	}

	public long getUnitNo() {
		return unitNo;
	}
}
