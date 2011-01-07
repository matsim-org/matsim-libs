/* *********************************************************************** *
 * project: org.matsim.*
 * GridBasedAccessibility.java
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
package playground.johannes.socialnetworks.gis;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.text.IconView;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraphBuilder;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.KMLIconVertexStyle;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;

import playground.johannes.socialnetworks.graph.spatial.io.NumericAttributeColorizer;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geomgraph.GeometryGraph;

/**
 * @author illenberger
 *
 */
public class GridBasedAccessibility {

	private SpatialCostFunction costFunction;
	
	private final GeometryFactory geoFactory = new GeometryFactory();
	
	private static final Logger logger = Logger.getLogger(GridBasedAccessibility.class);
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		GridBasedAccessibility access = new GridBasedAccessibility();
		access.costFunction = new GravityCostFunction(-1.6, 0, new CartesianDistanceCalculator());
		Set<Point> points = access.loadPoints(args[0]);
//		TObjectDoubleHashMap<Point> pointAccess = access.pointAccessibility(points);
		Envelope env = PointUtils.envelope(points);

//		logger.info("Calculation microscopic accessibility...");
//		SpatialGrid<Double> accessGridMicro = new SpatialGrid<Double>(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY(), 1000.0);
//		access.accessPoints(accessGridMicro, points);
//		SpatialGridTableWriter writer = new SpatialGridTableWriter();
		SpatialGridKMLWriter writer = new SpatialGridKMLWriter();
//		writer.write(accessGridMicro, args[1] + "access.micro.txt");
		
