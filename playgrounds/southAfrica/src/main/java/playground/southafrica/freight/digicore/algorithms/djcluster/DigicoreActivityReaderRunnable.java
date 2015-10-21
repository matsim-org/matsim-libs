/* *********************************************************************** *
 * project: org.matsim.*
 * RunnableDigicoreActivityReader.java
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

package playground.southafrica.freight.digicore.algorithms.djcluster;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;
import playground.southafrica.utilities.containers.MyZone;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class DigicoreActivityReaderRunnable implements Runnable {
	
	private int inCount = 0;
	private int outCount = 0;
	private GeometryFactory gf = new GeometryFactory();
	private final Map<Id<MyZone>, List<Coord>> map = new HashMap<Id<MyZone>, List<Coord>>();;
	private final File vehicleFile;
	private final QuadTree<MyZone> zoneQT;
	private Counter counter;
	
	public DigicoreActivityReaderRunnable(final File vehicleFile, QuadTree<MyZone> zoneQT, Counter counter) {
		this.vehicleFile = vehicleFile;
		this.zoneQT = zoneQT;
		this.counter = counter;
		
		for(MyZone mz : this.zoneQT.values()){
			map.put(mz.getId(), new ArrayList<Coord>());
		}
	}
	
	
	public int getInCount(){
		return this.inCount;
	}
	
	
	public int getOutCount(){
		return this.outCount;
	}
	

	@Override
	public void run() {
		DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
		dvr.parse(this.vehicleFile.getAbsolutePath());
		DigicoreVehicle dv = dvr.getVehicle();
		for(DigicoreChain dc : dv.getChains()){
			
			/* Read in ALL activities, not just minor activities. */
			for(DigicoreActivity da : dc.getAllActivities()){
				Point p = gf.createPoint(new Coordinate(da.getCoord().getX(), da.getCoord().getY()));

				/* Get all the zones surrounding the point.
				 *  
				 * PROBLEM: If only the entire area is given, for example Nelson
				 * Mandela Bay, and the single zone is put in the QT at its 
				 * centroid location, only points surrounding the centroid will
				 * be added. 
				 * 
				 * One way to solve this, albeit not very computationally 
				 * efficient, is to check the number of MyZones in the QT. If 
				 * only one, then check that one zone. Alternatively, follow 
				 * the original procedure of looking for only the surrounding 
				 * zones. */
				if(zoneQT.size() > 1){
					Collection<MyZone> neighbourhood =  zoneQT.getDisk(p.getX(), p.getY(), 10000);
					boolean found = false;
					Iterator<MyZone> iterator = neighbourhood.iterator();
					while(iterator.hasNext() && !found){
						MyZone mz = iterator.next();
						if(mz.getEnvelope().contains(p)){
							if(mz.contains(p)){
								found = true;
								map.get(mz.getId()).add(da.getCoord());
								inCount++;
							}
						}
					}
					if(!found){
						outCount++;
					}					
				} else{
					/* There is only ONE zone, i.e. the entire study area. 
					 * Check ALL points in that zone. To make it computationally
					 * a bit more efficient, first check the envelope. */
					MyZone zone = zoneQT.getClosest(p.getX(), p.getY());
					if(zone.getEnvelope().contains(p)){
						if(zone.contains(p)){
							map.get(zone.getId()).add(da.getCoord());
							inCount++;
						} else{
							outCount++;
						}
					} else{
						outCount++;
					}
				}
			}		
		}
		counter.incCounter();
	}


	public Map<Id<MyZone>, List<Coord>> getMap(){
		return this.map;
	}

}

