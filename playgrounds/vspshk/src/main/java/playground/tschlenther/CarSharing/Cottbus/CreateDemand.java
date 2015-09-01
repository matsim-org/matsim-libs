package playground.tschlenther.CarSharing.Cottbus;

	import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.geotools.io.TableWriter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
	 
	public class CreateDemand {
	 /*
	  * Andre: IO-Dateinamen/ Ordner ggf. anpassen
	  */
		private static final String NETWORKFILE = "C:/Users/Tille/WORK/CarSharing/cottbus/input/network.xml";
		private static final String KREISE = "C:/Users/Tille/WORK/CarSharing/cottbus/input/Landkreise/Kreise.shp";
		private static final String BUILDINGS = "C:/Users/Tille/WORK/CarSharing/cottbus/input/BuildingsCottbus/BuildingsCottbus.shp";
		private static final String PLANSFILEOUTPUT = "C:/Users/Tille/WORK/CarSharing/cottbus/input/plansWithCarSharing_0015.xml";
		private static final String SHOPS = "C:/Users/Tille/WORK/CarSharing/cottbus/input/shops.txt";
		private static final String KINDERGARTEN = "C:/Users/Tille/WORK/CarSharing/cottbus/input/kindergaerten.txt";
		
		private Scenario scenario;
		private Map<String,Geometry> shapeMap;
		private Map<String,Geometry> buildingsMap;
		
		/*
		 * SCALEFACTOR 1.5 = 100%-Szenario
		 * SCALEFACTOR 0.15 = 10%-Szenario
		 * SCALEFACTOR 0.015 = 1%-Szenario
		 */
		private static double SCALEFACTOR = 0.015;
		private Map<String,Coord> kindergartens;
		private Map<String,Coord> shops;
		private static final Random rnd = new Random(42);
		
		
		
//		public static void main(String[] args) {
//			CreateDemand cd = new CreateDemand(null);
//			cd.run();
//		}
		
		
		public CreateDemand (Config config){
			this.scenario = ScenarioUtils.createScenario(config);
			new MatsimNetworkReader(scenario).readFile(NETWORKFILE);
			this.run();
		}
		
		
		private void run() {
			this.shapeMap = readShapeFile(KREISE, "Nr");
			this.buildingsMap = readShapeFile(BUILDINGS, "osm_id");
			this.shops = readFacilityLocations(SHOPS);
			this.kindergartens = readFacilityLocations(KINDERGARTEN);

			
			//CB-CB (Stadtverkehr: Modal Split mit 50% IV/ÖV)
			double commuters = 22709*SCALEFACTOR;
			createPersons("12052000", "12052000", commuters, 0.5);
			
			//CB-LDS (Regionalverkehr: Modal Split mit 80% IV)
			commuters = 399*SCALEFACTOR;
			createPersons("12052000", "12061000", commuters, 0.8);
			
			//CB-LOS
			commuters = 139*SCALEFACTOR;
			createPersons("12052000", "12067000", commuters, 0.8);
			
			//CB-SPN
			commuters = 4338*SCALEFACTOR;
			createPersons("12052000", "12071000", commuters, 0.8);
					
			//LDS-CB
			commuters = 1322*SCALEFACTOR;
			createPersons("12061000", "12052000", commuters, 0.8);
					
			//LDS-SPN
			commuters = 522*SCALEFACTOR;
			createPersons("12061000", "12071000", commuters, 0.8);
			
			//LOS-CB
			commuters = 382*SCALEFACTOR;
			createPersons("12067000", "12052000", commuters, 0.8);
			
			//LOS-SPN
			commuters = 449*SCALEFACTOR;
			createPersons("12067000", "12071000", commuters, 0.8);
			
			//SPN-CB
			commuters = 11869*SCALEFACTOR;
			createPersons("12071000", "12052000", commuters, 0.8);
			
			//SPN-LDS
			commuters = 408*SCALEFACTOR;
			createPersons("12071000", "12061000", commuters, 0.8);
			
			//SPN-LOS
			commuters = 466*SCALEFACTOR;
			createPersons("12071000", "12067000", commuters, 0.8);
			
			//SPN-SPN
			commuters = 22524*SCALEFACTOR;
			createPersons("12071000", "12071000", commuters, 0.8);
			
			/*
			 * Achtung! 3 Nullen extra hier ggü. ISIS Daten
			 */
						
			System.out.println(this.shapeMap.keySet());
			
			createCarSharingVehicleLocationsFile();
			
			PopulationWriter pw = new PopulationWriter(scenario.getPopulation(),scenario.getNetwork());
			pw.write(PLANSFILEOUTPUT);
			
		}
		
		
		private void createPersons (String homeZone, String workZone, double commuters, double relativeAmountOfCarUses) {
			Geometry home = this.shapeMap.get(homeZone);
			Geometry work = this.shapeMap.get(workZone);
			for (int i = 0; i<= commuters;i++){
				String mode = "car";
				double carcommuters = commuters*relativeAmountOfCarUses;
				if (i>carcommuters) mode = "pt";
				
				Coord homec = this.setBuildingFromGeometry(home);
				
				//Coord workc = drawRandomPointFromGeometry(work); Andre: meines Erachtens sollten auch die Aktivitäten "Arbeiten" in Gebäuden erfolgen 
				Coord workc = this.setBuildingFromGeometry(work);
				
				double personalRandom = rnd.nextDouble();
				createOnePerson(i, homec, workc, mode, homeZone+ "_"+workZone+ "_", personalRandom);
			}
		}
		
		
		private void createOnePerson(int i, Coord coord, Coord coordWork, String mode, String toFromPrefix, double personalRandom) {
			Id<Person> personId = Id.createPersonId(toFromPrefix+i);
			Person person = scenario.getPopulation().getFactory().createPerson(personId); 

			Plan plan = scenario.getPopulation().getFactory().createPlan(); 
			
			//Planerzeugung inkl. Zufallslogik für Start- bzw. Endezeiten
			//60% Plan: home-work-home
			
			
			//CARSHARING MODE
			if(personalRandom<0.6){
				double csRandom = rnd.nextDouble();
				if(csRandom>0.33 && csRandom < 0.67) mode = "freefloating";
				if(csRandom >= 0.67) mode = "twowaycarsharing";
			}
			
			Activity home = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
			double startTime = 7.5*60*60+(2*60*60*rnd.nextDouble()); //ZufallsStartZeit 7.30-9.30Uhr
			home.setEndTime(startTime);
			plan.addActivity(home); 

			Leg hinweg1 = scenario.getPopulation().getFactory().createLeg(mode);
			plan.addLeg(hinweg1); 

			//10% Plan: home-KINDERGARTEN1-work-KINDERGARTEN2-home
			//kindergarten AM
			if ((personalRandom>0.6)&&(personalRandom<=0.7)){
				
				Activity kindergarten1 = scenario.getPopulation().getFactory().createActivityFromCoord("kindergarten1", this.findClosestCoordFromMap(coord, kindergartens));
				kindergarten1.setEndTime(startTime+(0.4*60*60)+(0.1*60*60*rnd.nextDouble())); //DropOff-Zeit 5-10min
				plan.addActivity(kindergarten1); 

				Leg hinweg2 = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(hinweg2); 			
			}		
			
			Activity work = scenario.getPopulation().getFactory().createActivityFromCoord("work", coordWork);
			double workEndTime = startTime+(7.5*60*60)+(1*60*60*rnd.nextDouble()); // Arbeitszeit 7,5-8,5 Stunden
			work.setEndTime(workEndTime);
			plan.addActivity(work); 

			Leg rueckweg1 = scenario.getPopulation().getFactory().createLeg(mode);
			plan.addLeg(rueckweg1); 
			
			//kindergarten PM
			if ((personalRandom>0.6)&&(personalRandom<=0.7)){
				
				Activity kindergarten2 = scenario.getPopulation().getFactory().createActivityFromCoord("kindergarten2", this.findClosestCoordFromMap(coord, kindergartens));
				kindergarten2.setEndTime(workEndTime+(0.4*60*60)+(0.1*60*60*rnd.nextDouble())); //PickUp-Zeit 5-10min
				plan.addActivity(kindergarten2); 

				Leg rückweg2 = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(rückweg2);
			}		
			
			//30% Plan: home-work-home-SHOPPEN-home
			if (personalRandom >0.7){
				
				Activity home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
				double startShoppingTime = workEndTime+(1*60*60)+(0.5*60*60*rnd.nextDouble());
				home2.setEndTime(startShoppingTime);
				plan.addActivity(home2);
				
				Leg zumShoppen = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(zumShoppen); 		
				
				Activity shopping = scenario.getPopulation().getFactory().createActivityFromCoord("shopping", this.findClosestCoordFromMap(coord, shops));
				shopping.setEndTime(startShoppingTime+(1*60*60)+(1*60*60*rnd.nextDouble())); //Shoppen 1-2 Stunden
				plan.addActivity(shopping);
				
				Leg vomShoppen = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(vomShoppen); 		
			}
			
			Activity home3 = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
			plan.addActivity(home3);

			person.addPlan(plan);
			scenario.getPopulation().addPerson(person);
		}
		
		
		private  Coord drawRandomPointFromGeometry(Geometry g) {
			   Random rnd = MatsimRandom.getLocalInstance();
			   Point p;
			   double x, y;
			   do {
			      x = g.getEnvelopeInternal().getMinX() +  rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			      y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			      p = MGC.xy2Point(x, y);
			   } while (!g.contains(p));
			   Coord coord = new CoordImpl(p.getX(), p.getY());
			   return coord;
		}
		
		
		public Map<String,Geometry> readShapeFile(String filename, String attrString){
			//attrString: Für Brandenburg: Nr
			//für OSM: osm_id
			
			Map<String,Geometry> shapeMap = new HashMap<String, Geometry>();
			
			for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
	 
					GeometryFactory geometryFactory= new GeometryFactory();
					WKTReader wktReader = new WKTReader(geometryFactory);
					Geometry geometry;
	 
					try {
						geometry = wktReader.read((ft.getAttribute("the_geom")).toString());
						shapeMap.put(ft.getAttribute(attrString).toString(),geometry);
	 
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} 
			return shapeMap;
		}	
		
		private Map<String,Coord> readFacilityLocations (String fileName){
			
			FacilityParser fp = new FacilityParser();
			TabularFileParserConfig config = new TabularFileParserConfig();
			config.setDelimiterRegex("\t");
			config.setCommentRegex("#");
			config.setFileName(fileName);
			new TabularFileParser().parse(config, fp);
			return fp.getFacilityMap();
			
		}
		
		private Coord findClosestCoordFromMap(Coord location, Map<String, Coord> shops2){
			Coord closest = null;
			double closestDistance = Double.MAX_VALUE;
			for (Coord coord : shops2.values()){
				double distance = CoordUtils.calcDistance(coord, location);
				if (distance<closestDistance) {
					closestDistance = distance;
					closest = coord;
				}
			}
			return closest;
		}
		
		private boolean isBuildingInZone (Geometry zone, Coord building){;
			Point p = MGC.xy2Point(building.getX(), building.getY());
		    return zone.contains(p);
		}
		
				
		private Coord findClosestBuildingFromCoord(Coord coord){
			Coord closest = coord;
			Coord ii = coord;
			double closestDistance = Double.MAX_VALUE;
			for(String key : this.buildingsMap.keySet()){
				ii = MGC.point2Coord(this.buildingsMap.get(key).getCentroid());
				double distance = CoordUtils.calcDistance(ii, coord);
				if(distance<closestDistance){ 
					closestDistance = distance;
					closest = ii;
				}
			}
			return closest;
		}
			
			
		private Coord setBuildingFromGeometry (Geometry zone){
			Coord coord ;
			do {
				Coord random = drawRandomPointFromGeometry(zone);
				coord = this.findClosestBuildingFromCoord(random);
			}
			while(!this.isBuildingInZone(zone, coord));
			return coord;
		}
	
		/**
		 * create 2 CarSharingVehicleLocations per region
		 */
	public void createCarSharingVehicleLocationsFile(){
		int vehicles = 500;
		String outputDir = "C:/Users/Tille/WORK/CarSharing/cottbus/input/VehicleLocations.txt";
		File file = new File(outputDir);
		BufferedWriter writer = null;
		try {
			 System.out.println("writing VehicleLocations to:\t" + file.getCanonicalPath());
			writer = new BufferedWriter(new FileWriter(file));
			String newline = System.getProperty("line.separator");
			writer.write("???\t???\tX\tY\t???\t???\t#Vehicles");
			writer.write(newline);
			for(Geometry g : this.shapeMap.values()){
				Coord station = this.drawRandomPointFromGeometry(g);
				station = this.findClosestBuildingFromCoord(station);
				writer.write(" - \t - \t" + station.getX() + "\t" + station.getY() + "\t - \t - \t" + vehicles);
				writer.write(newline);
				station = this.drawRandomPointFromGeometry(g);
				station = this.findClosestBuildingFromCoord(station);
				writer.write(" - \t - \t" + station.getX() + "\t" + station.getY() + "\t - \t - \t" + vehicles);
				writer.write(newline);
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
	 

	class FacilityParser implements TabularFileHandler{
		
		
	private Map<String,Coord> facilityMap = new HashMap<String, Coord>();	
	CoordinateTransformation ct = new GeotoolsTransformation("EPSG:4326", "EPSG:32633"); 
 
	@Override
	public void startRow(String[] row) {
		try{
		Double x = Double.parseDouble(row[2]);
		Double y = Double.parseDouble(row[1]);
		Coord coords = new CoordImpl(x,y);
		this.facilityMap.put(row[0],ct.transform(coords));
		}
		catch (NumberFormatException e){
			//skips line
		}
	}
 
	public Map<String, Coord> getFacilityMap() {
		return facilityMap;
	}
	

}