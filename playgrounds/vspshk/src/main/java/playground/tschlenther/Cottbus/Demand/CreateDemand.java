package playground.tschlenther.Cottbus.Demand;

	import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.core.utils.geometry.CoordImpl;
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

//------FIELDS TO BE MODIFIED -----------------------------------------------------------------------------------------//		
		//Modal-Split values representing the relative amount of car users
		private final double MS_INNER_CITY = 0.5;
		private final double MS_OUTSIDE = 1;
		
		/*
		 * sample size can be modified via SCALFEACTOR
		 * note: since data come from incomplete commuter statistics
		 * a sample size of 100% is equivalent to a SCALEFACTOR of 1.5		 * 
		 * SCALEFACTOR 1.5 = 100%-Szenario
		 * SCALEFACTOR 0.15 = 10%-Szenario
		 * SCALEFACTOR 0.015 = 1%-Szenario
		 */
		private static double SCALEFACTOR = 0.015;
//------ FIELDS NOT TO BE MODIFIED ------------------------------------------------------------------------------------//
		private Scenario scenario;
		private Map<String,Geometry> shapeMap;
		private Map<String,Geometry> buildingsMap;
		private Map<String,Coord> kindergartens;
		private Map<String,Coord> shops;
		private static final Random rnd = new Random(42);		
//----- FILE PATHS ----------------------------------------------------------------------------------------------------//
		private static final String NETWORKFILE = "C:/Users/Tille/WORK/Cottbus/Cottbus-pt/INPUT_mod/ADDEDLINKS_cap60.xml";
		private static final String KREISE = "C:/Users/Tille/WORK/Cottbus/Cottbus-pt/INPUT_mod/Landkreise/Kreise.shp";
		private static final String BUILDINGS = "C:/Users/Tille/WORK/Cottbus/Cottbus-pt/INPUT_mod/BuildingsCottbus/BuildingsCottbus.shp";
		private static final String SHOPS = "C:/Users/Tille/WORK/Cottbus/Cottbus-pt/INPUT_mod/shops.txt";
		private static final String KINDERGARTEN = "C:/Users/Tille/WORK/Cottbus/Cottbus-pt/INPUT_mod/kindergaerten.txt";
		
		private static final String PLANSFILEOUTPUT = "C:/Users/Tille/WORK/Cottbus/Cottbus-pt/INPUT_mod/plans_scale" + SCALEFACTOR + ".xml";
