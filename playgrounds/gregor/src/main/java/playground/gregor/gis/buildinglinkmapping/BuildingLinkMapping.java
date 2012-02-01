package playground.gregor.gis.buildinglinkmapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.evacuation.base.Building;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.MultiPolygon;

public class BuildingLinkMapping implements AgentDepartureEventHandler, AgentArrivalEventHandler {
	
	private static FeatureType ft;

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
		Config c = ConfigUtils.loadConfig(conf);
		Scenario sc = ScenarioUtils.loadScenario(c);
		Loader loader = new Loader(sc);
		loader.loadData();
		List<Building> buildings = loader.getBuildings();
		System.out.println(buildings.size());
		initFeatures();
		
		Coordinate c0 = new Coordinate(649694.00-100,9894872.00+100);
		Coordinate c1 = new Coordinate(653053.00+100,9892897.00-100);
		Envelope ev = new Envelope(c0, c1);
		
		EventsManager e = EventsUtils.createEventsManager();
		BuildingLinkMapping blm = new BuildingLinkMapping();
		e.addHandler(blm);
		new EventsReaderTXTv1(e).readFile("/Users/laemmel/svn/runs-svn/run1388/output/ITERS/it.1000/1000.events.txt.gz");
		
		Map<Id, AgentInfo> aiz = blm.getAis();
		
		List<Feature> features = new ArrayList<Feature>();
		Map<Id,BuildingInfo> bldM = new HashMap<Id,BuildingInfo>();
		for (Building b : buildings) {
			BuildingInfo bi = new BuildingInfo();
			bi.b = b;
			bldM.put(b.getId(),bi);
		}
		Map<Id, Id> pbm = new PersonBuildingMappingParser().getPersonBuildingMappingFromFile("/Users/laemmel/arbeit/papers/2012/lastMile/tmp/person_buildings_mapping");
		
		for (AgentInfo ai : aiz.values()) {
			Id id = ai.id;
			double time = ai.arr - ai.dep;
			Id bId = pbm.get(id);
			BuildingInfo bi = bldM.get(bId);
			bi.timeSum += time;
			bi.numP++;
		}
		
		for (BuildingInfo bi : bldM.values()) {
			double time = bi.timeSum/bi.numP;
			
			if (!ev.contains(bi.b.getGeo().getCentroid().getCoordinate())){
				continue;
			}
			try {
				Feature f = ft.create(new Object[]{bi.b.getGeo(),time,bi.b.getId().toString()});
				features.add(f);
			} catch (IllegalAttributeException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
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
		
		ShapeFileWriter.writeGeometries(features, "/Users/laemmel/arbeit/papers/2012/lastMile/matsim/buildings.shp");
//		((NetworkImpl)sc.getNetwork()).getNearestLink(coord)
	}
	
	
	private static void initFeatures() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG: 32747");
		AttributeType p = DefaultAttributeTypeFactory.newAttributeType(
				"MultiPolygon", MultiPolygon.class, true, null, null, targetCRS);
		AttributeType z = AttributeTypeFactory.newAttributeType(
				"dblAvgZ", Double.class);
		AttributeType t = AttributeTypeFactory.newAttributeType(
				"name", String.class);

		Exception ex;
		try {
			ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { p, z, t }, "MultiPolygon");
			return;
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);

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
	public void handleEvent(AgentArrivalEvent event) {
		AgentInfo ai = this.ais.get(event.getPersonId());
		ai.arr = event.getTime();
		
	}


	@Override
	public void handleEvent(AgentDepartureEvent event) {
		AgentInfo ai = new AgentInfo();
		ai.dep = event.getTime();
		ai.linkId = event.getLinkId();
		ai.id = event.getPersonId();
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
		double timeSum = 0;
		int numP=1;
	}
}
