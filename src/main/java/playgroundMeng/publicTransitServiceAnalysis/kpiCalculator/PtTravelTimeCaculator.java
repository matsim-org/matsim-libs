package playgroundMeng.publicTransitServiceAnalysis.kpiCalculator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder;
import ch.sbb.matsim.routing.pt.raptor.InitialStop;
import ch.sbb.matsim.routing.pt.raptor.LeastCostRaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.RaptorRoute;
import ch.sbb.matsim.routing.pt.raptor.RaptorStopFinder;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorCore;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import ch.sbb.matsim.routing.pt.raptor.RaptorRoute.RoutePart;
import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.Trip;
import playgroundMeng.publicTransitServiceAnalysis.others.PtAccessabilityConfig;

public class PtTravelTimeCaculator {

	private SwissRailRaptorCore swissRailRaptorCore;
	private SwissRailRaptorData data;
	private DefaultRaptorStopFinder stopFinder;
	private RaptorParameters parameters;
	private final RaptorParametersForPerson parametersForPerson;

	private Network network;

	private static PtTravelTimeCaculator ptTravelTimeCaculator = null;

	private PtTravelTimeCaculator() {
		this.network = PtAccessabilityConfig.getInstance().getNetwork();

		Config config = ConfigUtils.createConfig(PtAccessabilityConfig.getInstance().getConfigFile());
		TransitSchedule transitSchedule = PtAccessabilityConfig.getInstance().getTransitSchedule();
		data = SwissRailRaptorData.create(transitSchedule, RaptorUtils.createStaticConfig(config), network);
		stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), null);

		this.parametersForPerson = new DefaultRaptorParametersForPerson(config);
		this.swissRailRaptorCore = new SwissRailRaptorCore(data);

	}

	public static PtTravelTimeCaculator getInstance() {
		if (ptTravelTimeCaculator == null)
			ptTravelTimeCaculator = new PtTravelTimeCaculator();
		return ptTravelTimeCaculator;
	}

	public void caculate(Trip trip) throws Exception {
		Coord fromCoord = trip.getActivityEndImp().getCoord();
		Coord toCoord = trip.getActivityStartImp().getCoord();
		FakeFacility fromFacility = new FakeFacility(fromCoord);
		FakeFacility toFacility = new FakeFacility(toCoord);

		parameters = this.parametersForPerson.getRaptorParameters(null);
		if (parameters.getConfig().isUseRangeQuery()) {
			throw new Exception();
		}

		List<InitialStop> accessStops = findAccessStops(fromFacility, null, trip.getActivityEndImp().getTime(),
				parameters);
		List<InitialStop> egressStops = findEgressStops(toFacility, null, trip.getActivityEndImp().getTime(),
				parameters);

		RaptorRoute foundRoute = this.swissRailRaptorCore.calcLeastCostRoute(trip.getActivityEndImp().getTime(),
				fromFacility, toFacility, accessStops, egressStops, parameters);

		trip.setPtTraveInfo(new PtTraveInfo(foundRoute));
	}

	private List<InitialStop> findAccessStops(Facility facility, Person person, double departureTime,
			RaptorParameters parameters) {
		return this.stopFinder.findStops(facility, person, departureTime, parameters, this.data,
				RaptorStopFinder.Direction.ACCESS);
	}

	private List<InitialStop> findEgressStops(Facility facility, Person person, double departureTime,
			RaptorParameters parameters) {
		return this.stopFinder.findStops(facility, person, departureTime, parameters, this.data,
				RaptorStopFinder.Direction.EGRESS);
	}

	public class PtTraveInfo {
		RaptorRoute raptorRoute;
		boolean usePt;
		double travelTime;
		double traveLTimeWithOutWaitingTime;

		public PtTraveInfo(RaptorRoute raptorRoute) {
			this.usePt = false;
			if (raptorRoute != null) {
				this.raptorRoute = raptorRoute;
				for (RoutePart routePart : raptorRoute.getParts()) {
					if (routePart.mode.equals("pt")) {
						this.usePt = true;
						this.travelTime = raptorRoute.getTravelTime();
						double waitingTime = routePart.boardingTime - routePart.depTime;
						this.traveLTimeWithOutWaitingTime = this.travelTime - waitingTime;
						break;
					}
				}
			}
		}

		public double getTravelTime() {
			return travelTime;
		}

		public double getTraveLTimeWithOutWaitingTime() {
			return traveLTimeWithOutWaitingTime;
		}

		public boolean isUsePt() {
			return this.usePt;
		}

		@Override
		public String toString() {
			String string = null;
			for (RoutePart routePart : raptorRoute.getParts()) {
				string = string + routePart.route.getDescription() + " ";
			}
			return string;
		}

	}

	public final class FakeFacility implements Facility {
		private final Coord coord;
		private final Id<Link> linkId;

		public FakeFacility(Coord coord) {
			this(coord, null);
		}

		FakeFacility(Coord coord, Id<Link> linkId) {
			this.coord = coord;
			this.linkId = linkId;
		}

		public Coord getCoord() {
			return this.coord;
		}

		public Id getId() {
			throw new RuntimeException("not implemented");
		}

		public Map<String, Object> getCustomAttributes() {
			throw new RuntimeException("not implemented");
		}

		public Id getLinkId() {
			return this.linkId;
		}
	}
}
