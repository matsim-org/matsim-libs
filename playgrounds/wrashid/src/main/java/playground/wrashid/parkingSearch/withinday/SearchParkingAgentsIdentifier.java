package playground.wrashid.parkingSearch.withinday;

import java.util.Set;

import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.utils.EditRoutes;

public class SearchParkingAgentsIdentifier extends DuringLegIdentifier {

	/*
	 * TODO:
	 * Add a datastructure that logs when an agent has been replanned
	 */
	
	private LinkReplanningMap linkReplanningMap;
	
	public SearchParkingAgentsIdentifier(LinkReplanningMap linkReplanningMap) {
		this.linkReplanningMap = linkReplanningMap;
	}
	
	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {

		Set<PlanBasedWithinDayAgent> agents = this.linkReplanningMap.getLegPerformingAgents();
		
		for (PlanBasedWithinDayAgent agent : agents) {
			
			/*
			 * - get current link Id
			 * - get current link
			 * - get destination facility (leg after the parking activity!!!)
			 * - calculate distance to facility
			 * - decide whether replanning should be enabled
			 */
//			agent.getCurrentLinkId()
		}
		
		return null;
	}

}
