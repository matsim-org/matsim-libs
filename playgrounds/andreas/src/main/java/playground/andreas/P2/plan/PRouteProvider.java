package playground.andreas.P2.plan;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public interface PRouteProvider {
	
	public TransitLine createTransitLine(Id pLineId, double startTime, double endTime, int numberOfVehicles, TransitStopFacility startStop, TransitStopFacility endStop, Id routeId);

	public TransitStopFacility getRandomTransitStop();

	public int getIteration();

	public TransitLine createEmptyLine(Id id);

}
