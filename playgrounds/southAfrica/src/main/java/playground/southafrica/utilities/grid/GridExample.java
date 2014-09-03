/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.utilities.grid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.view.modes.ViewPositionMode;
import org.matsim.core.gbl.MatsimRandom;

import playground.southafrica.utilities.grid.GeneralGrid.GridType;
import playground.southafrica.utilities.grid.KernelDensityEstimator.KdeType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * A demonstration of how to use the {@link KernelDensityEstimator} class.
 *  
 * @author jwjoubert
 */
public class GridExample extends AbstractAnalysis{
	final private static Logger LOG = Logger.getLogger(GridExample.class);
	
	private final GeometryFactory gf = new GeometryFactory();
	private Random random = MatsimRandom.getLocalInstance();
	private int option;
	private GridType gridType;
	private KdeType kdeType;
	private GeneralGrid grid;
	private KernelDensityEstimator kde;
	

	/**
	 * Main class to run the grid example. At the end of the run, the code
	 * produces a visualisation of the points/lines. 
	 * 
	 * @param args Require three arguments:
	 * <ol>
	 * 		<li><b>example:</b> which example to run: either "1" for the point
	 * 			example; or "2" for the line example;
	 * 		<li><b>grid type:</b> the type of grid in which the geometry will be
	 * 		    demarcated into. Accepted values are <code>SQUARE</code> or 
	 * 			<code>HEX</code>.
	 * 		<li><b>smoothing function:</b> the type of kernel density estimation
	 * 			to use for the smoothing. Accepted values are <code>CELL</code>
	 * 			for point/line-only aggregation; <code>EPANECHNIKOV</code>; 
	 * 			<code>GAUSSIAN</code>; <code>TRIANGULAR</code>; <code>TRIWEIGHT</code>;
	 * 			and <code>UNIFORM</code>. 
	 */
	public static void main(String[] args) {
		int option = Integer.parseInt(args[0]);
		GridType gridType = GridType.valueOf(args[1]);
		KdeType kdeType = KdeType.valueOf(args[2]);
		
		GridExample example = new GridExample(option, gridType, kdeType);
		example.run();

		/* Visualise. */
		try {
			AnalysisLauncher.open(example);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot visualise!!");
		}
	}

	
	public GridExample(int option, GridType gridType, KdeType kdeType) {
		this.option = option;
		this.gridType = gridType;
		this.kdeType = kdeType;
	} 

	
	public void run(){
		switch (this.option) {
		case 1:
			runPointExample();
			break;
		case 2:
			runLineExample();
			break;
		default:
			break;
		}
	}
	
	
	/**
	 * Create a list of {@link Point}s randomly distributed around the origin.
	 * 
	 * @return
	 */
	private void runPointExample(){
		/* Create the polygon. */
		Coordinate c1 = new Coordinate(-1000.0, -1000.0);
		Coordinate c2 = new Coordinate(-1000.0, 1000.0);
		Coordinate c3 = new Coordinate(1000.0, 1000.0);
		Coordinate c4 = new Coordinate(1000.0, -1000.0);
		Coordinate[] ca = {c1, c2, c3, c4, c1};
		Polygon poly = gf.createPolygon(ca);
		
		/* Create the points. */
		List<Point> pointList = new ArrayList<Point>(1000);
		for(int i = 0; i < 20; i++){
			double x = -1000.0 + random.nextDouble()*2000;
			double y = -1000.0 + random.nextDouble()*2000;
			pointList.add(gf.createPoint(new Coordinate(x, y)));
		}
		
		/* Generate grid. */
		this.grid = new GeneralGrid(50.0, this.gridType);
		grid.generateGrid(poly);
		
		/* Generate kernel density estimate of all points. */
		this.kde = new KernelDensityEstimator(grid, kdeType, 200);
		for(Point p : pointList){
			kde.processPoint(p, 10.0);
		}
	}
	
	private void runLineExample(){
		/* Create the polygon. */
		Coordinate c1 = new Coordinate(-1000.0, -1000.0);
		Coordinate c2 = new Coordinate(-1000.0, 1000.0);
		Coordinate c3 = new Coordinate(1000.0, 1000.0);
		Coordinate c4 = new Coordinate(1000.0, -1000.0);
		Coordinate[] ca = {c1, c2, c3, c4, c1};
		Polygon poly = gf.createPolygon(ca);
		
		/* Create three lines. */
		List<LineString> lineList = new ArrayList<LineString>(3);

		for(int i = 0; i < 3; i++){
			double x1 = -1000.0 + random.nextDouble()*1000;
			double y1 = -1000.0 + random.nextDouble()*2000;
			Coordinate ci1 = new Coordinate(x1, y1);

			double x2 = 0.0 + random.nextDouble()*1000;
			double y2 = -1000.0 + random.nextDouble()*2000;
			Coordinate ci2 = new Coordinate(x2, y2);
			
			Coordinate[] cal = {ci1, ci2};
			LineString l = gf.createLineString(cal);
			lineList.add(l);
		}
		
		/* Generate grid. */
		this.grid = new GeneralGrid(50.0, this.gridType);
		grid.generateGrid(poly);
		
		/* Generate kernel density estimate of all points. */
		this.kde = new KernelDensityEstimator(grid, kdeType, 200);
		for(LineString l : lineList){
			kde.processLine(l, 10.0);
		}
	}
	

	@Override
	public void init() throws Exception {
		chart = AWTChartComponentFactory.chart(Quality.Advanced, "awt");
		Coord3d newViewPoint = new Coord3d(3*Math.PI/2, Math.PI/2, 5000);
		chart.getView().setViewPoint(newViewPoint, true);
		chart.getView().setViewPositionMode(ViewPositionMode.FREE);
		
		/* First draw the original geometry. */
		org.jzy3d.plot3d.primitives.Polygon polygon = new org.jzy3d.plot3d.primitives.Polygon();
		for(Coordinate c : this.grid.getGeometry().getCoordinates()){
			polygon.add(new org.jzy3d.plot3d.primitives.Point(new Coord3d(c.x, c.y, 1.0)));
		}
		polygon.setFaceDisplayed(false);
		polygon.setWireframeDisplayed(true);
		polygon.setWireframeColor(Color.RED);
		
		/* Determine the alpha extent. */
		double max = 0.0;
		double sum = 0.0;
		for(Point p : this.grid.getGrid().values()){
			max = Math.max(max, this.kde.getWeight(p));
			sum += this.kde.getWeight(p);
		}
		
		chart.getScene().add(polygon);
		
		for(Point p : this.grid.getGrid().values()){
			org.jzy3d.plot3d.primitives.Polygon cell = new org.jzy3d.plot3d.primitives.Polygon();
			Geometry g = this.grid.getCellGeometry(p);
			Coordinate[] ca = g.getCoordinates();
			for(int i = 0; i < ca.length-1; i++){
				cell.add(new org.jzy3d.plot3d.primitives.Point(new Coord3d(ca[i].x, ca[i].y, 0.0)));
			}
			double thisWeight = this.kde.getWeight(p);
			if(thisWeight > 0){
				LOG.debug("Some weight");
			}
			int alpha = (int) Math.round((thisWeight / max)*255.0);
			Color thisColor = new Color(0, 0, 0, alpha);
			
			cell.setFaceDisplayed(true);
			cell.setColor(thisColor);
			cell.setWireframeDisplayed(true);
			cell.setWireframeColor(thisColor);
			chart.getScene().add(cell);
		}
		LOG.info("Total weight: " + sum);
		
		chart.getView().shoot();
	}

}
