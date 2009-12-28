/* *********************************************************************** *
 * project: org.matsim.*
 * Speed2QGIS.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.yu.utils.qgis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.DefaultFeatureTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.SAXException;

import playground.yu.analysis.CalcLinksAvgSpeed;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author yu
 * 
 */
public class SpeedLevel2QGIS extends MATSimNet2QGIS {
	public static class SpeedLevel2PolygonGraph extends Network2PolygonGraph {
		private Set<Id> linkIds;

		public SpeedLevel2PolygonGraph(NetworkLayer network,
				CoordinateReferenceSystem crs, Set<Id> linkIds) {
			this.network = network;
			this.crs = crs;
			this.linkIds = linkIds;
			this.geofac = new GeometryFactory();
			features = new ArrayList<Feature>();
			AttributeType geom = DefaultAttributeTypeFactory.newAttributeType(
					"MultiPolygon", MultiPolygon.class, true, null, null, crs);
			AttributeType id = AttributeTypeFactory.newAttributeType("ID",
					String.class);
			defaultFeatureTypeFactory = new DefaultFeatureTypeFactory();
			defaultFeatureTypeFactory.setName("link");
			defaultFeatureTypeFactory
					.addTypes(new AttributeType[] { geom, id });
		}

		@Override
		public Collection<Feature> getFeatures() throws SchemaException,
				NumberFormatException, IllegalAttributeException {
			for (int i = 0; i < attrTypes.size(); i++)
				defaultFeatureTypeFactory.addType(attrTypes.get(i));
			FeatureType ftRoad = defaultFeatureTypeFactory.getFeatureType();
			for (Id linkId : linkIds) {
				LinkImpl link = network.getLink(linkId);
				LinearRing lr = getLinearRing(link);
				Polygon p = new Polygon(lr, null, this.geofac);
				MultiPolygon mp = new MultiPolygon(new Polygon[] { p },
						this.geofac);
				int size = 2 + parameters.size();
				Object[] o = new Object[size];
				o[0] = mp;
				o[1] = link.getId().toString();
				for (int i = 0; i < parameters.size(); i++) {
					o[i + 2] = parameters.get(i).get(link.getId());
				}
				Feature ft = ftRoad.create(o, "network");
				features.add(ft);
			}
			return features;
		}

	}

	public static List<Map<Id, Double>> createSpeedLevels(
			Collection<? extends Link> links, CalcLinksAvgSpeed clas) {
		List<Map<Id, Double>> speeds = new ArrayList<Map<Id, Double>>(24);
		for (int i = 0; i < 24; i++)
			speeds.add(i, null);

		for (Link link : links) {
			Id linkId = link.getId();

			for (int i = 0; i < 24; i++) {
				Map<Id, Double> m = speeds.get(i);
				if (m == null) {
					m = new HashMap<Id, Double>();
					speeds.add(i, m);
				}
				double speed = clas.getAvgSpeed(linkId, (i * 3600))
						/ 3.6 / link.getFreespeed(i * 3600.0);
				m.put(linkId, speed);
			}
		}
		return speeds;
	}

	public void setCrs(final String wkt, final NetworkLayer network,
			final CoordinateReferenceSystem crs, Set<Id> linkIds) {
		super.setCrs(wkt);
		setN2g(new SpeedLevel2PolygonGraph(network, crs, linkIds));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MATSimNet2QGIS mn2q = new MATSimNet2QGIS();
		mn2q.setCrs(ch1903);
		/*
		 * //////////////////////////////////////////////////////////////////////
		 * /Traffic speed level and MATSim-network to Shp-file
		 * /////////////////////////////////////////////////////////////////////
		 */
		mn2q.readNetwork("../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml");
		NetworkLayer net = mn2q.getNetwork();

		CalcLinksAvgSpeed clas = new CalcLinksAvgSpeed(net, 3600);
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600 - 1, net);
		mn2q.readEvents("../matsimTests/Calibration/e5_700/700.events.txt.gz",
				new EventHandler[] { clas, va });

		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(net);
		try {
			tollReader
					.parse("../schweiz-ivtch-SVN/baseCase/roadpricing/KantonZurich/KantonZurich.xml");
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		RoadPricingScheme rps = tollReader.getScheme();

		Collection<? extends Link> links = (rps != null) ? rps.getLinks() : net
				.getLinks().values();
		List<Map<Id, Double>> sls = createSpeedLevels(links, clas);

		Set<Id> linkIds = (rps != null) ? rps.getLinkIds() : net.getLinks()
				.keySet();
		for (int i = 6; i < 20; i++) {
			SpeedLevel2QGIS sl2q = new SpeedLevel2QGIS();

			sl2q.setCrs(ch1903, mn2q.network, mn2q.crs, linkIds);
			sl2q.addParameter("sl", Double.class, sls.get(i));
			sl2q
					.writeShapeFile("../matsimTests/Calibration/e5_700/speedLevels/700."
							+ (i + 1) + ".shp");
		}

		System.out.println("----->done!");
	}

}
