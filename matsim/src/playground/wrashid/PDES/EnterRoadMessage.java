package playground.wrashid.PDES;

import org.matsim.network.Link;
import org.matsim.plans.Leg;

public class EnterRoadMessage extends SelfhandleMessage {

	private Leg leg;
	private Vehicle vehicle;
	private int legIndex;
	private int linkIndex;
	private Link currentLink;

	@Override
	public void selfhandleMessage() {
		// TODO Auto-generated method stub
		//Continue here: ask road to really enter the road
		// => Road will then let us enter the road and tell us, when we can leave the road.
	}

	public EnterRoadMessage(Leg leg,Vehicle vehicle, int legIndex, int linkIndex, Link currentLink) {
		super();
		this.leg = leg;
		this.vehicle = vehicle;
		this.legIndex = legIndex;
		this.linkIndex=linkIndex;
		this.currentLink=currentLink;
	}
	
	@Override
	public void printMessageLogString() {
		System.out.println("arrivalTime="+this.getMessageArrivalTime() + "; VehicleId=" + vehicle.getOwnerPerson().getId().toString() + "; LinkId=" + currentLink.getId().toString() + "; Description=enter " );
		
	}

}
