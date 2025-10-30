package ch.sbb.matsim.contrib.railsim.qsimengine;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Time-distance data for a specific transit line and route.
 */
final class TimeDistanceData {

	final Id<TransitLine> lineId;
	final Id<TransitRoute> routeId;
	final List<Row> rows = new ArrayList<>();

	public TimeDistanceData(Id<TransitLine> lineId, Id<TransitRoute> routeId) {
		this.lineId = lineId;
		this.routeId = routeId;
	}

	public void add(double targetArrivalTime, double cumulativeDistance, Id<Link> linkId, Id<TransitStopFacility> stopId) {
		rows.add(new Row(targetArrivalTime, cumulativeDistance, linkId, stopId));
	}

	record Row(double time, double distance, Id<Link> linkId, Id<TransitStopFacility> stopId) {
	}
}
