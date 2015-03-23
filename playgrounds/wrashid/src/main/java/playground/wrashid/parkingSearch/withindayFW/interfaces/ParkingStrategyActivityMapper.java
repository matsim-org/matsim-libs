package playground.wrashid.parkingSearch.withindayFW.interfaces;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;

import playground.wrashid.parkingSearch.withindayFW.core.ParkingStrategy;

public interface ParkingStrategyActivityMapper {

	public Collection<ParkingStrategy> getParkingStrategies(Id agentId, String activityType);

	void addSearchStrategy(Id agentId, String activityType,
			ParkingStrategy parkingStrategy);
	
}
