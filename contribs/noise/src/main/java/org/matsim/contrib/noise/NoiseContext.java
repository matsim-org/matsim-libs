/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.noise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.locationtech.jts.algorithm.Angle;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.vehicles.Vehicle;

/**
 * Contains the grid and further noise-specific information.
 * 
 * @author lkroeger, ikaddoura
 *
 */
final class NoiseContext {
	
	private static final Logger log = Logger.getLogger(NoiseContext.class);
			
	private final Scenario scenario;
	private final NoiseConfigGroup noiseParams;
	private final Grid grid;
	private final ShieldingContext shielding;

	private final Map<Tuple<Integer,Integer>, List<Id<Link>>> zoneTuple2listOfLinkIds = new HashMap<>();
	private double xCoordMinLinkNode = Double.MAX_VALUE;
//	private double xCoordMaxLinkNode = Double.MIN_VALUE;
//	private double yCoordMinLinkNode = Double.MAX_VALUE;
	private double yCoordMaxLinkNode = Double.MIN_VALUE;
	
	private final Set<Id<Vehicle>> asBusConsideredTransitVehicleIDs = new HashSet<>();
	private final Set<Id<Vehicle>> notConsideredTransitVehicleIDs = new HashSet<>();
	private final Set<Id<Vehicle>> ignoredNetworkModeVehicleIDs = new HashSet<>();

	private final Map<Id<Link>, Map<Id<Vehicle>, Double>> linkId2vehicleId2lastEnterTime = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Person>> vehicleId2personId = new HashMap<>();
	
	// for routing purposes in case the default noise travel distuility is used
	private final Map<Double, Map<Id<Link>, NoiseLink>> timeInterval2linkId2noiseLinks = new HashMap<>();
	
	// time interval specific information
	
	private double currentTimeBinEndTime;
	private final Map<Id<Link>, NoiseLink> noiseLinks;
	private double eventTime = Double.MIN_VALUE;

	private final Map<Id<ReceiverPoint>, NoiseReceiverPoint> noiseReceiverPoints;
	
	// ############################################

	@Inject
	NoiseContext(Scenario scenario) {
		log.warn("instantiating NoiseContext") ;

		this.scenario = scenario;

//		if ((NoiseConfigGroup) this.scenario.getConfig().getModules().get(NoiseConfigGroup.GROUP_NAME) == null) {
		noiseParams = ConfigUtils.addOrGetModule(this.scenario.getConfig(), NoiseConfigGroup.class);
		if (noiseParams == null) {
			throw new RuntimeException("Could not find a noise config group. "
					+ "Check if the custom module is loaded, e.g. 'ConfigUtils.loadConfig(configFile, new NoiseConfigGroup())'"
					+ " Aborting...");
		}
		
//		this.noiseParams = (NoiseConfigGroup) this.scenario.getConfig().getModules().get(NoiseConfigGroup.GROUP_NAME);
//		this.noiseParams.checkNoiseParametersForConsistency();
		
		this.grid = new Grid(scenario);
				
		this.currentTimeBinEndTime = noiseParams.getTimeBinSizeNoiseComputation();
		
		this.noiseReceiverPoints = new HashMap<>();
		this.noiseLinks = new HashMap<>();
		
		checkConsistency();

        if(noiseParams.isConsiderNoiseBarriers()) {
            final Collection<FeatureNoiseBarrierImpl> barriers
                    = FeatureNoiseBarriersReader.read(noiseParams.getNoiseBarriersFilePath(), noiseParams.getNoiseBarriersSourceCRS(), scenario.getConfig().global().getCoordinateSystem());
            shielding = new ShieldingContext(barriers);
        } else {
            shielding = null;
        }

		setLinksMinMax();
		setLinksToZones();
		setRelevantLinkInfo();
	}

	// for routing purposes
	public final void storeTimeInterval() {

		Map<Id<Link>, NoiseLink> noiseLinksThisTimeBinCopy = new HashMap<>(this.noiseLinks);
		
		double currentTimeIntervalCopy = this.currentTimeBinEndTime;
		
		this.timeInterval2linkId2noiseLinks.put(currentTimeIntervalCopy, noiseLinksThisTimeBinCopy);
	}
	
