package playground.wrashid.PDES1;

public abstract class SelfhandleMessage extends Message {

	public abstract void selfhandleMessage();
	/*
	public void sendMessage(Scheduler scheduler,Message m, SimUnit targetUnit, double messageArrivalTime){
		m.sendingUnit=null;
		
		m.receivingUnit=targetUnit;
		
		m.setMessageArrivalTime(messageArrivalTime);
		scheduler.schedule(m);
	}
	*/

}
