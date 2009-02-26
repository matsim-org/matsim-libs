package playground.wrashid.PDES2;

import org.matsim.interfaces.core.v01.Link;

public class ZoneBorderMessage extends SelfhandleMessage {

	String debugString="";
	
	@Override
	public void printMessageLogString() {
		// TODO Auto-generated method stub
		
	}

	public ZoneBorderMessage(){
		super();
		priority=-1; // ZoneBorderMessages have the least priority, because for the same time stamp they
		             // should be processed last
	}

	@Override
	public void selfhandleMessage() {
		
	}
	
	public static void initialNullMessage(Road receivingRoad, double simTime){
		ZoneBorderMessage nm=MessageFactory.getZoneBorderMessage();
		nm.sendingUnit=null;
		nm.receivingUnit=receivingRoad;
		nm.messageArrivalTime=simTime;
		assert(nm.messageArrivalTime>=0):"negative simTime...";
		
		//Do not schedule the message, but rather immediately invoke the selfhandler.
		// Reason: A race condition can occur: The initialNullMessage from A to B can be handled later
		// than a message from X to A to B. The main reason is the scheduling here.
		//receivingRoad.scheduler.schedule(nm);
		nm.selfhandleMessage();
	}

	
}
