/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusSmallNetworkGenerator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.cottbus.scripts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.AttributeType;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.signalsystems.data.SignalsData;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.FeatureNetworkLinkFilter;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;
import playground.dgrether.signalsystems.utils.DgSignalsUtils;
import playground.dgrether.utils.DgNet2Shape;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;


public class DgCottbusSmallNetworkGenerator {
	
	private static final Logger log = Logger.getLogger(DgCottbusSmallNetworkGenerator.class);
	
	private GeometryFactory geoFac = new GeometryFactory();
	
	private CoordinateReferenceSystem networkSrs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);

	private Envelope boundingBox;
	
	public Network createSmallNetwork(){
		String netFile = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network.xml.gz";
		String signalsSystems = DgPaths.REPOS +  "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_systems.xml";
		Config c1 = ConfigUtils.createConfig();
		c1.network().setInputFile(netFile);
		c1.scenario().setUseSignalSystems(true);
		c1.signalSystems().setSignalSystemFile(signalsSystems);
		Scenario scenario = ScenarioUtils.loadScenario(c1);
		SignalsData signalsdata = scenario.getScenarioElement(SignalsData.class);
		Network net = scenario.getNetwork();
		
		Tuple<CoordinateReferenceSystem, Feature> cottbusFeatureTuple = CottbusUtils.loadCottbusFeature(DgPaths.REPOS
				 + "shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp");
		Feature cottbusFeature = cottbusFeatureTuple.getSecond();
		CoordinateReferenceSystem cottbusFeatureCrs = cottbusFeatureTuple.getFirst();
		
		
		//get all signalized link ids
		Map<Id, Set<Id>> signalizedLinkIdsBySystemIdMap = DgSignalsUtils.calculateSignalizedLinksPerSystem(signalsdata.getSignalSystemsData()); 
		Set<Id> signalizedLinkIds = new HashSet<Id>();
		for (Set<Id> set : signalizedLinkIdsBySystemIdMap.values()){
			signalizedLinkIds.addAll(set);
		}
		Feature boundingboxFeature = calcBoundingBox(net, signalizedLinkIds);
		

		NetworkFilterManager filterManager = new NetworkFilterManager(net);
		filterManager.addLinkFilter(new FeatureNetworkLinkFilter(networkSrs, boundingboxFeature, networkSrs));
		
		Network newNetwork = filterManager.applyFilters();
		
		String output = DgPaths.REPOS +  "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_small/network";
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		new DgNet2Shape().write(newNetwork, output + ".shp", crs);
		
		Collection<Feature> boundingBoxCollection = new ArrayList<Feature>();
		boundingBoxCollection.add(boundingboxFeature);
		ShapeFileWriter.writeGeometries(boundingBoxCollection, DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_small/bounding_box.shp");

		NetworkCleaner netCleaner = new NetworkCleaner();
		netCleaner.run(newNetwork);
		
		return newNetwork;
	}
	
	public CoordinateReferenceSystem getCrs(){
		return this.networkSrs;
	}
	
	public Envelope getBoundingBox() {
		return this.boundingBox;
	}
	
	private Feature calcBoundingBox(Network net, Set<Id> signalizedLinkIds) {
		Link l = null;
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (Id linkId : signalizedLinkIds){
			l = net.getLinks().get(linkId);
			if (l.getCoord().getX() < minX) {
				minX = l.getCoord().getX();
			}
			if (l.getCoord().getX() > maxX) {
				maxX = l.getCoord().getX();
			}
			if (l.getCoord().getY() > maxY) {
				maxY = l.getCoord().getY();
			}
			if (l.getCoord().getY() < minY) {
				minY = l.getCoord().getY();
			}
		}
		
		Coordinate[] coordinates = new Coordinate[5];
		coordinates[0] = new Coordinate(minX, minY);
		coordinates[1] = new Coordinate(minX, maxY);
		coordinates[2] = new Coordinate(maxX, maxY);
		coordinates[3] = new Coordinate(maxX, minY);
		coordinates[4] = coordinates[0];
		
		log.info("Found bounding box: "  + minX + " " + minY + " " + maxX + " " + maxY);
		
		this.boundingBox = new Envelope(coordinates[0], coordinates[2]);
		
		LinearRing linearRing = geoFac.createLinearRing(coordinates);
		Polygon polygon = geoFac.createPolygon(linearRing, null);
		FeatureType featureType = null;
		AttributeType [] attribs = new AttributeType[1];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("Polygon", Polygon.class, true, null, null, networkSrs);
		try {
			featureType = FeatureTypeBuilder.newFeatureType(attribs, "link");
			return featureType.create(new Object[] {polygon});
		} catch (Exception  e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} 
	}


	public static void main(String[] args){
		new DgCottbusSmallNetworkGenerator().createSmallNetwork();
	}
	
}

