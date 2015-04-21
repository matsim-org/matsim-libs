package playground.dhosse.siouxFalls;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.HashSet;
import java.util.Set;

public class SiouxFallsNetworkAndPopulationUtils {

	private static Scenario scenario;
	private static SimpleFeatureBuilder builder;
	
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);
		new PopulationReaderMatsimV5(scenario).readFile("C:/Users/Daniel/Desktop/Siouxfalls_population_initial.xml");
		
		String toDirectory = "C:/Users/Daniel/Dropbox/siouxfalls/siouxfalls_network.xml";
//		generateAndWriteNetwork(toDirectory);

		//optional
//		exportNetwork2Shp();
		activityOutput();
		
	}
	
	private static void generateAndWriteNetwork(String toDirectory){
		
		String osmFile = "C:/Users/Daniel/Dropbox/siouxfalls/network4.osm";
		Network network = scenario.getNetwork();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, "PROJCS[\"WGS 84 / UTM zone 14N\","+
				                                    "GEOGCS[\"WGS 84\","+
				                                           "DATUM[\"WGS_1984\","+
				                                              "SPHEROID[\"WGS 84\",6378137,298.257223563,"+
				                                                   "AUTHORITY[\"EPSG\",\"7030\"]],"+
				                                               "AUTHORITY[\"EPSG\",\"6326\"]],"+
				                                           "PRIMEM[\"Greenwich\",0,"+
				                                               "AUTHORITY[\"EPSG\",\"8901\"]],"+
				                                           "UNIT[\"degree\",0.01745329251994328,"+
				                                               "AUTHORITY[\"EPSG\",\"9122\"]],"+
				                                           "AUTHORITY[\"EPSG\",\"4326\"]],"+
				                                       "UNIT[\"metre\",1,"+
				                                           "AUTHORITY[\"EPSG\",\"9001\"]],"+
				                                       "PROJECTION[\"Transverse_Mercator\"],"+
				                                       "PARAMETER[\"latitude_of_origin\",0],"+
				                                       "PARAMETER[\"central_meridian\",-99],"+
				                                       "PARAMETER[\"scale_factor\",0.9996],"+
				                                       "PARAMETER[\"false_easting\",500000],"+
				                                       "PARAMETER[\"false_northing\",0],"+
				                                       "AUTHORITY[\"EPSG\",\"32614\"],"+
				                                       "AXIS[\"Easting\",EAST],"+
				                                       "AXIS[\"Northing\",NORTH]]");
		OsmNetworkReader or = new OsmNetworkReader(network, ct);
		or.parse(osmFile);
		new NetworkCleaner().run(network);
		new NetworkWriter(network).writeV1(toDirectory);
		
	}
	
	private static void activityOutput(){
		
		//...
		SimpleFeatureTypeBuilder tbuilder = new SimpleFeatureTypeBuilder();
		tbuilder.setName("shape");
		tbuilder.add("geometry",Point.class);
		tbuilder.add("type", String.class);
		
		builder = new SimpleFeatureBuilder(tbuilder.buildFeatureType());
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		
		GeometryFactory gf = new GeometryFactory();
		
		int i = 0;
		
		for(Person p : scenario.getPopulation().getPersons().values()){
			
			for(PlanElement pe : p.getSelectedPlan().getPlanElements()){

				if(pe instanceof Activity){
					
					Activity act = (Activity)pe;
					SimpleFeature feature = builder.buildFeature(Integer.toString(i),new Object[]{
						gf.createPoint(MGC.coord2Coordinate(act.getCoord())),
						act.getType()
					});
					i++;
					features.add(feature);
					
				}
				
			}
			
		}
	
		System.out.println(features.size());
		
		ShapeFileWriter.writeGeometries(features, "C:/Users/Daniel/Desktop/acts.shp");
		
	}
	
	private static void exportNetwork2Shp(){
		
		SimpleFeatureTypeBuilder tbuilder = new SimpleFeatureTypeBuilder();
		tbuilder.setName("shape");
		tbuilder.add("geometry",LineString.class);
		tbuilder.add("id", String.class);
		tbuilder.add("length", Double.class);
		tbuilder.add("capacity", Double.class);
		tbuilder.add("freespeed", Double.class);
		tbuilder.add("allowed_modes", String.class);
		
		builder = new SimpleFeatureBuilder(tbuilder.buildFeatureType());
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		
		GeometryFactory gf = new GeometryFactory();
		
		for(Link link : scenario.getNetwork().getLinks().values()){
			SimpleFeature feature = builder.buildFeature(link.getId().toString(), new Object[]{
					gf.createLineString(new Coordinate[]{
							new Coordinate(MGC.coord2Coordinate(link.getFromNode().getCoord())),
							new Coordinate(MGC.coord2Coordinate(link.getToNode().getCoord()))
					}),
					link.getId(),
					link.getLength(),
					link.getCapacity(),
					link.getFreespeed(),
					link.getAllowedModes().toString()
			});
			features.add(feature);
		}
		
		ShapeFileWriter.writeGeometries(features, "C:/Users/Daniel/Dropbox/siouxfalls/net.shp");
		
	}

}
