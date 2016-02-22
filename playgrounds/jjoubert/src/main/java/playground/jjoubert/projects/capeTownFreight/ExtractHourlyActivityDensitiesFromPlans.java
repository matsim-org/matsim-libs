/**
 * 
 */
package playground.jjoubert.projects.capeTownFreight;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections15.map.HashedMap;
import org.apache.log4j.Logger;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.grid.GeneralGrid;
import playground.southafrica.utilities.grid.GeneralGrid.GridType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Class to read in a population file, and aggregate the selected plans'
 * activities to a hexagonal grid on an hourly basis.
 * 
 * @author jwjoubert
 */
public class ExtractHourlyActivityDensitiesFromPlans {
	final private static Logger LOG = Logger.getLogger(ExtractHourlyActivityDensitiesFromPlans.class);
	final private static double WIDTH = 1000;
	final private static double HEIGHT = Math.sqrt(3.0)*WIDTH/2.0;
	final private static double W = 0.0107583369/2.0;
	final private static double H = 0.0078497390/2.0;
	
	private static Map<Point, int[]> map;
	

	/**
	 * Instantiating and executing the hourly aggregator.
	 * 
	 * @param args
	 * @throws SchemaException 
	 */
	public static void main(String[] args) throws SchemaException {
		Header.printHeader(ExtractHourlyActivityDensitiesFromPlans.class.toString(), args);
		
		String populationFile = args[0];
		String areaShapefile = args[1];
		String outputFile = args[2];
		String outputShapefile = args[3];

		/* Parse the population. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(populationFile);

		/* Parse the shapefile. */
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(areaShapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		if(features.size() > 1){
			LOG.warn("Multiple features in given shapefile. Number of features: " + features.size());
		}
		/* Get the first geometry. */		
		MultiPolygon city = null;
		Iterator<SimpleFeature> iterator = features.iterator();
		SimpleFeature sf = iterator.next();
		if(sf.getDefaultGeometry() instanceof MultiPolygon){
			LOG.info("Great! Geometry is MultiPolygon.");
			city = (MultiPolygon)sf.getDefaultGeometry();
		}
		/* Build grid from shapefile */
		LOG.info("Building grid from shapefile...");
		GeneralGrid grid = new GeneralGrid(WIDTH, GridType.HEX);
		grid.generateGrid(city);
		
		/* Set up the necessary hourly maps. */
		Collection<Point> cells = grid.getGrid().values();
		map = new HashedMap<Point, int[]>(cells.size());
		for(Point p : cells){
			int[] array = new int[24];
			map.put(p, array);
		}
		
		List<Double> listWidths = new ArrayList<Double>();
		List<Double> listHeights = new ArrayList<Double>();
		
		
		/* Process the plans of each individual. */
		LOG.info("Processing selected plans...");
		GeometryFactory gf = new GeometryFactory();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		Counter counter = new Counter("   person # ");
		for(Person person : sc.getPopulation().getPersons().values()){
			Plan plan = person.getSelectedPlan();
			double startTime = Double.NEGATIVE_INFINITY;
			
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Leg){
					Leg leg = (Leg)pe;
					startTime = leg.getDepartureTime() + leg.getTravelTime();
				} else if(pe instanceof Activity){
					Activity act = (Activity)pe;
					Coord c = act.getCoord();
					Point p = gf.createPoint(new Coordinate(c.getX(), c.getY()));
					
					/* Only consider the activity if it actually happens inside the grid. */
					if(grid.isInGrid(p)){
						/* Get the closest cell centroid. */
						Point centroid = grid.getGrid().getClosest(p.getX(), p.getY());
						
						/* Get the hour of day. */
						if(startTime > Double.NEGATIVE_INFINITY){
							int hour = getHour(startTime);
							int oldValue = map.get(centroid)[hour];
							map.get(centroid)[hour] = oldValue + 1;
						}
						
						/* Check the conversion ratio. */
						double radius = WIDTH/2.0;
						double height = Math.sqrt(3)/2.0*radius;
						Coord c1 = new Coord(p.getX() - radius, p.getY());
						Coord c2 = new Coord(p.getX() + radius, p.getY());
						Coord c3 = new Coord(p.getX() - 0.5 * radius, p.getY() + height);
						Coord c4 = new Coord(p.getX() - 0.5 * radius, p.getY() - height);
						
						Coord c1c = ct.transform(c1);
						Coord c2c = ct.transform(c2);
						Coord c3c = ct.transform(c3);
						Coord c4c = ct.transform(c4);
						double d1 = CoordUtils.calcEuclideanDistance(c1c, c2c);
						double d2 = CoordUtils.calcEuclideanDistance(c3c, c4c);
						listWidths.add(d1);
						listHeights.add(d2);
					}
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		
		/* Report the ratios to the console. */
		LOG.info(String.format("SA-Albers: width %.2f; height %.2f; ratio %.4f", WIDTH, HEIGHT, HEIGHT/WIDTH)); 
		double meanWidth = getMean(listWidths);
		double meanHeight = getMean(listHeights);
		LOG.info(String.format("WGS84: width %.10f; height %.10f; ratio %.4f", meanWidth, meanHeight, meanHeight/meanWidth)); 
		
		/* Print the output maps to file. */
		LOG.info("Writing output to file...");
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			bw.write("x,y,lon,lat,lon1,lat1,lon2,lat2,lon3,lat3,lon4,lat4,lon5,lat5,lon6,lat6,h00,h01,h02,h03,h04,h05,h06,h07,h08,h09,h10,h11,h12,h13,h14,h15,h16,h17,h18,h19,h20,h21,h22,h23");
			bw.newLine();
			for(Point p : map.keySet()){
				/* Transform the coordinate to WGS84. */
				Coord c1 = new Coord(p.getX(), p.getY());
				Coord c2 = ct.transform(c1);
				bw.write(String.format("%.4f,%.4f,%.8f,%.8f", p.getX(), p.getY(), c2.getX(), c2.getY()));
				
				/* Write the six hexagon coordinate (WGS84 only). */
				Polygon poly = getPolygonWgs84(p);
				Coordinate[] ca = poly.getCoordinates();
				for(int i = 0; i < 6; i++){
					Coordinate c = ca[i];
					bw.write(String.format(",%.6f,%.6f", c.x, c.y));
				}				
				
				/* Write the hourly values. */
				for(int i = 0; i < 24; i++){
					bw.write(",");
					bw.write(String.valueOf(map.get(p)[i]));
				}
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFile);
			}
		}
		
		SimpleFeatureCollection collection = createFeatureCollection();
		try {
			writeShapefile(outputShapefile, collection);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write shapefile.");
		}
		
		Header.printFooter();
	}
	
	
	private static double getMean(List<Double> list){
		double sum = 0.0;
		for(double d : list){
			sum += d;
		}
		return sum/((double)list.size());
	}
	
	
	/**
	 * Converting 'seconds-from-midnight' to 'hour-of-day'.
	 * 
	 * @param seconds
	 * @return
	 */
	private static int getHour(double seconds){
		int hour = 0;
		
		double t = seconds;
		while(t >= 24.0*60.0*60){
			t -= 24.0*60.0*60.0;
		}
		hour = (int) Math.round( Math.floor(t / (60.0*60.0) ) );
		return hour;
	}
	
	
	private static SimpleFeatureType createFeatureType(){
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("Zone");
		builder.setCRS(DefaultGeographicCRS.WGS84);
		
		/* Add the attributes in order. */
		builder.add("Zone", Polygon.class);
		builder.length(6).add("Id", Integer.class);
		builder.length(6).add("h00", Integer.class);
		builder.length(6).add("h01", Integer.class);
		builder.length(6).add("h02", Integer.class);
		builder.length(6).add("h03", Integer.class);
		builder.length(6).add("h04", Integer.class);
		builder.length(6).add("h05", Integer.class);
		builder.length(6).add("h06", Integer.class);
		builder.length(6).add("h07", Integer.class);
		builder.length(6).add("h08", Integer.class);
		builder.length(6).add("h09", Integer.class);
		builder.length(6).add("h10", Integer.class);
		builder.length(6).add("h11", Integer.class);
		builder.length(6).add("h12", Integer.class);
		builder.length(6).add("h13", Integer.class);
		builder.length(6).add("h14", Integer.class);
		builder.length(6).add("h15", Integer.class);
		builder.length(6).add("h16", Integer.class);
		builder.length(6).add("h17", Integer.class);
		builder.length(6).add("h18", Integer.class);
		builder.length(6).add("h19", Integer.class);
		builder.length(6).add("h20", Integer.class);
		builder.length(6).add("h21", Integer.class);
		builder.length(6).add("h22", Integer.class);
		builder.length(6).add("h23", Integer.class);
		
		/* build the type. */
		final SimpleFeatureType ZONE = builder.buildFeatureType();
		return ZONE;
	}
	