//-------------------------------------------------------------------------------------------------------------------//
		
		
		public static void main(String[] args) {
			CreateDemand cd = new CreateDemand();
			cd.run();
		}
		
		CreateDemand (){
			this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORKFILE);	
		}
		
		private void run() {
			
			// read all files
			this.shapeMap = readShapeFile(KREISE, "Nr");
			this.buildingsMap = readShapeFile(BUILDINGS, "osm_id");
			this.shops = readFacilityLocations(SHOPS);
			this.kindergartens = readFacilityLocations(KINDERGARTEN);
			
			
			//create Persons for every home zone - work zone - relation 
			//CB-CB
			double commuters = 22709*SCALEFACTOR;								//amount of 'commuters' for this h-w-relation
			createPersons("12052000", "12052000", commuters, MS_INNER_CITY);	
			
			//CB-LDS
			commuters = 399*SCALEFACTOR;
			createPersons("12052000", "12061000", commuters, MS_OUTSIDE);
			
			//CB-LOS
			commuters = 139*SCALEFACTOR;
			createPersons("12052000", "12067000", commuters, MS_OUTSIDE);
			
			//CB-SPN
			commuters = 4338*SCALEFACTOR;
			createPersons("12052000", "12071000", commuters, MS_OUTSIDE);
					
			//LDS-CB
			commuters = 1322*SCALEFACTOR;
			createPersons("12061000", "12052000", commuters, MS_OUTSIDE);
					
			//LDS-SPN
			commuters = 522*SCALEFACTOR;
			createPersons("12061000", "12071000", commuters, MS_OUTSIDE);
			
			//LOS-CB
			commuters = 382*SCALEFACTOR;
			createPersons("12067000", "12052000", commuters, MS_OUTSIDE);
			
			//LOS-SPN
			commuters = 449*SCALEFACTOR;
			createPersons("12067000", "12071000", commuters, MS_OUTSIDE);
			
			//SPN-CB
			commuters = 11869*SCALEFACTOR;
			createPersons("12071000", "12052000", commuters, MS_OUTSIDE);
			
			//SPN-LDS
			commuters = 408*SCALEFACTOR;
			createPersons("12071000", "12061000", commuters, MS_OUTSIDE);
			
			//SPN-LOS
			commuters = 466*SCALEFACTOR;
			createPersons("12071000", "12067000", commuters, MS_OUTSIDE);
			
			//SPN-SPN
			commuters = 22524*SCALEFACTOR;
			createPersons("12071000", "12071000", commuters, MS_OUTSIDE);
			
			System.out.println(this.shapeMap.keySet());
		
			PopulationWriter pw = new PopulationWriter(scenario.getPopulation(),scenario.getNetwork());
			pw.write(PLANSFILEOUTPUT);
		}
		
		/**
		 * sets all relevant coordinates and the leg mode and draws a random activityChain-double for each plan and then calls createOnePerson method 
		 * @param homeZone
		 * @param workZone
		 * @param commuters
		 * @param relativeAmountOfCarUses
		 */
		private void createPersons (String homeZone, String workZone, double commuters, double relativeAmountOfCarUses) {
			Geometry home = this.shapeMap.get(homeZone);
			Geometry work = this.shapeMap.get(workZone);
			for (int i = 0; i<= commuters;i++){
				String mode = "car";
				double carcommuters = commuters*relativeAmountOfCarUses;
				if (i>carcommuters) mode = "pt";
				
				// set coordinates of home and work activities to a randomly picked building
				Coord homec = this.setBuildingFromZone(home);
				Coord workc = this.setBuildingFromZone(work);
				
				double personalRandom = rnd.nextDouble();
				createOnePerson(i, homec, workc, mode, homeZone+ "_"+workZone+ "_", personalRandom);
			}
		}
		
		/**
		 * creates the person and its plan
		 * activityChain defines the activity chain of the plan according to this distribution:
		 * 60% home-work-home
		 * 10% home-KINDERGARTEN1-work-KINDERGARTEN2-home
		 * 30% home-work-home-SHOPPEN-home
		 * 
		 * @param i
		 * @param coord
		 * @param coordWork
		 * @param mode
		 * @param toFromPrefix
		 * @param activityChain
		 */
		private void createOnePerson(int i, Coord coord, Coord coordWork, String mode, String toFromPrefix, double activityChain) {
			Id<Person> personId = Id.createPersonId(toFromPrefix+i);
			
			// create the Person and Plan instances
			Person person = scenario.getPopulation().getFactory().createPerson(personId); 
			Plan plan = scenario.getPopulation().getFactory().createPlan(); 
			
			/* create all activities and legs of the plan 
			 * randomly distribute start and end times
			 */
			
			//60% plan: home-work-home
			Activity home = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
			double startTime = 7.5*60*60+(2*60*60*rnd.nextDouble()); //ZufallsStartZeit 7.30-9.30Uhr
			home.setEndTime(startTime);
			plan.addActivity(home); 

			Leg hinweg1 = scenario.getPopulation().getFactory().createLeg(mode);
			plan.addLeg(hinweg1); 

			//10% plan: home-KINDERGARTEN1-work-KINDERGARTEN2-home
			//kindergarten AM
			if ((activityChain>0.6)&&(activityChain<=0.7)){
				
				Activity kindergarten1 = scenario.getPopulation().getFactory().createActivityFromCoord("kindergarten1", this.findClosestCoordInMap(coord, kindergartens));
				kindergarten1.setEndTime(startTime+(0.4*60*60)+(0.1*60*60*rnd.nextDouble())); //dropOff-time 5-10min
				plan.addActivity(kindergarten1); 

				Leg hinweg2 = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(hinweg2); 			
			}		
			
			Activity work = scenario.getPopulation().getFactory().createActivityFromCoord("work", coordWork);
			double workEndTime = startTime+(7.5*60*60)+(1*60*60*rnd.nextDouble()); // working time 7.5-8.5 hours
			work.setEndTime(workEndTime);
			plan.addActivity(work); 

			Leg rueckweg1 = scenario.getPopulation().getFactory().createLeg(mode);
			plan.addLeg(rueckweg1); 
			
			//kindergarten PM
			if ((activityChain>0.6)&&(activityChain<=0.7)){
				
				Activity kindergarten2 = scenario.getPopulation().getFactory().createActivityFromCoord("kindergarten2", this.findClosestCoordInMap(coord, kindergartens));
				kindergarten2.setEndTime(workEndTime+(0.4*60*60)+(0.1*60*60*rnd.nextDouble())); //pickup-time 5-10min
				plan.addActivity(kindergarten2); 

				Leg rückweg2 = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(rückweg2);
			}		
			
			//30% plan: home-work-home-SHOPPEN-home
			if (activityChain >0.7){
				
				Activity home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
				double startShoppingTime = workEndTime+(1*60*60)+(0.5*60*60*rnd.nextDouble());
				home2.setEndTime(startShoppingTime);
				plan.addActivity(home2);
				
				Leg zumShoppen = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(zumShoppen); 		
				
				Activity shopping = scenario.getPopulation().getFactory().createActivityFromCoord("shopping", this.findClosestCoordInMap(coord, shops));
				shopping.setEndTime(startShoppingTime+(1*60*60)+(1*60*60*rnd.nextDouble())); //shopping 1-2 hours
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
			   Coord coord = new Coord(p.getX(), p.getY());
			   return coord;
		}
		

		/**
		 * read in a shape file and convert it into a map of geometries where
		 * keys are the values of the attribute defined by attrString
		 * @param filename
		 * @param attrString for counties: Nr
				 			for buildings: osm_id
		 * @return shapeMap
		 */
		public Map<String,Geometry> readShapeFile(String filename, String attrString){
			
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
		/**
		 * reads in a facility file and returns a map of the facilities to their coordinates
		 * @param fileName
		 * @return
		 */
		private Map<String,Coord> readFacilityLocations (String fileName){
			FacilityParser fp = new FacilityParser();
			TabularFileParserConfig config = new TabularFileParserConfig();
			config.setDelimiterRegex("\t");
			config.setCommentRegex("#");
			config.setFileName(fileName);
			new TabularFileParser().parse(config, fp);
			return fp.getFacilityMap();
		}
		
		/**
		 * finds the closest coordinate in facilityMap to the coordinate origin 
		 * @param facilityMap
		 * @param origin
		 * @return closest Coord
		 */
		private Coord findClosestCoordInMap(Coord origin, Map<String, Coord> facilityMap){
			Coord closest = null;
			double closestDistance = Double.MAX_VALUE;
			for (Coord coord : facilityMap.values()){
				double distance = CoordUtils.calcDistance(coord, origin);
				if (distance<closestDistance) {
					closestDistance = distance;
					closest = coord;
				}
			}
			return closest;
		}
		
		/**
		 * evaluates if a building(its coordinate) is within a geometry
		 * @param zone
		 * @param building
		 * @return
		 */
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
			
		/**
		 * returns the coordinate of a randomly picked building in zone
		 * @param zone
		 * @return
		 */
		private Coord setBuildingFromZone (Geometry zone){
			Coord coord ;
			do {
				Coord random = drawRandomPointFromGeometry(zone);
				coord = this.findClosestBuildingFromCoord(random);
			}
			while(!this.isBuildingInZone(zone, coord));
			return coord;
		}
				
}
	 

