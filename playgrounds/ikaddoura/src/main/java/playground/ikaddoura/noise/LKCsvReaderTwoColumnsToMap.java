package playground.ikaddoura.noise;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class LKCsvReaderTwoColumnsToMap {
	
	private final static Logger log = Logger.getLogger(LKCsvReaderTwoColumnsToMap.class);
	
	private static Scenario scenario;
	private static String networkFile = "/Users/Lars/workspace2/baseCaseCtd_8_250/output_network.xml.gz";
	private static String populationFile = "/Users/Lars/workspace2/baseCaseCtd_8_250/output_plans.xml.gz";
	private static SimpleFeatureBuilder builder;
	
	private static String ID = "link";
	private static String VALUE = "toll per agent per km";
	private final static String SEPARATOR_SEMIKOLON = ";";
	private String fileName = null;
//	private static String testString = "/Users/Lars/Desktop/test.csv";
	
	static String networkOutputFile = "/Users/Lars/Desktop/VERSUCH/network_output3.shp";
	static String outputPathCoords = "/Users/Lars/Desktop/VERSUCH/Sioux250.shp";
	
	public static void main(String[] args) throws IOException {
		
		loadScenario();
		
		Map<Id<Coord>, Double> pointId2x = readDouble("/Users/Lars/Desktop/VERSUCH/Sioux250.csv", "pointId", "xCoord");
		Map<Id<Coord>, Double> pointId2y = readDouble("/Users/Lars/Desktop/VERSUCH/Sioux250.csv", "pointId", "yCoord");
		Map<Id<Coord>,Double> id2counter = readDouble("/Users/Lars/Desktop/VERSUCH/Sioux250.csv", "pointId", "counter");
		
		Map<Id<Coord>,Coord> coords = new HashMap<Id<Coord>, Coord>();
		for(Id<Coord> id : pointId2x.keySet()) {
			Coord coord = new CoordImpl(pointId2x.get(id), pointId2y.get(id));
			coords.put(id,coord);
		}
		
//		Map<Id, Double> linkId2tollValueAB = readDouble("/Users/Lars/Desktop/VERSUCH/TollStatsAB.csv", ID, VALUE);
//		Map<Id, Double> linkId2tollValueHB = readDouble("/Users/Lars/Desktop/VERSUCH/TollStatsHB.csv", ID, VALUE);
//		
//		List<Id> listIds = new ArrayList<Id>();
//		for(Id id : linkId2tollValueAB.keySet()) {
//			listIds.add(id);
//		}
//		for(Id id : linkId2tollValueHB.keySet()) {
//			if(!(listIds.contains(id))) {
//				listIds.add(id);
//			}
//		}
//		
//		Map<Id,Double> linkId2Value = new HashMap<Id, Double>();
//		
//		for(Id id : listIds) {
//			double value = (linkId2tollValueAB.get(id)) - (linkId2tollValueHB.get(id));
//			linkId2Value.put(id, value);
//			
//		}
		
//		exportNetwork2Shp(scenario.getNetwork(),linkId2Value);
		exportCoords2Shp(coords,id2counter);
	}
	
private static void exportCoords2Shp(Map<Id<Coord>,Coord> coords, Map<Id<Coord>,Double> id2counter ){
		
		
	
		SimpleFeatureTypeBuilder tbuilder = new SimpleFeatureTypeBuilder();
		tbuilder.setName("shape");
		tbuilder.add("geometry", Point.class);
		tbuilder.add("counter", Double.class);
		
		builder = new SimpleFeatureBuilder(tbuilder.buildFeatureType());
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		
		GeometryFactory gf = new GeometryFactory();
		
		int i = 0;
	
		for(Id<Coord> id : coords.keySet()) {
					
			SimpleFeature feature = builder.buildFeature(Integer.toString(i),new Object[]{
				gf.createPoint(MGC.coord2Coordinate(coords.get(id))),
				id2counter.get(id)
		});
		i++;
		features.add(feature);
	
		}
		
		log.info("Writing out activity points shapefile... ");
		ShapeFileWriter.writeGeometries(features, outputPathCoords);
		log.info("Writing out activity points shapefile... Done.");		
	}
	
	private static void loadScenario() {
		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);
	}
	
	public static Map<Id<Coord>, Double> readDouble(String fileName , String header1 , String header2){
		
		Map <Id<Coord>,Double> id2value = new HashMap<>();
		
//		this.fileName = fileName;
		ID = header1;
		VALUE = header2;
		
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(new File(fileName)));
			
			String line = br.readLine();
			
			Map<String,Integer> idxFromKey = HeaderParser.createIdxFromKey(line, SEPARATOR_SEMIKOLON);