	private void checkConsistency() {
		
		if (this.scenario.getPopulation() == null || this.scenario.getPopulation().getPersons().isEmpty()) {
			throw new RuntimeException("The noise computation requires a loaded population to identify passenger cars."
					+ " Please provide a scenario which contains a loaded population. Aborting...");
		}
		List<String> consideredActivitiesForDamagesList = new ArrayList<String>();
		List<String> consideredActivitiesForReceiverPointGridList = new ArrayList<String>();

		Collections.addAll(consideredActivitiesForDamagesList, this.grid.getGridParams().getConsideredActivitiesForDamageCalculationArray());

		Collections.addAll(consideredActivitiesForReceiverPointGridList, this.grid.getGridParams().getConsideredActivitiesForReceiverPointGridArray());
		
		if (this.noiseParams.isComputeNoiseDamages()) {
			
			if (consideredActivitiesForDamagesList.size() == 0) {
				log.warn("Not considering any activity type for the noise damage computation."
						+ "The computation of noise damages should be disabled.");
			}
			
			if (this.grid.getGridParams().getReceiverPointsGridMaxX() != 0.
					|| this.grid.getGridParams().getReceiverPointsGridMinX() != 0.
					|| this.grid.getGridParams().getReceiverPointsGridMaxY() != 0.
					|| this.grid.getGridParams().getReceiverPointsGridMinY() != 0.) {
				log.warn("In order to keep track of the agent activities, the grid of receiver points should not be limited to a set of predefined coordinates."
						+ "For a grid covering all activity locations, set the minimum and maximum x/y parameters to 0.0. "
						+ "There will be more agents mapped to the receiver points at the edges. Only the inner receiver points should be used for analysis.");
			}
						
			if (this.grid.getGridParams().getReceiverPointsGridMinX() == 0. && this.grid.getGridParams().getReceiverPointsGridMinY() == 0. && this.grid.getGridParams().getReceiverPointsGridMaxX() == 0. && this.grid.getGridParams().getReceiverPointsGridMaxY() == 0.) {
				for (String type : consideredActivitiesForDamagesList) {
					if (!consideredActivitiesForReceiverPointGridList.contains(type)) {
						throw new RuntimeException("An activity type which is considered for the damage calculation (" + type
								+ ") should also be considered for the minimum and maximum coordinates of the receiver point grid area. Aborting...");
					}
				}
			}
		}
	}

	private void setRelevantLinkInfo() {

		Counter cnt = new Counter("set relevant link-info # ");
		// go through all rp's and throw them away. We need noise-rps from here on.
		ConcurrentLinkedQueue<ReceiverPoint> rps = new ConcurrentLinkedQueue<>(this.grid.getAndClearReceiverPoints().values());
		ReceiverPoint rp = null;
		while((rp = rps.poll()) != null){
			
			NoiseReceiverPoint nrp = new NoiseReceiverPoint(rp.getId(), rp.getCoord());

			// get the zone grid cell around the receiver point
			Set<Id<Link>> potentialLinks = new HashSet<>();
			Tuple<Integer,Integer>[] zoneTuples = getZoneTuplesForLinks(nrp.getCoord());
			for(Tuple<Integer, Integer> key: zoneTuples) {
				List<Id<Link>> links = zoneTuple2listOfLinkIds.get(key);
				if(links != null) {
					potentialLinks.addAll(links);
				}
			}

			// go through these potential relevant link Ids
			Set<Id<Link>> relevantLinkIds = ConcurrentHashMap.newKeySet();
			potentialLinks.parallelStream().forEach( linkId -> {
				if (!(relevantLinkIds.contains(linkId))) {
					Link candidateLink = scenario.getNetwork().getLinks().get(linkId);
					double projectedDistance = CoordUtils.distancePointLinesegment(candidateLink.getFromNode().getCoord(), candidateLink.getToNode().getCoord(), nrp.getCoord());

					if (projectedDistance < noiseParams.getRelevantRadius()){
						
						relevantLinkIds.add(linkId);
						// wouldn't it be good to check distance < minDistance here? DR20180215
						if (projectedDistance == 0) {
							double minimumDistance = 5.;
							projectedDistance = minimumDistance;
							log.warn("Distance between " + linkId + " and " + nrp.getId() + " is 0. The calculation of the correction term Ds requires a distance > 0. Therefore, setting the distance to a minimum value of " + minimumDistance + ".");
						}
						double correctionTermDs = NoiseEquations.calculateDistanceCorrection(projectedDistance);
						double correctionTermAngle = calculateAngleImmissionCorrection(nrp.getCoord(), scenario.getNetwork().getLinks().get(linkId));
						nrp.setLinkId2distanceCorrection(linkId, correctionTermDs);
						nrp.setLinkId2angleCorrection(linkId, correctionTermAngle);
						if(noiseParams.isConsiderNoiseBarriers()) {
							Coord projectedSourceCoord = CoordUtils.orthogonalProjectionOnLineSegment(
									candidateLink.getFromNode().getCoord(), candidateLink.getToNode().getCoord(), nrp.getCoord());
							double correctionTermShielding =
                                    shielding.determineShieldingCorrection(nrp, candidateLink, projectedSourceCoord);
							nrp.setLinkId2ShieldingCorrection(linkId, correctionTermShielding);
						}
					}
				}
			});
			
			this.noiseReceiverPoints.put(nrp.getId(), nrp);
			cnt.incCounter();
		}
		cnt.printCounter();
	}

