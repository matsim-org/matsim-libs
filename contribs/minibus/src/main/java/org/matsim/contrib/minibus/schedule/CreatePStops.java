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
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Create one TransitStopFacility for each car mode link of the network
 *
 * @author aneumann, droeder
 *
 */
public final class CreatePStops{

	private final static Logger log = LogManager.getLogger(CreatePStops.class);

	private final Network net;
	private final PConfigGroup pConfigGroup;
	private TransitSchedule transitSchedule;

	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;

	private final LinkedHashMap<Id<Link>, TransitStopFacility> linkId2StopFacilityMap;

	private List<Integer> topoTypesForStops = null;

	private NetworkCalcTopoType networkCalcTopoType;

	public static TransitSchedule createPStops(Network network, PConfigGroup pConfigGroup){
		return createPStops(network, pConfigGroup, null);
	}

	public static TransitSchedule createPStops(Network network, PConfigGroup pConfigGroup, TransitSchedule realTransitSchedule) {
		CreatePStops cS = new CreatePStops(network, pConfigGroup, realTransitSchedule);
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
    private CreatePStops(Network net, PConfigGroup pConfigGroup, TransitSchedule realTransitSchedule) {
		this.net = net;
		this.pConfigGroup = pConfigGroup;
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
		if(!(this.topoTypesForStops == null)){
			this.networkCalcTopoType = new NetworkCalcTopoType();
			this.networkCalcTopoType.run(net);
		}
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

	private void run(){
		this.transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		int stopsAdded = 0;

		for (Link link : this.net.getLinks().values()) {
			if(link.getAllowedModes().contains(TransportMode.car)){
				stopsAdded += addStopOnLink(link);
			}
		}

		log.info("Added " + stopsAdded + " additional stops for paratransit services");
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
