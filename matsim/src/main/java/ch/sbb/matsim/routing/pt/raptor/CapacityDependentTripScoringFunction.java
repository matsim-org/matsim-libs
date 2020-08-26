package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.routing.pt.raptor.OccupancyData.DepartureData;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData.RouteData;
import ch.sbb.matsim.routing.pt.raptor.RaptorInVehicleCostCalculator.RouteSegmentIterator;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.Iterator;
import java.util.Set;

/**
 * @author mrieser / Simunto
 */
public class CapacityDependentTripScoringFunction implements LegScoring {

	private final static Logger LOG = Logger.getLogger(CapacityDependentTripScoringFunction.class);

	private final Person person;
	private final ScoringParameters params;
	private final Set<String> ptModes;
	private final OccupancyData data;
	private final CapacityDependentInVehicleCostCalculator inVehicleCostCalculator;
	private double score = 0.0;

	public CapacityDependentTripScoringFunction(Person person, ScoringParameters params, Set<String> ptModes, OccupancyData data, CapacityDependentInVehicleCostCalculator inVehicleCostCalculator) {
		this.person = person;
		this.params = params;
		this.ptModes = ptModes;
		this.inVehicleCostCalculator = inVehicleCostCalculator;
		this.data = data;
	}

	@Override
	public void finish() {
	}

	@Override
	public double getScore() {
		return this.score;
	}

	@Override
	public void handleLeg(Leg leg) {
		String mode = leg.getMode();
		boolean isPtLeg = this.ptModes.contains(mode);
		if (!isPtLeg) {
			return;
		}

		double depTime = leg.getDepartureTime().seconds();
		double arrTime = depTime + leg.getTravelTime().seconds();
		double travelTime = arrTime - depTime;

		ModeUtilityParameters modeParams = this.params.modeParams.get(mode);
		double margUtil_s = modeParams.marginalUtilityOfTraveling_s;
		double defaultScore = travelTime * margUtil_s;

		double capacityScore;
		Route route = leg.getRoute();
		if (route instanceof TransitPassengerRoute) {
			TransitPassengerRoute paxRoute = (TransitPassengerRoute) route;

			TransitRouteSegmentIterator segments = new TransitRouteSegmentIterator(this.person, this.data, paxRoute);
			Vehicle vehicle = segments.getTransitVehicle();

			capacityScore = -this.inVehicleCostCalculator.getInVehicleCost(travelTime, margUtil_s, this.person, vehicle, null, segments);

			this.score = +capacityScore - defaultScore; // it's actually only the score correction on top of the already calculated score

		} else {
			LOG.warn("no route available.");
		}
	}

	public static class TransitRouteSegmentIterator implements RouteSegmentIterator {
		DepartureData currentDepData = null;
		boolean hasNext = false;
		TransitRouteStop nextFromStop = null;
		TransitRouteStop nextToStop = null;
		DepartureData nextFromStopDepData = null;
		DepartureData nextToStopDepData = null;
		Iterator<TransitRouteStop> stopIter;
		final RouteData routeData;
		final Id<Departure> departureId;
		final Id<TransitStopFacility> accessStopId;
		final Id<TransitStopFacility> egressStopId;
		final Vehicle ptVehicle;

		public TransitRouteSegmentIterator(Person person, OccupancyData data, TransitPassengerRoute paxRoute) {
			Id<TransitLine> lineId = paxRoute.getLineId();
			this.routeData = data.getRouteData(lineId, paxRoute.getRouteId());
			TransitRoute ptRoute = this.routeData.route;
			this.departureId = data.getLastUsedDeparture(person.getId());
			this.ptVehicle = this.routeData.vehicles.get(this.departureId);

			this.accessStopId = paxRoute.getAccessStopId();
			this.egressStopId = paxRoute.getEgressStopId();

			this.stopIter = ptRoute.getStops().iterator();

			// initalize
			this.next();
		}

		public Vehicle getTransitVehicle() {
			return this.ptVehicle;
		}

		@Override
		public boolean hasNext() {
			return this.hasNext;
		}

		@Override
		public void next() {
			this.currentDepData = this.nextFromStopDepData;
			boolean searchStart = !this.hasNext;
			this.hasNext = false;
			this.nextFromStopDepData = this.nextToStopDepData;
			while (this.stopIter.hasNext()) {
				this.nextToStop = this.stopIter.next();
				if (searchStart && this.nextFromStop != null && this.nextFromStop.getStopFacility().getId().equals(this.accessStopId)) {
					this.nextFromStopDepData = this.routeData.stopData.get(this.nextFromStop.getStopFacility().getId()).depData.get(this.departureId);
					this.hasNext = true;
					break;
				}
				if (!searchStart) {
					this.hasNext = true;
					if (this.nextFromStop.getStopFacility().getId().equals(this.egressStopId)) {
						this.hasNext = false;
					}
					break;
				}
				this.nextFromStop = this.nextToStop;
			}
			if (this.hasNext) {
				this.nextToStopDepData = this.routeData.stopData.get(this.nextToStop.getStopFacility().getId()).depData.get(this.departureId);
				this.nextFromStop = this.nextToStop;
			}
		}

		@Override
		public double getInVehicleTime() {
			return this.nextFromStopDepData.vehDepTime - this.currentDepData.vehDepTime;
		}

		@Override
		public double getPassengerCount() {
			return this.currentDepData.paxCountAtDeparture;
		}

		@Override
		public double getTimeOfDay() {
			return this.currentDepData.vehDepTime;
		}
	}
}
