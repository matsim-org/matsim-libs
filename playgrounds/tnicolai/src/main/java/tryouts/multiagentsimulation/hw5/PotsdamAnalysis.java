package tryouts.multiagentsimulation.hw5;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.ConfigUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class PotsdamAnalysis implements Runnable {
	
	private Map<Id, AnalysisLink> linkDeltas = new HashMap<Id, AnalysisLink>();
	
	private FeatureType featureType;
	
	private GeometryFactory geometryFactory = new GeometryFactory();
	
	public static void main(String[] args) {
		PotsdamAnalysis potsdamAnalysis = new PotsdamAnalysis();
		potsdamAnalysis.run();
	}

	@Override
	public void run() {
		String eventsFileBefore = "./tnicolai/configs/brandenburg/events_before.xml.gz";
		String eventsFileAfter = "./tnicolai/configs/brandenburg/events_after.xml.gz";
		EventsManager before = new EventsManagerImpl();
		EventsManager after = new EventsManagerImpl();
		
		
		String network = "./tnicolai/configs/brandenburg/network_before.xml";
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(network);
		for (Entry<Id, Link> entry : scenario.getNetwork().getLinks().entrySet()) {
			linkDeltas.put(entry.getKey(), new AnalysisLink());
		}
		
		before.addHandler(new Before());
		after.addHandler(new After());
		
		new MatsimEventsReader(before).readFile(eventsFileBefore);
		new MatsimEventsReader(after).readFile(eventsFileAfter);

		initFeatureType();
		
		ArrayList<Feature> features = new ArrayList<Feature>();
		for (Entry<Id, Link> entry : scenario.getNetwork().getLinks().entrySet()) {
			features.add(getFeature(entry.getValue()));
		}
		
		try {
			ShapeFileWriter.writeGeometries(features, "./tnicolai/configs/brandenburg/qgis/delta-network");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public class AnalysisLink {
		int delta;
	}
	
	public class Before implements LinkLeaveEventHandler {

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			linkDeltas.get(event.getLinkId()).delta--;
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public class After implements LinkLeaveEventHandler {

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			linkDeltas.get(event.getLinkId()).delta++;
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private void initFeatureType() {
		AttributeType [] attribs = new AttributeType[3];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, MGC.getCRS(TransformationFactory.WGS84_UTM35S));
		attribs[1] = AttributeTypeFactory.newAttributeType("ID", String.class);
		attribs[2] = AttributeTypeFactory.newAttributeType("flowDelta", Double.class);
		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attribs, "link");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}
	
	public Feature getFeature(final Link link) {
		LineString ls = this.geometryFactory.createLineString(new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())});

		Object [] attribs = new Object[3];
		attribs[0] = ls;
		attribs[1] = link.getId().toString();
		attribs[2] = linkDeltas.get(link.getId()).delta;

		try {
			return this.featureType.create(attribs);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}

	}
	
}
