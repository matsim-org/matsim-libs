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

package playground.southafrica.projects.digicore.tmp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Polygon;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.matsim.core.utils.collections.QuadTree;

public class ScatterDemo extends AbstractAnalysis{
	private final static Logger LOG = Logger.getLogger(ScatterDemo.class);

	public static void main(String[] args) throws Exception {
		AnalysisLauncher.open(new ScatterDemo());
	}

	private void initOriginal() throws Exception {
		int size = 500000;
		float x;
		float y;
		float z;
		float a;
		
		Coord3d[] points = new Coord3d[size];
		Color[] colors = new Color[size];
		
		Random r = new Random();
		r.setSeed(0);
		
		for(int i = 0; i < size; i++){
			x = r.nextFloat() - 0.5f;
			y = r.nextFloat() - 0.5f;
			z = r.nextFloat() - 0.5f;
			points[i] = new Coord3d(x, y, z);
			a = 0.25f;
			colors[i] = new Color(x, y, z, a);
		}
		
		Scatter scatter = new Scatter(points, colors);
		chart = AWTChartComponentFactory.chart(Quality.Advanced, "newt");
		chart.getScene().add(scatter);
	}

	
	private void initSample() throws Exception{
		Coord3d[] points = readSample("/Users/jwjoubert/Documents/r/MATSim-SA/digicore/data/sample.csv");
		
		/* Get the centroid of the data set. */
		double sumX = 0.0;
		double sumY = 0.0;
		double sumZ = 0.0;
		for(Coord3d c : points){
			sumX += c.x;
			sumY += c.y;
			sumZ += c.z;
		}
		double cX = sumX / (double)points.length;
		double cY = sumY / (double)points.length;
		double cZ = sumZ / (double)points.length;
		Coord3d centroid = new Coord3d(cX, cY, cZ);

		/* Report the mean. */
		LOG.info(String.format("Mean X: %.2f", cX));
		LOG.info(String.format("Mean Y: %.2f", cY));
		LOG.info(String.format("Mean Z: %.2f", cZ));
		
		/* Calculate the distance from the centroid for each point. */
		Double[] distances = new Double[points.length];
		Double[] distancesAdjusted = new Double[points.length];
		double maxD = Double.NEGATIVE_INFINITY;
		double maxDAdjusted = Double.NEGATIVE_INFINITY;
		for(int i = 0; i < points.length; i++){
			Coord3d c = points[i];
			double d = Math.sqrt(
					Math.pow(cX-c.x, 2) + 
					Math.pow(cY-c.y, 2) + 
					Math.pow(cZ-c.z, 2));
			distances[i] = d;
			maxD = Math.max(maxD, d);
			
			/* Adjust distance to account for angle as well. Diagonals should be
			 * penalised more. */
			double multiplier = 1e-5;
			double angleXY = Math.atan2(
					Math.abs(c.y - centroid.y), 
					Math.abs(c.x - centroid.x));
			double angleXZ = Math.atan2(
					Math.abs(c.z - centroid.z),
					Math.abs(c.x - centroid.x));
			distancesAdjusted[i] = d * 
					Math.abs( Math.sin(angleXY*2) ) * 
//					Math.abs(  Math.sin(angleXZ*2) ) *
					multiplier;
			maxDAdjusted = Math.max(maxDAdjusted, distancesAdjusted[i]);
		}
		
		/* Set the colour for each point, based on the distances from the
		 * data centroid. */
		ColorMapRainbow map = new ColorMapRainbow();
		Color[] colors = new Color[points.length];
		for(int i = 0; i < points.length; i++){
			colors[i] = map.getColor(0, 0, distancesAdjusted[i], 0, maxDAdjusted);
		}

		/* Make rainbow colors. */
		Scatter scatter = new Scatter(points, colors, 4f);
		chart = AWTChartComponentFactory.chart(Quality.Advanced, "newt");
		
		Polygon polygon1 = new Polygon();
		polygon1.add(new Point(new Coord3d(-500, -500, 1000)));
		polygon1.add(new Point(new Coord3d(-500, 500, 1000)));
		polygon1.add(new Point(new Coord3d(500, 500, 1000)));
		polygon1.add(new Point(new Coord3d(500, -500, 1000)));
		polygon1.setColor(new Color(0f, 1f, 0f, 0.5f));
		polygon1.setFaceDisplayed(true);

		Polygon polygon2 = new Polygon();
		polygon2.add(new Point(new Coord3d(-500, -500, 800)));
		polygon2.add(new Point(new Coord3d(-500, 500, 800)));
		polygon2.add(new Point(new Coord3d(500, 500, 800)));
		polygon2.add(new Point(new Coord3d(500, -500, 800)));
		polygon2.setColor(new Color(1f, 0f, 0f, 0.5f));
		polygon2.setFaceDisplayed(true);
		
		
//		chart.getScene().add(polygon1);
//		chart.getScene().add(polygon2);
		chart.getScene().add(scatter);
		
	}
	
	
	private Coord3d[] readSample(String filename) throws FileNotFoundException{
		LOG.info("Reading the data from " + filename);
		
		List<Coord3d> list = new ArrayList<Coord3d>();

		BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
		try{
			/* Assuming the file does not have a header. */
			String line = null;
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				float x = Float.parseFloat(sa[5]);
				float y = Float.parseFloat(sa[6]);
				float z = Float.parseFloat(sa[7]);
				list.add(new Coord3d(x, y, z));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		
		Coord3d[] points = new Coord3d[list.size()];
		return list.toArray(points);
	}

	@Override
	public void init() throws Exception {
		initSample();
	}
	
	private void buildOcTree(){
		QuadTree<Coord3d> qt = null;
	}

}
