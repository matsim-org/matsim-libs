package playground.mzilske.withinday;

import org.matsim.api.core.v01.Id;

public interface TeleportationBehavior {
	
	public void doSimStep(TeleportationWorld world);

	public Id getDestinationLinkId();

	public String getMode();

	public double getTravelTime();

}
