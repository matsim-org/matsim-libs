package playground.dhosse.bachelorarbeit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.matsim4opus.utils.network.NetworkBoundaryBox;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class PopulationGenerator {
	
	private SimpleFeatureBuilder builder;

	private Scenario sc;
	private Network net;
	private Population pop;
	private NetworkBoundaryBox bbox = new NetworkBoundaryBox();
	private Geometry hull;
	
	public PopulationGenerator() {
		
	}
	
	public static void main(String args[]){
		PopulationGenerator gen = new PopulationGenerator();
		gen.run();
		gen.activityOutput();
	}
	
	public void run(){
		Config config = ConfigUtils.createConfig();
		sc = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(sc).readFile("C:/Users/Daniel/Dropbox/bsc/input/berlin_osm.xml");
		net = sc.getNetwork();
		
		bbox.setDefaultBoundaryBox(net);
		
		Coordinate[] coordinate = new Coordinate[]{new Coordinate(bbox.getXMin(),bbox.getYMin()),
				new Coordinate(bbox.getXMin(),bbox.getYMax()),new Coordinate(bbox.getXMax(),bbox.getYMax()),
				new Coordinate(bbox.getXMax(),bbox.getYMin()),new Coordinate(bbox.getXMin(),bbox.getYMin())};
		LinearRing shell = new LinearRing(new CoordinateArraySequence(coordinate), new GeometryFactory());
		Polygon poly = new Polygon(shell, null, new GeometryFactory());
		hull = poly.convexHull();
		
		pop = sc.getPopulation();
		
		generatePop();
		
		MatsimWriter popWriter = new PopulationWriter(pop, net);
		popWriter.write("C:/Users/Daniel/Dropbox/bsc/input/test_population.xml");
		
	}
	
	private void generatePop(){
		
		PopulationFactory pf = pop.getFactory();
		
		for(int i=1;i<=30000;i++){
			Person p = pop.getFactory().createPerson(new IdImpl("p_"+i));
			Plan plan = pf.createPlan();
			Coord homeCoordinates = drawRandomPoint();
			Coord workCoordinates = drawRandomPoint();
			plan.addActivity(createH(homeCoordinates));
			plan.addLeg(pop.getFactory().createLeg(TransportMode.car));
			plan.addActivity(createW(workCoordinates));
			plan.addLeg(pop.getFactory().createLeg(TransportMode.car));
			plan.addActivity(createH(homeCoordinates));
			p.addPlan(plan);
			pop.addPerson(p);
		}
		
	}

	private Activity createH(Coord homeCoordinates) {
		Activity act = pop.getFactory().createActivityFromCoord("home", homeCoordinates);
		act.setEndTime(28800);
		return act;
	}
	
	private Activity createW(Coord workCoordinates) {
		Activity act = pop.getFactory().createActivityFromCoord("work", workCoordinates);
		act.setEndTime(64800);
		return act;
	}

	private Coord drawRandomPoint() {
		Random rnd = new Random();
		return new CoordImpl(bbox.getXMin() + rnd.nextDouble() * (bbox.getXMax() - bbox.getXMin()),
				bbox.getYMin() + rnd.nextDouble() * (bbox.getYMax() - bbox.getYMin()));
	}
	
	private void activityOutput(){

		initFeatureType();
		Collection<SimpleFeature> features = createFeatures(sc.getPopulation());
		ShapeFileWriter.writeGeometries(features, "C:/Users/Daniel/Dropbox/bsc/output/pop.shp");
		
//		XY2Links xy2Links = new XY2Links((NetworkImpl) sc.getNetwork());
//		xy2Links.run(sc.getPopulation());
//		String directory = "C:/Users/Daniel/Dropbox/bsc/output";
//		SelectedPlans2ESRIShape plans2Shape = new SelectedPlans2ESRIShape(sc.getPopulation(), sc.getNetwork(), MGC.getCRS(TransformationFactory.WGS84), directory);
//		plans2Shape.setWriteActs(true);
//		plans2Shape.setWriteLegs(false);
//		plans2Shape.write();
		
	}
	
	private void initFeatureType(){
		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("node");
		typeBuilder.setCRS(crs);
		typeBuilder.add("location",Point.class);
		typeBuilder.add("ID",String.class);
		this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
	}
	
	private Collection<SimpleFeature> createFeatures(final Population population) {
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
		for(Person p : population.getPersons().values()){
			features.add(getFeature(p));
		}
		return features;
	}
	
	private SimpleFeature getFeature(final Person person) {

		int i = 0;
		
		for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
			
			if(pe instanceof Activity){
				i++;
				Point p = MGC.coord2Point(((Activity)pe).getCoord());
				
				try{
					return this.builder.buildFeature(null, new Object[]{p,new String(person.getId().toString()+i)});
				}
				catch(IllegalArgumentException e){
					throw new RuntimeException(e);
				}
				
			}
			
		}
		
		return null;
	}

}
