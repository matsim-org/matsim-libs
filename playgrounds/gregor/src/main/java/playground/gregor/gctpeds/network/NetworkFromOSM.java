/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.gregor.gctpeds.network;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.Volume;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.casim.proto.CALinkInfos;
import playground.gregor.casim.proto.CALinkInfos.CALinInfos.Builder;
import playground.gregor.casim.proto.CALinkInfos.CALinInfos.CALinkInfo;
import playground.gregor.casim.simulation.physics.AbstractCANetwork;
import playground.gregor.sim2d_v4.io.osmparser.OSM;
import playground.gregor.sim2d_v4.io.osmparser.OSMNode;
import playground.gregor.sim2d_v4.io.osmparser.OSMWay;
import playground.gregor.sim2d_v4.io.osmparser.OSMXMLParser;

public class NetworkFromOSM {

	private Scenario sc;
	private String osmFile;
	private MathTransform transform;
	
	private int lId = 0;
	private Builder caLinkInfosB;
	private Counts counts;
	private static final Map<String,Timing> timings = new HashMap<>();
	static {
		timings.put("vanderbilt_ave_42nd_str_north", new Timing("11:00-14:00",34+54,16,15,90));
		timings.put("vanderbilt_ave_42nd_str_east", new Timing("11:00-14:00",34+54,16,15,90));
		timings.put("vanderbilt_ave_42nd_str_west", new Timing("11:00-14:00",34+54,16,15,90));
//		timings.put("vanderbilt_ave_42nd_str_north", new Timing("AOT",48+45,19+2+2+2,15,90));
//		timings.put("vanderbilt_ave_42nd_str_east", new Timing("AOT",48+45,19+2+2+2,15,90));
//		timings.put("vanderbilt_ave_42nd_str_west", new Timing("AOT",48+45,19+2+2+2,15,90));
		
//		timings.put("lexington_ave_42nd_str_west", new Timing("AOT",5,5+28,10+3+2+3,90));
//		timings.put("lexington_ave_42nd_str_east", new Timing("AOT",5,5+28,10+3+2+3,90));
//		timings.put("lexington_ave_42nd_str_north", new Timing("AOT",5+48,3+4+14+2,14,90));
//		timings.put("lexington_ave_42nd_str_south", new Timing("AOT",5+48,3+4+14+2,14,90));
		timings.put("lexington_ave_42nd_str_west", new Timing("08:00-20:00",5,5+28,10+3+2+3,90));
		timings.put("lexington_ave_42nd_str_east", new Timing("08:00-20:00",5,5+28,10+3+2+3,90));
		timings.put("lexington_ave_42nd_str_north", new Timing("08:00-20:00",5+48,3+4+14+2,14,90));
		timings.put("lexington_ave_42nd_str_south", new Timing("08:00-20:00",5+48,3+4+14+2,14,90));
		
		timings.put("madison_ave_42nd_str_west", new Timing("AAT",88,5+23,17,90));
		timings.put("madison_ave_42nd_str_east", new Timing("AAT",88,5+23,17,90));
		timings.put("madison_ave_42nd_str_north", new Timing("AAT",88+50,12+2+2+2,17,90));
		timings.put("madison_ave_42nd_str_south", new Timing("AAT",88+50,12+2+2+2,17,90));
		
		timings.put("park_ave_42nd_str_west", new Timing("AAT",34+54,10+2+2+2,15,90));
		timings.put("park_ave_42nd_str_east", new Timing("AAT",34+54,10+2+2+2,15,90));
		
		
		timings.put("park_ave_41st_str_north", new Timing("AOT",88,5+36,6,90));
		timings.put("park_ave_41st_str_west", new Timing("AOT",88,5+36,6,90));
		timings.put("park_ave_41st_str_east", new Timing("AOT",88,5+36,6,90));
		timings.put("park_ave_41st_str_south", new Timing("AOT",88+52,1+2+2+2,25,90));
		
//		timings.put("park_ave_41st_str_north", new Timing("MON-FRI_11:00-14:00",88,5+36,6,90));
//		timings.put("park_ave_41st_str_north", new Timing("MON-FRI_11:00-14:00",88,5+36,6,90));
//		timings.put("park_ave_41st_str_north", new Timing("MON-FRI_11:00-14:00",88,5+36,6,90));
//		timings.put("park_ave_41st_str_north", new Timing("MON-FRI_11:00-14:00",88,5+36,6,90));
		
		
		
		
		timings.put("5th_ave_42nd_str_west", new Timing("AAT",48,5+23,17,90));
		timings.put("5th_ave_42nd_str_east", new Timing("AAT",48,5+23,17,90));
		timings.put("5th_ave_42nd_str_north", new Timing("AAT",48+5+23+17+3+2,14+2+2+2,15,90));
		timings.put("5th_ave_42nd_str_south", new Timing("AAT",48+5+23+17+3+2,14+2+2+2,15,90));
		
		timings.put("madision_ave_43rd_str_north", new Timing("AAT",4+5+31+9+3+2,14+2+2+2,15,90));
		timings.put("madision_ave_43rd_str_south", new Timing("AAT",4+5+31+9+3+2,14+2+2+2,15,90));
		timings.put("madision_ave_43rd_str_west", new Timing("AAT",4,5+31,9,90));
		timings.put("madision_ave_43rd_str_east", new Timing("AAT",4,5+31,9,90));
		
//		timings.put("madision_ave_41st_str_south", new Timing("AOT",88+49,7+12+4+2,11,90));
//		timings.put("madision_ave_41st_str_north", new Timing("AOT",88+49,7,12,90));
//		timings.put("madision_ave_41st_str_east", new Timing("AOT",88,5+31,8,90));
//		timings.put("madision_ave_41st_str_west", new Timing("AOT",88,5+31,8,90));
		timings.put("madision_ave_41st_str_south", new Timing("MON-FRI_08:00-20:00",88+49,7+12+4+2,11,90));
		timings.put("madision_ave_41st_str_north", new Timing("MON-FRI_08:00-20:00",88+49,7,12,90));
		timings.put("madision_ave_41st_str_east", new Timing("MON-FRI_08:00-20:00",88,5+31,8,90));
		timings.put("madision_ave_41st_str_west", new Timing("MON-FRI_08:00-20:00",88,5+31,8,90));
		
		
		
		
		
	}
	
