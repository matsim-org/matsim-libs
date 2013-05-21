package playground.dziemke.potsdam.analysis.aggregated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

public class PotsdamAggregatedAnalysis implements Runnable {

//	private FeatureType featureType;
	private PolylineFeatureFactory polylineFeatureFactory;
	// private GeometryFactory geometryFactory = new GeometryFactory();
	
	String configBase = "D:/Workspace/container/potsdam-pg/config/Config_run_07.xml";
	String networkBase = "D:/Workspace/container/potsdam-pg/data/potsdamNetwork.xml";
	String eventsBase = "D:/Workspace/container/potsdam-pg/output/run_07/ITERS/it.500/500.events.xml.gz";
	
	String configAnalysis =  "D:/Workspace/container/potsdam-pg/config/Config_run_x7b.xml";
	String eventsAnalysis = "D:/Workspace/container/potsdam-pg/output/run_x7b/ITERS/it.150/150.events.xml.gz";
	
	Map <Id, Integer> differenceMap = new HashMap <Id, Integer>();
	
	
	public static void main(String[] args) {
		PotsdamAggregatedAnalysis potsdamAggregatedAnalysis = new PotsdamAggregatedAnalysis();
		potsdamAggregatedAnalysis.run();
	}

	
	@Override
	public void run() {
		analysis();
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkBase);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		writeShapeFileLines(scenario, differenceMap);
	}
	

	private void analysis() {
		Map<Id,Integer> baseCaseMap = PotsdamEventFileReaderLoad.EventFileReader(configBase, eventsBase);
		System.out.println("Die Map fuer den Nullfall wurde geschrieben.");
	
		Map<Id,Integer> analysisCaseMap = PotsdamEventFileReaderLoad.EventFileReader(configAnalysis, eventsAnalysis);
		System.out.println("Die Map fuer den Planfall wurde geschrieben.");
		
		for (Id id : baseCaseMap.keySet()){
			int loadBase = baseCaseMap.get(id);
			int loadAnalysis = analysisCaseMap.get(id);
			int value = loadAnalysis - loadBase;
			differenceMap.put(id, value);
		}
	}
	
	
	private void writeShapeFileLines(Scenario scenario, Map<Id,Integer> DifferenzMap) {
		initFeatures();
		// Collection<Feature> features = createFeatures(scenario, DifferenzMap);
		Collection <SimpleFeature> features = createFeatures(scenario);
		ShapeFileWriter.writeGeometries(features, "D:/Workspace/container/potsdam-pg/analysis/difference.shp");
		System.out.println("Das Differenznetz wurde in ein ShapeFile geschrieben.");
	}

	
	// private ArrayList<Feature> createFeatures(Scenario scenario, Map<Id,Integer> DifferenzMap) {
	private List <SimpleFeature> createFeatures(Scenario scenario) {
		List <SimpleFeature> features = new ArrayList <SimpleFeature>() ; 
		// ArrayList<Feature> features = new ArrayList <Feature>() ; 
		
		for (Link link : scenario.getNetwork().getLinks().values()){
			// features.add(getFeature1(link,differenceMap.get(link.getId())));
			features.add(getFeature(link));
			}

		return features;
	}


	private void initFeatures() {
//		AttributeType [] attribs = new AttributeType[3];
//		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, MGC.getCRS(TransformationFactory.WGS84_UTM35S));
//		attribs[1] = AttributeTypeFactory.newAttributeType("ID", String.class);
//		attribs[2] = AttributeTypeFactory.newAttributeType("Difference", Integer.class);
		this.polylineFeatureFactory = new PolylineFeatureFactory.Builder().
				setCrs(MGC.getCRS(TransformationFactory.WGS84_UTM35S)).
				setName("links").
				addAttribute("ID", String.class).
				addAttribute("Length", Double.class).
				addAttribute("Difference", Integer.class).
				create();
				
//		try {
//			this.featureType = FeatureTypeBuilder.newFeatureType(attribs, "link");
//		} catch (FactoryRegistryException e) {
//			e.printStackTrace();
//		} catch (SchemaException e) {
//			e.printStackTrace();
//		}
	}

	
	private SimpleFeature getFeature(Link link) {
	// private Feature getFeature1(Link link, Integer difference) {
//		LineString ls = this.geometryFactory.createLineString(new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())});
//		Object [] attribs = new Object[3];
//		attribs[0] = ls;
//		attribs[1] = link.getId().toString();
//		attribs[2] = difference;
		Coordinate[] coordinates = new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()),
				MGC.coord2Coordinate(link.getToNode().getCoord())};
		Object[] attributes = new Object [] {link.getId().toString(), link.getLength(), this.differenceMap.get(link.getId())};
		
		return this.polylineFeatureFactory.createPolyline(coordinates, attributes, null);
		}

//	try {
//		return this.featureType.create(attribs);
//	} catch (IllegalAttributeException e) {
//			throw new RuntimeException(e);
//	}
//	}

}