		for (int i = 1; i < 2; i++) {
			double size = i*1000;
			logger.info("Calculating accessibility on " + (int)size + " grid...");
			SpatialGrid<Double> accessGridMacro = new SpatialGrid<Double>(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY(), 1000.0);
			SpatialGrid<Double> targetGrid = new SpatialGrid<Double>(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY(), size);
						
//			SpatialGrid<Set<Point>> pointGrid = access.createPointGrid(points, size);	
//			access.accessGrid(accessGridMacro, pointGrid);
//			writer.write(accessGridMacro, CRSUtils.getCRS(21781), args[1] + "access.macro." + (int)size +".kmz");
			
			writer.write(targetGrid, CRSUtils.getCRS(21781), args[1] + "targetGrid." + (int)size +".kmz");
//			writer.write(accessGridMacro, args[1] + "access.macro." + (int)size +".txt");

//			SpatialGrid<Double> deltaGrid = delta(accessGridMacro, accessGridMicro);
			
			
//			writer.write(deltaGrid, args[1] + "delta." + (int)size + ".txt");
			
		}
	}
	
	private static SpatialGrid<Double> delta(SpatialGrid<Double> grid1, SpatialGrid<Double> grid2) {
		SpatialGrid<Double> deltaGrid = new SpatialGrid<Double>(grid1);
		for(int i = 0; i < grid1.getNumRows(); i++) {			
			for(int j = 0; j < grid1.getNumCols(i); j++) {
				deltaGrid.setValue(i, j, grid1.getValue(i, j) - grid2.getValue(i, j));
			}
		}
		return deltaGrid;
	}
	
	private void writeTable(SpatialGrid<Double> grid, String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		for(int i = 0; i < grid.getNumRows(); i++) {
			for(int j = 0; j < grid.getNumCols(i); j++) {
				Double val = grid.getValue(i, j);
				if(val != null) {
					writer.write(String.valueOf(grid.getXmin() + j * grid.getResolution()));
					writer.write("\t");
					writer.write(String.valueOf(grid.getYmin() + i * grid.getResolution()));
					writer.write("\t");
					writer.write(String.valueOf(val));
					writer.newLine();
				}
			}
		}
		writer.close();
	}
	private void writePointTable(TObjectDoubleHashMap<Point> deltas, String file) throws IOException {
		SpatialSparseGraphBuilder builder = new SpatialSparseGraphBuilder(CRSUtils.getCRS(21781));
		SpatialSparseGraph graph = builder.createGraph();
		
		TObjectDoubleHashMap<Vertex> values = new TObjectDoubleHashMap<Vertex>();
		TObjectDoubleIterator<Point> it = deltas.iterator();
		for(int i = 0; i < deltas.size(); i++) {
			it.advance();
			values.put(builder.addVertex(graph, it.key()), it.value());
		}
		
		NumericAttributeColorizer colorizer = new NumericAttributeColorizer(values);
		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		KMLIconVertexStyle style = new KMLIconVertexStyle(graph);
		style.setVertexColorizer(colorizer);
		writer.setKmlVertexStyle(style);
		writer.addKMZWriterListener(style);
		writer.write(graph, file);
//		TDoubleObjectHashMap<TDoubleDoubleHashMap> y = new TDoubleObjectHashMap<TDoubleDoubleHashMap>();
//		TObjectDoubleIterator<Point> it = deltas.iterator();
//		
//		double[] xCoords = new double[deltas.size()];
//		
//		for(int i = 0; i < deltas.size(); i++) {
//			it.advance();
//			Point p = it.key();
//			TDoubleDoubleHashMap x = y.get(p.getY());
//			if(x == null) {
//				x = new TDoubleDoubleHashMap();
//				y.put(p.getY(), x);
//			}
//			x.put(p.getX(), it.value());
//			xCoords[i] = p.getX();
//		}
//		
//		double[] yCoords = y.keys();
//		Arrays.sort(yCoords);
//		Arrays.sort(xCoords);
//		
//		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
//		
//		for(int x = 0; x < xCoords.length; x++) {
//			writer.write("\t");
//			writer.write(String.valueOf(xCoords[x]));
//		}
//		writer.newLine();
//		
//		for(int yidx = 0; yidx < yCoords.length; yidx++) {
//			writer.write(String.valueOf(yCoords[yidx]));
//			TDoubleDoubleHashMap x = y.get(yCoords[yidx]);
//			for(int xidx = 0; xidx < xCoords.length; xidx++) {
//				writer.write("\t");
//				double val = x.get(xCoords[xidx]);
//				if(val > 0)
//					writer.write(String.valueOf(val));
//				else
//					writer.write("NA");
//			}
//			writer.newLine();
//		}
//		
//		writer.close();
	}
	
	private SpatialGrid<Double> mse(TObjectDoubleHashMap<Point> pointAccess, SpatialGrid<Double> gridAccess) {
		SpatialGrid<Double> mseGrid = new SpatialGrid<Double>(gridAccess.getXmin(), gridAccess.getYmin(), gridAccess.getXmax(), gridAccess.getYmax(), gridAccess.getResolution());
		Set<Point> points = new HashSet<Point>();
		TObjectDoubleIterator<Point> it = pointAccess.iterator();
		for(int i = 0; i < pointAccess.size(); i++) {
			it.advance();
			points.add(it.key());
		}
		SpatialGrid<Set<Point>> pointGrid = createPointGrid(points, gridAccess.getResolution());
		
		for(int i = 0; i < gridAccess.getNumRows(); i++) {
			for(int j = 0; j < gridAccess.getNumCols(i); j++) {
				Double val = gridAccess.getValue(i, j);
				Set<Point> set = pointGrid.getValue(i, j);
				if(val != null && set != null) {
					double sum = 0;
					for(Point p : set) {
						double pval = pointAccess.get(p);
						sum += Math.pow(val - pval, 2);
					}
					mseGrid.setValue(i, j, sum/(double)set.size());
				}
			}
		}
		
		return mseGrid;
	}
