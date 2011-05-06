package playground.demandde.pendlermatrix;

import java.io.IOException;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.mzilske.pipeline.PopulationReaderTask;
import playground.mzilske.pipeline.PopulationWriterTask;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class GVPlanReader {
	
	
	private static final String GV_NETWORK_FILENAME = "/Users/michaelzilske/workspace/prognose_2025/demand/network_cleaned_wgs84.xml.gz";
	
	private static final String NETWORK_FILENAME = "/Users/michaelzilske/osm/motorway_germany.xml";
	
	private static final String GV_PLANS = "/Users/michaelzilske/workspace/run1061/1061.output_plans.xml.gz";
	
	private static final String FILTER_FILENAME = "/Users/michaelzilske/workspace/prognose_2025/demand/filter.shp";
	
	private static final String LANDKREISE = "/Users/michaelzilske/workspace/prognose_2025/osm_zellen/landkreise.shp";
	
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
		Scenario gvNetwork = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(gvNetwork).readFile(GV_NETWORK_FILENAME);
		Scenario osmNetwork = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(osmNetwork).readFile(NETWORK_FILENAME);
		Set<Feature> featuresInShape;
		featuresInShape = new ShapeFileReader().readFileAndInitialize(FILTER_FILENAME);
		
		PopulationReaderTask populationReaderTask = new PopulationReaderTask(GV_PLANS, gvNetwork.getNetwork());
		
		PersonDereferencerTask personDereferencerTask = new PersonDereferencerTask();
		
		PersonGeoTransformatorTask personGeoTransformatorTask = new PersonGeoTransformatorTask(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
		
		PersonRouterFilter personRouterFilter = new PersonRouterFilter(osmNetwork.getNetwork());
		GeometryFactory factory = new GeometryFactory();
		for (Node node : osmNetwork.getNetwork().getNodes().values()) {
			if (isCoordInShape(node.getCoord(), featuresInShape, factory)) {
				personRouterFilter.getInterestingNodeIds().add(node.getId());
			}
		}
		
		PersonVerschmiererTask personVerschmiererTask = new PersonVerschmiererTask(LANDKREISE);
		
		PopulationWriterTask populationWriterTask = new PopulationWriterTask("/Users/michaelzilske/workspace/prognose_2025/demand/naechster_versuch_gv.xml", gvNetwork.getNetwork());
		
		populationReaderTask.setSink(personDereferencerTask);
		personDereferencerTask.setSink(personGeoTransformatorTask);
		personGeoTransformatorTask.setSink(personRouterFilter);
		personRouterFilter.setSink(personVerschmiererTask);
		personVerschmiererTask.setSink(populationWriterTask);
		
		populationReaderTask.run();
	}
	
}