	private static final Timing DEFAULT_TIMING = new Timing("default",0,15,15,90); 
	
	
	private static final Set<String> MODES = new HashSet<>();
	static {
		MODES.add("car");
		MODES.add("walkca");
	}
	
	private static final CoordinateReferenceSystem osmCrs;
	static {
		try {
			osmCrs = CRS.decode("EPSG:4326", true); //WGS84
		} catch (NoSuchAuthorityCodeException e) {
			throw new IllegalArgumentException(e);
		} catch (FactoryException e) {
			throw new IllegalArgumentException(e);
		} 
	}

	public NetworkFromOSM(Scenario sc, String osmFile, Builder caLinkInfosBuilder, Counts c) {
		this.sc = sc;
		this.osmFile = osmFile;
		this.caLinkInfosB = caLinkInfosBuilder;
		this.counts = c;
	}
	
	public void run(){
		OSM osm = new OSM();
		osm.addKey("width");
		osm.addKey("crossing");
		osm.addKey("side");
		osm.addKey("departures");
		osm.addKey("arrivals");
		osm.addKey("nb_count");
		osm.addKey("sb_count");
		osm.addKey("eb_count");
		osm.addKey("wb_count");
		OSMXMLParser parser = new OSMXMLParser(osm);
		parser.setValidating(false);
		parser.parse(this.osmFile);
		findTransformation(osm);
		createNodes(osm);
		createLinks(osm);
		
	}
	
	
	private void createLinks(OSM osm) {

		for (OSMWay w : osm.getWays()) {
			Iterator<Long> it = w.getNodeRefs().iterator();
			long lId0 = it.next();
			while (it.hasNext()) {
				long lId1 = it.next();
				Id<Node> id0 = Id.createNodeId(lId0);
				Id<Node> id1 = Id.createNodeId(lId1);
				createLink(id0,id1,w);
				createLink(id1,id0,w);
				
				lId0 = lId1;
			}
			
		}
		
	}