	/**
	 * @param coord
	 * @return
	 */
	private Tuple<Integer, Integer>[] getZoneTuplesForLinks(Coord coord) {
		Tuple<Integer, Integer> zoneTuple = getZoneTupleForLinks(coord);
		int x = zoneTuple.getFirst();
		int y = zoneTuple.getSecond();
		return new Tuple[] {
				zoneTuple,
				new Tuple<Integer, Integer>(x-1, y-1),
				new Tuple<Integer, Integer>(x, y-1),
				new Tuple<Integer, Integer>(x+1, y-1),
				new Tuple<Integer, Integer>(x-1, y),
				new Tuple<Integer, Integer>(x+1, y),
				new Tuple<Integer, Integer>(x-1, y+1),
				new Tuple<Integer, Integer>(x, y+1),
				new Tuple<Integer, Integer>(x+1, y+1)
		};
	}

	private void setLinksMinMax() {
		log.info("compute network bounding box");
		double[] bb = NetworkUtils.getBoundingBox(scenario.getNetwork().getNodes().values());
		xCoordMinLinkNode = bb[0];
//		yCoordMinLinkNode = bb[1];
//		xCoordMaxLinkNode = bb[2];
		yCoordMaxLinkNode = bb[3];
	}
	
	private void setLinksToZones() {
		Counter cnt = new Counter("set links to zones #");
		for (Link link : scenario.getNetwork().getLinks().values()){
			
			// split up the link into link segments with the following length
			double partLength = 0.25 * noiseParams.getRelevantRadius();
			int parts = (int) (link.getLength()/partLength);

			double fromX = link.getFromNode().getCoord().getX();
			double fromY = link.getFromNode().getCoord().getY();
			double toX = link.getToNode().getCoord().getX();
			double toY = link.getToNode().getCoord().getY();
			double vectorX = toX - fromX;
			double vectorY = toY - fromY;
			
			// collect the coordinates of this link
			Set<Coord> coords = new HashSet<Coord>();
			coords.add(link.getFromNode().getCoord());
			coords.add(link.getToNode().getCoord());
			for (int i = 1 ; i<parts ; i++) {
				double x = fromX + (i*((1./(parts))*vectorX));
				double y = fromY + (i*((1./(parts))*vectorY));
				Coord  coordTmp = new Coord(x, y);
				coords.add(coordTmp);
			}
			
			// get zone grid cells for these coordinates
			Set<Tuple<Integer,Integer>> relevantTuples = new HashSet<Tuple<Integer,Integer>>();
			for (Coord coord : coords) {
				relevantTuples.add(getZoneTupleForLinks(coord));
			}
			
			// go through these zone grid cells and save the link Id 			
			for(Tuple<Integer,Integer> tuple : relevantTuples) {
				List<Id<Link>> linkIds = zoneTuple2listOfLinkIds.computeIfAbsent(tuple, k -> new ArrayList<>());
				linkIds.add(link.getId());
			}
			cnt.incCounter();
		}
		cnt.printCounter();
	}
	
