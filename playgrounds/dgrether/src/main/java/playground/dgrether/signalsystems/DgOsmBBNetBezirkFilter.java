/* *********************************************************************** *
 * project: org.matsim.*
 * DgOsmBBNetBezirkFilter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.dgrether.visualization.KmlNetworkVisualizer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;


/**
 * @author dgrether
 *
 */
public class DgOsmBBNetBezirkFilter {
	
	private static final Logger log = Logger.getLogger(DgOsmBBNetBezirkFilter.class);
	
	private List<Feature> bezirke = new ArrayList<Feature>();
	
	private Map<String, Feature> bezirksFeatures = new HashMap<String, Feature>();
	
	public DgOsmBBNetBezirkFilter(String bezirksShape){
		this.readBezirksShape(bezirksShape);
	}
	
	private void readBezirksShape(final String bezirksShape){
		try {
			 FeatureSource fs = ShapeFileReader.readDataFile(bezirksShape);
			 Iterator it = fs.getFeatures().iterator();
				while (it.hasNext()) {
					Feature ft = (Feature) it.next();
					String name = (String) ft.getAttribute("name");
					log.info("adding bezirksdata of: " + name);
					this.bezirksFeatures.put(name, ft);
				}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public void addBezirkFilter(String bezirksname){
		if (this.bezirksFeatures.containsKey(bezirksname)){
			this.bezirke.add(this.bezirksFeatures.get(bezirksname));
		}
		else {
			log.warn("Cannot find feature for Bezirksname: " + bezirksname);
		}
	}
	
	public Network filterNetwork(Network net){
		GeometryFactory factory = new GeometryFactory();
		NetworkImpl n = new ScenarioImpl().getNetwork();
		n.setCapacityPeriod(net.getCapacityPeriod());
		n.setEffectiveLaneWidth(net.getEffectiveLaneWidth());
		for (Link orgLink : net.getLinks().values()){
			for (Feature ft : this.bezirke){
				Geometry geo = factory.createPoint(new Coordinate(orgLink.getCoord().getX(), orgLink.getCoord().getY()));
				if (ft.getDefaultGeometry().contains(geo)){
					Node fromNode = orgLink.getFromNode();
					Node toNode = orgLink.getToNode();
					Node nn;
					if (!n.getNodes().containsKey(fromNode.getId())){
						nn = n.getFactory().createNode(fromNode.getId(), fromNode.getCoord());
						n.addNode(nn);
					}
					if (!n.getNodes().containsKey(toNode.getId())){
						nn = n.getFactory().createNode(toNode.getId(), toNode.getCoord());
						n.addNode(nn);
					}
					Link nl = n.getFactory().createLink(orgLink.getId(), fromNode.getId(), toNode.getId());
					nl.setCapacity(orgLink.getCapacity());
					nl.setFreespeed(orgLink.getFreespeed());
					nl.setLength(orgLink.getLength());
					nl.setNumberOfLanes(orgLink.getNumberOfLanes());
					n.addLink(nl);
					break;
				}
			}
		}
		return n;
	}
	
	
	public static void main(String[] args){
		DgOsmBBNetBezirkFilter filter = new DgOsmBBNetBezirkFilter(DgOsmBBPaths.BASE_OUT_DIR + "berliner_bezirke/berliner_bezirke.shp");
		Scenario sc = new ScenarioImpl();
		MatsimNetworkReader reader = new MatsimNetworkReader(sc);
		reader.readFile(DgOsmBBPaths.NETWORK_GENERATED);
//		filter.addBezirkFilter("Reinickendorf");
//		filter.addBezirkFilter("Spandau");
//		filter.addBezirkFilter("Tempelhof-Schoeneberg");

//		filter.addBezirkFilter("Charlottenburg-Wilmersdorf");
//		filter.addBezirkFilter("Steglitz-Zehlendorf");

//		filter.addBezirkFilter("Friedrichshain-Kreuzberg");
//		filter.addBezirkFilter("Neukoelln");
//	
//				filter.addBezirkFilter("Mitte");
//		filter.addBezirkFilter("Pankow");

//		filter.addBezirkFilter("Marzahn-Hellersdorf");
//		filter.addBezirkFilter("Lichtenberg");

		filter.addBezirkFilter("Treptow-Koepenick");

		Network net = filter.filterNetwork(sc.getNetwork());
		
		String kmlOut = DgOsmBBPaths.BASE_OUT_DIR + "treptow_network.kml";
		KmlNetworkVisualizer kmlnetwriter = new KmlNetworkVisualizer(net);
		kmlnetwriter.write(kmlOut, TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84));
		
	}
	
}
