package playground.wrashid.PDES;

import org.matsim.plans.Act;
import org.matsim.plans.Leg;

public class StartingLegMessage extends Message {
	private Leg leg=null;
	private String vehicleId;
	// at which position in plan.getActsLegs(), the current Leg is located
	// this is needed for finding out, which leg to take next
	private int legIndex;
	// A route is made up of several links. So we need the index of the link here.
	private int linkIndex;

	public StartingLegMessage(Leg leg,String vehicleId, int legIndex, int linkIndex) {
		super();
		this.leg = leg;
		this.vehicleId = vehicleId;
		this.legIndex = legIndex;
		this.linkIndex=linkIndex;
	}

	@Override
	public void printMessageLogString() {
		System.out.println("arrivalTime="+this.getMessageArrivalTime() + "; VehicleId=" + vehicleId + "; LinkId=" + leg.getRoute().getLinkRoute()[linkIndex].getId().toString() + "; Description=enter " + leg);
	}

	public int getLegIndex() {
		return legIndex;
	}
}