//			log.info(ID);
//			log.info(idxFromKey);
			final int idId = idxFromKey.get(ID);
//			log.info(VALUE);
//			log.info(idxFromKey);
			final int idValue = idxFromKey.get(VALUE);
			
			String[] parts;
			
			while( (line = br.readLine()) != null ){
				
//				log.info(line);
				
				parts = line.split(SEPARATOR_SEMIKOLON);
				
				Id<Coord> id = Id.create(parts[idId], Coord.class);
				double value = Double.parseDouble(parts[idValue]);
			
				id2value.put(id,value);
			}
			
			br.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return id2value;
		
	}
	
	// ************************************************************
	
	public static void exportNetwork2Shp(Network network , Map<Id<Link>,Double> linkId2noiseEmission){

		if (scenario.getNetwork().getLinks().size() == 0) {
			new NetworkReaderMatsimV1(scenario).parse(networkFile);
		}
				
		SimpleFeatureTypeBuilder tbuilder = new SimpleFeatureTypeBuilder();
		tbuilder.setName("shape");
		tbuilder.add("geometry", LineString.class);
		tbuilder.add("id", String.class);
		tbuilder.add("length", Double.class);
		tbuilder.add("capacity", Double.class);
		tbuilder.add("freespeed", Double.class);
		tbuilder.add("modes", String.class);
		tbuilder.add("noiseEmissions", Double.class);
		
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
					link.getAllowedModes().toString(),
					linkId2noiseEmission.get(link.getId()),
			});
			features.add(feature);
		}
		
		log.info("Writing out network lines shapefile... ");
		ShapeFileWriter.writeGeometries(features, networkOutputFile);
		log.info("Writing out network lines shapefile... Done.");
	}
	
	// ************************************************************
	
	private SimpleFeatureBuilder initFeatureType() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(MGC.getCRS(TransformationFactory.WGS84_UTM35S));
		b.setName("multiPolygon");
		b.add("location", MultiPolygon.class);
		b.add("Id", String.class);
		
		return new SimpleFeatureBuilder(b.buildFeatureType());
	}

	public void writeShapeFileGeometry(Map<Integer, Geometry> zoneId2geometry, String outputFile) {

		SimpleFeatureBuilder factory = initFeatureType();
		Set<SimpleFeature> features = createFeatures(zoneId2geometry, factory);
		ShapeFileWriter.writeGeometries(features, outputFile);
		System.out.println("ShapeFile " + outputFile + " written.");	
	}

	private Set<SimpleFeature> createFeatures(Map<Integer, Geometry> zoneId2geometry, SimpleFeatureBuilder factory) {
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		for (Integer nr : zoneId2geometry.keySet()){
			features.add(getFeature(nr, zoneId2geometry.get(nr), factory));
		}
		return features;
		
	}

	private SimpleFeature getFeature(Integer nr, Geometry geometry,	SimpleFeatureBuilder factory) {

		GeometryFactory geometryFactory = new GeometryFactory();
		MultiPolygon g = (MultiPolygon) geometryFactory.createGeometry(geometry);
		
		Object [] attribs = new Object[7];
		attribs[0] = g;
		attribs[1] = String.valueOf(nr);
	
		return factory.buildFeature(null, attribs);
	}
	
	public static class HeaderParser {
		
		public static Map<String,Integer> createIdxFromKey( String line, String seperator ) {
			String[] keys = line.split( seperator ) ;

			Map<String,Integer> idxFromKey = new ConcurrentHashMap<String, Integer>() ;
			for ( int i=0 ; i<keys.length ; i++ ) {
				idxFromKey.put(keys[i], i ) ;
			}
			return idxFromKey ;
		}
		
		public static Map<Integer,Integer> createIdxFromKey( int keys[]) {

			Map<Integer,Integer> idxFromKey = new ConcurrentHashMap<Integer, Integer>() ;
			for ( int i=0 ; i<keys.length ; i++ ) {
				idxFromKey.put(keys[i], i ) ;
			}
			return idxFromKey ;
		}

	}
	
}
