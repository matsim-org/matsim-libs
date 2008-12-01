package playground.wrashid.DES;

public abstract class SelfhandleMessage extends Message {

	public abstract void selfhandleMessage();
	
	public void sendMessage(Scheduler scheduler,Message m, long targetUnitId, double messageArrivalTime){
		m.setSendingUnit(null);
		
		SimUnit targetUnit=(SimUnit) scheduler.getSimUnit(targetUnitId);
		m.setReceivingUnit(targetUnit);
		
		m.setMessageArrivalTime(messageArrivalTime);
		scheduler.schedule(m);
	}

}
