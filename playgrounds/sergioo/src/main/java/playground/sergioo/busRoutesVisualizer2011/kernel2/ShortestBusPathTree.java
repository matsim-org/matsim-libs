package playground.sergioo.busRoutesVisualizer2011.kernel2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImplFactory;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.sergioo.busRoutesVisualizer2011.gui2.ShortestBusRoutesPainter;
import playground.sergioo.visualizer2D2012.networkVisualizer.SimpleNetworkWindow;

public class ShortestBusPathTree {

	private static final int NUM_TRANSFERS = 1;

	/**
	 * @param args
	 * 0 - Network file
	 * 1 - Transit Schedule file
	 * 2 - Time in the day
	 * 3 - Number of stops
	 * 4... - Stop ids
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(args[1]);
		double time = Time.parseTime(args[2]);
		int numStops = new Integer(args[3]);
		scenario.getConfig().transitRouter().setSearchRadius(1);
		scenario.getConfig().transitRouter().setExtensionRadius(0);
		scenario.getConfig().transitRouter().setMaxBeelineWalkConnectionDistance(1);
		scenario.getConfig().planCalcScore().setUtilityOfLineSwitch(-100);
		scenario.getConfig().planCalcScore().setTravelingWalk_utils_hr(-1000);
		TransitRouter transitRouter = new TransitRouterImplFactory(scenario.getTransitSchedule(), new TransitRouterConfig(scenario.getConfig().planCalcScore(), scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(), scenario.getConfig().vspExperimental())).get();
		ExperimentalTransitRouteFactory routesFactory = new ExperimentalTransitRouteFactory();
		for(int i=0; i<numStops; i++) {
			TransitStopFacility mainStop = scenario.getTransitSchedule().getFacilities().get(Id.create(args[4+i], ActivityFacility.class));
			Set<Coord>[] stopCoords = new Set[NUM_TRANSFERS+2];
			for(int j=0; j<stopCoords.length; j++)
				stopCoords[j] = new HashSet<Coord>();
			stopCoords[0].add(mainStop.getCoord());
			Set<Link>[] links = new Set[NUM_TRANSFERS+2];
			for(int j=0; j<stopCoords.length; j++)
				links[j] = new HashSet<Link>();
			links[0].add(scenario.getNetwork().getLinks().get(mainStop.getLinkId()));
			int k=0, numAccessStops = 0, total = scenario.getTransitSchedule().getFacilities().size();
			for(TransitStopFacility stop:scenario.getTransitSchedule().getFacilities().values()) {
				List<Leg> legs = transitRouter.calcRoute(mainStop.getCoord(), stop.getCoord(), time, null);
				if(legs==null)
					continue;
				for(int j=0; j<legs.size(); j++)
					if(legs.get(j).getMode().equals("transit_walk")) {
						legs.remove(j);
						j--;
					}
				if(legs.size()>0 && legs.size()<=NUM_TRANSFERS+1) {
					stopCoords[legs.size()].add(stop.getCoord());
					numAccessStops++;
					for(int j=0; j<legs.size(); j++) {
						Route origRoute = legs.get(j).getRoute();
						ExperimentalTransitRoute route = (ExperimentalTransitRoute) routesFactory.createRoute(origRoute.getStartLinkId(), origRoute.getEndLinkId());
						route.setStartLinkId(origRoute.getStartLinkId());
						route.setEndLinkId(origRoute.getEndLinkId());
						route.setRouteDescription(origRoute.getRouteDescription());
						NetworkRoute networkRoute = scenario.getTransitSchedule().getTransitLines().get(route.getLineId()).getRoutes().get(route.getRouteId()).getRoute();
						Id<Link> startId = scenario.getTransitSchedule().getFacilities().get(route.getAccessStopId()).getLinkId();
						Id<Link> endId = scenario.getTransitSchedule().getFacilities().get(route.getEgressStopId()).getLinkId();
						networkRoute = networkRoute.getSubRoute(startId, endId);
						for(Id<Link> id:networkRoute.getLinkIds())
							links[legs.size()].add(scenario.getNetwork().getLinks().get(id));
					}
				}
				if(++k%100==0)
					System.out.println("Stop "+i+": "+k+" stops of "+total+" ("+stopCoords[1].size()+","+numAccessStops+")");
			}
			new SimpleNetworkWindow(args[4+i], new ShortestBusRoutesPainter(scenario.getNetwork(), stopCoords, links)).setVisible(true);
		}
	}

}
