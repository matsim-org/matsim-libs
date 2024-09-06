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

package org.matsim.contrib.minibus.schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkMergeDoubleLinks;
import org.matsim.core.network.algorithms.NetworkMergeDoubleLinks.LogInfoLevel;
import org.matsim.core.network.algorithms.NetworkMergeDoubleLinks.MergeType;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.algorithms.intersectionSimplifier.IntersectionSimplifier;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Create one TransitStopFacility for each car mode link of the network
 *
 * @author aneumann, droeder, gleich
 *
 */
public final class CreatePStopsOnJunctionApproachesAndBetweenJunctions{

	private final static Logger log = LogManager.getLogger(CreatePStopsOnJunctionApproachesAndBetweenJunctions.class);

	private final Network net;
	private final Network intersectionSimplifiedRoadNetwork;
	private final PConfigGroup pConfigGroup;
	private TransitSchedule transitSchedule;
	private final NetworkConfigGroup networkConfigGroup;

	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;

	private final LinkedHashMap<Id<Link>, TransitStopFacility> linkId2StopFacilityMap;

	private List<Integer> topoTypesForStops = null;
	/** This is only a rough approximation of a desirable stop distance. It is not implemented to precisely resemble the value given. */
	private final double stopDistance;

	private NetworkCalcTopoType networkCalcTopoType;

	public static TransitSchedule createPStops(Network network, PConfigGroup pConfigGroup, NetworkConfigGroup networkConfigGroup) {
		return createPStops(network, pConfigGroup, null, networkConfigGroup);
	}

	public static TransitSchedule createPStops(Network network, PConfigGroup pConfigGroup, TransitSchedule realTransitSchedule, NetworkConfigGroup networkConfigGroup) {
		CreatePStopsOnJunctionApproachesAndBetweenJunctions cS = new CreatePStopsOnJunctionApproachesAndBetweenJunctions(network, pConfigGroup, realTransitSchedule, networkConfigGroup);
		cS.run();
		return cS.getTransitSchedule();
	}

