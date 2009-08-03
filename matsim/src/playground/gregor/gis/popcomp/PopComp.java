package playground.gregor.gis.popcomp;

import java.io.IOException;
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
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.Scenario;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.api.experimental.population.Person;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.base.BuildingsShapeReader;
import org.matsim.evacuation.base.EvacuationPopulationFromShapeFileLoader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import playground.gregor.analysis.GTH;

public class PopComp {

	
	
	final static Envelope ENVELOPE = new Envelope(648815,655804,9888424,9902468);
	final static double LENGTH = 500;
	Map<Id,Count> linkCount = new HashMap<Id,Count>();
	private QuadTree<Count> quad;
	private ArrayList<Feature> features;
	private FeatureType ftRunCompare;
	
	public void run(Population pop1, Population pop2) {
	
		initFeatures();
		createPolygons();
		for (Person pers : pop1.getPersons().values()) {
//			Coord coord = pers.getPlans().get(0).getFirstActivity().getCoord();
			ActivityImpl act = pers.getPlans().get(0).getFirstActivity();
//			LegImpl leg = pers.getPlans().get(0).getNextLeg(act);
//			ActivityImpl act2 = pers.getPlans().get(0).getNextActivity(leg);
			Coord coord = act.getCoord();
			
			Count c = this.quad.get(coord.getX(), coord.getY());
			c.census++;
		}

		for (Person pers : pop1.getPersons().values()) {
//			Coord coord = pers.getPlans().get(0).getFirstActivity().getCoord();
			ActivityImpl act = pers.getPlans().get(0).getFirstActivity();
			LegImpl leg = pers.getPlans().get(0).getNextLeg(act);
			ActivityImpl act2 = pers.getPlans().get(0).getNextActivity(leg);
			Coord coord = act2.getCoord();
			
			Count c = this.quad.get(coord.getX(), coord.getY());
			c.shapeFile++;
		}
		
//		for (Person pers : pop2.getPersons().values()) {
//			Coord coord = pers.getPlans().get(0).getFirstActivity().getCoord();
//			Count c = this.quad.get(coord.getX(), coord.getY());
//			c.shapeFile++;
//		}
		
		ArrayList<Feature> fts = new ArrayList<Feature>();
		for (Count c : this.quad.values()) {
			try {
				fts.add(this.ftRunCompare.create(new Object[]{c.p,(double)c.census,(double)c.shapeFile,(double)c.shapeFile-c.census}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
		}
		try {
			ShapeFileWriter.writeGeometries(fts, "compDynExpDayNight.shp");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	private void createPolygons() {
		this.quad = new QuadTree<Count>(ENVELOPE.getMinX(),ENVELOPE.getMinY(),ENVELOPE.getMaxX(),ENVELOPE.getMaxY());
		GTH gth = new GTH(new GeometryFactory());
		for (double x = ENVELOPE.getMinX(); x < ENVELOPE.getMaxX(); x += LENGTH) {
			for (double y = ENVELOPE.getMinY(); y < ENVELOPE.getMaxY(); y += LENGTH) {
				Polygon p = gth.getSquare(new Coordinate(x,y), LENGTH);
				Count c = new Count();
				c.p = p;
				this.quad.put(x,y,c);
			}
		}
		
	}

	
	private void initFeatures() {
		this.features = new ArrayList<Feature>();
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Polygon",Polygon.class, true, null, null, crs);
		AttributeType tt1 = AttributeTypeFactory.newAttributeType("night", Double.class);
		AttributeType tt2 = AttributeTypeFactory.newAttributeType("day", Double.class);
		AttributeType tt1DiffTt2 = AttributeTypeFactory.newAttributeType("diffDynExpDayNight", Double.class);
		try {
			this.ftRunCompare = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, tt1,tt2,tt1DiffTt2}, "gridShape");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}

		
	}

	public static void main(String [] args) {
		String config = "../../inputs/configs/shapeFileEvac.xml";
		Config c = Gbl.createConfig(new String [] {config});
		c.plans().setInputFile("../../inputs/networks/padang_plans_transport_v20090604.xml.gz");
		Scenario sc = new ScenarioImpl(c);
		NetworkLayer net = sc.getNetwork();
		new MatsimNetworkReader(net).readFile(c.network().getInputFile());
		
		Population pop = sc.getPopulation();
		new PopulationReaderMatsimV4(sc).readFile(c.plans().getInputFile());
		
		List<Building> buildings = BuildingsShapeReader.readDataFile(c.evacuation().getBuildingsFile());
		
		
		EvacuationPopulationFromShapeFileLoader popGen = new EvacuationPopulationFromShapeFileLoader(buildings, sc);
		Population pop2 = popGen.getPopulation();
		
		System.out.println("pop1:" + pop.getPersons().size() + " pop2:" + pop2.getPersons().size());
	
		new PopComp().run(pop, pop2);
	
		
		
	}
	
	private static class Count {
		int census = 0;
		int shapeFile = 0;
		Polygon p;
	}
}
