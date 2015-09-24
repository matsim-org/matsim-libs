package cottbusAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;

/**
 * 
 * @author tthunig
 * @deprecated
 */
public class TtDetermineComRoutes implements PersonDepartureEventHandler, LinkEnterEventHandler{

	private Map<Id, List<Id>> agentRoutes = new HashMap<Id, List<Id>>();
	
	@Override
	public void reset(int iteration) {
		agentRoutes = new HashMap<Id, List<Id>>();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// add entered link to the route of the agent
		agentRoutes.get(event.getPersonId()).add(event.getLinkId());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// every agents departures only once
		List<Id> route = new ArrayList<Id>();
		route.add(event.getLinkId());
		agentRoutes.put(event.getPersonId(), route);
	}
	
	public List<Id> getAgentRoute(Id agentId){
		return agentRoutes.get(agentId);
	}
	
	public String getComIdOfAgent(Id agentId){
		// the first five digits of the personId are the commodityId
		return agentId.toString().substring(0, 5);
	}

	public Map<Id, List<Id>> getAgentRoutes() {
		return agentRoutes;
	}

}