	/**
	 * Creates PStops in two ways. First, if a serviceAreaFile is defined in the config and this file exists, the file is used.
	 * Second, the (default) min/max-x/y-values are used.
	 *
	 * Following FileTypes are supported:
	 * <ul>
	 * 	<li>Shapefiles with polygons. If one ore more attributes are defined, the last one is parsed
	 *	 	to Boolean and used to get include- and exclude-areas.</li>
	 * 	<li>Textfile, containing a List of x/y-pairs per row, divided by semicolon. The first and the last coordinate should be equal
	 * 		to get a closed and well defined Geometry.</li>
	 * </ul>
	 * @param net
	 * @param pConfigGroup
	 * @param realTransitSchedule
	 */
    private CreatePStopsOnJunctionApproachesAndBetweenJunctions(Network net, PConfigGroup pConfigGroup, TransitSchedule realTransitSchedule, NetworkConfigGroup networkConfigGroup) {
		this.net = net;
		this.pConfigGroup = pConfigGroup;
		this.networkConfigGroup = networkConfigGroup;
		this.factory = new GeometryFactory();

		this.linkId2StopFacilityMap = new LinkedHashMap<>();

		Set<Id<TransitStopFacility>> stopsWithoutLinkIds = new TreeSet<>();

		int warnCounter = 10;

		if (realTransitSchedule != null) {
			for (TransitStopFacility stopFacility : realTransitSchedule.getFacilities().values()) {
				if (stopFacility.getLinkId() != null) {
					if (this.linkId2StopFacilityMap.get(stopFacility.getLinkId()) != null) {
						if (warnCounter > 0) {
							log.warn("There is more than one stop registered on link " + stopFacility.getLinkId() + ". "
									+ this.linkId2StopFacilityMap.get(stopFacility.getLinkId()).getId() + " stays registered as paratransit stop. Will ignore stop " + stopFacility.getId());
							warnCounter--;
						} if (warnCounter == 0) {
							log.warn("Future occurences of this logging statement are suppressed.");
							warnCounter--;
						}
					} else {
						this.linkId2StopFacilityMap.put(stopFacility.getLinkId(), stopFacility);
					}
				} else {
					stopsWithoutLinkIds.add(stopFacility.getId());
				}
			}
		}

		this.exclude = this.factory.buildGeometry(new ArrayList<Geometry>());
		if(!new File(pConfigGroup.getServiceAreaFile()).exists()){
			log.warn("file " + this.pConfigGroup.getServiceAreaFile() + " not found. Falling back to min/max serviceArea parameters.");
			createServiceArea(pConfigGroup.getMinX(), pConfigGroup.getMaxX(), pConfigGroup.getMinY(), pConfigGroup.getMaxY());
		}else{
			log.warn("using " + this.pConfigGroup.getServiceAreaFile() + " for servicearea. x/y-values defined in the config are not used.");
			createServiceArea(pConfigGroup.getServiceAreaFile());
		}

		if (stopsWithoutLinkIds.size() > 0) {
			log.warn("There are " + stopsWithoutLinkIds.size() + " stop facilities without a link id, namely: " + stopsWithoutLinkIds.toString());
		}
		this.topoTypesForStops = this.pConfigGroup.getTopoTypesForStops();
		this.networkCalcTopoType = new NetworkCalcTopoType();
		this.networkCalcTopoType.run(net);

		// parse StopLocationSelectorParameter from config
		String[] stopLocationSelectorParameter = pConfigGroup.getStopLocationSelectorParameter().split(",");
		if (stopLocationSelectorParameter.length != 3) {
			log.warn("StopLocationSelectorParameter should be \"pmin,epsilon,stopDistance\" but has a different number of values. Using default values instead.");
			stopLocationSelectorParameter = "50.0,2,500".split(",");
		}
		double pmin = Double.parseDouble(stopLocationSelectorParameter[0]);
		int epsilon = Integer.parseInt(stopLocationSelectorParameter[1]);

		if (stopLocationSelectorParameter[2].contains("nfinity") || stopLocationSelectorParameter[2].contains("NFINITY")) {
			stopDistance = Double.POSITIVE_INFINITY;
		} else {
			stopDistance = Double.parseDouble(stopLocationSelectorParameter[2]);
		}

		intersectionSimplifiedRoadNetwork = generateIntersectionSimplifiedNetwork(pmin, epsilon);

	}

	/**
	 * @param minX
	 * @param maxX
	 * @param minY
	 * @param maxY
	 */
	private void createServiceArea(double minX, double maxX, double minY, double maxY) {
		Coordinate[] c = new Coordinate[4];
		c[0] = new Coordinate(minX, minY);
		c[1] = new Coordinate(minX, maxY);
		c[2] = new Coordinate(maxX, minY);
		c[3] = new Coordinate(maxX, maxY);
		this.include = this.factory.createMultiPoint(c).convexHull();
	}

	/**
	 * @param serviceAreaFile
	 */
	private void createServiceArea(String serviceAreaFile) {
		if(serviceAreaFile.endsWith(".txt")){
			createServiceAreaTxt(serviceAreaFile);
		}else if (serviceAreaFile.endsWith(".shp")){
			createServiceAreaShp(serviceAreaFile);
		}else{
			log.warn(serviceAreaFile + ". unknown filetype. Falling back to simple x/y-values...");
			this.createServiceArea(pConfigGroup.getMinX(), pConfigGroup.getMaxX(), pConfigGroup.getMinY(), pConfigGroup.getMaxY());
		}
	}

