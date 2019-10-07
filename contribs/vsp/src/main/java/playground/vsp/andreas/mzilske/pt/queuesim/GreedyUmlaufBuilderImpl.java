package playground.vsp.andreas.mzilske.pt.queuesim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufBuilder;
import org.matsim.pt.UmlaufImpl;
import org.matsim.pt.UmlaufInterpolator;
import org.matsim.pt.UmlaufStueck;
import org.matsim.pt.UmlaufStueckI;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class GreedyUmlaufBuilderImpl implements UmlaufBuilder {
	
	public class UmlaufKey {
		
		private Id<TransitLine> lineId;
		private Id<TransitStopFacility> stopFacilityId;
		private double lastArrivalTime;
		private Id<Umlauf> umlaufId;
		

		public Id<Umlauf> getUmlaufId() {
			return umlaufId;
		}

		public UmlaufKey(Id<TransitLine> lineId, Id<TransitStopFacility> stopFacilityId, double lastArrivalTime, Id<Umlauf> umlaufId) {
			super();
			this.lineId = lineId;
			this.stopFacilityId = stopFacilityId;
			this.lastArrivalTime = lastArrivalTime;
			this.umlaufId = umlaufId;
		}

		public Id<TransitLine> getLineId() {
			return lineId;
		}
		
		public Id<TransitStopFacility> getStopFacility() {
			return stopFacilityId;
		}

		public double getLastArrivalTime() {
			return lastArrivalTime;
		}
		
		@Override
		public String toString() {
			return stopFacilityId + " at " + lastArrivalTime + "(" + getLineId() + ")";
		}
		

	}

	private static final Comparator<UmlaufStueck> departureTimeComparator = new Comparator<UmlaufStueck>() {

		@Override
		public int compare(UmlaufStueck o1, UmlaufStueck o2) {
			return Double.compare(o1.getDeparture().getDepartureTime(), o2.getDeparture().getDepartureTime());
		}
		
	};

	private static final Comparator<UmlaufKey> umlaufKeyComparator = new Comparator<UmlaufKey>() {

		@Override
		public int compare(UmlaufKey o1, UmlaufKey o2) {
			int c = o1.getStopFacility().compareTo(o2.getStopFacility());
			if (c != 0) {
				return c;
			} else {
				int cc = o1.getLineId().compareTo(o2.getLineId()); 
				if (cc != 0) {
					return cc;
				} else {
					int ccc = Double.compare(o1.getLastArrivalTime(), o2.getLastArrivalTime());
					if (ccc != 0) {
						return ccc;
					} else {
						return o1.getUmlaufId().compareTo(o2.getUmlaufId());
					}
				}
			}
		}
		
	};
	
	private Collection<TransitLine> transitLines;
	private SortedMap<UmlaufKey,Umlauf> umlaeufe = new TreeMap<UmlaufKey,Umlauf>(umlaufKeyComparator);
	private ArrayList<UmlaufStueck> umlaufStuecke;

	private UmlaufInterpolator interpolator;

	public GreedyUmlaufBuilderImpl(UmlaufInterpolator interpolator, Collection<TransitLine> transitLines) {
		this.interpolator = interpolator;
		this.transitLines = transitLines;
	}
	
	public boolean canBuild() {
		return true;
	}
	
	@Override
	public Collection<Umlauf> build() {
		if (!canBuild()) {
			throw new IllegalArgumentException();
		}
		createUmlaufStuecke();
		int id = 0;
		for (UmlaufStueck umlaufStueck : umlaufStuecke) {
			Umlauf umlauf = findFittingUmlauf(umlaufStueck);
			if (umlauf == null) {
				umlauf = new UmlaufImpl(Id.create(id++, Umlauf.class));
			} else {
				umlaeufe.remove(getKey(umlauf));
			}
			interpolator.addUmlaufStueckToUmlauf(umlaufStueck, umlauf);
			umlaeufe.put(getKey(umlauf), umlauf);
		}
		return umlaeufe.values();
	}
	
	private double getLastArrivalTime(Umlauf umlauf) {
		TransitRouteStop previousStop = getLastStop(umlauf);
		double previousDepartureTime = getLastDeparture(umlauf).getDepartureTime();
		double arrivalOffset = previousStop.getArrivalOffset();
		double previousArrivalTime = previousDepartureTime + arrivalOffset;			
		return previousArrivalTime;
	}

	private Departure getLastDeparture(Umlauf umlauf) {
		return getLastUmlaufStueck(umlauf).getDeparture();
	}

	private TransitRouteStop getLastStop(Umlauf umlauf) {
		UmlaufStueckI lastUmlaufStueck = getLastUmlaufStueck(umlauf);
		return getLastStop(lastUmlaufStueck);
	}

	private TransitRouteStop getLastStop(UmlaufStueckI umlaufStueck) {
		List<TransitRouteStop> stops = umlaufStueck.getRoute().getStops();
		TransitRouteStop previousStop = stops.get(stops.size() - 1);
		return previousStop;
	}

	private UmlaufStueckI getLastUmlaufStueck(Umlauf umlauf) {
		return umlauf.getUmlaufStuecke().get(umlauf.getUmlaufStuecke().size() - 1);
	}

	private String getLastStopPostAreaId(Umlauf umlauf) {
		return getLastStop(umlauf).getStopFacility().getStopAreaId().toString();
	}

	private UmlaufKey getKey(Umlauf umlauf) {
		return new UmlaufKey(umlauf.getLineId(), Id.create(getLastStopPostAreaId(umlauf), TransitStopFacility.class), getLastArrivalTime(umlauf), umlauf.getId());
	}

	private Umlauf findFittingUmlauf(UmlaufStueck umlaufStueck) {
		String firstStopPostAreaId = umlaufStueck.getRoute().getStops().get(0).getStopFacility().getStopAreaId().toString();
		Id<TransitLine> lineId = umlaufStueck.getLine().getId();
		UmlaufKey earliestAtPoint = new UmlaufKey(lineId, Id.create(firstStopPostAreaId, TransitStopFacility.class), 0.0, Id.create(0, Umlauf.class));
		UmlaufKey latestAtPoint = new UmlaufKey(lineId, Id.create(firstStopPostAreaId, TransitStopFacility.class), umlaufStueck.getDeparture().getDepartureTime(), Id.create(0, Umlauf.class));
		log("Looking between " + earliestAtPoint + " and " + latestAtPoint);
		SortedMap<UmlaufKey,Umlauf> fittingUmlaeufe = umlaeufe.subMap(earliestAtPoint, latestAtPoint);
		Umlauf fittingUmlauf;
		if (fittingUmlaeufe.isEmpty()) {
			fittingUmlauf = null;
		} else {
			fittingUmlauf = fittingUmlaeufe.get(fittingUmlaeufe.firstKey());
			log("Found " + getKey(fittingUmlauf));
		}
		return fittingUmlauf;
	}

	private void log(String string) {
		System.out.println(string);
	}

	private void createUmlaufStuecke() {
		int i=0;
		this.umlaufStuecke = new ArrayList<UmlaufStueck>();
		for (TransitLine line : transitLines) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					UmlaufStueck umlaufStueck = new UmlaufStueck(line, route, departure);
					umlaufStuecke.add(umlaufStueck);
					++i;
				}
			}
		}
		Collections.sort(this.umlaufStuecke, departureTimeComparator);
	}

	
	

}
