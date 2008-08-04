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
			assert(messageArrivalTime>=0);
			Road outRoad=Road.allRoads.get(outLink.getId().toString());
			
			NullMessage nm=MessageFactory.getNullMessage();
			nm.sendingUnit=currentRoad;
			nm.receivingUnit=outRoad;
			nm.messageArrivalTime=messageArrivalTime+currentRoad.linkTravelTime-SimulationParameters.delta;
			if (nm.messageArrivalTime<0){
				System.out.println();
			}
			assert(nm.messageArrivalTime>=0);
			outRoad.roadEntryHandler.registerNullMessage(nm);
		}
		
	}
	
	public static void scheduleNullMessage(Road receivingRoad, double simTime){
		NullMessage nm=MessageFactory.getNullMessage();
		nm.sendingUnit=null;
		nm.receivingUnit=receivingRoad;
		nm.messageArrivalTime=simTime;
		assert(nm.messageArrivalTime>=0):"negative simTime...";
		receivingRoad.scheduler.schedule(nm);
	}

	@Override
	public void recycleMessage() {
		MessageFactory.disposeNullMessage(this);
		
	}
	
	
	
}
