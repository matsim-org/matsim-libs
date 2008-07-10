package playground.wrashid.DES;

public abstract class SelfhandleMessage extends Message {

	public abstract void selfhandleMessage();
	
	public void sendMessage(Scheduler scheduler,Message m, long targetUnitId, double messageArrivalTime){
		m.sendingUnit=null;
		
		SimUnit targetUnit=(SimUnit) scheduler.getSimUnit(targetUnitId);
		m.receivingUnit=targetUnit;
		
		m.setMessageArrivalTime(messageArrivalTime);
		scheduler.schedule(m);
	}

}
