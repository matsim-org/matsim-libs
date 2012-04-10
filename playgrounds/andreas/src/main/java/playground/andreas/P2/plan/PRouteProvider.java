package playground.andreas.P2.plan;

import java.util.ArrayList;
import java.util.Collection;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public interface PRouteProvider {
	
	public TransitLine createTransitLine(Id pLineId, double startTime, double endTime, int numberOfVehicles, ArrayList<TransitStopFacility> stopsToBeServed, Id routeId);

	public TransitStopFacility getRandomTransitStop();
	
	public Collection<TransitStopFacility> getAllPStops();

	public TransitLine createEmptyLine(Id id);

}
