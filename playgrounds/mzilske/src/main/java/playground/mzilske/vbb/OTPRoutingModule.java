package playground.mzilske.vbb;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
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
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.TransitVertex;

public class OTPRoutingModule implements RoutingModule {


	int nonefound = 0;

	int npersons = 0;


	private TransitScheduleFactory tsf = new TransitScheduleFactoryImpl();

	private PathService pathservice;

	private TransitSchedule transitSchedule;

	public OTPRoutingModule(PathService pathservice, TransitSchedule transitSchedule) {
		this.pathservice = pathservice;
		this.transitSchedule = transitSchedule;
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
		LinkedList<Leg> baseTrip = routeLeg(fromFacility, toFacility);
		return baseTrip;
		//	return fillWithActivities(baseTrip, fromFacility, toFacility);
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return new StageActivityTypesImpl( Arrays.asList( PtConstants.TRANSIT_ACTIVITY_TYPE ) );
	}

	private LinkedList<Leg> routeLeg(Facility fromFacility, Facility toFacility) {
		LinkedList<Leg> legs = new LinkedList<Leg>();
		TraverseModeSet modeSet = new TraverseModeSet();
		modeSet.setWalk(true);
		modeSet.setBicycle(true);
		modeSet.setFerry(true);
		modeSet.setTrainish(true);
		modeSet.setBusish(true);
		modeSet.setTransit(true);
		RoutingRequest options = new RoutingRequest(modeSet);
		options.setWalkBoardCost(3 * 60); // override low 2-4 minute values
		// TODO LG Add ui element for bike board cost (for now bike = 2 * walk)
		options.setBikeBoardCost(3 * 60 * 2);
		// there should be a ui element for walk distance and optimize type
		options.setOptimize(OptimizeType.QUICK);
		options.setMaxWalkDistance(Double.MAX_VALUE);

		Date when = null;
		try {
			when = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2011-10-15 10:00:00");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		options.setDateTime(when);

		options.from =  fromFacility.getCoord().getY() +"," +fromFacility.getCoord().getX();
		options.to   =  toFacility.getCoord().getY()+ "," +  toFacility.getCoord().getX();
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

		Id currentLinkId = fromFacility.getLinkId();
		if (paths != null) {
			GraphPath path = paths.get(0);
			path.dump();
			boolean onBoard = false;
			String stop = null;
			long time = 0;
			boolean justMadeWalkLeg = false;
			for (State state : path.states) {
				Edge backEdge = state.getBackEdge();
				if (backEdge != null) {
					System.out.print(backEdge.getName());
					Trip backTrip = state.getBackTrip();
					if (backTrip != null) {
						System.out.println(" +++ " + backTrip + " +++ " + backEdge.getClass());
						if (backEdge instanceof TransitBoardAlight) {
							if (!onBoard) {
								stop = ((TransitVertex) state.getVertex()).getStopId().getId();
								onBoard = true;
								time = state.getElapsedTime();
							} else {
								Leg leg = new LegImpl(TransportMode.pt);
								String newStop = ((TransitVertex) state.getVertex()).getStopId().getId();
								TransitStopFacility egressFacility = transitSchedule.getFacilities().get(new IdImpl(newStop));
								leg.setRoute(new ExperimentalTransitRoute(transitSchedule.getFacilities().get(new IdImpl(stop)), createLine(backTrip), createRoute(), egressFacility));
								leg.setTravelTime(state.getElapsedTime() - time);
								legs.add(leg);
								onBoard = false;
								time = state.getElapsedTime();
								stop = newStop;
								currentLinkId = egressFacility.getLinkId();
								justMadeWalkLeg = false;
							}
						}
					} else if (backEdge instanceof FreeEdge) {
						Leg leg = new LegImpl(TransportMode.transit_walk);
						String newStop = ((TransitVertex) state.getVertex()).getStopId().getId();
						Id startLinkId;
						if (stop == null) {
							startLinkId = fromFacility.getLinkId();
						} else {
							startLinkId = transitSchedule.getFacilities().get(new IdImpl(stop)).getLinkId();
							
						}
						System.out.println(newStop);
						Id endLinkId = transitSchedule.getFacilities().get(new IdImpl(newStop)).getLinkId();
						GenericRouteWithStartEndLinkId route = new GenericRouteWithStartEndLinkId(startLinkId, endLinkId);
						leg.setRoute(route);
						legs.add(leg);
						stop = newStop;
						currentLinkId = endLinkId;
						justMadeWalkLeg = true;
					} else {
						System.out.println(" +++|");
					}
				}
			}

		} else {
			System.out.println("None found " + nonefound++);
		}
		System.out.println("---------" + npersons++);

		if (!currentLinkId.equals(toFacility.getLinkId())) {
			Leg leg = new LegImpl(TransportMode.transit_walk);
			GenericRouteWithStartEndLinkId route = new GenericRouteWithStartEndLinkId(currentLinkId, toFacility.getLinkId());
			leg.setRoute(route);
			legs.add(leg);
		}
		return legs;
	}

	private TransitRoute createRoute() {
		List<TransitRouteStop> emptyList = Collections.emptyList();
		return tsf.createTransitRoute(new IdImpl(""), null , emptyList, null);
	}



	private TransitLine createLine(Trip backTrip) {
		return tsf.createTransitLine(new IdImpl(backTrip.getRoute().getId().getId()+ "_"+backTrip.getRoute().getShortName()));
	}



	private TransitStopFacility createStop(String stop) {
		return tsf.createTransitStopFacility(new IdImpl(stop), null, false);
	}



}
