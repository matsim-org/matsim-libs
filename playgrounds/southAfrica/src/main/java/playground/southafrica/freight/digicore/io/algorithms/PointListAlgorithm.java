/* *********************************************************************** *
 * project: org.matsim.*
 * PointListAlgorithm.java
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
package playground.southafrica.freight.digicore.io.algorithms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Counter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.containers.MyZone;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

/**
 *
 * @author jwjoubert
 */
public class PointListAlgorithm implements DigicoreVehiclesAlgorithm {
	final private Logger log = Logger.getLogger(PointListAlgorithm.class);
	private final GeometryFactory gf = new GeometryFactory();
	private QuadTree<MyZone> qt;
	private final Polygon qtArea;
	private String root = null;
	private Counter counter = new Counter("   vehicles # ");
	private List<Id<MyZone>> existingSerialisedFiles = new ArrayList<>();
	private int emptyMaps = 0;


	
	public PointListAlgorithm(String shapefile, int idField) throws IOException {
		MyMultiFeatureReader mfr = new MyMultiFeatureReader();
		mfr.readMultizoneShapefile(shapefile, idField);
		
		/* Build a QuadTree from the given shapefile. */
		List<MyZone> zoneList = mfr.getAllZones();
		
		/* Build a QuadTree of the Zones. */
		log.info(" Building QuadTree from zones...");
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for(MyZone mz : zoneList){
			minX = Math.min(minX, mz.getEnvelope().getCoordinates()[0].x);
			maxX = Math.max(maxX, mz.getEnvelope().getCoordinates()[2].x);
			minY = Math.min(minY, mz.getEnvelope().getCoordinates()[0].y);
			maxY = Math.max(maxY, mz.getEnvelope().getCoordinates()[2].y);
		}
		qt = new QuadTree<MyZone>(minX, minY, maxX, maxY);
		for(MyZone mz : zoneList){
			qt.put(mz.getEnvelope().getCentroid().getX(), mz.getEnvelope().getCentroid().getY(), mz);
		}
		log.info("Done building QuadTree.");
		
		/* Create the envelope of the QuadTree. */
		Coordinate c1 = new Coordinate(qt.getMinEasting(), qt.getMinNorthing());
		Coordinate c2 = new Coordinate(qt.getMaxEasting(), qt.getMinNorthing());
		Coordinate c3 = new Coordinate(qt.getMaxEasting(), qt.getMaxNorthing());
		Coordinate c4 = new Coordinate(qt.getMinEasting(), qt.getMaxNorthing());
		Coordinate[] ca = {c1, c2, c3, c4, c1};
		this.qtArea = gf.createPolygon(ca);
		
	}
	
	public void setRoot(String root){
		root += root.endsWith("/") ? "" : "/";
		this.root = root;
		/* Make the necessary 'serialised' folder. */
		File folder = new File(root + "serial/");
		if(!folder.exists()){
			boolean makeSerialFolder = folder.mkdirs();
			if(!makeSerialFolder){
				throw new RuntimeException("Cannot create the folder in which objects will be serialized");
			}
			log.info("Created the folder for serialised files.");
		} else{
			this.existingSerialisedFiles = getAllSerializedIdsExceptLast(root);
			log.info("The folder for serialised files exists and contains " + 
					this.existingSerialisedFiles.size() + " usable files.");
		}
	}
	
	/** 
	 * Check how many serialised objects are already there, and use all 
	 * the IDs except the last. Why? Because the last one may not have 
	 * been serialised in entirety. */
	public List<Id<MyZone>> getAllSerializedIdsExceptLast(String folder){
		List<Id<MyZone>> list = new ArrayList<>();
		List<File> serialFiles = FileUtils.sampleFiles(new File(folder), Integer.MAX_VALUE, FileUtils.getFileFilter(".data"));
		long lastModified = Long.MIN_VALUE;
		if(serialFiles != null){
			for(File f : serialFiles){
				lastModified = Math.max(lastModified, f.lastModified());
			}
			for(File f : serialFiles){
				if(f.lastModified() < lastModified){
					list.add(Id.create(f.getName().substring(0, f.getName().indexOf(".")), MyZone.class));
				} else{
					/* Remove the last modified file(s). */
					f.delete();
				}
			}
		}
		return list;
	}
	
	public void printCounter(){
		this.counter.printCounter();
	}
	
	public void printNumberOfEmptyMaps(){
		log.info("Total number of vehicles with no activities in study area: " + emptyMaps);
	}
	
	
	/* (non-Javadoc)
	 * @see playground.southafrica.freight.digicore.io.algorithms.DigicoreVehiclesAlgorithm#apply(playground.southafrica.freight.digicore.containers.DigicoreVehicle)
	 */
	@Override
	public void apply(DigicoreVehicle vehicle) {
		if(this.root == null){
			throw new RuntimeException("Cannot apply the algorithm if no root folder is set.");
		}
		
		/* Ignore this vehicle if it has already been serialized. */
		if(!this.existingSerialisedFiles.contains(vehicle.getId())){
			HashMap<Id<MyZone>, ArrayList<Coord>> map = new HashMap<Id<MyZone>, ArrayList<Coord>>();
			for(DigicoreChain chain : vehicle.getChains()){
				for(DigicoreActivity activity : chain.getAllActivities()){
					Point p = gf.createPoint(new Coordinate(
							activity.getCoord().getX(), 
							activity.getCoord().getY()));
					if(qtArea.covers(p)){
						double radius = 1000.0;
						boolean found = false;

						while(!found){
							Collection<MyZone> possibleZones = this.qt.getDisk(p.getX(), p.getY(), radius);
							if(possibleZones.size() > 0){
								Iterator<MyZone> iterator = possibleZones.iterator();
								while(!found && iterator.hasNext()){
									MyZone zone = iterator.next();
									/* First check the envelope, then the geometry. */
									if(zone.getEnvelope().covers(p)){
										if(zone.covers(p)){
											found = true;
											if(!map.containsKey(zone.getId())){
												map.put(zone.getId(), new ArrayList<Coord>());
											}
											map.get(zone.getId()).add(activity.getCoord());
										}
									}
								}
								if(!found){
									radius *= 2;
									if(possibleZones.size() == this.qt.size()){
										/* This point is within the envelope of the
										 * QuadTree, but are actually not inside ANY
										 * of the zones, for example in Lesotho. The
										 * point should therefore be removed, 
										 * otherwise we run into an infinite loop. */
										found = true;
									}
								}
							} else{
								radius *= 2;
							}
						}
					} else{
						/* Ignore the activity: it falls outside the study area. */
					}
				}
			}

			/* Serialise the map and write to file, but only if there IS a map
			 * to serialize. */
			if(map.size() > 0){
				FileOutputStream fos = null;
				ObjectOutputStream oos = null;
				try{
					fos = new FileOutputStream(root + "serial/" + vehicle.getId().toString() + ".data");
					oos = new ObjectOutputStream(fos);
					oos.writeObject(map);
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot serialise vehicle " + vehicle.getId().toString());
				} finally{
					try {
						fos.close();
						oos.close();
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("Cannot serialise vehicle " + vehicle.getId().toString());
					}
				}
			} else{
				emptyMaps++;
			}
		}
		counter.incCounter();
	}

}