	private Tuple<Integer,Integer> getZoneTupleForLinks(Coord coord) {
		 
		double xCoord = coord.getX();
		double yCoord = coord.getY();
		
		int xDirection = (int) ((xCoord - xCoordMinLinkNode) / (noiseParams.getRelevantRadius() / 1.));	
		int yDirection = (int) ((yCoordMaxLinkNode - yCoord) / noiseParams.getRelevantRadius() / 1.);

		return new Tuple<>(xDirection, yDirection);
	}
	
	private double calculateAngleImmissionCorrection(Coord receiverPointCoord, Link link) {

		double angle = 0;
		
		double pointCoordX = receiverPointCoord.getX();
		double pointCoordY = receiverPointCoord.getY();
		
		double fromCoordX = link.getFromNode().getCoord().getX();
		double fromCoordY = link.getFromNode().getCoord().getY();
		double toCoordX = link.getToNode().getCoord().getX();
		double toCoordY = link.getToNode().getCoord().getY();

		if (pointCoordX == fromCoordX && pointCoordY == fromCoordY) {
			// receiver point is situated on the link (fromNode)
			// assume a maximum angle immission correction for this case
			angle = 0;
			
		} else if (pointCoordX == toCoordX && pointCoordY == toCoordY) {
			// receiver point is situated on the link (toNode)
			// assume a zero angle immission correction for this case
			angle = 180;		
			
		} else {
			// all other cases
            angle = Angle.toDegrees(Angle.angleBetween(
                    CoordUtils.createGeotoolsCoordinate(link.getFromNode().getCoord()),
                    CoordUtils.createGeotoolsCoordinate(receiverPointCoord),
                    CoordUtils.createGeotoolsCoordinate(link.getToNode().getCoord())));
		}

		// since zero is not defined
		if (angle == 0.) {
			// zero degrees is not defined
			angle = 0.0000000001;
		}
					
//		System.out.println(receiverPointCoord + " // " + link.getId() + "(" + link.getFromNode().getCoord() + "-->" + link.getToNode().getCoord() + " // " + angle);
        return NoiseEquations.calculateAngleCorrection(angle);
	}
	
	public final Scenario getScenario() {
		return scenario;
	}
	
	public final Map<Id<ReceiverPoint>, NoiseReceiverPoint> getReceiverPoints() {
		return noiseReceiverPoints;
	}
	
	public final NoiseConfigGroup getNoiseParams() {
		return noiseParams;
	}

	public final double getCurrentTimeBinEndTime() {
		return currentTimeBinEndTime;
	}

	public final void setCurrentTimeBinEndTime(double currentTimeBinEndTime) {
		this.currentTimeBinEndTime = currentTimeBinEndTime;
	}

	public final Map<Id<Link>, NoiseLink> getNoiseLinks() {
		return noiseLinks;
	}

	public final Map<Double, Map<Id<Link>, NoiseLink>> getTimeInterval2linkId2noiseLinks() {
		return timeInterval2linkId2noiseLinks;
	}

	public final void setEventTime(double time) {
		this.eventTime = time;
	}

	public final double getEventTime() {
		return eventTime;
	}

	public final Grid getGrid() {
		return grid;
	}

	public Set<Id<Vehicle>> getBusVehicleIDs() {
		return asBusConsideredTransitVehicleIDs;
	}

	public Map<Id<Link>, Map<Id<Vehicle>, Double>> getLinkId2vehicleId2lastEnterTime() {
		return linkId2vehicleId2lastEnterTime;
	}

	public Set<Id<Vehicle>> getNotConsideredTransitVehicleIDs() {
		return notConsideredTransitVehicleIDs;
	}

	public Map<Id<Vehicle>, Id<Person>> getVehicleId2PersonId() {
		return vehicleId2personId;
	}

	public Set<Id<Vehicle>> getIgnoredNetworkModeVehicleIDs() {
		return ignoredNetworkModeVehicleIDs;
	}
}
