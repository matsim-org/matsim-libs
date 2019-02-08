package org.matsim.pt.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * @author mrieser / senozon
 */
public class TransitRouterCustomDataTest {
	 @Test
	public void testCustomDataIntegration() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		createTestSchedule(scenario);
		
		TransitRouterConfig trConfig = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(),
				scenario.getConfig().vspExperimental());
		trConfig.setBeelineWalkSpeed(0.1); // something very slow, so the agent does not walk over night
		trConfig.setSearchRadius(200);
		
		TransitRouterNetworkTravelTimeAndDisutility transitRouterNetworkTravelTimeAndDisutility = new TransitRouterNetworkTravelTimeAndDisutility(trConfig);
		
		MockingTransitTravelDisutility disutility = new MockingTransitTravelDisutility(transitRouterNetworkTravelTimeAndDisutility);
		
		TransitRouterNetwork transitNetwork = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), trConfig.getBeelineWalkConnectionDistance());

		TransitRouterImpl router = new TransitRouterImpl(trConfig, new PreparedTransitSchedule(scenario.getTransitSchedule()), transitNetwork, transitRouterNetworkTravelTimeAndDisutility, disutility);

		double x = -100;
		List<Leg> legs = router.calcRoute(new FakeFacility( new Coord(x, (double) 0)), new FakeFacility( new Coord((double) 3100, (double) 0)), 5.9*3600, null);
		Assert.assertEquals(1, legs.size());
		
		/* the following is not really nice as a test, but I had to somehow
		 * keep track of the internal state and have to replay this now
		 * to ensure everything works as expected...
		 */
		Assert.assertEquals(8, disutility.states.size());
		Assert.assertEquals("[[1>2 @ 22240.0]]", disutility.states.get(0)); // first link to be expanded: from stop 1 to stop 2, line 1 
		Assert.assertEquals("[[1>3 @ 22240.0]]", disutility.states.get(1)); // now from stop 1 to stop 3, line 2
		Assert.assertEquals("[[1>2 @ 22240.0], [2>3 @ 108300.0]]", disutility.states.get(2)); // from stop 2 to stop 3, coming from stop 1 before, line 1
		Assert.assertEquals("[[1>2 @ 22240.0], [2>3 @ 108300.0], [3>4 @ 108600.0]]", disutility.states.get(3)); // continuing on line 1
		Assert.assertEquals("[[1>2 @ 22240.0], [2>3 @ 108300.0], [3>3 @ 108600.0]]", disutility.states.get(4)); // transfer at stop 3, coming from line 1, to line 2
		Assert.assertEquals("[[1>3 @ 22240.0], [3>5 @ 108690.0]]", disutility.states.get(5)); // continue on line 2
		Assert.assertEquals("[[1>3 @ 22240.0], [3>3 @ 108690.0]]", disutility.states.get(6)); // transfer at stop 3, coming from line 2, should be a dead end
		Assert.assertEquals("[[1>2 @ 22240.0], [2>3 @ 108300.0], [3>4 @ 108600.0], [4>5 @ 108900.0]]", disutility.states.get(7)); // continue on line 1
		
		/* important to notice:
		 * state[4] should be ignored, as it is a worse result than previously found solutions.
		 * The ignoring of the state can be seen e.g. in state[5] which continues on line 2 state[4] switched to,
		 * but using the original travel sequence.
		 */
	}
	
	private static class MockingTransitTravelDisutility implements TransitTravelDisutility {

		private final TransitRouterNetworkTravelTimeAndDisutility routerDisutility;
		private ArrayList<String> states = new ArrayList<>();
		
		public MockingTransitTravelDisutility(final TransitRouterNetworkTravelTimeAndDisutility routerDisutility) {
			this.routerDisutility = routerDisutility;
		}

		@Override
		public double getLinkTravelDisutility(Link link, double time,
				Person person, Vehicle vehicle, CustomDataManager dataManager) {
			double val = this.routerDisutility.getLinkTravelDisutility(link, time, person, vehicle, dataManager);

			System.out.print("handling link " + ((TransitRouterNetworkNode) link.getFromNode()).getStop().getStopFacility().getId() 
					+ ">" + ((TransitRouterNetworkNode) link.getToNode()).getStop().getStopFacility().getId());
			ArrayList<LoggerData> links = new ArrayList<>();
			Object o = dataManager.getFromNodeCustomData();
			if (o instanceof ArrayList) {
				links.addAll((Collection<? extends LoggerData>) o);
			}
			links.add(new LoggerData(link, time));
			String newState = Arrays.toString(links.toArray(new LoggerData[links.size()]));
			System.out.println(" :  " + newState);
			this.states.add(newState);
			dataManager.setToNodeCustomData(links);
			
			return val;
		}
		
		@Override
		public double getWalkTravelDisutility(Person person, Coord coord, Coord toCoord) {
			return routerDisutility.getWalkTravelDisutility(person, coord, toCoord);
		}

		@Override
		public double getWalkTravelTime(Person person, Coord coord, Coord toCoord) {
			return routerDisutility.getWalkTravelTime(person, coord, toCoord);
		}
		
	}
	
	private static class LoggerData {
		private final Link link;
		private final double time;
		public LoggerData(final Link link, final double time) {
			this.link = link;
			this.time = time;
		}
		@Override
		public String toString() {
			return "[" + ((TransitRouterNetworkNode) link.getFromNode()).getStop().getStopFacility().getId() 
					+ ">" + ((TransitRouterNetworkNode) link.getToNode()).getStop().getStopFacility().getId()
					+ " @ " + time + "]";
		}
	}
	
	/**
	 * Creates the following test schedule:
	 * 
	 * 
	 *      (2)       (4)
	 *     /   \     /   \
	 *    /     \   /     \
	 *   /       \ /       \
	 * (1)-------(3)-------(5)
	 * 
	 * line 1 traveling from 1 to 2 to 3 to 4 to 5
	 * line 2 traveling from 1 to 3 to 5, slower from 1 to 3 than line 1, faster from 3 to 5 and from 5 to 7
	 * 
	 */
	private final void createTestSchedule(final Scenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory f = schedule.getFactory();

		TransitStopFacility f1 = f.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
		TransitStopFacility f2 = f.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord((double) 500, (double) 500), false);
		TransitStopFacility f3 = f.createTransitStopFacility(Id.create("3", TransitStopFacility.class), new Coord((double) 1000, (double) 0), false);
		TransitStopFacility f4 = f.createTransitStopFacility(Id.create("4", TransitStopFacility.class), new Coord((double) 1500, (double) 500), false);
		TransitStopFacility f5 = f.createTransitStopFacility(Id.create("5", TransitStopFacility.class), new Coord((double) 2000, (double) 0), false);
		
		schedule.addStopFacility(f1);
		schedule.addStopFacility(f2);
		schedule.addStopFacility(f3);
		schedule.addStopFacility(f4);
		schedule.addStopFacility(f5);
		
		TransitLine line1 = f.createTransitLine(Id.create("1", TransitLine.class));
		List<TransitRouteStop> stops = new ArrayList<>();
		stops.add(f.createTransitRouteStop(f1, Time.getUndefinedTime(), 0.0));
		stops.add(f.createTransitRouteStop(f2, Time.getUndefinedTime(), 300.0));
		stops.add(f.createTransitRouteStop(f3, Time.getUndefinedTime(), 600.0));
		stops.add(f.createTransitRouteStop(f4, Time.getUndefinedTime(), 900.0));
		stops.add(f.createTransitRouteStop(f5, 1200.0, Time.getUndefinedTime()));
		TransitRoute route1 = f.createTransitRoute(Id.create("1", TransitRoute.class), null, stops, "pt");
		line1.addRoute(route1);
		schedule.addTransitLine(line1);
		route1.addDeparture(f.createDeparture(Id.create("1", Departure.class), 6.0*3600));
		
		TransitLine line2 = f.createTransitLine(Id.create("2", TransitLine.class));
		List<TransitRouteStop> stops2 = new ArrayList<>();
		stops2.add(f.createTransitRouteStop(f1, Time.getUndefinedTime(), 0.0));
		stops2.add(f.createTransitRouteStop(f3, Time.getUndefinedTime(), 750.0));
		stops2.add(f.createTransitRouteStop(f5, 1100.0, Time.getUndefinedTime()));
		TransitRoute route2 = f.createTransitRoute(Id.create("2", TransitRoute.class), null, stops2, "pt");
		line2.addRoute(route2);
		schedule.addTransitLine(line2);
		route2.addDeparture(f.createDeparture(Id.create("2", Departure.class), 6.0*3600 - 60));
	}
}
