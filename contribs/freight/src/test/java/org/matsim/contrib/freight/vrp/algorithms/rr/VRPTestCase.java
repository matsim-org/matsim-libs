/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Ignore;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgentFactoryFinder;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.TourCost;
import org.matsim.contrib.freight.vrp.basics.Coordinate;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.Locations;
import org.matsim.contrib.freight.vrp.basics.ManhattanCosts;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemType;
import org.matsim.contrib.freight.vrp.utils.VrpBuilder;
import org.matsim.contrib.freight.vrp.utils.VrpUtils;

@Ignore
public class VRPTestCase extends TestCase{
	
	static class MyLocations implements Locations{

		private Map<String,Coordinate> locations = new HashMap<String, Coordinate>();

		@Override
		public Coordinate getCoord(String id) {
			return locations.get(id);
		}
		
		public void add(String id, Coordinate coord){
			locations .put(id,coord);
		}
		
	}
	
	public Tour tour;
	
	public VehicleRoutingCosts costs;
	
	public MyLocations locations;
	
	public boolean init = false;
	
//	public TourStatusProcessor tourStatusProcessor;
	
//	public TourFactory tourFactory;
	
	
	/*
	 * 	|
	 * 10	|
	 * 9|
	 * 8|
	 * 7|
	 * 6|
	 * 5|
	 * 4|
	 * 3|
	 * 2|
	 * 1|_________________________________
	 *   1 2 3 4 5 6 7 8 9 10
	 * depotNode=0,0
	 * node(1,0)=1,0 --> 0+1=1=ungerade => delivery => demand=-1
	 * node(1,1)=1,1 --> 1+1=2=gerade => pickup => demand=+1
	 *   
	 */
	
	protected void initJobsInPlainCoordinateSystem(){
		locations = new MyLocations();
		createLocations();
		costs = new ManhattanCosts(locations);
//		tourStatusProcessor = new TourCostAndTWProcessor(costs);
//		tourFactory = new PickupAndDeliveryTourFactory(costs, constraints, tourStatusProcessor);
	}

	
	private void createLocations() {
		for(int i=0;i<11;i++){
			for(int j=0;j<11;j++){
				String id = makeId(i,j);
				Coordinate coord = makeCoord(i,j);
				locations.add(id, coord);
			}
		}
	}

	protected VehicleRoutingProblem getVRP(int nOfVehicles, int capacity){
		String depot = makeId(0,0);
		VrpBuilder vrpBuilder = new VrpBuilder(costs);
//		vrpBuilder.setDepot(depot, 0.0, Double.MAX_VALUE);
		vrpBuilder.addJob(VrpUtils.createShipment("1", makeId(0,0), makeId(0,10), 1, 
				VrpUtils.createTimeWindow(0.0, Double.MAX_VALUE), VrpUtils.createTimeWindow(0.0, Double.MAX_VALUE)));
		vrpBuilder.addJob(VrpUtils.createShipment("2", makeId(0,0), makeId(10,10), 1, 
				VrpUtils.createTimeWindow(0.0, Double.MAX_VALUE), VrpUtils.createTimeWindow(0.0, Double.MAX_VALUE)));
		vrpBuilder.addJob(VrpUtils.createShipment("3", makeId(1,4), makeId(1,5), 1, 
				VrpUtils.createTimeWindow(0.0, Double.MAX_VALUE), VrpUtils.createTimeWindow(0.0, Double.MAX_VALUE)));
		for(int i=0; i<nOfVehicles;i++){
			vrpBuilder.addVehicle(VrpUtils.createVehicle(""+i, depot, capacity, "standard"));
		}
		return vrpBuilder.build();
	}
	
	protected RuinAndRecreateSolution getInitialSolution(VehicleRoutingProblem vrp){
		TourCost tourCost = new TourCost(){

			@Override
			public double getTourCost(Tour tour, Driver driver, Vehicle vehicle) {
				return tour.tourData.transportCosts;
			}
			
		};
		return new InitialSolutionBestInsertion(new ServiceProviderAgentFactoryFinder(tourCost,vrp.getCosts()).getFactory(VehicleRoutingProblemType.CVRPTW)).createInitialSolution(vrp);
	}
	
	protected ServiceProviderAgent getTourAgent(VehicleRoutingProblem vrp, Tour tour1, Vehicle vehicle) {
		TourCost tourCost = new TourCost(){

			@Override
			public double getTourCost(Tour tour, Driver driver, Vehicle vehicle) {
				return tour.tourData.transportCosts;
			}
			
		};
		ServiceProviderAgentFactory spFactory = new ServiceProviderAgentFactoryFinder(tourCost, vrp.getCosts()).getFactory(VehicleRoutingProblemType.CVRPTW);
		return spFactory.createAgent(vehicle, new Driver(){}, tour1);
	}
	
	private Coordinate makeCoord(int i, int j) {
		return new Coordinate(i,j);
	}

	protected String makeId(int i, int j) {
		return "" + i + "," + j;
	}
	
	protected Shipment createShipment(String id, String from, String to){
		Shipment s1 = VrpUtils.createShipment(id, from, to, 1, VrpUtils.createTimeWindow(0, Double.MAX_VALUE), 
				VrpUtils.createTimeWindow(0, Double.MAX_VALUE));
		return s1;
	}


}
