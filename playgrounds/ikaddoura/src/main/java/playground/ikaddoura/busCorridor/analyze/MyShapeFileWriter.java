package playground.ikaddoura.busCorridor.analyze;

import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;


public class MyShapeFileWriter implements Runnable {
	
	static String networkFile = "../../shared-svn/studies/ihab/busCorridor/input_final/network.xml";
	static String populationFile = "../../shared-svn/studies/ihab/busCorridor/input_final/population.xml";
	
	private GeometryFactory geometryFactory = new GeometryFactory();
	ArrayList<Feature> FeatureList = new ArrayList<Feature>();
	private FeatureType featureType;
	
	public static void main(String[] args) {
	MyShapeFileWriter potsdamAnalyse = new MyShapeFileWriter();
	potsdamAnalyse.run();
	}

	@Override
	public void run() {
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		SortedMap<Id,Coord> homeKoordinaten = getHomeKoordinaten(scenario);
		SortedMap<Id,Coord> workKoordinaten = getWorkKoordinaten(scenario);
	
	writeShapeFilePoints(scenario, homeKoordinaten, "../../shared-svn/studies/ihab/busCorridor/output_analyse/pointShapeFile_home.shp");
	writeShapeFilePoints(scenario, workKoordinaten, "../../shared-svn/studies/ihab/busCorridor/output_analyse/pointShapeFile_work.shp");
	writeShapeFileLines(scenario);

	}

	private SortedMap<Id, Coord> getWorkKoordinaten(Scenario scenario) {
		SortedMap<Id,Coord> id2koordinaten = new TreeMap<Id,Coord>();
		for(Person person : scenario.getPopulation().getPersons().values()){
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				if (pE instanceof Activity){
					Activity act = (Activity) pE;
					if (act.getType().equals("work")){
						Coord coord = act.getCoord();
						id2koordinaten.put(person.getId(), coord);
					}
					else {}
				}
			}
		}
		return id2koordinaten;
	}

	private SortedMap<Id, Coord> getHomeKoordinaten(Scenario scenario) {
		SortedMap<Id,Coord> id2koordinaten = new TreeMap<Id,Coord>();
		for(Person person : scenario.getPopulation().getPersons().values()){
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				if (pE instanceof Activity){
					Activity act = (Activity) pE;
					if (act.getType().equals("home")){
						Coord coord = act.getCoord();
						id2koordinaten.put(person.getId(), coord);
					}
					else {}
				}
			}
		}
		return id2koordinaten;
	}
	
	private void writeShapeFileLines(Scenario scenario) {
		initFeatureType1();
		Collection<Feature> features = createFeatures1(scenario);
		ShapeFileWriter.writeGeometries(features, "../../shared-svn/studies/ihab/busCorridor/output_analyse/lineShapeFile.shp");
		System.out.println("ShapeFile geschrieben (Netz)");			
	}
	
	private void writeShapeFilePoints(Scenario scenario, SortedMap<Id,Coord> koordinaten, String outputFile) {
		if (koordinaten.isEmpty()==true){
			System.out.println("Map ist leer!");
		}
		else {
			initFeatureType2();
			Collection<Feature> features = createFeatures2(scenario, koordinaten);
			ShapeFileWriter.writeGeometries(features,  outputFile);
			System.out.println("ShapeFile geschrieben (Points)");	
		}
	}
	
	private void initFeatureType1() {
		AttributeType [] attribs = new AttributeType[2];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, MGC.getCRS(TransformationFactory.WGS84_UTM35S));
		attribs[1] = AttributeTypeFactory.newAttributeType("ID", String.class);

		try {
		this.featureType = FeatureTypeBuilder.newFeatureType(attribs, "link");
		} catch (FactoryRegistryException e) {
		e.printStackTrace();
		} catch (SchemaException e) {
		e.printStackTrace();
		}		
	}
	
	private void initFeatureType2() {
		AttributeType [] attribs = new AttributeType[2];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, MGC.getCRS(TransformationFactory.WGS84_UTM35S));
		attribs[1] = AttributeTypeFactory.newAttributeType("PersonID", String.class);
		
		try {
		this.featureType = FeatureTypeBuilder.newFeatureType(attribs, "point");
		} catch (FactoryRegistryException e) {
		e.printStackTrace();
		} catch (SchemaException e) {
		e.printStackTrace();
		}		
	}
	
	private Collection<Feature> createFeatures1(Scenario scenario) {
		ArrayList<Feature> liste = new ArrayList<Feature>();
		for (Link link : scenario.getNetwork().getLinks().values()){
			liste.add(getFeature1(link));
		}
		return liste;
	}
	
	private Collection<Feature> createFeatures2(Scenario scenario, SortedMap<Id,Coord> Koordinaten) {
		ArrayList<Feature> liste = new ArrayList<Feature>();
		for (Entry<Id,Coord> entry : Koordinaten.entrySet()){
			liste.add(getFeature2((Coord)entry.getValue(), (Id)entry.getKey()));
		}
		return liste;
	}

	private Feature getFeature1(Link link) {
		LineString ls = this.geometryFactory.createLineString(new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())});
		Object [] attribs = new Object[2];
		attribs[0] = ls;
		attribs[1] = link.getId().toString();
		
		try {
		return this.featureType.create(attribs);
		} catch (IllegalAttributeException e) {
		throw new RuntimeException(e);
		}
	}
	
	private Feature getFeature2(Coord coord, Id id) {
		Coordinate homeCoordinate = new Coordinate(coord.getX(), coord.getY());
		Point p = this.geometryFactory.createPoint(homeCoordinate);
		Object [] attribs = new Object[2];
		attribs[0] = p;
		attribs[1] = id;
		
		try {
		return this.featureType.create(attribs);
		} catch (IllegalAttributeException e) {
		throw new RuntimeException(e);
		}
	}
}