//	private TObjectDoubleHashMap<Point> mse(TObjectDoubleHashMap<Point> pointAccess, SpatialGrid<Double> gridAccess) {
//		TObjectDoubleHashMap<Point> deltas = new TObjectDoubleHashMap<Point>();
//		TObjectDoubleIterator<Point> it = pointAccess.iterator();
//		for(int i = 0; i < pointAccess.size(); i++) {
//			it.advance();
//			Double val = gridAccess.getValue(it.key());
//			if(val != null)
//				deltas.put(it.key(), it.value() - val);
//			
//		}
//		
//		return deltas;
//	}
	
	private Set<Point> loadPoints(String file) {
		SpatialGraph g = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read(file);
		Set<Point> points = new HashSet<Point>();
		for(SpatialVertex vertex : g.getVertices())
			points.add(vertex.getPoint());
		
		return points;
	}

	private SpatialGrid<Set<Point>> createPointGrid(Set<Point> points, double size) {
		Envelope env = PointUtils.envelope(points);
		SpatialGrid<Set<Point>> grid = new SpatialGrid<Set<Point>>(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY(), size);
		
		for(Point point : points) {
			Set<Point> set = grid.getValue(point);
			if(set == null) {
				set = new HashSet<Point>();
				grid.setValue(set, point);
			}
			set.add(point);
		}
		
		return grid;
	}
	
	private SpatialGrid<Point> createCenterGrid(SpatialGrid<Set<Point>> pointGrid) {
		SpatialGrid<Point> centerGrid = new SpatialGrid<Point>(pointGrid.getXmin(), pointGrid.getYmin(), pointGrid.getXmax(), pointGrid.getYmax(), pointGrid.getResolution());
		
		for(int i = 0; i < pointGrid.getNumRows(); i++) {
			for(int j = 0; j < pointGrid.getNumCols(i); j++) {
				Set<Point> cell = pointGrid.getValue(i, j);
				if(cell != null && !cell.isEmpty())
					centerGrid.setValue(i, j, PointUtils.centerOfMass(cell));
			}
		}
		
		return centerGrid;
	}
	
	private void accessGrid(SpatialGrid<Double> sourceGird, SpatialGrid<Set<Point>> targetGrid) {	
		SpatialGrid<Point> centerGrid = createCenterGrid(targetGrid);
		
		for(int i = 0; i < sourceGird.getNumRows(); i++) {			
			for(int j = 0; j < sourceGird.getNumCols(i); j++) {
				double x = sourceGird.getXmin() + j * sourceGird.getResolution();
				double y = sourceGird.getYmin() + i * sourceGird.getResolution();
				Point source = geoFactory.createPoint(new Coordinate(x, y));
				sourceGird.setValue(i, j, cellAccessibility(source, targetGrid, centerGrid));
			}
		}
		
		
	}
	
	private void accessPoints(SpatialGrid<Double> sourceGird, Set<Point> targets) {
		for(int i = 0; i < sourceGird.getNumRows(); i++) {			
			for(int j = 0; j < sourceGird.getNumCols(i); j++) {
				double x = sourceGird.getXmin() + j * sourceGird.getResolution();
				double y = sourceGird.getYmin() + i * sourceGird.getResolution();
				Point source = geoFactory.createPoint(new Coordinate(x, y));
				sourceGird.setValue(i, j, pointAccessibility(source, targets));
			}
		}
	}
	
	private double cellAccessibility(Point source, SpatialGrid<Set<Point>> pointGrid, SpatialGrid<Point> centerGrid) {
		double sum = 0;
		
		for(int i = 0; i < pointGrid.getNumRows(); i++) {			
			for(int j = 0; j < pointGrid.getNumCols(i); j++) {
				Set<Point> targetCell = pointGrid.getValue(i, j);
				if(targetCell != null && !targetCell.isEmpty()) {
					double X_ij = targetCell.size();
					sum += X_ij * Math.exp(costFunction.costs(source, centerGrid.getValue(i, j)));
					
				}
			}
		}
		
		return Math.log(sum);
	}
	
	private double pointAccessibility(Point source, Set<Point> points) {
		double sum = 0;
		for (Point p2 : points) {
			if (source != p2) {
				sum += Math.exp(costFunction.costs(source, p2));
			}
		}

		return Math.log(sum);
	}
}
