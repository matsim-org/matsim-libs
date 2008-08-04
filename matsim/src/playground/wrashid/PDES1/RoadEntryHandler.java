package playground.wrashid.PDES1;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.LinkedList;
import org.matsim.network.Link;

public class RoadEntryHandler {

	private Road belongsToRoad=null;
	private int numberOfIncomingRoads=0;
	private LinkedList<Road> pendingMessagesFromRoads=new LinkedList<Road>();
	private PriorityQueue<Message> messageQueue=new PriorityQueue<Message>();
	
	RoadEntryHandler(Road belongsToRoad){
		this.belongsToRoad = belongsToRoad;
		this.numberOfIncomingRoads = belongsToRoad.getLink().getFromNode().getInLinks().size();
	}
	
	public void registerNullMessage(NullMessage nm){
		// assumption: only one thread from each road can enter here
		
			synchronized (pendingMessagesFromRoads){
				if (!pendingMessagesFromRoads.contains(nm.sendingUnit)){
					pendingMessagesFromRoads.add((Road)nm.sendingUnit);
				}	
				
				
				messageQueue.add(nm);
				
				processMessageQueue();
			}
		
		
	}
	
	public void registerEnterRequestMessage(Road fromRoad, Vehicle vehicle, double simTime){
		synchronized (pendingMessagesFromRoads){
			if (!pendingMessagesFromRoads.contains(fromRoad) && fromRoad!=belongsToRoad){
				pendingMessagesFromRoads.add(fromRoad);
			}
			
			
			
			EnterRequestMessage erm=MessageFactory.getEnterRequestMessage(vehicle);
			erm.sendingUnit = fromRoad;
			erm.receivingUnit=belongsToRoad;
			erm.messageArrivalTime=simTime;
			messageQueue.add(erm);
			
			processMessageQueue();
		}
		
		// send a null message to all other roads (going out of the same conjunction)
		for (Link otherLinks:fromRoad.getLink().getToNode().getOutLinks().values()){
			Road otherRoad=Road.allRoads.get(otherLinks.getId().toString());
			if (otherRoad!=belongsToRoad){
				NullMessage.scheduleNullMessage(otherRoad,simTime + belongsToRoad.inverseOutFlowCapacity - SimulationParameters.delta);
			}
		}
	}
	
	private void processMessageQueue(){
		while (pendingMessagesFromRoads.size()==this.numberOfIncomingRoads){
			// as long as all roads have messages on them, take the least message 
			Message m=messageQueue.poll();
			
			// if there is no other message with the same sendingUnit, remove the sendingUnit from pendingMessages from Road
			boolean hasNoMoreMessagesFromSameSource=true;
			Iterator<Message> iter=messageQueue.iterator();
			
			while (iter.hasNext()){
				Message message=iter.next();
				if (message.sendingUnit==m.sendingUnit){
					hasNoMoreMessagesFromSameSource=false;
				}
			}
			
			if (hasNoMoreMessagesFromSameSource){
				pendingMessagesFromRoads.remove(m.sendingUnit);
			}
			
			belongsToRoad.scheduler.schedule(m);
		}
	}
	
	
}
