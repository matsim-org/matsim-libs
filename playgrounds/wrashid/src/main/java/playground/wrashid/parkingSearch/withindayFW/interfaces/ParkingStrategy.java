package playground.wrashid.parkingSearch.withindayFW.interfaces;

import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;

public interface ParkingStrategy {

	public void giveControlOfAgentToStrategy(PlanBasedWithinDayAgent withinDayAgent);
	
}
