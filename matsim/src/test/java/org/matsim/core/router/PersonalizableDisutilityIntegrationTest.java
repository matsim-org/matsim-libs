/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.router;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.util.FastAStarEuclideanFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessEuclidean;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;

/**
 * @author mrieser / senozon
 */
public class PersonalizableDisutilityIntegrationTest {
	
	@Test
	public void testPersonAvailableForDisutility_Dijkstra() {
		Fixture f = new Fixture();
		
		Dijkstra router = new Dijkstra(f.network, f.costFunction, new FreeSpeedTravelTime());
		router.calcLeastCostPath(
				f.network.getNodes().get(Id.create("2", Node.class)), 
				f.network.getNodes().get(Id.create("1", Node.class)),
				07*3600, f.person, f.vehicle);
		// hopefully there was no Exception until here...
		
		Assert.assertEquals(22, f.costFunction.cnt); // make sure the costFunction was actually used
	}
	
	@Test
	public void testPersonAvailableForDisutility_FastDijkstra() {
		Fixture f = new Fixture();
		
		LeastCostPathCalculatorFactory routerFactory = new FastDijkstraFactory();
		LeastCostPathCalculator router = routerFactory.createPathCalculator(f.network, f.costFunction, new FreeSpeedTravelTime());
		router.calcLeastCostPath(
				f.network.getNodes().get(Id.create("2", Node.class)), 
				f.network.getNodes().get(Id.create("1", Node.class)), 
				07*3600, f.person, f.vehicle);
		// hopefully there was no Exception until here...
		
		Assert.assertEquals(22, f.costFunction.cnt); // make sure the costFunction was actually used
	}

	@Test
	public void testPersonAvailableForDisutility_AStarEuclidean() {
		Fixture f = new Fixture();
		PreProcessEuclidean preprocess = new PreProcessEuclidean(f.costFunction);
		preprocess.run(f.network);
		AStarEuclidean router = new AStarEuclidean(f.network, preprocess, new FreeSpeedTravelTime());
		router.calcLeastCostPath(
				f.network.getNodes().get(Id.create("2", Node.class)), 
				f.network.getNodes().get(Id.create("1", Node.class)), 
				07*3600, f.person, f.vehicle);
		// hopefully there was no Exception until here...
		
		Assert.assertEquals(22, f.costFunction.cnt); // make sure the costFunction was actually used
	}
	
	@Test
	public void testPersonAvailableForDisutility_FastAStarEuclidean() {
		Fixture f = new Fixture();
		PreProcessEuclidean preprocess = new PreProcessEuclidean(f.costFunction);
		preprocess.run(f.network);
		AStarEuclidean router = new AStarEuclidean(f.network, preprocess, new FreeSpeedTravelTime());
		router.calcLeastCostPath(
				f.network.getNodes().get(Id.create("2", Node.class)), 
				f.network.getNodes().get(Id.create("1", Node.class)), 
				07*3600, f.person, f.vehicle);
		// hopefully there was no Exception until here...
		
		Assert.assertEquals(22, f.costFunction.cnt); // make sure the costFunction was actually used
	}

	@Test
	public void testPersonAvailableForDisutility_AStarLandmarks() {
		Fixture f = new Fixture();
		LeastCostPathCalculatorFactory routerFactory = new FastAStarEuclideanFactory(f.network, f.costFunction);
		LeastCostPathCalculator router = routerFactory.createPathCalculator(f.network, f.costFunction, new FreeSpeedTravelTime());
		router.calcLeastCostPath(
				f.network.getNodes().get(Id.create("2", Node.class)), 
				f.network.getNodes().get(Id.create("1", Node.class)), 
				07*3600, f.person, f.vehicle);
		// hopefully there was no Exception until here...
		
		Assert.assertEquals(22, f.costFunction.cnt); // make sure the costFunction was actually used
	}
	
	@Test
	public void testPersonAvailableForDisutility_FastAStarLandmarks() {
		Fixture f = new Fixture();
		LeastCostPathCalculatorFactory routerFactory = new FastAStarLandmarksFactory(f.network, f.costFunction);
		LeastCostPathCalculator router = routerFactory.createPathCalculator(f.network, f.costFunction, new FreeSpeedTravelTime());
		router.calcLeastCostPath(
				f.network.getNodes().get(Id.create("2", Node.class)), 
				f.network.getNodes().get(Id.create("1", Node.class)), 
				07*3600, f.person, f.vehicle);
		// hopefully there was no Exception until here...
		
		Assert.assertEquals(22, f.costFunction.cnt); // make sure the costFunction was actually used
	}
	
	private static class Fixture {
		/*package*/ final Scenario scenario;
		/*package*/ final Network network;
		/*package*/ final Vehicle vehicle;
		/*package*/ final Person person;
		/*package*/ final PersonEnforcingTravelDisutility costFunction;
		
		public Fixture() {
			this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			new MatsimNetworkReader(this.scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");
			
			this.person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
			
			this.vehicle = new VehicleImpl(null, null);
			
			this.costFunction = new PersonEnforcingTravelDisutility();
			this.costFunction.setExpectations(this.person, this.vehicle);
			
			this.network = this.scenario.getNetwork();
		}
	}
	
	private static class PersonEnforcingTravelDisutility implements TravelDisutility {

		private Person person = null;
		private Vehicle veh = null;
		
		/*package*/ int cnt = 0;

		/*package*/ void setExpectations(final Person person, final Vehicle veh) {
			this.person = person;
			this.veh = veh;
			this.cnt = 0;
		}

		@Override
		public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
			Assert.assertEquals("different person than expected!", this.person, person);
			Assert.assertEquals("different vehicle than expected!", this.veh, vehicle);
			this.cnt++;
			return 1.0;
		}

		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			return 1.0;
		}
		
	}

}
