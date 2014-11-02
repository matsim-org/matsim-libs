package playground.mzilske.otp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.TransferEdge;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TransitVertex;

public class OTPRoutingModule implements RoutingModule {

	private int nonefound = 0;

	private int npersons = 0;

	private TransitScheduleFactory tsf = new TransitScheduleFactoryImpl();

	private PathService pathservice;

	private TransitSchedule transitSchedule;

	private CoordinateTransformation ct;

	private Date day;

	public OTPRoutingModule(PathService pathservice, TransitSchedule transitSchedule, String date, CoordinateTransformation ct) {
		this.pathservice = pathservice;
		this.transitSchedule = transitSchedule;
		this.ct = ct;
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			df.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
			this.day = df.parse(date);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
		LinkedList<Leg> baseTrip = routeLeg(fromFacility, toFacility, departureTime);
		return baseTrip;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return new StageActivityTypesImpl( Arrays.asList( PtConstants.TRANSIT_ACTIVITY_TYPE ) );
	}

	private LinkedList<Leg> routeLeg(Facility fromFacility, Facility toFacility, double departureTime) {
		LinkedList<Leg> legs = new LinkedList<Leg>();
		TraverseModeSet modeSet = new TraverseModeSet();
		modeSet.setWalk(true);
		modeSet.setTransit(true);
		RoutingRequest options = new RoutingRequest(modeSet);
		options.setWalkBoardCost(3 * 60); // override low 2-4 minute values
		options.setBikeBoardCost(3 * 60 * 2);
		options.setOptimize(OptimizeType.QUICK);
		options.setMaxWalkDistance(Double.MAX_VALUE);
		 
		Calendar when = Calendar.getInstance();

		when.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
		when.setTime(day);
		when.add(Calendar.SECOND, (int) departureTime);
		
		options.setDateTime(when.getTime());

		Coord fromCoord = ct.transform(fromFacility.getCoord());
		Coord toCoord = ct.transform(toFacility.getCoord());
		options.from =  fromCoord.getY() +"," +fromCoord.getX();
		options.to   =  toCoord.getY()+ "," +  toCoord.getX();
		options.numItineraries = 1;
		System.out.println("--------");
		System.out.println("Path from " + options.from + " to " + options.to + " at " + when);
		System.out.println("\tModes: " + modeSet);
		System.out.println("\tOptions: " + options);

		List<GraphPath> paths = null;
		try {
			paths = pathservice.getPaths(options);
		} catch (VertexNotFoundException e) {
			System.out.println("None found " + nonefound++);
		}

		Id<Link> currentLinkId = fromFacility.getLinkId();
		if (paths != null) {
			GraphPath path = paths.get(0);
			path.dump();
			boolean onBoard = false;
			String stop = null;
			long time = 0;
			for (State state : path.states) {
				Edge backEdge = state.getBackEdge();
				if (backEdge != null) {
					final long travelTime = state.getElapsedTime() - time;
					if (backEdge instanceof TransitBoardAlight) {
						Trip backTrip = state.getBackTrip();
						if (!onBoard) {
							stop = ((TransitVertex) state.getVertex()).getStopId().getId();
							onBoard = true;
							time = state.getElapsedTime();
							TransitStopFacility accessFacility = transitSchedule.getFacilities().get(Id.create(stop, TransitStopFacility.class));
							if(!currentLinkId.equals(accessFacility.getLinkId())) {
								throw new RuntimeException();
							}
						} else {
							Leg leg = new LegImpl(TransportMode.pt);
							String newStop = ((TransitVertex) state.getVertex()).getStopId().getId();
							TransitStopFacility accessFacility = transitSchedule.getFacilities().get(Id.create(stop, TransitStopFacility.class));
							TransitStopFacility egressFacility = transitSchedule.getFacilities().get(Id.create(newStop, TransitStopFacility.class));
							final ExperimentalTransitRoute route = new ExperimentalTransitRoute(accessFacility, createLine(backTrip), createRoute(), egressFacility);
							route.setTravelTime(travelTime);
							leg.setRoute(route);
							leg.setTravelTime(travelTime);
							legs.add(leg);
							onBoard = false;
							time = state.getElapsedTime();
							stop = newStop;
							currentLinkId = egressFacility.getLinkId();
						}
					} else if (backEdge instanceof FreeEdge || backEdge instanceof TransferEdge || backEdge instanceof PlainStreetEdge) {
						Leg leg = new LegImpl(TransportMode.transit_walk);
						if (state.getVertex() instanceof TransitVertex) {
							String newStop = ((TransitVertex) state.getVertex()).getStopId().getId();
							if (!newStop.equals(stop)) {
								Id<Link> startLinkId;
								if (stop == null) {
									startLinkId = fromFacility.getLinkId();
								} else {
									startLinkId = transitSchedule.getFacilities().get(Id.create(stop, TransitStopFacility.class)).getLinkId();
								}
								Id<Link> endLinkId = transitSchedule.getFacilities().get(Id.create(newStop, TransitStopFacility.class)).getLinkId();
								GenericRouteImpl route = new GenericRouteImpl(startLinkId, endLinkId);
								route.setTravelTime(travelTime);
								leg.setRoute(route);
								leg.setTravelTime(travelTime);
								legs.add(leg);
								time = state.getElapsedTime();
								stop = newStop;
								currentLinkId = endLinkId;
							}
						} else if (state.getVertex() instanceof StreetLocation) {
							System.out.println("Wir sind da.");
							GenericRouteImpl route = new GenericRouteImpl(currentLinkId, toFacility.getLinkId());
							route.setTravelTime(travelTime);
							leg.setRoute(route);
							legs.add(leg);
							time = state.getElapsedTime();
							currentLinkId = toFacility.getLinkId();
						} else if (state.getVertex() instanceof IntersectionVertex) {

						} else {
							throw new RuntimeException("Unexpected vertex: " + state.getVertex().getClass());
						}

					} 
				}

			}
			if (!currentLinkId.equals(toFacility.getLinkId())) {
				throw new RuntimeException();
			}

		} else {
			System.out.println("None found " + nonefound++);
		}
		System.out.println("---------" + npersons++);

		if (!currentLinkId.equals(toFacility.getLinkId())) {

		}
		return legs;
	}

	private TransitRoute createRoute() {
		List<TransitRouteStop> emptyList = Collections.emptyList();
		return tsf.createTransitRoute(Id.create("", TransitRoute.class), null , emptyList, null);
	}

	private TransitLine createLine(Trip backTrip) {
		return tsf.createTransitLine(Id.create(backTrip.getRoute().getId().getId()+ "_"+backTrip.getRoute().getShortName(), TransitLine.class));
	}

}
