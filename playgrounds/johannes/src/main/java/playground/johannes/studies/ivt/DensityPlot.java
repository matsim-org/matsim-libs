/* *********************************************************************** *
 * project: org.matsim.*
 * DensityPlot.java
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
package playground.johannes.studies.ivt;

import com.vividsolutions.jts.geom.*;
import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.EsriShapeIO;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.contrib.socnetgen.sna.graph.GraphBuilder;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.socnetgen.socialnetworks.gis.io.ZoneLayerSHP;
import org.matsim.contrib.socnetgen.socialnetworks.graph.spatial.analysis.SpatialFilter;
import org.matsim.contrib.socnetgen.socialnetworks.graph.spatial.analysis.ZoneUtils;
import org.matsim.contrib.socnetgen.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import org.matsim.contrib.socnetgen.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import org.matsim.contrib.socnetgen.socialnetworks.snowball2.social.SocialSampledGraphProjectionBuilder;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.io.GraphReaderFacade;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;

import java.io.IOException;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class DensityPlot {

	private static final Logger logger = Logger.getLogger(DensityPlot.class);
	
	private static final String MODE = "density";//absolute, density, weighted
	
	private static final int RESOLUTION = 5000;
	
	private static final String GEOMETRY = "zone";
	
	private static final String zoneFile = "/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/gg-qg.merged.shp";
	
	public static void main(String[] args) throws IOException, FactoryException {
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = GraphReaderFacade.read("/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/graph.graphml");
		graph.getDelegate().transformToCRS(CRSUtils.getCRS(21781));
		
		SpatialSparseGraph popGraph = null;
		if(MODE.equals("density") || MODE.equals("weighted"))
			popGraph = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.10.xml");
		
		Geometry boundary = null;
		if(MODE.equals("density") || MODE.equals("weighted")) {
			SimpleFeature feature = EsriShapeIO.readFeatures("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1L08.shp").iterator().next();
			boundary = (Geometry) feature.getDefaultGeometry();
			boundary.setSRID(21781);
		} else {
			GeometryFactory factory = new GeometryFactory();
			Coordinate[] coordinates = new Coordinate[5];
			coordinates[0] = new Coordinate(450000, 0);
			coordinates[1] = new Coordinate(450000, 350000);
			coordinates[2] = new Coordinate(900000, 350000);
			coordinates[3] = new Coordinate(900000, 0);
			coordinates[4] = coordinates[0];
			LinearRing ring = factory.createLinearRing(coordinates);
			boundary = factory.createPolygon(ring, null);
			boundary.setSRID(21781);
		}
		
		logger.info("Applying spatial filter...");
		SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> builder = new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
		SpatialFilter filter = new SpatialFilter((GraphBuilder) builder, boundary);
		graph = (SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>) filter.apply(graph);
		
		ZoneLayer<Set<SpatialVertex>> sampleLayer = null;
		if(GEOMETRY.equals("grid")) {
			sampleLayer = ZoneUtils.createGridLayer(RESOLUTION, boundary);
		} else {
//			sampleLayer = ZoneLayerSHP.read(zoneFile);
			System.err.println("Code needs update.");
			System.exit(-1);
		}
		sampleLayer.overwriteCRS(CRSUtils.getCRS(21781));
		ZoneUtils.fillZoneLayer(sampleLayer, (Set)graph.getVertices());
		
		ZoneLayer<Set<SpatialVertex>> popLayer = null;
		if(MODE.equals("density") || MODE.equals("weighted")) {
			if(GEOMETRY.equals("grid"))
				popLayer = ZoneUtils.createGridLayer(RESOLUTION, boundary);
			else {
//				popLayer = ZoneLayerSHP.read(zoneFile);
				System.err.println("Code needs update.");
				System.exit(-1);
			}
			popLayer.overwriteCRS(CRSUtils.getCRS(21781));
			ZoneUtils.fillZoneLayer(popLayer, (Set)popGraph.getVertices());
			
		}
		
		GeometryFactory factory = new GeometryFactory();
		Point zrh = factory.createPoint(new Coordinate(8.55, 47.36));
		zrh = CRSUtils.transformPoint(zrh, CRS.findMathTransform(DefaultGeographicCRS.WGS84, CRSUtils.getCRS(21781)));
		DistanceCalculator calc = new CartesianDistanceCalculator();
		Discretizer disc = new LinearDiscretizer(1000);
		
		ZoneLayer<Double> layer = null;
		if(GEOMETRY.equals("grid"))
			layer = ZoneUtils.createGridLayer(RESOLUTION, boundary);
		else {
//			layer = ZoneLayerSHP.read(zoneFile);
			System.err.println("Code needs update.");
			System.exit(-1);
		}
		layer.overwriteCRS(CRSUtils.getCRS(21781));
		for(Zone<Double> zone : layer.getZones()) {
			double rho = 0;
			Point p = zone.getGeometry().getInteriorPoint();
			p.setSRID(21781);
			Zone<Set<SpatialVertex>> sampleZone = sampleLayer.getZone(p);
			
			if (MODE.equals("density") || MODE.equals("weighted")) {
				Zone<Set<SpatialVertex>> popZone = popLayer.getZone(p);

				Set<SpatialVertex> sampleSet = sampleZone.getAttribute();
				Set<?> popSet = popZone.getAttribute();

				if (popSet != null) {
					int samples = 0;
					if (sampleSet != null) {
						if(MODE.equals("weighted")) {
							for(SpatialVertex v : sampleSet) {
								double d = calc.distance(zrh, v.getPoint());
								d = disc.discretize(d);
								samples += 1/Math.pow(d, -1.4);
							}
						} else
							samples = sampleSet.size();
					}

					int inhabitants = popSet.size() * 10;
					if (inhabitants > 0) {
						rho = samples / (double) inhabitants;
					}
				}
			} else {
				Set<?> sampleSet = sampleZone.getAttribute();
				if(sampleSet != null)
					rho = sampleSet.size();
			}
			
			zone.setAttribute(rho);
		}

		
		ZoneLayerSHP.write(layer, "/Users/jillenberger/Work/phd/doc/tex/ch3/fig/data/chplot/" + MODE + "." + GEOMETRY +".shp");
		
	}
}
