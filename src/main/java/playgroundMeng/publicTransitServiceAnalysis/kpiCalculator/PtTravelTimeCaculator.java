package playgroundMeng.publicTransitServiceAnalysis.kpiCalculator;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder;
import ch.sbb.matsim.routing.pt.raptor.LeastCostRaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.Trip;
import playgroundMeng.publicTransitServiceAnalysis.others.PtAccessabilityConfig;

public class PtTravelTimeCaculator {
	private SwissRailRaptor swissRailRaptor;
	private Network network;

	private static PtTravelTimeCaculator ptTravelTimeCaculator = null;

	private PtTravelTimeCaculator() {
		this.network = PtAccessabilityConfig.getInstance().getNetwork();

		Config config = ConfigUtils.createConfig(PtAccessabilityConfig.getInstance().getConfigFile());
		TransitSchedule transitSchedule = PtAccessabilityConfig.getInstance().getTransitSchedule();
		SwissRailRaptorData data = SwissRailRaptorData.create(transitSchedule, RaptorUtils.createStaticConfig(config),
				network);
		DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null,
				new DefaultRaptorIntermodalAccessEgress(), null);
		this.swissRailRaptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(config),
				new LeastCostRaptorRouteSelector(), stopFinder);

	}

	public static PtTravelTimeCaculator getInstance() {
		if (ptTravelTimeCaculator == null)
			ptTravelTimeCaculator = new PtTravelTimeCaculator();
		return ptTravelTimeCaculator;
	}

	public void caculate(Trip trip) {

		Coord fromCoord = trip.getActivityEndImp().getCoord();
		Coord toCoord = trip.getActivityStartImp().getCoord();
		List<Leg> legs = swissRailRaptor.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord),
				trip.getActivityEndImp().getTime(), null);
		trip.setPtTraveInfo(new PtTraveInfo(legs));
	}

	public class PtTraveInfo {
		List<Leg> ptLegs = new LinkedList<Leg>();
		boolean usePt;
		double travelTime;

		public PtTraveInfo(List<Leg> ptLegs) {
			this.ptLegs = ptLegs;
			this.travelTime = this.setTravelTime(ptLegs);
		}

		private double setTravelTime(List<Leg> ptLegs) {
			double time = 0;
			for (Leg leg : ptLegs) {
				time = time + leg.getTravelTime();
			}
			return time;
		}

		public double getTravelTime() {
			return travelTime;
		}

		public void setPtLegs(List<Leg> ptLegs) {
			this.ptLegs = ptLegs;
		}

		public List<Leg> getPtLegs() {
			return ptLegs;
		}

		private String legsToString() {
			String legString = null;
			for (Leg leg : this.ptLegs) {
				legString = legString + leg + " ";
			}
			return legString;
		}

		public boolean isUsePt() {
			boolean hasPt = false;
			for (Leg leg : this.getPtLegs()) {
				if (leg.getMode().equals("pt")) {
					hasPt = true;
					break;
				}
			}
			return hasPt;
		}

		@Override
		public String toString() {
			return "PTTraveInfo [ptLegs=" + this.legsToString() + ", travelTime=" + travelTime + "]";
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
