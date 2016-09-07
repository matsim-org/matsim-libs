package scenarios.braess.analysis;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import scenarios.illustrative.braess.analysis.TtAnalyzeBraess;

/**
 * This class tests the functionality of the class
 * TtAnalyzeBraessRouteDistributionAndTT by feeding it with artificial events.
 * 
 * @author tthunig
 * 
 */
public class TtAnalyzeBraessRouteDistributionAndTTTest {

	@Test
	public void testHandlerViaEvents(){
		TtAnalyzeBraess handler = new TtAnalyzeBraess();
		
		Id<Link> firstLinkId = Id.createLinkId("0_1");
		Id<Link> middleLinkId = Id.createLinkId("3_4");
		Id<Link> lastLinkId = Id.createLinkId("5_6");
		
		Id<Person> agent1Id = Id.createPersonId(1);
		Id<Vehicle> vehicle1Id = Id.createVehicleId(1);
		Id<Person> agent2Id = Id.createPersonId(2);
		Id<Vehicle> vehicle2Id = Id.createVehicleId(2);
		
		double expectedTotalTT = 20 + 30;
		int[] expectedRouteUsers = {0,2,0};
		int[] expectedRouteDeparturesTimeZero = {0,1,0};
		int[] expectedRouteDeparturesTimeOne = {0,1,0};
		double[] expectedTotalRouteTTs = {Double.NaN, 50, Double.NaN};
		double[] expectedAvgRouteTTs = {Double.NaN, 25, Double.NaN};
		double[] expectedRouteTTsAtDepartureTimeZero = {Double.NaN, 20, Double.NaN};
		double[] expectedRouteTTsAtDepartureTimeOne = {Double.NaN, 30, Double.NaN};
		double[] expectedTotalRouteTolls = {Double.NaN, 5, Double.NaN};
		double[] expectedAvgRouteTolls = {Double.NaN, 2.5, Double.NaN};
		double[] expectedRouteTollsAtDepartureTimeZero = {Double.NaN, 5, Double.NaN};
		double[] expectedRouteTollsAtDepartureTimeOne = {Double.NaN, 0, Double.NaN};
		
		handler.handleEvent(new PersonDepartureEvent(0, agent1Id, firstLinkId, TransportMode.car));
		handler.handleEvent(new PersonEntersVehicleEvent(0, agent1Id, vehicle1Id));
		handler.handleEvent(new LinkEnterEvent(10, vehicle1Id, middleLinkId));
		handler.handleEvent(new PersonMoneyEvent(15, agent1Id, -5));
		handler.handleEvent(new PersonArrivalEvent(20, agent1Id, lastLinkId, TransportMode.car));
		
		double departureAgent2 = 1;
		handler.handleEvent(new PersonDepartureEvent(departureAgent2, agent2Id, firstLinkId, TransportMode.car));
		handler.handleEvent(new PersonEntersVehicleEvent(0, agent2Id, vehicle2Id));
		handler.handleEvent(new LinkEnterEvent(11, vehicle2Id, middleLinkId));
		handler.handleEvent(new PersonArrivalEvent(30 + departureAgent2, agent2Id, lastLinkId, TransportMode.car));
		
		Assert.assertEquals("total travel time not correct", expectedTotalTT , handler.getTotalTT(), MatsimTestUtils.EPSILON);
		Assert.assertArrayEquals("number of route users not correct", expectedRouteUsers, handler.getRouteUsers());
		Assert.assertArrayEquals("avg route travel times not correct", expectedAvgRouteTTs , handler.calculateAvgRouteTTs(), MatsimTestUtils.EPSILON);
		Assert.assertArrayEquals("total route travel times not correct", expectedTotalRouteTTs , handler.getTotalRouteTTs(), MatsimTestUtils.EPSILON);
		Assert.assertArrayEquals("total route tolls not correct", expectedTotalRouteTolls , handler.getTotalRouteTolls(), MatsimTestUtils.EPSILON);
		Assert.assertArrayEquals("avg route tolls not correct", expectedAvgRouteTolls , handler.calculateAvgRouteTolls(), MatsimTestUtils.EPSILON);
		Assert.assertArrayEquals("number of route departures at time 0 not correct", expectedRouteDeparturesTimeZero, handler.getRouteDeparturesPerSecond().get(0.));
		Assert.assertArrayEquals("number of route departures at time 1 not correct", expectedRouteDeparturesTimeOne, handler.getRouteDeparturesPerSecond().get(1.));
		Assert.assertArrayEquals("avg route travel times at departure time 0 not correct", expectedRouteTTsAtDepartureTimeZero , handler.calculateAvgRouteTTsByDepartureTime().get(0.), MatsimTestUtils.EPSILON);
		Assert.assertArrayEquals("avg route travel times at departure time 1 not correct", expectedRouteTTsAtDepartureTimeOne , handler.calculateAvgRouteTTsByDepartureTime().get(1.), MatsimTestUtils.EPSILON);
		Assert.assertArrayEquals("avg route tolls at departure time 0 not correct", expectedRouteTollsAtDepartureTimeZero , handler.calculateAvgRouteTollsByDepartureTime().get(0.), MatsimTestUtils.EPSILON);
		Assert.assertArrayEquals("avg route tolls at departure time 1 not correct", expectedRouteTollsAtDepartureTimeOne , handler.calculateAvgRouteTollsByDepartureTime().get(1.), MatsimTestUtils.EPSILON);
	}

}
