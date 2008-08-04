package playground.wrashid.PDES1;

import org.matsim.network.Link;

public class NullMessage extends SelfhandleMessage {

	@Override
	public void printMessageLogString() {
		// TODO Auto-generated method stub
		
	}

	public NullMessage(){
		super();
	}

	@Override
	public void selfhandleMessage() {
		// send a null message to 
		Road currentRoad = (Road) receivingUnit;
		for (Link outLink:currentRoad.getLink().getToNode().getOutLinks().values()){
			Road outRoad=Road.allRoads.get(outLink.getId().toString());
			
			NullMessage nm=new NullMessage();
			nm.sendingUnit=currentRoad;
			nm.receivingUnit=outRoad;
			nm.messageArrivalTime=messageArrivalTime+currentRoad.linkTravelTime-SimulationParameters.delta;
			outRoad.roadEntryHandler.registerNullMessage(nm);
		}
		
	}
	
	public static void scheduleNullMessage(Road receivingRoad, double simTime){
		NullMessage nm=new NullMessage();
		nm.sendingUnit=null;
		nm.receivingUnit=receivingRoad;
		nm.messageArrivalTime=simTime;
		receivingRoad.scheduler.schedule(nm);
	}

	@Override
	public void recycleMessage() {
		MessageFactory.disposeNullMessage(this);
		
	}
	
	
	
}