	private void createLink(Id<Node> id0, Id<Node> id1, OSMWay w) {
		Network net = this.sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		double cap = Double.parseDouble(w.getTags().get("width"))*0.3048; //width in feet
		Node n0 = net.getNodes().get(id0);
		Node n1 = net.getNodes().get(id1);
		double length = CoordUtils.calcDistance(n0.getCoord(), n1.getCoord());
		
		Link l = fac.createLink(Id.createLinkId(this.lId++), n0, n1);
		l.setLength(length);
		l.setCapacity(cap);
		l.setNumberOfLanes(cap/AbstractCANetwork.PED_WIDTH);//pedestrian lanes
		l.setFreespeed(AbstractCANetwork.V_HAT);
		l.setAllowedModes(MODES);
		net.addLink(l);
		
		playground.gregor.casim.proto.CALinkInfos.CALinInfos.CALinkInfo.Builder b = null;
		String crossing = w.getTags().get("crossing");
		if (crossing != null) {
			b = CALinkInfos.CALinInfos.CALinkInfo.newBuilder().setId(l.getId().toString());
			String side = w.getTags().get("side");
			Timing t = timings.get(crossing+"_"+side);
			if (t == null) {
				t = DEFAULT_TIMING;
			}
			b.setCrossing(crossing).setSide(side)
			.setOffset(t.offset).setWk(t.wk).setFldw(t.fldw).setCycle(t.cycle);
			
		}
		
		String departures = w.getTags().get("departures");
		String arrivals = w.getTags().get("arrivals");
		if (departures != null){
			if (b == null) {
				b = CALinkInfos.CALinInfos.CALinkInfo.newBuilder().setId(l.getId().toString());
			}
			b.setDepartures(Integer.parseInt(departures));
		
		}
		if (arrivals != null){
			if (b == null) {
				b = CALinkInfos.CALinInfos.CALinkInfo.newBuilder().setId(l.getId().toString());
			}
			b.setArrivals(Integer.parseInt(arrivals));
		
		}
	
		
		String nb_cnt = w.getTags().get("nb_count");
		if (nb_cnt != null) {
			if (b == null) {
				b = CALinkInfos.CALinInfos.CALinkInfo.newBuilder().setId(l.getId().toString());
			}
			b.setMonitor(true);
			if (n0.getCoord().getY() < n1.getCoord().getY()) {
				Count cnt = counts.createAndAddCount(l.getId(), l.getId().toString());
//				for (int i = 1; i < 16; i++) {
//					cnt.createVolume(i, 0);
//				}
				for (int i = 16; i < 17; i++) {
					cnt.createVolume(i, Integer.parseInt(nb_cnt));
				}
//				for (int i = 17; i < 24; i++) {
//					cnt.createVolume(i, 0);
//				}
			}
		}
		String sb_cnt = w.getTags().get("sb_count");

		if (sb_cnt != null) {
			if (b == null) {
				b = CALinkInfos.CALinInfos.CALinkInfo.newBuilder().setId(l.getId().toString());
			}
			b.setMonitor(true);
			if (n0.getCoord().getY() > n1.getCoord().getY()) {
				Count cnt = counts.createAndAddCount(l.getId(), l.getId().toString());
//				for (int i = 1; i < 16; i++) {
//					cnt.createVolume(i, 0);
//				}
				for (int i = 16; i < 17; i++) {
					cnt.createVolume(i, Integer.parseInt(sb_cnt));
				}
//				for (int i = 19; i < 24; i++) {
//					cnt.createVolume(i, 0);
//				}
			}
		}
		
		String eb_cnt = w.getTags().get("eb_count");
		
		if (eb_cnt != null) {
			if (b == null) {
				b = CALinkInfos.CALinInfos.CALinkInfo.newBuilder().setId(l.getId().toString());
			}
			b.setMonitor(true);
			if (n0.getCoord().getX() < n1.getCoord().getX()) {
				Count cnt = counts.createAndAddCount(l.getId(), l.getId().toString());
//				for (int i = 1; i < 16; i++) {
//					cnt.createVolume(i, 0);
//				}
				for (int i = 16; i < 17; i++) {
					cnt.createVolume(i, Integer.parseInt(eb_cnt));
				}
//				for (int i = 19; i < 24; i++) {
//					cnt.createVolume(i, 0);
//				}
			}
		}
		String wb_cnt = w.getTags().get("wb_count");
		if (eb_cnt != null) {
			if (b == null) {
				b = CALinkInfos.CALinInfos.CALinkInfo.newBuilder().setId(l.getId().toString());
			}
			b.setMonitor(true);
			if (n0.getCoord().getX() > n1.getCoord().getX()) {
				Count cnt = counts.createAndAddCount(l.getId(), l.getId().toString());
//				for (int i = 1; i < 16; i++) {
//					cnt.createVolume(i, 0);
//				}
				for (int i = 16; i < 17; i++) {
					cnt.createVolume(i, Integer.parseInt(wb_cnt));
				}
//				for (int i = 19; i < 24; i++) {
//					cnt.createVolume(i, 0);
//				}
			}
		}
//		((LinkImpl)l).
		
		if (b != null) {
			this.caLinkInfosB.addCaLinkInfo(b.build());
		}
		
	}