	private static SimpleFeatureCollection createFeatureCollection(){
		LOG.info("Creating feature collection for shapefile...");
		DefaultFeatureCollection collection = new DefaultFeatureCollection();
		
		Counter counter = new Counter("   feature # ");
		for(Point p : map.keySet()){
			SimpleFeatureBuilder builder = new SimpleFeatureBuilder(createFeatureType());

			/* Build polygon. */
			builder.add(getPolygonWgs84(p));
			builder.add(counter.getCounter());
			
			/* Add the hourly values. */
			int[] ia = map.get(p);
			for(int i : ia){
				builder.add(i);
			}
			SimpleFeature feature = builder.buildFeature(null);
			collection.add(feature);
			
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done creating feature collection.");
		
		return collection;
	}
	
	private static Polygon getPolygonWgs84(Point p){
		GeometryFactory gf = new GeometryFactory();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		/* Create the polygon. */
		double x = p.getX();
		double y = p.getY();
		double w = WIDTH/2.0;
		double h = HEIGHT/2.0;
		List<Coord> cl = new ArrayList<Coord>();
		cl.add(new Coord(x - w, y));
		cl.add(new Coord(x - 0.5 * w, y + h));
		cl.add(new Coord(x + 0.5 * w, y + h));
		cl.add(new Coord(x + w, y));
		cl.add(new Coord(x + 0.5 * w, y - h));
		cl.add(new Coord(x - 0.5 * w, y - h));
		List<Coordinate> clc = new ArrayList<Coordinate>();
		for(Coord c : cl){
			Coord cc = ct.transform(c);
			clc.add(new Coordinate(cc.getX(), cc.getY()));
		}
		clc.add(clc.get(0));
		Coordinate[] ca = new Coordinate[clc.size()];
		
		return gf.createPolygon(clc.toArray(ca));
	}

	
	private static void writeShapefile(String filename, FeatureCollection collection) throws IOException{
		LOG.info("Writing shapefile to " + filename);
		File file = new File(filename);
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put("url", file.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);
		
		ShapefileDataStore dataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		dataStore.createSchema(createFeatureType());
		
		Transaction transaction = new DefaultTransaction("create");
		
		String typeName = dataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
		if(featureSource instanceof SimpleFeatureSource){
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			featureStore.setTransaction(transaction);
			try{
				featureStore.addFeatures(collection);
				transaction.commit();
			} finally{
				transaction.close();
			}
		} else{
			System.exit(1);
		}
	}

}
