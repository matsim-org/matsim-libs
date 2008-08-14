package playground.wrashid.PDES2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.LinkedList;
import java.util.concurrent.PriorityBlockingQueue;

import org.matsim.network.Link;

import playground.wrashid.DES.utils.Timer;

public class RoadEntryHandler {

	private Road belongsToRoad=null;
	
	RoadEntryHandler(Road belongsToRoad){
		this.belongsToRoad = belongsToRoad;
	}
	
	public void registerEnterRequestMessage(Road fromRoad, Vehicle vehicle, double simTime){
			EnterRequestMessage erm=MessageFactory.getEnterRequestMessage(vehicle);
			erm.sendingUnit = fromRoad;
			erm.receivingUnit=belongsToRoad;
			erm.messageArrivalTime=simTime;
			if (fromRoad.getZoneId()!=belongsToRoad.getZoneId()){
				erm.isAcrossBorderMessage=true;
				System.out.println(fromRoad.getLink().getId() +" - " + belongsToRoad.getLink().getId() +" - " + vehicle.getOwnerPerson().getId());
			}
			
			belongsToRoad.scheduler.schedule(erm);
		
		
	}
}
