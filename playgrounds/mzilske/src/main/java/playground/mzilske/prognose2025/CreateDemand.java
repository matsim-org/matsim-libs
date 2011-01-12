package playground.mzilske.prognose2025;

import java.io.IOException;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.mzilske.pipeline.PersonMerger;
import playground.mzilske.pipeline.PopulationReaderTask;
import playground.mzilske.pipeline.PopulationWriterTask;
import playground.mzilske.pipeline.RoutePersonTask;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public abstract class CreateDemand {
	
private static final String GV_NETWORK_FILENAME = "/Users/michaelzilske/workspace/prognose_2025/demand/network_cleaned_wgs84.xml.gz";
	
	private static final String NETWORK_FILENAME = "/Users/michaelzilske/osm/motorway_germany.xml";
	
	private static final String GV_PLANS = "/Users/michaelzilske/workspace/run1061/1061.output_plans.xml.gz";
	
	private static final String FILTER_FILENAME = "/Users/michaelzilske/workspace/prognose_2025/demand/filter.shp";
	
	private static final String LANDKREISE = "/Users/michaelzilske/workspace/prognose_2025/osm_zellen/landkreise.shp";
	
	private static final String OUTPUT_POPULATION_FILENAME = "/Users/michaelzilske/workspace/prognose_2025/demand/naechster_versuch.xml";
	
	private static boolean isCoordInShape(Coord linkCoord, Set<Feature> features, GeometryFactory factory) {
		boolean found = false;
		Geometry geo = factory.createPoint(new Coordinate(linkCoord.getX(), linkCoord.getY()));
		for (Feature ft : features) {
			if (ft.getDefaultGeometry().contains(geo)) {
				found = true;
				break;
			}
		}
		return found;
	}

	public static void main(String[] args) {
//		Scenario gvNetwork = new ScenarioImpl();
//		new MatsimNetworkReader(gvNetwork).readFile(GV_NETWORK_FILENAME);
		Scenario osmNetwork = new ScenarioImpl();
//		new MatsimNetworkReader(osmNetwork).readFile(NETWORK_FILENAME);
//		Set<Feature> featuresInShape;
//		try {
//			featuresInShape = new ShapeFileReader().readFileAndInitialize(FILTER_FILENAME);
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
		
//		PopulationReaderTask gvPopulationReaderTask = new PopulationReaderTask(GV_PLANS, gvNetwork.getNetwork());
		
//		PersonDereferencerTask personDereferencerTask = new PersonDereferencerTask();
		
//		PersonGeoTransformatorTask personGeoTransformatorTask = new PersonGeoTransformatorTask(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
		
//		PersonRouterFilter personRouterFilter = new PersonRouterFilter(osmNetwork.getNetwork());
//		GeometryFactory factory = new GeometryFactory();
//		for (Node node : osmNetwork.getNetwork().getNodes().values()) {
//			if (isCoordInShape(node.getCoord(), featuresInShape, factory)) {
//				personRouterFilter.getInterestingNodeIds().add(node.getId());
//			}
//		}
		
		PersonVerschmiererTask personVerschmiererTask = new PersonVerschmiererTask(LANDKREISE);
		
		PersonMerger personMerger = new PersonMerger(2);
		
//		RoutePersonTask router = new RoutePersonTask(osmNetwork.getConfig(), osmNetwork.getNetwork());
		
		PopulationWriterTask populationWriter = new PopulationWriterTask(OUTPUT_POPULATION_FILENAME, osmNetwork.getNetwork());
		
//		gvPopulationReaderTask.setSink(personDereferencerTask);
//		personDereferencerTask.setSink(personGeoTransformatorTask);
//		personGeoTransformatorTask.setSink(personRouterFilter);
//		personRouterFilter.setSink(personMerger.getSink(0));
		
		
		PopulationGenerator populationBuilder = new PopulationGenerator();
		
//		RouterFilter routerFilter = new RouterFilter(osmNetwork.getNetwork());
//		for (Node node : osmNetwork.getNetwork().getNodes().values()) {
//			if (isCoordInShape(node.getCoord(), featuresInShape, factory)) {
//				routerFilter.getInterestingNodeIds().add(node.getId());
//			}
//		}
//		
		
//		PV2025MatrixReader pvMatrixReader = new PV2025MatrixReader();
		PendlerMatrixReader pvMatrixReader = new PendlerMatrixReader();
		pvMatrixReader.setFlowSink(populationBuilder);
//		routerFilter.setSink(populationBuilder);
//		populationBuilder.setSink(personMerger.getSink(1));
		
//		personMerger.setSink(personVerschmiererTask);
//		personVerschmiererTask.setSink(router);
		populationBuilder.setSink(populationWriter);
		
		
		pvMatrixReader.run();
//		gvPopulationReaderTask.run();
		
		System.err.println("some landkreises do not work because of gebietsreform; check!") ;
	}
	
}
