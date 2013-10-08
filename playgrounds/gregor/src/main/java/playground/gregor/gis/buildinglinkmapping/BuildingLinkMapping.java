/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.gis.buildinglinkmapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.contrib.evacuation.base.Building;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.MultiPolygon;

public class BuildingLinkMapping implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	//private final Map<Id,LinkInfo> lis = new HashMap<Id,LinkInfo>();

	private final Map<Id,AgentInfo> ais = new HashMap<Id,AgentInfo>();

	//private Map<Id,LinkInfo> getLis() {
	//	return this.lis;
	//}

	private Map<Id,AgentInfo> getAis() {
		return this.ais;
	}

	public static void main(String [] args) {
		String conf = "/Users/laemmel/arbeit/papers/2012/lastMile/matsim/config.xml";
		String runBase = "/Users/laemmel/svn/runs-svn/run1394/";
		Config c = ConfigUtils.loadConfig(conf);
		
//		c.plans().setInputFile(runBase + "/output/output_plans.xml.gz"); 
		
		Scenario sc = ScenarioUtils.loadScenario(c);
		Loader loader = new Loader(sc);
		loader.loadData();
		List<Building> buildings = loader.getBuildings();
		System.out.println(buildings.size());
		PolygonFeatureFactory factory = initFeatures();

		Coordinate c0 = new Coordinate(649694.00-100,9894872.00+100);
		Coordinate c1 = new Coordinate(653053.00+100,9892897.00-100);
		Envelope ev = new Envelope(c0, c1);

		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
		Map<Id,BuildingInfo> bldM = new HashMap<Id,BuildingInfo>();
		for (Building b : buildings) {
			BuildingInfo bi = new BuildingInfo();
			bi.b = b;
			bldM.put(b.getId(),bi);
		}
		Map<Id, Id> pbm = new PersonBuildingMappingParser().getPersonBuildingMappingFromFile(runBase + "/output/person_buildings_mapping");


		for (int i =1000; i <= 1000; i ++) {
			EventsManager e = EventsUtils.createEventsManager();
			BuildingLinkMapping blm = new BuildingLinkMapping();
			e.addHandler(blm);

			new EventsReaderTXTv1(e).readFile(runBase + "output/ITERS/it." + i +"/" + i + ".events.txt.gz");
			Map<Id, AgentInfo> aiz = blm.getAis();
			for (AgentInfo ai : aiz.values()) {
				Id id = ai.id;
				double time = ai.arr - ai.dep;
				Id bId = pbm.get(id);
				BuildingInfo bi = bldM.get(bId);
				bi.times.add(time);
				int intId = Integer.parseInt(ai.linkId.toString());
				if (intId > 100000) {
					intId -= 100000;
				}
				if (bi.linkId == null) {
					bi.linkId = new IdImpl(intId);
				} else {
					int intIdB = Integer.parseInt(bi.linkId.toString());
					if (intIdB != intId) {
						throw new RuntimeException("expected: " + intIdB + " got:" + intId);
					}
				}
				
			}
		}

		
		Map<Id,LinkInfo> linkTimes = new HashMap<Id,LinkInfo>();
		for (BuildingInfo bi : bldM.values() ) {
			
			if (bi.linkId == null) {
				continue;
			}
		
			LinkInfo lt = linkTimes.get(bi.linkId);
			if (lt == null) {
				lt = new LinkInfo();
				linkTimes.put(bi.linkId, lt);
			}
			lt.times.addAll(bi.times);
		}

//		for (Person  pers : sc.getPopulation().getPersons().values()) {
//			Plan pl = pers.getSelectedPlan();
//			Activity act = (Activity) pl.getPlanElements().get(0);
//			Id id = act.getLinkId();
//			int intId = Integer.parseInt(id.toString());
//			if ( intId > 100000) {
//				intId -= 100000;
//				id = new IdImpl(intId);
//			}
//			Leg l = (Leg) pl.getPlanElements().get(1);
//			double time = l.getTravelTime();
//			
//			LinkInfo lt = linkTimes.get(id);
//			if (lt == null) {
//				lt = new LinkInfo();
//				linkTimes.put(id, lt);
//			}
//			
//			lt.times.add(time);
//		}
		
		int lt15 = 0;
		int lt30 = 0;
		int gt30 = 0;
		int total = 0;

		for (BuildingInfo bi : bldM.values()) {
			LinkInfo lt = linkTimes.get(bi.linkId);
			if (lt == null || lt.times.size() == 0) {
				continue;
			}
			if (!lt.sorted) {
				Collections.sort(lt.times);
				lt.sorted = true;
				double sum = 0;
				for (Double time : lt.times) {
					sum += time;

				}
				
				lt.avg = sum/lt.times.size();
				if (lt.times.get(lt.times.size()-1)- lt.times.get(0) > 1000) {
					System.out.println("min:  " + lt.times.get(0) + " max:" + lt.times.get(lt.times.size()-1));
				}
			}
			double time =lt.avg; //lt.times.get(lt.times.size()/2);

			if (!ev.contains(bi.b.getGeo().getCentroid().getCoordinate())){
				continue;
			}
			if (time <= 900) {
				lt15 += bi.b.getPopDay();
			} else if (time <=1800) {
				lt30 += bi.b.getPopDay();
			} else {
				gt30 += bi.b.getPopDay();
			}
			total += bi.b.getPopDay();
			
			SimpleFeature f = factory.createPolygon((MultiPolygon) bi.b.getGeo(), new Object[]{time,bi.b.getId().toString()}, null);
			features.add(f);
		}

		//		
		//			Coord cc = MGC.point2Coord(b.getGeo().getCentroid());
		//			LinkImpl l = ((NetworkImpl)sc.getNetwork()).getNearestLink(cc);
		//			try {
		//				
		//	
		//				LinkInfo li = liz.get(l.getId());
		//				if (li == null) {
		//					continue;
		//				}
		//				
		////				while(l.getId().toString().contains("s")) {
		////					l = (LinkImpl) l.getFromNode().getInLinks().values().iterator().next();
		////				}
		//				String srtId = l.getId().toString();
		//				if (srtId.contains("e")) {
		//					continue;
		//				}
		//				int intId = Integer.parseInt(srtId);
		//				if (intId > 100000) {
		//					intId -= 100000;
		//				}
		//				
		//				int x = b.isQuakeProof() ? intId = -1 : 0 ;
		//				
		//				
		//				double time = li.getTime();
		//				
		//				
		//				Feature f = ft.create(new Object[]{b.getGeo(),time,x});
		//				features.add(f);
		//			} catch (IllegalAttributeException ee) {
		//				// TODO Auto-generated catch block
		//				ee.printStackTrace();
		//			} 

		//		}
		
		System.out.println("lt15:" + lt15 + " lt30:" + lt30 + " gt30:" + gt30 + " total:" + total);

		ShapeFileWriter.writeGeometries(features, "/Users/laemmel/arbeit/papers/2012/lastMile/matsim/buildings_b.shp");
		//		((NetworkImpl)sc.getNetwork()).getNearestLink(coord)
	}


	private static PolygonFeatureFactory initFeatures() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG: 32747");
		return new PolygonFeatureFactory.Builder().
				setCrs(targetCRS).
				addAttribute("dblAvgZ", Double.class).
				addAttribute("name", String.class).
				create();
	}

	//	private static class LinkInfo {
	//		Map<Id,AgentInfo> ais = new HashMap<Id,AgentInfo>();
	//
	//		double time = -1;
	//		
	//		public double getTime() {
	//			if (this.time == -1) {
	//				this.time = 0;
	//				for (AgentInfo ai : this.ais.values()) {
	//					this.time += ai.arr - ai.dep;
	//				}
	//				this.time /= this.ais.size();
	//			}
	//			return this.time;
	//		}
	//	}
	private static class AgentInfo {
		double dep;
		double arr;
		Id linkId;
		Id id;
	}
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}


	@Override
	public void handleEvent(PersonArrivalEvent event) {
		AgentInfo ai = this.ais.get(event.getPersonId());
		ai.arr = event.getTime();

	}


	@Override
	public void handleEvent(PersonDepartureEvent event) {
		AgentInfo ai = new AgentInfo();
		ai.dep = event.getTime();
		ai.linkId = event.getLinkId();
		ai.id = event.getPersonId();
		ai.arr = 24 * 3600; 
		if (event.getLinkId().toString().contains("s")) {
			System.err.println(event.getLinkId());
		}
		this.ais.put(event.getPersonId(), ai);
		//		LinkInfo li = this.lis.get(event.getLinkId());
		//		if (li == null) {
		//			li = new LinkInfo();
		//			this.lis.put(event.getLinkId(), li);
		//		}
		//		li.ais.put(event.getPersonId(), ai);
	}

	private static final class BuildingInfo {
		Building b;
		List<Double> times = new ArrayList<Double>();
		Id linkId = null;
	}
	
	private static final class LinkInfo {
		public double avg;
		List<Double> times = new ArrayList<Double>();
		boolean sorted = false;
	}

}