	/**
	 * @param serviceAreaFile
	 */
	private void createServiceAreaTxt(String serviceAreaFile) {

		List<String> lines = new ArrayList<>();
		String line;
		try {
			BufferedReader reader = IOUtils.getBufferedReader(serviceAreaFile);
			line = reader.readLine();
			do{
				if(!(line == null)){
					if(line.contains(";")){
						lines.add(line);
					}
					line = reader.readLine();
				}
			}while(!(line == null));
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(lines.size() < 3){
			log.warn("an area needs at least 3 points, to be defined. Falling back to simple (default) x/y-values...");
			this.createServiceArea(pConfigGroup.getMinX(), pConfigGroup.getMaxX(), pConfigGroup.getMinY(), pConfigGroup.getMaxY());
			return;
		}

		Coordinate[] c = new Coordinate[lines.size() + 1];

		double x,y;
		for(int i = 0; i < lines.size(); i++){
			x = Double.parseDouble(lines.get(i).split(";")[0]);
			y = Double.parseDouble(lines.get(i).split(";")[1]);
			c[i] = new Coordinate(x, y);
		}
		// a linear ring has to be closed, so add the first coordinate again at the end
		c[lines.size()] = c[0];
		this.include = this.factory.createPolygon(this.factory.createLinearRing(c), null);
	}

	/**
	 * @param serviceAreaFile
	 */
	private void createServiceAreaShp(String serviceAreaFile) {
		Collection<SimpleFeature> features = new GeoFileReader().readFileAndInitialize(serviceAreaFile);
		Collection<Geometry> include = new ArrayList<>();
		Collection<Geometry> exclude = new ArrayList<>();

		for(SimpleFeature f: features){
			boolean incl = true;
			Geometry g = null;
			for(Object o: f.getAttributes()){
				if(o instanceof Polygon){
					g = (Geometry) o;
				}else if (o instanceof MultiPolygon){
					g = (Geometry) o;
				}
				// TODO use a better way to get the attributes, maybe directly per index.
				// Now the last attribute is used per default...
				else if (o instanceof String){
					incl = Boolean.parseBoolean((String) o);
				}
			}
			if(! (g == null)){
				if(incl){
					include.add(g);
				}else{
					exclude.add(g);
				}
			}
		}
		this.include = this.factory.createGeometryCollection(
				include.toArray(new Geometry[include.size()])).buffer(0);
		this.exclude = this.factory.createGeometryCollection(
				exclude.toArray(new Geometry[exclude.size()])).buffer(0);
	}

	/* Generate a simplified network to determine stop locations (the simplified network will not be used in simulation) */
	private Network generateIntersectionSimplifiedNetwork(double pmin, int epsilon) {
		// Extract road network
		NetworkFilterManager nfmCar = new NetworkFilterManager(net, networkConfigGroup);
		nfmCar.addLinkFilter(new NetworkLinkFilter() {

			@Override
			public boolean judgeLink(Link l) {
				if (l.getAllowedModes().contains("car")) return true;
				else return false;
			}
		});
		Network roadNetwork = nfmCar.applyFilters();

		// Remove low capacity links
		NetworkFilterManager nfm = new NetworkFilterManager(roadNetwork, networkConfigGroup);
		nfm.addLinkFilter(new NetworkLinkFilter() {

			@Override
			public boolean judgeLink(Link l) {
				if (l.getCapacity() >= pConfigGroup.getMinCapacityForStops()) return true;
				else return false;
			}
		});
		Network newRoadNetwork = nfm.applyFilters();
		new NetworkCleaner().run(newRoadNetwork);

		// Run Johan's intersection clustering algorithm
		IntersectionSimplifier ns = new IntersectionSimplifier(pmin, epsilon);
		Network newClusteredIntersectionsRoadNetwork = ns.simplify(newRoadNetwork);
		new NetworkCleaner().run(newClusteredIntersectionsRoadNetwork);

		// intersection clustering leaves some duplicate links (same start and end node), merge them
		NetworkMergeDoubleLinks mergeDoubleLinks = new NetworkMergeDoubleLinks(MergeType.MAXIMUM, LogInfoLevel.NOINFO);
		mergeDoubleLinks.run(newClusteredIntersectionsRoadNetwork);

		// Merge all links between two junctions
		NetworkSimplifier simplifier = new NetworkSimplifier();
		// Merge links with different attributes, because we will not use the output network for simulation
		simplifier.setMergeLinkStats(true);
		simplifier.run(newClusteredIntersectionsRoadNetwork);
		new NetworkCleaner().run(newClusteredIntersectionsRoadNetwork);

		return(newClusteredIntersectionsRoadNetwork);
	}

	private void run(){
		this.transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		int stopsAdded = 0;

		/* handle all (merged) links between junction */
		for (Link link : this.intersectionSimplifiedRoadNetwork.getLinks().values()) {
			if(link.getAllowedModes().contains(TransportMode.car)){
				stopsAdded += addStopForSimplifiedNetworkLink(link);
			}
		}

		log.info("Added " + stopsAdded + " additional stops for paratransit services");
	}

	private int addStopForSimplifiedNetworkLink(Link simplifiedNetworkLink) {
		int numberOfStopsCreated = 0;
		String[] originalIdsMergedLink = simplifiedNetworkLink.getId().toString().split("-");
		/* Bus stops should be placed on approaches to road junctions, and not inside junction
		 * areas.
		 * The Intersection simplifier creates a network where most junction nodes are merged into
		 * one node per junction. By merging two neighbouring nodes, all links in between are removed.
		 * So, initially no links are modified, but most links located inside a junction area are
		 * removed. Than the NetworkSimplifier merges short links between intersections, thereby
		 * creating a network where between two junctions there is at most one link per direction.
		 * Given that simplified network the corresponding stop locations are looked up on the
		 * original network using the link ids remaining in the simplified network.
		 *
		 * First add stops at links approaching junction areas:
		 * Start from last part of the merged link and move forward until the first intersection
		 * (>=2 outlinks) is reached. This should be in most cases the begin of the junction area.
		 *
		 * It could be that the original network already had "-" in link ids,
		 * so the link would not be found here. Add previous id parts of the merged link.
		 * Go from the last link id backwards, because we want to find the last link.
		 */
		int indexOfNextLinkIdToBeAppended = originalIdsMergedLink.length - 1;
		String reappendedlinkIds = originalIdsMergedLink[indexOfNextLinkIdToBeAppended];
		Link lastPartOfMergedLink = net.getLinks().get(Id.createLinkId(reappendedlinkIds));

		while (lastPartOfMergedLink == null) {
			indexOfNextLinkIdToBeAppended--;
			if (indexOfNextLinkIdToBeAppended < 0) {
				throw new RuntimeException("Link id from simplified network (IntersectionSimplifier) not found in original network");
			}
			reappendedlinkIds = originalIdsMergedLink[indexOfNextLinkIdToBeAppended] + "-" + reappendedlinkIds;
			lastPartOfMergedLink = net.getLinks().get(Id.createLinkId(reappendedlinkIds));
		}

		/* Get node ids of all nodes merged into the clustered node where the merged link ends */
		Set<Id<Node>> originalNodeIdsBeforeClustering = new HashSet<>();
		String[] clusteredNodeIdSplit = simplifiedNetworkLink.getToNode().getId().toString().split("-");
		for (String originalNodeId : clusteredNodeIdSplit) {
			originalNodeIdsBeforeClustering.add(Id.createNodeId(originalNodeId));
		}

		while (lastPartOfMergedLink.getToNode().getOutLinks().size() <= 1 &&
				! originalNodeIdsBeforeClustering.contains(lastPartOfMergedLink.getToNode().getId())) {
			lastPartOfMergedLink = lastPartOfMergedLink.getToNode().getOutLinks().values().iterator().next();
		}

		numberOfStopsCreated += addStopOnLink(lastPartOfMergedLink);

		/* Go backward from junction approach and add infill stops to create a proper bus stop spacing between junctions. */
		double distanceFromLastStop = lastPartOfMergedLink.getLength();
		if (numberOfStopsCreated == 0) {
			/* No stop was created on link approaching the junction (due to link characteristics excluded
			in pConfigGroup, see {@link addStopOnLink(Link link)}), so make the algorithm add one on the
			next suitable link */
			distanceFromLastStop = stopDistance;
		}

		Link currentLink = lastPartOfMergedLink;
		reappendedlinkIds = "";

		/* Stop if the previous junction area is reached */
		while (indexOfNextLinkIdToBeAppended > 0) {
			indexOfNextLinkIdToBeAppended--;
			if (reappendedlinkIds.equals("")) {
				reappendedlinkIds = originalIdsMergedLink[indexOfNextLinkIdToBeAppended];
			} else {
				reappendedlinkIds = originalIdsMergedLink[indexOfNextLinkIdToBeAppended] + "-" + reappendedlinkIds;
			}
			Link tryLinkId = net.getLinks().get(Id.createLinkId(reappendedlinkIds));

			if (tryLinkId == null) {
				/* Next backward link could not be found, because it's id already contains "-", e.g. because */
				continue;
			} else {
				distanceFromLastStop += currentLink.getLength();
				currentLink = tryLinkId;
				reappendedlinkIds = "";

				if (distanceFromLastStop >= stopDistance) {
					/* Try to add a new stop on current link */
					int stopCreated = addStopOnLink(currentLink);

					// TODO: Add check of distance to next junction further backward
					if (stopCreated > 0) {
						/* If stop was added, reset distanceFromLastStop */
						numberOfStopsCreated += stopCreated;
						distanceFromLastStop = 0;
					}
				}
			}
		}

		return numberOfStopsCreated;
	}

	private int addStopOnLink(Link link) {
		if(link == null){
			return 0;
		}

		if(!linkToNodeInServiceArea(link)){
			return 0;
		}

		if (linkHasAlreadyAFormalPTStopFromTheGivenSchedule(link)) {
			return 0;
		}

		if(!topoTypeAllowed(link)){
			return 0;
		}

		if (link.getFreespeed() >= this.pConfigGroup.getSpeedLimitForStops()) {
			return 0;
		}

		if (link.getCapacity() < this.pConfigGroup.getMinCapacityForStops()) {
			return 0;
		}

		if (this.linkId2StopFacilityMap.get(link.getId()) != null) {
			log.warn("Link " + link.getId() + " has already a stop in the given (non-paratransit) TransitSchedule. This should not happen. Check code.");
			return 0;
		}

		if (this.transitSchedule.getFacilities().get(Id.create(pConfigGroup.getPIdentifier() + link.getId().toString(), TransitStopFacility.class)) != null) {
			log.warn("Link " + link.getId() + " has already a stop. This should not happen. Check code.");
			return 0;
		}

		Id<TransitStopFacility> stopId = Id.create(this.pConfigGroup.getPIdentifier() + link.getId(), TransitStopFacility.class);
		TransitStopFacility stop = this.transitSchedule.getFactory().createTransitStopFacility(stopId, link.getToNode().getCoord(), false);
		stop.setLinkId(link.getId());
		this.transitSchedule.addStopFacility(stop);
		return 1;
	}

	private boolean topoTypeAllowed(Link link) {
		if(this.topoTypesForStops == null){
			// flag not set or null in config
			return true;
		}
		Integer topoType = this.networkCalcTopoType.getTopoType(link.getToNode());
		return this.topoTypesForStops.contains(topoType);
	}

	private boolean linkToNodeInServiceArea(Link link) {
		Point p = factory.createPoint(MGC.coord2Coordinate(link.getToNode().getCoord()));
		if(this.include.contains(p)){
			if(exclude.contains(p)){
				return false;
			}
			return true;
		}
		return false;
	}

	private boolean linkHasAlreadyAFormalPTStopFromTheGivenSchedule(Link link) {
		if (this.linkId2StopFacilityMap.containsKey(link.getId())) {
			// There is already a stop at this link, used by formal public transport - Use this one instead
			this.transitSchedule.addStopFacility(this.linkId2StopFacilityMap.get(link.getId()));
			return true;
		} else {
			return false;
		}
	}

	private TransitSchedule getTransitSchedule() {
		return this.transitSchedule;
	}
}
