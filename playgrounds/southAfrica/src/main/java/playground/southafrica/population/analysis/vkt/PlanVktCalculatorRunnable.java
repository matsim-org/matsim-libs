/* *********************************************************************** *
 * project: org.matsim.*
 * PlanVkmRunnable.java
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

package playground.southafrica.population.analysis.vkt;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.AStarLandmarks;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;


public class PlanVktCalculatorRunnable implements Runnable {
	private final Logger LOG = Logger.getLogger(PlanVktCalculatorRunnable.class);
	private Plan plan;
	private NetworkImpl network;
	private AStarLandmarks router;
	private String filename;
	private Map<RoadType, Double> typeVkmTotal;
	private Counter counter;

	public PlanVktCalculatorRunnable(Plan plan, DigicoreNetworkRouterFactory factory, 
			String outputfolder, Counter counter) {
		this.plan = plan;
		this.network = factory.getNetwork();
		this.router = factory.createRouter();
		this.filename = outputfolder + plan.getPerson().getId().toString() + ".tmp";
		this.counter = counter;
		this.typeVkmTotal = new TreeMap<RoadType, Double>();
		for(RoadType type : RoadType.values()){
			typeVkmTotal.put(type, new Double(0.0));
		}
	}

	@Override
	public void run() {
		Double vkm = new Double(getEstimatedVkt(plan));
		int numberOfActivities = 0;
		for(PlanElement pe : plan.getPlanElements()){
			if(pe instanceof Activity){
				numberOfActivities++;
			}
		}
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			bw.write(plan.getPerson().getId().toString());
			bw.write(",");
			bw.write(String.format("%.2f,%d,", vkm/1000.0, numberOfActivities));
			bw.write(String.format("%.2f,", typeVkmTotal.get(RoadType.FREEWAY)));
			bw.write(String.format("%.2f,", typeVkmTotal.get(RoadType.ARTERIAL)));
			bw.write(String.format("%.2f,", typeVkmTotal.get(RoadType.STREET)));
			bw.write(String.format("%.2f", typeVkmTotal.get(RoadType.OTHER)));
			bw.newLine();
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + filename);
			}
		}			
		this.plan = null;
		this.network = null;
		this.router = null;
		this.filename = null;
		this.typeVkmTotal = null;

		counter.incCounter();
		this.counter = null;
	}
	
		
	/**
	 * Returns the vehicle-kilometres travelled (vkt) travelled for a given 
	 * {@link Plan}. If the {@link Plan} has a {@link Leg} that already has a
	 * {@link Route} associated with it, the actual {@link Route} distance will
	 * be used. Otherwise, the shortest path between the two consecutive 
	 * activities will be calculated. For each activity, the network node 
	 * closest to the activity is used.
	 * TODO Create a test!!!
	 * 
	 * <h5>Note:</h5> Use sparingly! I don't know what the computational burden is
	 * of creating a router every time this method is called (JWJ 201207).<br><br>
	 * @param  
	 */
	private double getEstimatedVkt(Plan plan){
		double vkt = 0;
		for(int i = 0; i < plan.getPlanElements().size()-1; i+=2){
			
			/* Read the sequence of activity-leg-activity. */
			Object a1 = plan.getPlanElements().get(i);
			Node fromNode = null;
			if(a1 instanceof Activity){
				fromNode = network.getNearestNode(((Activity) a1).getCoord());
			} else{
				LOG.error("Origin node not of type Activity.");
			}
			
			Object l = plan.getPlanElements().get(i+1);
			LegImpl leg = null;
			if(l instanceof Leg){
				leg = (LegImpl) l;
			} else{
				LOG.error("PlanElement between " + ((Activity)a1).getType() + " and " +
						((Activity)l).getType() + " is not a Leg.");
			}
			
			Object a2 = plan.getPlanElements().get(i+2);
			Node toNode = null;
			if(a2 instanceof Activity){
				toNode = network.getNearestNode(((Activity) a2).getCoord());
			} else{
				LOG.error("Destination node not of type Activity.");
			}
			
			/* Check if a route exists. If so, use it, else estimate a new route. */
			Route route = leg.getRoute();
			
			List<Link> listOfLegLinks = null;
			if(route != null){
				/* Use actual leg route. */
				listOfLegLinks = RouteUtils.getLinksFromNodes(RouteUtils.getNodes((NetworkRoute) route, network));
				vkt += RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) route, network);
			} else{
				/* Estimate leg route. */
				double startTime = Math.max(((Activity) a1).getEndTime(), ((Activity) a2).getStartTime());
				Path path = router.calcLeastCostPath(fromNode, toNode, startTime, null, null);
				listOfLegLinks = path.links;
				vkt += path.travelCost;
			}
			splitVkmOnRoadType(listOfLegLinks);
		}
		return vkt;
	}


	private void splitVkmOnRoadType(List<Link> links){
		for(Link link : links){
			LinkImpl li = (LinkImpl) link;
			RoadType roadType = RoadType.getRoadType(li.getType());
			double oldValue = typeVkmTotal.get(roadType);
			typeVkmTotal.put(roadType, oldValue + (li.getLength()/1000));
 		}
	}

}

