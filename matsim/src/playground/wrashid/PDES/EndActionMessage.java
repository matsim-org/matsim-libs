package playground.wrashid.PDES;

import org.matsim.plans.Act;

public class EndActionMessage extends Message {
	private Act action=null;
	private String vehicleId;
	// at which position in plan.getActsLegs(), the current Act is located
	// this is needed for finding out, which action to take next
	private int actionLegIndex;

	public EndActionMessage(Act action,String vehicleId, int actionLegIndex) {
		super();
		this.action = action;
		this.vehicleId = vehicleId;
		this.actionLegIndex = actionLegIndex;
	}

	@Override
	public void printMessageLogString() {
		System.out.println("arrivalTime="+this.getMessageArrivalTime() + "; VehicleId=" + vehicleId + "; LinkId=" + action.getLinkId().toString() + "; Description=actEnd " + action.getType());
	}

	public int getActionLegIndex() {
		return actionLegIndex;
	}
}
