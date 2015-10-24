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
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.EsriShapeIO;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.socnetgen.sna.gis.PointUtils;
import org.matsim.contrib.socnetgen.sna.gis.SpatialGrid;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.contrib.socnetgen.sna.gis.io.ZoneLayerSHP;
import org.matsim.contrib.socnetgen.sna.graph.GraphBuilder;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.SpatialFilter;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.ZoneUtils;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.Population2SpatialGraph;
import org.matsim.contrib.socnetgen.sna.snowball.social.SocialSampledGraphProjection;
import org.matsim.contrib.socnetgen.sna.snowball.social.SocialSampledGraphProjectionBuilder;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.io.GraphReaderFacade;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class DensityPlotBiTree {

	private static final Logger logger = Logger.getLogger(DensityPlotBiTree.class);
	
	private static GeometryFactory geoFactory = new GeometryFactory();
	
	public static void main(String[] args) throws IOException, FactoryException {
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = GraphReaderFacade.read("/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/graph.graphml");
		
		SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> builder = new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
		
		SpatialSparseGraph popData = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.10.xml");
		
		SimpleFeature feature = EsriShapeIO.readFeatures("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1L08.shp").iterator().next();
		Geometry chBorder = (Geometry) feature.getDefaultGeometry();
		chBorder.setSRID(21781);
		
		graph.getDelegate().transformToCRS(CRSUtils.getCRS(21781));
	
		logger.info("Applying spatial filter...");
		SpatialFilter filter = new SpatialFilter((GraphBuilder) builder, chBorder);
		graph = (SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>) filter.apply(graph);
		
		Set<Point> points = new HashSet<Point>();
		for(SpatialVertex v : graph.getVertices()) {
			points.add(v.getPoint());
		}
		
		GeometryFactory factory = new GeometryFactory();
		Point zrh = factory.createPoint(new Coordinate(8.55, 47.36));
		zrh = CRSUtils.transformPoint(zrh, CRS.findMathTransform(DefaultGeographicCRS.WGS84, CRSUtils.getCRS(21781)));
		
//		graph.getDelegate().transformToCRS(DefaultGeographicCRS.WGS84);
//		graph2.transformToCRS(DefaultGeographicCRS.WGS84);
		
		logger.info("Segmenting tiles...");
//		BiTreeGrid<Double> sampleGrid = BiTreeGridBuilder.createEqualCountGrid(points, 200, 1000);
		Envelope env = PointUtils.envelope(points);
		SpatialGrid<Double> sampleGrid = new SpatialGrid<Double>(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY(), 5000);
		
		DistanceCalculator calc = new CartesianDistanceCalculator();
		Discretizer disc = new LinearDiscretizer(1000.0);
		
		logger.info("Creating survey grid...");
		for(Point p : points) {
//			Tile<Double> tile = sampleGrid.getTile(p.getCoordinate());
			Double data = sampleGrid.getValue(p);
			double val = 0;
//			if(tile.data != null)
//				val = tile.data;
			if(data != null)
				val = data.doubleValue();
			
			double d = disc.discretize(calc.distance(p, zrh));
			d = Math.max(1, d);
			double proba = Math.pow(d, -1.4);
//			val += 1/proba;
			val ++;
//			tile.data = new Double(val);
			sampleGrid.setValue(val, p);
		}
		
		logger.info("Creating population grid...");
//		BiTreeGrid<Integer> popGrid = new BiTreeGrid<Integer>(sampleGrid);
		SpatialGrid<Integer> popGrid = new SpatialGrid<Integer>(sampleGrid);
		for(SpatialVertex v : popData.getVertices()) {
//			Tile<Integer> tile = popGrid.getTile(v.getPoint().getCoordinate());
			Integer data = popGrid.getValue(v.getPoint());
//			if (tile != null) {
//				int val = 0;
//				if (tile.data != null)
//					val = tile.data;
//				val++;
//				tile.data = new Integer(val);
//			}
			int val = 0;
			if(data != null) {
				val = data.intValue();
			}
			val++;
			popGrid.setValue(val, v.getPoint());
		}
		
		logger.info("Creating density grid...");
//		BiTreeGrid<Double> densityGrid = new BiTreeGrid<Double>(sampleGrid);
		SpatialGrid<Double> densityGrid = new SpatialGrid<Double>(sampleGrid);
		
//		Set<Tile<Double>> tiles = densityGrid.tiles();
//		for(Tile<Double> tile : tiles) {
//			Tile<Double> surveyTile = sampleGrid.getTile(tile.envelope.centre());
//			Tile<Integer> popTile = popGrid.getTile(tile.envelope.centre());
//			
//			if(surveyTile != null && popTile != null) {
//				if(surveyTile.data != null && popTile.data != null)
//					tile.data = surveyTile.data/(double)popTile.data;
//				else
//					tile.data = new Double(0);
//			} else
//				tile.data = new Double(0);
//		}
		
		for(int row = 0; row < densityGrid.getNumRows(); row++) {
			for(int col = 0; col < densityGrid.getNumCols(row); col++) {
				Integer inhabitants = popGrid.getValue(row, col);
				Double samples = sampleGrid.getValue(row, col);
				if(inhabitants != null && samples != null) {
					double density = samples/(double)inhabitants;
					densityGrid.setValue(row, col, density);
				} else {
					densityGrid.setValue(row, col, 0.0);
				}
			}
		}
	
		ZoneLayer<Double> layer = ZoneUtils.createGridLayer(5000, chBorder);
		layer.overwriteCRS(CRSUtils.getCRS(21781));
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for(int row = 0; row < densityGrid.getNumRows(); row++) {
			for(int col = 0; col < densityGrid.getNumCols(row); col++) {
				Point p = makeCoordinate(densityGrid, row, col);
				p.setSRID(21781);
				Zone<Double> z = layer.getZone(p);
				if(z != null) {
					double val = densityGrid.getValue(row, col);
					if(val > 0) {
						stats.addValue(val);
					}
					z.setAttribute(val);
				}
			}
		}

//		double min = stats.getMin();
//		double max = stats.getPercentile(80);
//		double bins = 20;
//		double width = (max-min)/bins;
//		Discretizer discretizer = new BoundedLinearDiscretizer(width, min, max);
//		for(Zone<Double> z : layer.getZones()) {
//			Double val = z.getAttribute();
//			if(val != null && val > 0) {
//				val = discretizer.index(val-min);
//				z.setAttribute(val);
//			}
//		}
		
		ZoneLayerSHP.write(layer, "/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/density.grid.shp");
//		logger.info("Writing KML...");
//		SpatialGridKMLWriter writer = new SpatialGridKMLWriter();
//		writer.write(densityGrid, CRSUtils.getCRS(21781), "/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/density.grid.kml");
//		writer.write(densityGrid, "/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/density.grid.kml");
//		GeometryFactory factory = new GeometryFactory();
//		Set<Geometry> geometries = new HashSet<Geometry>();
//		TObjectDoubleHashMap values = new TObjectDoubleHashMap();
////		tiles = sampleGrid.tiles();
//		for(Tile<Double> n : tiles) {
//			Coordinate[] coords = new Coordinate[5];
//			coords[0] = new Coordinate(n.envelope.getMinX(), n.envelope.getMinY());
//			coords[1] = new Coordinate(n.envelope.getMinX(), n.envelope.getMaxY());
//			coords[2] = new Coordinate(n.envelope.getMaxX(), n.envelope.getMaxY());
//			coords[3] = new Coordinate(n.envelope.getMaxX(), n.envelope.getMinY());
//			coords[4] = new Coordinate(n.envelope.getMinX(), n.envelope.getMinY());
//			LinearRing shell = factory.createLinearRing(coords);
//			shell.setSRID(21781);
//			geometries.add(shell);
//			values.put(shell, n.data);
//		}
//		
//		NumericAttributeColorizer colorizer = new NumericAttributeColorizer(values);
//		colorizer.setLogscale(true);
//		FeatureKMLWriter writer = new FeatureKMLWriter();
//		writer.setColorizable(colorizer);
//		writer.write(geometries, "/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/density.kml");
	}

	private static Point makeCoordinate(SpatialGrid<?> grid, int row, int col) {
		double x = grid.getXmin() + (col * grid.getResolution()) + grid.getResolution()/2.0;
//		double x = grid.getXmax() - (col * grid.getResolution()) - grid.getResolution()/2.0;
//		double y = grid.getYmin() + (row * grid.getResolution());
		double y = grid.getYmax() - (row * grid.getResolution()) - grid.getResolution()/2.0;
		Point point;
		point = geoFactory.createPoint(new Coordinate(x,y));
		return point;
	}
}
