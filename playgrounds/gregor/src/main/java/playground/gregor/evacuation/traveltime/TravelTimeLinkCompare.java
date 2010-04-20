package playground.gregor.evacuation.traveltime;

import java.io.IOException;
import java.util.ArrayList;
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
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.MultiPolygon;

public class TravelTimeLinkCompare {


	private final String eventsFile1;
	private final String eventsFile2;
	private final ScenarioImpl scenario;
	private final String outfile;
	private final CoordinateReferenceSystem crs;
	private Map<Id, Feature> streetMap;
	private final Map<Id, LinkInfo> linkMap = new HashMap<Id,LinkInfo>();
	private ArrayList<Feature> features;
	private FeatureType ftRunCompare;
	
	private static final String network = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/gis/network_v20080618/links_v20090728.shp";
	private static final double startTime = 16 * 3600;
	
	public TravelTimeLinkCompare(String eventsFile1, String eventsFile2,
			ScenarioImpl scenario, String outfile, CoordinateReferenceSystem crs) {
		this.eventsFile1 = eventsFile1;
		this.eventsFile2 = eventsFile2;
		this.scenario = scenario;
		this.outfile = outfile;
		this.crs = crs;
	}
	
	public void run() {
		try {
			buildStreetMap();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		EventsManagerImpl events1 = new EventsManagerImpl();
		EventsManagerImpl events2 = new EventsManagerImpl();
		
		EventHandler eh1 = new EventHandler();
		events1.addHandler(eh1);
		EventHandler eh2 = new EventHandler();
		events2.addHandler(eh2);
		
		new EventsReaderTXTv1(events1).readFile(this.eventsFile1);
		
		new EventsReaderTXTv1(events2).readFile(this.eventsFile2);
		
		collectLinkStats(eh1,eh2);
		
		createFeatures();
	}

	private void createFeatures() {
		initFeatures();
		for (Entry<Id, LinkInfo> e : this.linkMap.entrySet()) {
			Feature ft = this.streetMap.get(e.getKey());
			if (ft == null) {
				continue;
			}
			double tt1 = e.getValue().tt1/e.getValue().count1;
			double tt2 = e.getValue().tt2/e.getValue().count2;
			if (e.getValue().count2 == 0) {
				tt2 = 0;
			}
			double diff = tt1 - tt2;
			try {
				this.features.add(this.ftRunCompare.create(new Object[]{ft.getDefaultGeometry(),tt1,tt2,diff}));
			} catch (IllegalAttributeException e1) {
				e1.printStackTrace();
			}
		}
		
		try {
			ShapeFileWriter.writeGeometries(this.features, this.outfile);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	private void initFeatures() {
		this.features = new ArrayList<Feature>();

		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, this.crs);
		AttributeType tt1 = AttributeTypeFactory.newAttributeType("TT1", Double.class);
		AttributeType tt2 = AttributeTypeFactory.newAttributeType("TT2", Double.class);
		AttributeType tt1DiffTt2 = AttributeTypeFactory.newAttributeType("tt1DiffTt2", Double.class);
		try {
			this.ftRunCompare = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, tt1,tt2,tt1DiffTt2}, "gridShape");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}

		
	}

	private void collectLinkStats(EventHandler eh1, EventHandler eh2) {
			for (AgentInfo ai : eh1.getAgentInfos().values()) {
				Id id = ai.linkId;
				if (id.toString().contains("l")){
					continue;
				}
				int intId = Integer.parseInt(id.toString());
				if (intId >= 100000) {
					id = new IdImpl(intId - 100000);
				}
				
				LinkInfo li = this.linkMap.get(id);
				if (li == null) {
					li = new LinkInfo();
					this.linkMap.put(id, li);
				}
				if (li.block1) {
					continue;
				}
				double ttime = ai.arrivalTime - startTime;
				if (ttime >= 24 *3600) {
					li.block1 = true;
					li.count1 = 1;
					li.tt1 = 24 * 3600;
				} else {
					li.count1++;
					li.tt1 += ttime;
				}
			}
			
			for (AgentInfo ai : eh2.getAgentInfos().values()) {
				Id id = ai.linkId;
				if (id.toString().contains("l")){
					continue;
				}
				int intId = Integer.parseInt(id.toString());
				if (intId >= 100000) {
					id = new IdImpl(intId - 100000);
				}
				LinkInfo li = this.linkMap.get(id);
				if (li == null) {
					li = new LinkInfo();
					this.linkMap.put(id, li);
				}
				if (li.block2) {
					continue;
				}
				double ttime = ai.arrivalTime - startTime;
				if (ttime >= 24 *3600) {
					li.block2 = true;
					li.count2 = 1;
					li.tt2 = 24 * 3600;
				} else {
					li.count2++;
					li.tt2 += ttime;
				}
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
	
	private static class EventHandler implements AgentDepartureEventHandler, AgentArrivalEventHandler {

		private final Map<Id, AgentInfo> agentMap = new HashMap<Id, AgentInfo>();
		
		
		@Override
		public void handleEvent(AgentDepartureEvent event) {
			AgentInfo ai = new AgentInfo(event.getLinkId());
			this.agentMap.put(event.getPersonId(), ai);
			
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void handleEvent(AgentArrivalEvent event) {
			AgentInfo ai = this.agentMap.get(event.getPersonId());
			ai.arrivalTime  = event.getTime();
		}
		
		public Map<Id,AgentInfo> getAgentInfos() {
			return this.agentMap;
		}
		
	}

	private static class AgentInfo {

		public double arrivalTime = 24 * 3600;
		private final Id linkId;

		public AgentInfo(Id linkId) {
			this.linkId = linkId;
		}
		
	}
	
	private static class LinkInfo {
		int count1 = 0;
		double tt1 = 0;
		boolean block1 = false;
		
		int count2 = 0;
		double tt2 = 0;
		boolean block2 = false;
	}
	public static void main(String [] args) {
		String eventsFile1 = "/home/laemmel/arbeit/svn/runs-svn/run1030/output/ITERS/it.500/500.events.txt.gz";
		String eventsFile2 = "/home/laemmel/arbeit/svn/runs-svn/run1032/output/ITERS/it.500/500.events.txt.gz";
		String network = "/home/laemmel/arbeit/svn/runs-svn/run1032/output/output_network.xml.gz";
		String outfile = "/home/laemmel/arbeit/svn/runs-svn/run1030/analysis/linkETimeComp.shp";
		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(network);

		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		new TravelTimeLinkCompare(eventsFile1, eventsFile2, scenario,  outfile, crs).run();
	}
}
