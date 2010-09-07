/* *********************************************************************** *
 * project: org.matsim.*
 * CongestionAnalyzer.java
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
package playground.gregor.evacuation.congestion;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.Time;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.MultiPolygon;

public class CongestionAnalyzer {
	private static final String network = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/gis/network_v20080618/links_v20090728.shp";
	private static final double DURATION = 30;
	private Map<Id, Feature> streetMap;
	private final String eventsFile1;
	private final ScenarioImpl scenario;
	private final String outfile;
	private final CoordinateReferenceSystem crs;
	private Map<Id, LinkInfo> lis;
	private ArrayList<Feature> features;
	private FeatureType ftRunCompare;

	
	public CongestionAnalyzer(String eventsFile, ScenarioImpl scenario, String outfile, CoordinateReferenceSystem crs) {
		this.eventsFile1 = eventsFile;
		this.scenario = scenario;
		this.outfile = outfile;
		this.crs = crs;
	}
	
	public void run(){
		try {
			buildStreetMap();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		initFeatures();
		initLis();
	
		LOSCalculator los = new LOSCalculator(this.lis);
		EventsManagerImpl events1 = new EventsManagerImpl();
		events1.addHandler(los);
		new EventsReaderTXTv1(events1).readFile(this.eventsFile1);
		
		calcStats();
		
	}
	
	private void calcStats2() {
		for (Entry<Id, LinkInfo> e : this.lis.entrySet()) {
			if (e.getValue().speed75> 1 || e.getValue().speed50> 1 ||e.getValue().speed25> 1 ||e.getValue().speedlt25> 1 ) {
				System.out.println(e.getValue().speed75 + " " + e.getValue().speed50 + " " + e.getValue().speed25 + " " + e.getValue().speedlt25);
			}
			
			
		}
		
		
	}
	
	
	private void calcStats() {
		double maxRho = 0;
		for (Entry<Id, Feature> e : this.streetMap.entrySet()) {
			LinkInfo li1 = this.lis.get(e.getKey());
			if (li1 == null) {
				continue;
			}
			int intId = Integer.parseInt(e.getKey().toString()) + 100000;
			LinkInfo li2 = this.lis.get(new IdImpl(intId));
			if (li2 == null) {
				continue;
			}
			double tCount = Math.max(li1.maxCount,li2.maxCount);
			double aPp = 0;
			if (li1.aPp < Double.POSITIVE_INFINITY) {
				aPp = li1.aPp;
			} 
			if (li2.aPp < Double.POSITIVE_INFINITY) {
				aPp = Math.max(aPp, li2.aPp);
			}
			double rho = tCount / li1.simArea;
			if (rho > 0.303030304) {
				System.out.println("rho: " + rho + " app:" + aPp);
			}
			if (rho > maxRho) {
				maxRho = rho;
			}
			
			double losB = Math.max(li1.losB, li2.losB)/ (DURATION *60.) ;
			double losC = Math.max(li1.losC, li2.losC)  / (DURATION*60.) ;
			double losD = Math.max(li1.losD, li2.losD) / (DURATION*60.) ;
			double losE = Math.max(li1.losE, li2.losE) / (DURATION*60.) ;
			double losF = Math.max(li1.losF, li2.losF) / (DURATION*60.) ;
			double losA = 1 - losB - losC - losD - losE - losF;
			losB += losC + losD + losE + losF;
			losC += losD + losE + losF;
			losD += losE + losF;
			losE += losF;
			
			double speed75 = Math.max(li1.speed75 , li2.speed75) / (DURATION *60.) ;
			double speed50 = Math.max(li1.speed50 , li2.speed50) / (DURATION *60.) ;
			double speed25 = Math.max(li1.speed25 , li2.speed25) / (DURATION *60.) ;
			double speedlt25 = Math.max(li1.speedlt25 , li2.speedlt25) / (DURATION *60.) ;
			double speedgt100 = 1 - speed75 - speed50 - speed25 - speedlt25;
			speed75 += speed50 + speed25 + speedlt25;
			speed50 += speed25 + speedlt25;
			speed25 += speedlt25;
			
			
			
			try {
				this.features.add(this.ftRunCompare.create(new Object[]{e.getValue().getDefaultGeometry(),losA,losB,losC,losD,losE,losF,speedgt100, speed75, speed50, speed25, speedlt25, Math.max(li1.numEvac, li2.numEvac)}));
			} catch (IllegalAttributeException e1) {
				e1.printStackTrace();
			}
		}
		System.out.println("maxRho: " + maxRho);
		try {
			ShapeFileWriter.writeGeometries(this.features, this.outfile);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	
	private void initFeatures() {
		this.features = new ArrayList<Feature>();

		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, this.crs);
		AttributeType losA = AttributeTypeFactory.newAttributeType("losA", Double.class);
		AttributeType losB = AttributeTypeFactory.newAttributeType("losB", Double.class);
		AttributeType losC = AttributeTypeFactory.newAttributeType("losC", Double.class);
		AttributeType losD = AttributeTypeFactory.newAttributeType("losD", Double.class);
		AttributeType losE = AttributeTypeFactory.newAttributeType("losE", Double.class);
		AttributeType losF = AttributeTypeFactory.newAttributeType("losF", Double.class);
		AttributeType speedgt100 = AttributeTypeFactory.newAttributeType("speedgt100", Double.class);
		AttributeType speed75 = AttributeTypeFactory.newAttributeType("speed75", Double.class);
		AttributeType speed50 = AttributeTypeFactory.newAttributeType("speed50", Double.class);
		AttributeType speed25 = AttributeTypeFactory.newAttributeType("speed25", Double.class);
		AttributeType speedlt25 = AttributeTypeFactory.newAttributeType("speedlt25", Double.class);
		AttributeType numEvac = AttributeTypeFactory.newAttributeType("numEvac", Integer.class);
		try {
			this.ftRunCompare = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, losA, losB, losC, losD, losE, losF,speedgt100, speed75, speed50, speed25, speedlt25, numEvac}, "links");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}

		
	}
	
	private void initLis() {
		double laneWidth = this.scenario.getNetwork().getEffectiveLaneWidth();
		this.lis = new HashMap<Id,LinkInfo>();
		for (Link l : this.scenario.getNetwork().getLinks().values()) {

//		    double storageCapacity = (l.getLength() * l.getNumberOfLanes(Time.UNDEFINED_TIME)) / (this.scenario.getNetwork().getEffectiveCellSize());
			double area = l.getLength() * l.getNumberOfLanes(Time.UNDEFINED_TIME) * laneWidth;
//			double rho = storageCapacity / area;
//			if (rho > 1) {
//				System.out.println("rho:" + rho);
//			}
//			double area2 = l.getLength() * l.getCapacity(Time.UNDEFINED_TIME)/1.33;
//			if (area2 < area) {
//				System.err.println("area-area2: " + area2 + "\t" + area);
//			}
			LinkInfo li = new LinkInfo(area, l.getLength());
			this.lis.put(l.getId(), li);
		}
	}

	private void buildStreetMap() throws IOException {
		this.streetMap = new HashMap<Id,Feature>();
		FeatureSource fts = ShapeFileReader.readDataFile(network);
		Iterator it = fts.getFeatures().iterator();
		while ( it.hasNext()) {
			Feature ft = (Feature) it.next();
			Long intId = (Long) ft.getAttribute("ID");
			Id id = new IdImpl(intId);
			if (this.streetMap.get(id) != null) {
				throw new RuntimeException("Id already exists!");
			}
			this.streetMap.put(id, ft);
		}
	}
	
	private static class LOSCalculator implements LinkLeaveEventHandler, LinkEnterEventHandler, AgentDepartureEventHandler {
		private double startTime = -1;
		private final Map<Id, LinkInfo> lis;
		private double endTime = Double.POSITIVE_INFINITY;
		
		
		public LOSCalculator(Map<Id,LinkInfo> lis) {
			this.lis = lis;
		}
		
		
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			
			LinkInfo li = this.lis.get(event.getLinkId());
			if (li.time == -1) {
				li.time = event.getTime();
			}
			Double d = li.queue.pollFirst();
			double tt = event.getTime()-d;
			double speed = li.length / tt;
			if (speed < li.currentMinSpeed) {
				li.currentMinSpeed = speed;
			}
			if (event.getTime() > this.endTime) {
			
				updateLos(li, event.getTime());
				li.oldCount = 0;
				li.currentCount = 0;
				return;
			}
			
			if (event.getTime() > li.time) {
				claculateMinSpeed(event.getTime(),li);
				calculateLOS(event.getTime(),event.getLinkId());
			}
			
			li.currentCount --;

			if (li.currentCount < 0) {
				throw new RuntimeException("should never happen!");
			}
			
		}

		private void claculateMinSpeed(double time2, LinkInfo li) {
			int duration = (int) (time2 - li.time);
			double speed = li.currentMinSpeed;
			if (speed > 1.0) {
				return;
			} 
			if (speed > 0.75) {
				li.speed75 += duration;
			} else if (speed > 0.5) {
				li.speed50 += duration;
			} else if (speed > 0.25) {
				li.speed25 += duration;
			} else {
				li.speedlt25 += duration;
			}
			
		}


		private void calculateLOS(double time, Id linkId) {
			LinkInfo li = this.lis.get(linkId);
			
			if (this.startTime == -1) {
				this.endTime = time + DURATION *60;
				li.oldCount = li.currentCount;
				this.startTime = time;
				return;
			}
			updateLos(li, time);
			li.oldCount = li.currentCount;			
			li.time = time;
			li.currentMinSpeed = Double.POSITIVE_INFINITY;
			
		}

		//update fruin level of service
		private void updateLos(LinkInfo li, double time) {
			double avgOnLink = (li.oldCount + li.currentCount) / 2;
//			double avgOnLink = Math.max(li.oldCount, li.currentCount);
			if (avgOnLink > li.maxCount) {
				li.maxCount = avgOnLink;
			}
			if (avgOnLink == 0) {
				return;
			}
			double rho = li.simArea / avgOnLink;
			if (rho < li.aPp) {
				li.aPp = rho;
			}
			
			
			int duration = (int) (time - li.time);
			
			if (rho >= 3.3) {
				return;
			}
			if (rho < 3.3 && rho >= 2.3) {
				li.losB += duration;
			} else if (rho >= 1.4) {
				li.losC += duration;
			} else if (rho >= 0.93) {
				li.losD += duration;
				if (li.losD > (li.time- this.startTime)) {
					int ii = 0;
					ii++;
				}
			} else if (rho >= 0.46) {
				li.losE += duration;
			} else if (rho < 0.46) {
				li.losF += duration;
			}
		}


		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			
			LinkInfo li = this.lis.get(event.getLinkId());
			if (li.time == -1) {
				li.time = event.getTime();
			}
			li.numEvac++;
			li.queue.addLast(event.getTime());
			if (event.getTime() > this.endTime) {
				updateLos(li, event.getTime());
				li.oldCount = 0;
				li.currentCount = 0;
				return;
			}
			if (event.getTime() > li.time) {
				claculateMinSpeed(event.getTime(),li);
				calculateLOS(event.getTime(), event.getLinkId());
			}
			li.currentCount++;
		}


		@Override
		public void handleEvent(AgentDepartureEvent event) {
			LinkInfo li = this.lis.get(event.getLinkId());
			if (li.time == -1) {
				li.time = event.getTime();
			}
			li.numEvac++;
			li.currentCount++;
			li.queue.addLast(event.getTime());
		}
		
	}
	
	private static class LinkInfo {
		public double time = -1;
		
		public int numEvac = 0;
		
		public int speedlt25 = 0;

		public int speed25 = 0;

		public int speed50 = 0;

		public int speed75 = 0;

		Deque<Double> queue = new ArrayDeque<Double>();

		double simArea = 0;
		int oldCount = 0;
		int currentCount = 0;
		int losB = 0;
		int losC = 0;
		int losD = 0;
		int losE = 0;
		int losF = 0;
		double maxCount = 0;
		double aPp = Double.POSITIVE_INFINITY;

		private final double length;
		double currentMinSpeed = Double.POSITIVE_INFINITY;
		
		public LinkInfo(double area, double d) {
			this.simArea = area;
			this.length = d;
		}
	}
	
	
	public static void main(String [] args) {
		String eventsFile1 = "/home/laemmel/arbeit/svn/runs-svn/run1014/output/ITERS/it.500/500.events.txt.gz";
		String network = "/home/laemmel/arbeit/svn/runs-svn/run1014/output/output_network.xml.gz";
		String outfile = "/home/laemmel/arbeit/svn/runs-svn/run1014/analysis/linkLOS.shp";
		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(network);

		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		new CongestionAnalyzer(eventsFile1, scenario, outfile, crs).run();
	}

}
