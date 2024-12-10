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

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Contains the grid and further noise-specific information.
 * 
 * @author lkroeger, ikaddoura
 *
 */
final class NoiseContextImpl implements NoiseContext {
	
	private static final Logger log = LogManager.getLogger(NoiseContextImpl.class);

	@Inject
	private Scenario scenario;
	private final NoiseConfigGroup noiseParams;
	private final Grid grid;

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
	private NoiseContextImpl(Scenario scenario) {
		this.scenario = scenario;

		noiseParams = ConfigUtils.addOrGetModule(this.scenario.getConfig(), NoiseConfigGroup.class);
		if (noiseParams == null) {
			throw new RuntimeException("Could not find a noise config group. "
					+ "Check if the custom module is loaded, e.g. 'ConfigUtils.loadConfig(configFile, new NoiseConfigGroup())'"
					+ " Aborting...");
		}
		
		this.grid = new Grid(scenario);
				
		this.currentTimeBinEndTime = noiseParams.getTimeBinSizeNoiseComputation();
		
		this.noiseReceiverPoints = new HashMap<>();
		this.noiseLinks = new HashMap<>();
		checkConsistency();
		setLinksMinMax();
		setLinksToZones();
		checkTunnels();
	}

	private void checkTunnels() {
		if(noiseParams.getTunnelLinkIdFile() != null) {
			final URL tunnelUrl = this.noiseParams.getTunnelLinkIDsFileURL(this.scenario.getConfig().getContext());
			try {
				if (Files.exists(Paths.get(tunnelUrl.toURI()))) {

					if (this.noiseParams.getTunnelLinkIDsSet().size() > 0) {
						log.warn("Loading the tunnel link IDs from a file. Deleting the existing tunnel link IDs that are added manually.");
						this.noiseParams.getTunnelLinkIDsSet().clear();
					}

					// loading tunnel link IDs from file
					BufferedReader br = IOUtils.getBufferedReader(tunnelUrl.getFile());

					String line = null;
					try {
						line = br.readLine();
					} catch (IOException e) {
						e.printStackTrace();
					} // headers

					log.info("Reading tunnel link Id file...");
					try {
						int countWarning = 0;
						while ((line = br.readLine()) != null) {

							String[] columns = line.split(";");
							Id<Link> linkId = null;
							for (int column = 0; column < columns.length; column++) {
								if (column == 0) {
									linkId = Id.createLinkId(columns[column]);
								} else {
									if (countWarning < 1) {
										log.warn("Expecting the tunnel link Id to be in the first column. Ignoring further columns...");
									} else if (countWarning == 1) {
										log.warn("This message is only given once.");
									}
									countWarning++;
								}
							}
							log.info("Adding tunnel link ID " + linkId);
							this.noiseParams.getTunnelLinkIDsSet().add(linkId);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}

					log.info("Reading tunnel link Id file... Done.");
				}
			} catch (URISyntaxException e) {
				log.warn("Could not read tunnels.");
			}
		}
	}

	// for routing purposes
	@Override
	public final void storeTimeInterval() {

		Map<Id<Link>, NoiseLink> noiseLinksThisTimeBinCopy = new HashMap<>(this.noiseLinks);

		double currentTimeIntervalCopy = this.currentTimeBinEndTime;

		this.timeInterval2linkId2noiseLinks.put(currentTimeIntervalCopy, noiseLinksThisTimeBinCopy);
	}

	private void checkConsistency() {
		

		List<String> consideredActivitiesForDamagesList = new ArrayList<String>();
		List<String> consideredActivitiesForReceiverPointGridList = new ArrayList<String>();

		Collections.addAll(consideredActivitiesForDamagesList, this.grid.getGridParams().getConsideredActivitiesForDamageCalculationArray());

		Collections.addAll(consideredActivitiesForReceiverPointGridList, this.grid.getGridParams().getConsideredActivitiesForReceiverPointGridArray());
		
		if (this.noiseParams.isComputeNoiseDamages()) {

			if (this.scenario.getPopulation() == null || this.scenario.getPopulation().getPersons().isEmpty()) {
				throw new RuntimeException("The noise computation requires a loaded population to identify passenger cars."
						+ " Please provide a scenario which contains a loaded population. Aborting...");
			}
			
			if (consideredActivitiesForDamagesList.size() == 0) {
				log.warn("Not considering any activity type for the noise damage computation."
						+ "The computation of noise damages should be disabled.");
			}
			
			if (this.grid.getGridParams().getReceiverPointsGridMaxX() != 0.
					|| this.grid.getGridParams().getReceiverPointsGridMinX() != 0.
					|| this.grid.getGridParams().getReceiverPointsGridMaxY() != 0.
					|| this.grid.getGridParams().getReceiverPointsGridMinY() != 0.) {
				log.warn("In order to keep track of ALL the agent activities, the grid of receiver points should not be limited to a set of predefined coordinates."
						+ "For a grid covering all activity locations, set the minimum and maximum x/y parameters to 0.0. "
						+ "Damages will be computed only for activities that are performed within the receiver point grid.");
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
				new Tuple<>(x-1, y-1),
				new Tuple<>(x, y-1),
				new Tuple<>(x+1, y-1),
				new Tuple<>(x-1, y),
				new Tuple<>(x+1, y),
				new Tuple<>(x-1, y+1),
				new Tuple<>(x, y+1),
				new Tuple<>(x+1, y+1)
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

	@Override
	public final Scenario getScenario() {
		return scenario;
	}

	@Override
	public final Map<Id<ReceiverPoint>, NoiseReceiverPoint> getReceiverPoints() {
		return noiseReceiverPoints;
	}

	@Override
	public final NoiseConfigGroup getNoiseParams() {
		return noiseParams;
	}

	@Override
	public final double getCurrentTimeBinEndTime() {
		return currentTimeBinEndTime;
	}

	@Override
	public final void setCurrentTimeBinEndTime(double currentTimeBinEndTime) {
		this.currentTimeBinEndTime = currentTimeBinEndTime;
	}

	@Override
	public final Map<Id<Link>, NoiseLink> getNoiseLinks() {
		return noiseLinks;
	}

	@Override
	public final Map<Double, Map<Id<Link>, NoiseLink>> getTimeInterval2linkId2noiseLinks() {
		return timeInterval2linkId2noiseLinks;
	}

	@Override
	public final void setEventTime(double time) {
		this.eventTime = time;
	}

	@Override
	public final double getEventTime() {
		return eventTime;
	}

	@Override
	public final Grid getGrid() {
		return grid;
	}

	@Override
	public Set<Id<Vehicle>> getBusVehicleIDs() {
		return asBusConsideredTransitVehicleIDs;
	}

	@Override
	public Map<Id<Link>, Map<Id<Vehicle>, Double>> getLinkId2vehicleId2lastEnterTime() {
		return linkId2vehicleId2lastEnterTime;
	}

	@Override
	public Set<Id<Vehicle>> getNotConsideredTransitVehicleIDs() {
		return notConsideredTransitVehicleIDs;
	}

	@Override
	public Map<Id<Vehicle>, Id<Person>> getVehicleId2PersonId() {
		return vehicleId2personId;
	}

	@Override
	public Set<Id<Vehicle>> getIgnoredNetworkModeVehicleIDs() {
		return ignoredNetworkModeVehicleIDs;
	}

	@Override
	public void reset() {
		this.getNoiseLinks().clear();
		this.getTimeInterval2linkId2noiseLinks().clear();
		this.getLinkId2vehicleId2lastEnterTime().clear();
		this.setCurrentTimeBinEndTime(this.getNoiseParams().getTimeBinSizeNoiseComputation());
		this.getVehicleId2PersonId().clear();
	}

	@Override
	public Set<Id<Link>> getPotentialLinks(NoiseReceiverPoint nrp) {
		Set<Id<Link>> potentialLinks = new HashSet<>();
		Tuple<Integer, Integer>[] zoneTuples = getZoneTuplesForLinks(nrp.getCoord());
		for (Tuple<Integer, Integer> key : zoneTuples) {
			List<Id<Link>> links = zoneTuple2listOfLinkIds.get(key);
			if (links != null) {
				potentialLinks.addAll(links);
			}
		}
		return potentialLinks;
	}
}
