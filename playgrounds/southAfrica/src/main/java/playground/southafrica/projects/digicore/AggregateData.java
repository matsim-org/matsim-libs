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

package playground.southafrica.projects.digicore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Polygon;
import org.jzy3d.plot3d.primitives.Quad;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.lights.Light;
import org.jzy3d.plot3d.rendering.view.modes.ViewPositionMode;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;

public class AggregateData extends AbstractAnalysis{
	private final static Logger LOG = Logger.getLogger(AggregateData.class);
	private double scale;
	private OcTree<Coord3d> ot;
	private Map<Coord3d, Integer> map;
	private Map<Coord3d, Integer> mapRating;
	private double countMax;
	private double maxLines;
	
	/* Specify colours */
	private final static Color MY_GREEN = new Color(147, 214, 83, 255);
	private final static Color MY_YELLOW = new Color(248, 215,85, 255);
	private final static Color MY_ORANGE = new Color(250, 164, 54, 255);
	private final static Color MY_RED = new Color(246, 43, 32, 255);

	public static void main(String[] args) {
		Header.printHeader(AggregateData.class.toString(), args);
		
		AggregateData ad = new AggregateData();
		ad.run(args);
		
		try {
			AnalysisLauncher.open(ad);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot visualise!!");
		}
		
		Header.printFooter();
	}
	
	
	private void run(String[] args){
		String filename = args[0];
		String rFile = args[1];
		this.scale = Double.parseDouble(args[2]);
		this.maxLines = Double.parseDouble(args[3]);
		
		List<Double> zoneThresholds = new ArrayList<Double>();
		int argsIndex = 4;
		while(args.length > argsIndex){
			zoneThresholds.add(Double.parseDouble(args[argsIndex++]));
		}
		
		/* Read the raw data. */
		LOG.info("Parsing the data extent...");
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;
		
		Counter counter = new Counter("   lines # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while( (line = br.readLine()) != null){
				String[] sa = line.split(",");
				double x = Double.parseDouble(sa[5]);
				double y = Double.parseDouble(sa[6]);
				double z = Double.parseDouble(sa[7]);
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				minZ = Math.min(minZ, z);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
				maxZ = Math.max(maxZ, z);
				counter.incCounter();
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
		counter.printCounter();
		counter.reset();
		
		/* Establish the centroid grid given the point extent. */
		FCCGrid fccg = new FCCGrid(minX, maxX, minY, maxY, minZ, maxZ, this.scale);
		GridPoint[] ga = fccg.getFcGrid();
		for(GridPoint gp : ga){
			minX = Math.min(minX, gp.getX());
			minY = Math.min(minY, gp.getY());
			minZ = Math.min(minZ, gp.getZ());
			maxX = Math.max(maxX, gp.getX());
			maxY = Math.max(maxY, gp.getY());
			maxZ = Math.max(maxZ, gp.getZ());
		}

		/* Establish and populate the OcTree given the centroid extent. */
		map = new HashMap<Coord3d, Integer>(ga.length);
		LOG.info("Building OcTree with dodecahedron centroids...");
		ot = new OcTree<Coord3d>(minX, minY, minZ, maxX, maxY, maxZ);
		for(GridPoint gp : ga){
			Coord3d c = new Coord3d(gp.getX(), gp.getY(), gp.getZ());
			ot.put(gp.getX(), gp.getY(), gp.getZ(), c);
			map.put(c, new Integer(0));
		}
		LOG.info("Done populating centroid grid: " + ga.length + " points.");
		
		/* Populate the Dodecahedra. */
		LOG.info("Populating the dodecahedra with point observations...");
		double pointsConsidered = 0.0;
		br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while( (line = br.readLine()) != null && counter.getCounter() < maxLines){
				String[] sa = line.split(",");
				String id = sa[1];
				double x = Double.parseDouble(sa[5]);
				double y = Double.parseDouble(sa[6]);
				double z = Double.parseDouble(sa[7]);
				double speed = Double.parseDouble(sa[8]);
				double road = Double.parseDouble(sa[9]);
				
				/* Put data conditions here. */
				if(
//						id.equalsIgnoreCase("37ff9d8e04c164ee793e172a561c7b1e") &	/* Specific individual, A. */
//						id.equalsIgnoreCase("9a01080c086096aaaaff7504a01ea9e3") &	/* Specific individual, B. */
//						id.equalsIgnoreCase("0ae0c60759b410c2c38fa0ba135a8e16") &	/* Specific individual, C. */
//						road <= 2 & 												/* Road is a highway */
//						speed <= 60.0 &												/* Low speed */
//						speed > 60.0 &												/* High speed */
						true
						){
					Coord3d c = ot.get(x, y, z);
					map.put(c, map.get(c)+1);
					pointsConsidered++;
				}
				
				counter.incCounter();
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
		counter.printCounter();
		
		
		Comparator<Coord3d> coordComparator = new Comparator<Coord3d>() {

			@Override
			public int compare(Coord3d o1, Coord3d o2) {
				return map.get(o2).compareTo(map.get(o1));
			}
		};
		/* Sort the polyhedra based on their counts.*/
		LOG.info("Ranking polyhedra based on point-counts only.");
		List<Coord3d> sortedCoords = new ArrayList<Coord3d>(map.keySet());
		Collections.sort(sortedCoords, coordComparator);
		LOG.info("   10 polyhedra with largest number of observations:");
		for(int i = 0; i < 10; i++){
			LOG.info(String.format("      %d: %d observations", i+1, map.get(sortedCoords.get(i))));
		}
		double totalAdded = 0.0;
		double cumulative = 0.0;
		mapRating = new TreeMap<Coord3d, Integer>(coordComparator);
		for(int i = 0; i < sortedCoords.size(); i++){
			Coord3d c = sortedCoords.get(i);
			int obs = map.get(c);
			totalAdded += (double)obs;
			cumulative = totalAdded / pointsConsidered;
			
			/* Get the rating class for this value. */
			Integer ratingZone = null;
			int zoneIndex = 0;
			while(ratingZone == null && zoneIndex < zoneThresholds.size()){
				if(cumulative <= zoneThresholds.get(zoneIndex)){
					ratingZone = new Integer(zoneIndex);
				} else{
					zoneIndex++;
				}
			}
			mapRating.put(c, ratingZone);
		}
		
		/* Remove zero-count dodecahedra. */
		List<Coord3d> centroidsToRemove = new ArrayList<Coord3d>();
		for(Coord3d c : map.keySet()){
			int count = map.get(c);
			countMax = Math.max(countMax, (double)count);
			if(count == 0){
				centroidsToRemove.add(c);
			}
		}
		for(Coord3d c : centroidsToRemove){
			map.remove(c);
		}
		
		/* Write values out to file, for visualisation in R. */
		LOG.info("Writing centroid counts to file...");
		BufferedWriter bw = IOUtils.getBufferedWriter(rFile);
		try{
			for(Coord3d c : map.keySet()){
				bw.write(c.toString());
				bw.write(",");
				bw.write(String.valueOf(map.get(c)));
				bw.write(",");
				bw.write(String.valueOf(mapRating.get(c)));
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + rFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + rFile);
			}
		}
		
		LOG.info("A total of " + map.size() + " dodecahedra contains points (max value: " + countMax + ")");
	}

	
	@Override
	public void init() throws Exception {
//		printCentroids();
		printPolyhedra();
//		printPolyhedraSlice();
	}
	
	public AggregateData() {
		this.countMax = 0.0;
	}
	
	private void printCentroids(){
		/* Cool, now let's plot the buggers. First as spheres. */
		int index = 0;
		Coord3d[] coords = new Coord3d[map.size()];
		Color[] colors = new Color[map.size()];
		for(Coord3d c : map.keySet()){
			coords[index] = c;
			float a = (float) (((double)map.get(c))/countMax);
			colors[index++] = new Color(0, 0, 0, a);
		}
		Scatter scatter = new Scatter(coords, colors, 6f);
		chart = AWTChartComponentFactory.chart(Quality.Advanced, "awt");
		chart.getView().updateBounds();
		chart.getScene().add(scatter);
	}
	
	private void printPolyhedra(){
		/* Set up the chart. */
		chart = AWTChartComponentFactory.chart(Quality.Nicest, "awt");
		Light light = chart.addLight(new Coord3d(-1000f, -300f, 12000f));
		light.setRepresentationRadius(100);
		light.setEnabled(true);
		light.setEnabled(false);

		chart.addKeyController();
		Coord3d oldViewPoint = chart.getCanvas().getView().getViewPoint();
		Coord3d newViewPoint = new Coord3d(oldViewPoint.x, oldViewPoint.y/3, 500);

		chart.getView().setSquared(false);
		chart.getView().setSquared(true);
		chart.getView().setMaximized(true);
		chart.getView().setViewPoint(newViewPoint, true);
		chart.getView().setViewPositionMode(ViewPositionMode.FREE);
		chart.getView().setBoundManual(new BoundingBox3d(-800f, 800f, -800f, 800f, 600f, 1500f));
		chart.getView().updateBounds();
		
		for(Coord3d c : map.keySet()){
			/* Set up polyhedra. */
			ArrayList<org.jzy3d.plot3d.primitives.Polygon> body = new ArrayList<org.jzy3d.plot3d.primitives.Polygon>();
			
			/* Determine colour. */
			float a = (float) (((double)map.get(c))/countMax);
			Color lightGray = new Color(200, 200, 200, 255);
			Color darkerGray = new Color(100, 100, 100, 255);
			Color gray = Color.GRAY;
			Color white = new Color(255, 255, 255, 255); 
			Color yellow = new Color(255, 255, 180, 255);
			
			/* Change colour based on rating zone. */
			Color fillColor = null;
			int zone = mapRating.get(c);
			switch (zone) {
			case 0:
				fillColor = MY_GREEN;
				break;
			case 1:
				fillColor = MY_YELLOW;
				break;
			case 2:
				fillColor = MY_ORANGE;
				break;
			case 3:
				fillColor = MY_RED;
				break;
			default:
			}
			if(fillColor == null){
				LOG.error("Something wrong!!");
			}
			
			
			/* Limit faces to certain colours. */
			if(zone < 4){
				/* Create faces. */
				FCCPolyhedron poly = new FCCPolyhedron(c.x, c.y, c.z, scale);
				for(NDPolygon face : poly.getFcPolyhedron()){
					Quad q = new Quad();
					for(GridPoint point : face.getPolyFace()){
						q.add(new Point(new Coord3d(point.getX(), point.getY(), point.getZ())));
					}
					body.add((org.jzy3d.plot3d.primitives.Polygon)q);
				}
				
				Shape shape = new Shape(body);
				shape.setFaceDisplayed(true);
				shape.setColor(fillColor);
				shape.setFaceDisplayed(true);
				shape.setWireframeColor(darkerGray);
				chart.getScene().add(shape);
			}

			chart.getView().shoot();
		}
	}
	
	
	private void printPolyhedraSlice(){
		/* Set up the chart. */
		chart = AWTChartComponentFactory.chart(Quality.Advanced, "awt");
		Light light = chart.addLight(new Coord3d(-1000f, -300f, 12000f));
		light.setRepresentationRadius(100);
		light.setEnabled(true);
		light.setEnabled(false);
		
		chart.addKeyController();
		Coord3d oldViewPoint = chart.getCanvas().getView().getViewPoint();
		Coord3d newViewPoint = new Coord3d(2*Math.PI/2, Math.PI/2, 2000);
		
		chart.getView().setSquared(false);
		chart.getView().setSquared(true);
		chart.getView().setMaximized(true);
		chart.getView().setViewPoint(newViewPoint, true);
		chart.getView().setViewPositionMode(ViewPositionMode.FREE);
		chart.getView().setBoundManual(new BoundingBox3d(-600f, 600f, -700f, 700f, 600f, 1500f));
		chart.getView().updateBounds();
		
		for(Coord3d c : map.keySet()){
			/* Determine colour. */
			Color darkerGray = new Color(100, 100, 100, 255);
			
			/* Change colour based on rating zone. */
			Color fillColor = null;
			int zone = mapRating.get(c);
			switch (zone) {
			case 0:
				fillColor = MY_GREEN;
				break;
			case 1:
				fillColor = MY_YELLOW;
				break;
			case 2:
				fillColor = MY_ORANGE;
				break;
			case 3:
				fillColor = MY_RED;
				break;
			default:
			}
			if(fillColor == null){
				LOG.error("Something wrong!!");
			}
			
			
			/* Create slice face. */
//			for(double depth = 800; depth <= 1200; depth+=10){
			{double depth = 1009.0;
				FCCPlanePolygon fccPoly = new FCCPlanePolygon(c.x, c.y, c.z, 3, depth, scale);
				Polygon poly = new Polygon();
				
				for(GridPoint point : fccPoly.getFcPlanePolygon().getPolyFace()){
					poly.add(new Point(new Coord3d(point.getX(), point.getY(), 0)));
				}
				
				poly.setFaceDisplayed(true);
				poly.setColor(fillColor);
				poly.setWireframeColor(darkerGray);
				chart.getScene().add(poly);
				chart.getView().shoot();
//				try {
//					chart.screenshot(new File("/Users/jwjoubert/Pictures/Digicore/Slice_" + depth + ".png"));
//				} catch (IOException e) {
//					e.printStackTrace();
//					LOG.error("Could not write image for depth " + depth);
//				}
			}
		}
	}
	
	

}
