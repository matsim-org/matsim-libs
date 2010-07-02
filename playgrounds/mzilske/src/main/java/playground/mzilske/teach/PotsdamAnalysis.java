package playground.mzilske.teach;

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
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;

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
		String populationFileName = "output/brandenburg/output_plans.xml";
		String eventsFile1 = "output/brandenburg/ITERS/it.20/20.events.txt.gz";
		String eventsFile2 = "output/brandenburg-broken-bridge/ITERS/it.50/50.events.txt.gz";
		EventsManager before = new EventsManagerImpl();
		EventsManager after = new EventsManagerImpl();
		
		
		String network = "inputs/brandenburg/network.xml";
		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(network);
		new MatsimPopulationReader(scenario).readFile(populationFileName);
		
		Population population = scenario.getPopulation();
		
		for (Entry<Id, Link> entry : scenario.getNetwork().getLinks().entrySet()) {
			linkDeltas.put(entry.getKey(), new AnalysisLink());
		}
		
		before.addHandler(new Before());
		after.addHandler(new After());
		
		new MatsimEventsReader(before).readFile(eventsFile1);
		new MatsimEventsReader(after).readFile(eventsFile2);

		initFeatureType();
		
		ArrayList<Feature> features = new ArrayList<Feature>();
		for (Entry<Id, Link> entry : scenario.getNetwork().getLinks().entrySet()) {
			features.add(getFeature(entry.getValue()));
		}
		
		try {
			ShapeFileWriter.writeGeometries(features, "output/brandenburg/delta-network");
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
