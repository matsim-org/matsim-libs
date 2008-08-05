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
	private double test_timeOfLastScheduledMessage=0;
	
	RoadEntryHandler(Road belongsToRoad){
		this.belongsToRoad = belongsToRoad;
		this.numberOfIncomingRoads = belongsToRoad.getLink().getFromNode().getInLinks().size();
	}
	
	public void registerNullMessage(NullMessage nm){
		// assumption: only one thread from each road can enter here
		
		assert(nm.sendingUnit==null || belongsToRoad.getLink().getFromNode().getInLinks().values().contains(((Road) nm.sendingUnit).getLink())): "wrong unit sent a message";
		
			synchronized (pendingMessagesFromRoads){
				if (!pendingMessagesFromRoads.contains(nm.sendingUnit) && nm.sendingUnit!=null){
					pendingMessagesFromRoads.add((Road)nm.sendingUnit);
				}	
				
				
				if (nm.messageArrivalTime<test_timeOfLastScheduledMessage){
					//System.out.println();
				}
				
				//TODO: enable this assertion again after some time (problem at the moment not found
				assert(nm.messageArrivalTime>=test_timeOfLastScheduledMessage): "new: " + nm.messageArrivalTime + "; last: " + test_timeOfLastScheduledMessage;
				
				messageQueue.add(nm);
				
				processMessageQueue();
			}
		
		
	}
	
	public void registerEnterRequestMessage(Road fromRoad, Vehicle vehicle, double simTime){
		
		assert(fromRoad==belongsToRoad || belongsToRoad.getLink().getFromNode().getInLinks().values().contains(fromRoad.getLink())): "wrong unit sent a message";
		
		synchronized (pendingMessagesFromRoads){
			if (!pendingMessagesFromRoads.contains(fromRoad) && fromRoad!=belongsToRoad){
				pendingMessagesFromRoads.add(fromRoad);
			}
			
			System.out.println("register entry road");
			
			EnterRequestMessage erm=MessageFactory.getEnterRequestMessage(vehicle);
			erm.sendingUnit = fromRoad;
			erm.receivingUnit=belongsToRoad;
			erm.messageArrivalTime=simTime;
			messageQueue.add(erm);
			
			assert(erm.messageArrivalTime>=test_timeOfLastScheduledMessage): "new: " + erm.messageArrivalTime + "; last: " + test_timeOfLastScheduledMessage;
			
			processMessageQueue();
		}
		
		// send a null message to all other roads (going out of the same conjunction)
		for (Link otherLinks:fromRoad.getLink().getToNode().getOutLinks().values()){
			Road otherRoad=Road.allRoads.get(otherLinks.getId().toString());
			if (otherRoad!=belongsToRoad){
				NullMessage nm=MessageFactory.getNullMessage();
				nm.sendingUnit=fromRoad;
				nm.receivingUnit=otherRoad;
				nm.messageArrivalTime=simTime + fromRoad.inverseOutFlowCapacity - SimulationParameters.delta;
				assert(nm.messageArrivalTime>=0):"negative simTime...";
				otherRoad.roadEntryHandler.registerNullMessage(nm);
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
				assert(pendingMessagesFromRoads.contains(m.sendingUnit));
				pendingMessagesFromRoads.remove(m.sendingUnit);
			}
			
			//System.out.println("sdfasdf: " + m.messageArrivalTime);
			// TODO: enable this assertion again after some time (problem at the moment not found
			assert(m.messageArrivalTime>=test_timeOfLastScheduledMessage): "new: " + m.messageArrivalTime + "; last: " + test_timeOfLastScheduledMessage;
			
			
			if (m.messageArrivalTime>=test_timeOfLastScheduledMessage){
				test_timeOfLastScheduledMessage=m.messageArrivalTime;
				belongsToRoad.scheduler.schedule(m);
			}
		}
	}
	
	
}