	private void createNodes(OSM osm) {
		Network net = this.sc.getNetwork();
		
		NetworkFactory fac = net.getFactory();
		for (OSMNode n : osm.getNodes()) {
			double lat = n.getLat();
			double lon = n.getLon();
			long lId = n.getId();
			Id<Node> id = Id.createNodeId(lId);
			Coordinate c = new Coordinate(lon,lat);
			try {
				JTS.transform(c, c, transform);
			} catch (TransformException e) {
				throw new IllegalArgumentException(e);
			}
			Coord cc = MGC.coordinate2Coord(c);
			Node nn = fac.createNode(id, cc);
			net.addNode(nn);
		}
	}

	private void findTransformation(OSM osm) {
		
		double minLat = Double.POSITIVE_INFINITY,minLon = Double.POSITIVE_INFINITY, 
				maxLat = Double.NEGATIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;
		for (OSMNode n : osm.getNodes()) {
			double lat = n.getLat();
			double lon = n.getLon();
			if (lat < minLat) {
				minLat = lat;
			} 
			if (lat > maxLat) {
				maxLat = lat;
			}
			if (lon < minLon) {
				minLon = lon;
			}
			if (lon > maxLon) {
				maxLon = lon;
			}
			
		}
		double centerLon = (minLon+maxLon)/2;
		double centerLat = (minLat+maxLat)/2;
		String epsgCode = getEpsgCode(centerLon,centerLat);
		this.sc.getConfig().global().setCoordinateSystem(epsgCode);
		CoordinateReferenceSystem targetCrs = null;
		try {
			targetCrs = CRS.decode(epsgCode);
		} catch (NoSuchAuthorityCodeException e) {
			throw new IllegalArgumentException(e);
		} catch (FactoryException e) {
			throw new IllegalArgumentException(e);
		} 
		
		try {
			this.transform = CRS.findMathTransform(osmCrs, targetCrs);
		} catch (FactoryException e) {
			e.printStackTrace();
		}
	}

	private String getEpsgCode(double lon, double lat) {
		int utmZone = (int) (Math.ceil((180+lon) / 6)+0.5);
		String epsgCode = null;
		if (lat > 0 ) { //northern hemisphere 
		  epsgCode = "EPSG:326" + utmZone;
		} else { //southern hemisphere
		  epsgCode = "EPSG:327" + utmZone;
		}
		return epsgCode;
	}

	public static void main(String [] args) {
		String osmFile = "/Users/laemmel/devel/nyc/gct_vicinity/gct_weekdays_pm_peek_2.osm";
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		Builder b = CALinkInfos.CALinInfos.newBuilder();
		Counts cnt = new Counts();
		cnt.setDescription("ped project GCT vicinity");
		cnt.setYear(2015);
		cnt.setName("weekday pm peak");
		NetworkFromOSM netConv = new NetworkFromOSM(sc, osmFile,b,cnt);
		netConv.run();
		
		c.network().setInputFile("/Users/laemmel/devel/nyc/gct_vicinity/network.xml.gz");
		NetworkImpl net = (NetworkImpl) sc.getNetwork();
		net.setCapacityPeriod(1.0);
		new NetworkWriter(sc.getNetwork()).write("/Users/laemmel/devel/nyc/gct_vicinity/network.xml.gz");
		
		c.counts().setCountsFileName("/Users/laemmel/devel/nyc/gct_vicinity/counts.xml.gz");
		 new CountsWriter(cnt).write(c.counts().getCountsFileName());
		
		
		new ConfigWriter(c).write("/Users/laemmel/devel/nyc/gct_vicinity/config.xml.gz");
		try {
			FileOutputStream str;
			str = new FileOutputStream("/Users/laemmel/devel/nyc/gct_vicinity/ca_link_infos");
			b.build().writeTo(str);
			str.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static final class Timing {
		
		double offset;
		double cycle;
		String time;
		double wk;
		double fldw;
		
		public Timing(String time, double offset, double wk, double fldw, double cycle) {
			this.offset = offset;
			this.wk = wk;
			this.fldw = fldw;
			this.cycle = cycle;
			this.time = time;
		}
;
	}

}
