/* *********************************************************************** *
 * project: org.matsim.*
 * SheltersReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.shelters;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.basic.v01.BasicOpeningTime;
import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicOpeningTime.DayType;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.OpeningTime;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.ActivityOption;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.ShapeFileReader;
import org.matsim.world.algorithms.WorldConnectLocations;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class SheltersReader {

	private final NetworkLayer network;
	private final Facilities facilities;

	private final static double saveBX = 662440;
	private final static double saveBY = 9898860;
	
	public SheltersReader(final NetworkLayer network, final Facilities facilities) {
		this.network = network;
		this.facilities = facilities;
		
	}
	
	public void read(final String filename) {
		FeatureSource fs = null;
		try {
			fs = ShapeFileReader.readDataFile(filename);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		Iterator fit;
		try {
			fit = fs.getFeatures().iterator();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		
		
		int count = 0;
		Coord superC = new CoordImpl(saveBX, saveBY);
		Node superN1 = this.network.getNearestNode(superC);
		Node superN2 = this.network.createNode(new IdImpl("shelter" + count), superC);
		this.network.createLink(new IdImpl("shelter" + count), superN1, superN2, 1, 1000000, 1000000, 1);
//		this.network.createLink(new IdImpl("rev_shelter" + count), superN2, superN1, 1, 1000000, 1000000, 1);	
		Facility superFac = this.facilities.createFacility(new IdImpl("shelter" + count++), superC);
		ActivityOption superAct = superFac.createActivity("evacuated");
		superAct.setCapacity(1000000);
		Map<DayType, SortedSet<BasicOpeningTime>> opentimes = new TreeMap<DayType, SortedSet<BasicOpeningTime>>();
		DayType dt = DayType.wk;
		BasicOpeningTime t = new OpeningTime(dt,0,3600*30);
		TreeSet<BasicOpeningTime> ts = new TreeSet<BasicOpeningTime>();
		ts.add(t);
		opentimes.put(dt, ts);
		while (fit.hasNext()) { 
			Feature ft = (Feature) fit.next();
			Geometry geo = ft.getDefaultGeometry();
			if (!(geo instanceof Point)){
				throw new RuntimeException("Wrong shape file format! The geometries in shape file need to be of type Point");
			}
			
			double flowCap = (Double) ft.getAttribute(2);
			
			int storageCap = (int)(((double)((Double) ft.getAttribute(1))) * Gbl.getConfig().simulation().getStorageCapFactor());
			 			
			Coord c = MGC.coordinate2Coord(geo.getCoordinate());
			Node n1 = this.network.getNearestNode(c);
			Node n2 = this.network.createNode(new IdImpl("shelter" + count), c);
			this.network.createLink(new IdImpl("shelter" + count), n1, n2, 1, 1.66, flowCap, 1);
//			this.network.createLink(new IdImpl("rev_shelter" + count), n2, n1, 1, 1.66, flowCap, 1);//just to make this networkcleaner save
			
			Facility fac = this.facilities.createFacility(new IdImpl("shelter" + count++), c);
			ActivityOption act = fac.createActivity("evacuated");
			act.setOpentimes(opentimes);
			act.setCapacity(storageCap);

			
		}

		
		new WorldConnectLocations().run(Gbl.getWorld());
//		System.out.println("s     " + (superFac.getLink() == null) + " " + superFac.getId());
//		throw new RuntimeException("s     " + (superFac.getLink() == null) + " " + superFac.getId());
	}
	
	public static void main(final String [] args) {
		String shelters = "../inputs/padang/network_v20080618/shelters.shp";
		String netfile = "../inputs/networks/padang_net_v20080618.xml";
		
		Gbl.createWorld();
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netfile);
		Gbl.getWorld().setNetworkLayer(network);
		Facilities fac = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		Gbl.getWorld().complete();
		new SheltersReader(network,fac).read(shelters);
		
	}
}
