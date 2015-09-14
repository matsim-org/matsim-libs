package playground.gthunig.vw_rufbus.demandGeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
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

public class VWRCreateDemand {

	private VWRConfig config;
	
	private Scenario scenario;
	private Map<String,Geometry> counties;
	
	private Map<String, Coord> commercial;
	private Map<String, Coord> industrial;
	private Map<String, Coord> residential;
	private Map<String, Coord> retail;
	private Map<String, Coord> schools;
	private Map<String, Coord> universities;
	
	private Id<Link> vwGate1LinkID = Id.createLinkId(43996);
	private Id<Link> vwGate2LinkID = Id.createLinkId(2611);
	private Id<Link> vwGate3LinkID = Id.createLinkId(42267);
	private Id<Link> vwGate4LinkID = Id.createLinkId(63449);
	private Id<Link> vwSourceLinkID = Id.createLinkId(32575);
	private Id<Link> vwDeliveryGateLinkID = Id.createLinkId(47900);
	private Id<Link> a2TruckerEastLinkID = Id.createLinkId(41090);
	private Id<Link> a2TruckerWestLinkID = Id.createLinkId(8511);
	
	private Random random = MatsimRandom.getRandom();
	
	private int commuterCounter = 0;
	private int vwWorkerCounter = 0;
	private int workerCounter = 0;

	public VWRCreateDemand (VWRConfig config){
		this.config = config;
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(scenario).readFile(config.getNetworkFileString());
		
		this.counties = readShapeFile(config.getCountiesFileString(), "RS");

		this.commercial = geometryMapToCoordMap(readShapeFile(config.getCommercialFileString(), "osm_id"));
		this.industrial = geometryMapToCoordMap(readShapeFile(config.getIndustrialFileString(), "osm_id"));
		this.residential = geometryMapToCoordMap(readShapeFile(config.getResidentialFileString(), "osm_id"));
		this.retail = geometryMapToCoordMap(readShapeFile(config.getRetailFileString(), "osm_id"));
		this.schools = geometryMapToCoordMap(readShapeFile(config.getSchoolsFileString(), "osm_id"));
		this.universities = geometryMapToCoordMap(readShapeFile(config.getUniversitiesFileString(), "osm_id"));
		
	}
	
	public static void main(String[] args) {

		String basedir = "C:/Users/Gabriel/Desktop/VSP/SVN VSP/shared-svn/projects/vw_rufbus/scenario/network/generation/";
		String network = "network.xml";
		String counties = "landkreise/landkreise.shp";
		String commercial = "landuse/commercial.shp";
		String industrial = "landuse/industrial.shp";
		String residential = "landuse/residential.shp";
		String retail = "landuse/retail.shp";
		String schools = "landuse/schools.shp";
		String universities = "landuse/universities.shp";
		String plansOutputComplete = basedir + "plans_output.xml";
		double scalefactor = 1.0;
		
		VWRConfig config = new VWRConfig(basedir, network, counties, commercial, industrial, 
											residential, retail, schools, universities, plansOutputComplete, scalefactor);
		
		VWRCreateDemand cd = new VWRCreateDemand(config);
		cd.run();
	}
	
	private void run() {
		
//		Braunschweig, Stadt			- BS
//		Wolfsburg, Stadt			- WB
//		Gifhorn						- GH
//		Wolfenbüttel				- WL
//		Helmstedt					- HS
//		Peine						- PE
//		Salzgitter, Stadt			- SG
//		Börde						- BR
//		Altmarkkreis Salzwedel		- AS
//		Region Hannover				- RH
//		Goslar						- GL
//		Harz						- HZ
//		Hildesheim					- HH
//		Magdeburg, Landeshauptstadt	- MB
//		Celle						- CL
//		Uelzen						- UL
//		Stendal						- SD
//		Göttingen					- GG
//		Salzlandkreis				- SL
//		Jerochower Land				- JL
//		Osnabrück, Stadt			- OB
//		Osterode am Harz			- OR
		
		
//		Braunschweig, Stadt - Braunschweig, Stadt | work
		createWorkers("BS", "BS", 62158*config.getScalefactor(), 0.45, 0.2, 0.2, "03101", "03101");
		
//		Braunschweig, Stadt - Braunschweig, Stadt | school
		createPupils("BS", "BS", 24907*config.getScalefactor(), 0.1, 0.13, 0.22, "03101", "03101");
		
//		Braunschweig, Stadt - Braunschweig, Stadt | university
		createStudents("BS", "BS", 29455*config.getScalefactor(), 0.1, 0.13, 0.22, "03101", "03101");
		
//		Wolfsburg, Stadt - Wolfsburg, Stadt | work
		createWorkers("WB", "WB", 41470*config.getScalefactor(), 0.55, 0.13, 0.22, "03103", "03103");
	
//		Wolfsburg, Stadt - Wolfsburg, Stadt | school
		createPupils("WB", "WB", 13867*config.getScalefactor(), 0.2, 0.3, 0.2, "03103", "03103");
		
//		Wolfsburg, Stadt - Wolfsburg, Stadt | university
		createStudents("WB", "WB", 8823*config.getScalefactor(), 0.2, 0.3, 0.2, "03103", "03103");
		
//		Gifhorn - Wolfsburg, Stadt | work
		createWorkers("GH", "WB", 26484*config.getScalefactor(), 0.8, 0.0, 0.0, "03151", "03103");
		
//		Gifhorn - Gifhorn | work
		createWorkers("GH", "GH", 26414*config.getScalefactor(), 0.63, 0.15, 0.15, "03151", "03151");
		
//		Gifhorn - Gifhorn | school
		createPupils("GH", "GH", 26429*config.getScalefactor(), 0.2, 0.3, 0.2, "03151", "03151");
		
//		Wolfenbüttel - Braunschweig, Stadt | work
		createWorkers("WL", "BS", 13304*config.getScalefactor(), 0.8, 0.0, 0.0, "03158", "03101");
		
//		Helmstedt - Wolfsburg, Stadt | work
		createWorkers("HS", "WB", 12731*config.getScalefactor(), 0.8, 0.0, 0.0, "03154", "03103");
		
//		Braunschweig, Stadt - Wolfsburg, Stadt | work
		createWorkers("BS", "WB", 10273*config.getScalefactor(), 0.45, 0.21, 0.2, "03101", "03103");
		
//		Peine - Braunschweig, Stadt | work
		createWorkers("PE", "BS", 9089*config.getScalefactor(), 0.8, 0.0, 0.0, "03157", "03101");
		
//		Gifhorn - Braunschweig, Stadt | work
		createWorkers("GH", "BS", 7586*config.getScalefactor(), 0.8, 0.0, 0.0, "03151", "03101");
		
//		Salzgitter, Stadt - Braunschweig, Stadt | work
		createWorkers("SG", "BS", 5572*config.getScalefactor(), 0.8, 0.0, 0.0, "03102", "03101");
		
//		Helmstedt - Braunschweig, Stadt | work
		createWorkers("HS", "BS", 4618*config.getScalefactor(), 0.8, 0.0, 0.0, "03154", "03101");
		
//		Börde - Wolfsburg, Stadt | work
		createWorkers("BR", "WB", 3685*config.getScalefactor(), 0.8, 0.0, 0.0, "15085", "03103");
		
//		Altmarkkreis Salzwedel - Wolfsburg, Stadt | work
		createWorkers("AS", "WB", 3305*config.getScalefactor(), 0.8, 0.0, 0.0, "15081", "03103");
		
//		Region Hannover - Wolfsburg, Stadt | work
		createWorkers("RH", "WB", 2850*config.getScalefactor(), 0.8, 0.0, 0.0, "03241", "03103");
		
//		Region Hannover - Braunschweig, Stadt | work
		createWorkers("RH", "BS", 2833*config.getScalefactor(), 0.8, 0.0, 0.0, "03241", "03101");
		
//		Wolfenbüttel - Wolfsburg, Stadt | work
		createWorkers("WL", "WB", 2676*config.getScalefactor(), 0.8, 0.0, 0.0, "03158", "03103");
		
//		Braunschweig, Stadt - Gifhorn | work
		createWorkers("BS", "GH", 2635*config.getScalefactor(), 0.8, 0.0, 0.0, "03101", "03151");
		
//		Braunschweig, Stadt - Wolfburg, Stadt | work
		createWorkers("BS", "WB", 2310*config.getScalefactor(), 0.8, 0.0, 0.0, "03101", "03103");
		
//		Braunschweig, Stadt - Goslar | work
		createWorkers("BS", "GL", 1960*config.getScalefactor(), 0.8, 0.0, 0.0, "03101", "03153");
		
//		Wolfsburg, Stadt - Gifhorn | work
		createWorkers("WB", "GH", 1844*config.getScalefactor(), 0.8, 0.0, 0.0, "03103", "03151");
		
//		Peine - Wolfburg, Stadt | work
		createWorkers("PE", "WB", 1756*config.getScalefactor(), 0.8, 0.0, 0.0, "03157", "03103");
		
//		Harz - Braunschweig, Stadt | work
		createWorkers("HZ", "BS", 1685*config.getScalefactor(), 0.8, 0.0, 0.0, "15085", "03101");

//		Altmarkkreis Salzwedel - Gifhorn | work
		createWorkers("AS", "GH", 1649*config.getScalefactor(), 0.8, 0.0, 0.0, "15081", "03151");

//		Börde - Braunschweig, Stadt | work
		createWorkers("BR", "BS", 1260*config.getScalefactor(), 0.8, 0.0, 0.0, "15085", "03101");

//		Hildesheim - Braunschweig, Stadt | work
		createWorkers("HH", "BS", 1195*config.getScalefactor(), 0.8, 0.0, 0.0, "03254", "03101");

//		Salzgitter, Stadt - Wolfburg, Stadt | work
		createWorkers("SG", "WB", 860*config.getScalefactor(), 0.8, 0.0, 0.0, "03102", "03103");
		
//		Magdeburg, Landeshauptstadt - Wolfburg, Stadt | work
		createWorkers("MB", "WB", 847*config.getScalefactor(), 0.8, 0.0, 0.0, "15003", "03103");
		
//		Peine - Gifhorn | work
		createWorkers("PE", "GH", 834*config.getScalefactor(), 0.8, 0.0, 0.0, "03157", "03151");
		
//		Celle - Gifhorn | work
		createWorkers("CL", "GH", 815*config.getScalefactor(), 0.8, 0.0, 0.0, "03351", "03151");
		
//		Helmstedt - Gifhorn | work
		createWorkers("HS", "GH", 735*config.getScalefactor(), 0.8, 0.0, 0.0, "03154", "03151");
		
//		Region Hannover - Gifhorn | work
		createWorkers("RH", "GH", 680*config.getScalefactor(), 0.8, 0.0, 0.0, "03241", "03151");
		
//		Celle - Wolfburg, Stadt | work
		createWorkers("CL", "WB", 607*config.getScalefactor(), 0.8, 0.0, 0.0, "03351", "03103");
		
//		Magdeburg, Landeshauptstadt - Braunschweig, Stadt | work
		createWorkers("MB", "BS", 511*config.getScalefactor(), 0.8, 0.0, 0.0, "15003", "03101");
		
//		Wolfenbüttel - Gifhorn | work
		createWorkers("WB", "GH", 496*config.getScalefactor(), 0.8, 0.0, 0.0, "03158", "03151");
		
//		Harz - Wolfburg, Stadt | work
		createWorkers("HZ", "WB", 476*config.getScalefactor(), 0.8, 0.0, 0.0, "15085", "03103");
		
//		Uelzen - Gifhorn | work
		createWorkers("UL", "GH", 474*config.getScalefactor(), 0.8, 0.0, 0.0, "03360", "03151");
		
//		Stendal - Wolfburg, Stadt | work
		createWorkers("SD", "WB", 456*config.getScalefactor(), 0.8, 0.0, 0.0, "15090", "03103");
		
//		Celle - Braunschweig, Stadt | work
		createWorkers("CL", "BS", 440*config.getScalefactor(), 0.8, 0.0, 0.0, "03351", "03101");
		
//		Börde - Gifhorn | work
		createWorkers("BR", "GH", 400*config.getScalefactor(), 0.8, 0.0, 0.0, "15083", "03151");
		
//		Hildesheim - Wolfburg, Stadt | work
		createWorkers("HH", "WB", 391*config.getScalefactor(), 0.8, 0.0, 0.0, "03254", "03103");
		
//		Goslar - Wolfburg, Stadt | work
		createWorkers("GL", "WB", 384*config.getScalefactor(), 0.8, 0.0, 0.0, "03153", "03103");
		
//		Göttingen - Braunschweig, Stadt | work
		createWorkers("GG", "BS", 369*config.getScalefactor(), 0.8, 0.0, 0.0, "03152", "03101");
		
//		Uelzen - Wolfburg, Stadt | work
		createWorkers("UL", "WB", 338*config.getScalefactor(), 0.8, 0.0, 0.0, "03360", "03103");
		
//		Altmarkkreis Salzwedel - Braunschweig, Stadt | work
		createWorkers("AS", "BS", 245*config.getScalefactor(), 0.8, 0.0, 0.0, "15081", "03101");
		
//		Salzlandkreis - Wolfburg, Stadt | work
		createWorkers("SL", "WB", 214*config.getScalefactor(), 0.8, 0.0, 0.0, "15089", "03103");
		
//		Salzlandkreis - Braunschweig, Stadt | work
		createWorkers("SL", "BS", 185*config.getScalefactor(), 0.8, 0.0, 0.0, "15089", "03101");
		
//		Salzgitter, Stadt - Gifhorn | work
		createWorkers("SG", "GH", 174*config.getScalefactor(), 0.8, 0.0, 0.0, "03102", "03151");
		
//		Jerichower Land - Wolfburg, Stadt | work
		createWorkers("JL", "WB", 170*config.getScalefactor(), 0.8, 0.0, 0.0, "15086", "03103");
		
//		Harz - Gifhorn | work
		createWorkers("HZ", "GH", 148*config.getScalefactor(), 0.8, 0.0, 0.0, "15085", "03151");

//		Osnabrück, Stadt - Wolfburg, Stadt | work
		createWorkers("OB", "WB", 136*config.getScalefactor(), 0.8, 0.0, 0.0, "03404", "03103");
		
//		Osterode am Harz - Braunschweig, Stadt | work
		createWorkers("OR", "BS", 129*config.getScalefactor(), 0.8, 0.0, 0.0, "03156", "03101");
		
//		Jerichower Land - Braunschweig, Stadt | work
		createWorkers("JL", "BS", 117*config.getScalefactor(), 0.8, 0.0, 0.0, "15086", "03101");
		
//		Magdeburg, Landeshauptstadt - Gifhorn | work
		createWorkers("MB", "GH", 114*config.getScalefactor(), 0.8, 0.0, 0.0, "15003", "03151");
		
//		Uelzen - Braunschweig, Stadt | work
		createWorkers("UL", "BS", 110*config.getScalefactor(), 0.8, 0.0, 0.0, "03360", "03101");
		
//		Stendal - Braunschweig, Stadt | work
		createWorkers("SD", "BS", 108*config.getScalefactor(), 0.8, 0.0, 0.0, "15090", "03101");
		
//		Goslar - Gifhorn | work
		createWorkers("GL", "GH", 101*config.getScalefactor(), 0.8, 0.0, 0.0, "03153", "03151");
		
		
		createA2TransitTruckers(10000);
		createVWTruckers(600);
		
		System.out.println("generated Agents: " + commuterCounter);
		System.out.println("VW Workers: " + vwWorkerCounter);
		System.out.println("Workers: " + workerCounter);
		PopulationWriter pw = new PopulationWriter(scenario.getPopulation(),scenario.getNetwork());
		pw.write(config.getPlansOutputString());
	}
	
	private void createWorkers(String from, String to, double commuters, double carcommuterFactor, double bikecommuterFactor, 
								double walkcommuterFactor, String origin, String destination) {
		Geometry homeCounty = this.counties.get(origin);
		Geometry commuteDestinationCounty = this.counties.get(destination);
		
		double walkcommuters = commuters * walkcommuterFactor;
		double bikecommuters = commuters * bikecommuterFactor;
		double carcommuters = commuters * carcommuterFactor;
		
		for (int i = 0; i <= commuters; i++){
			String mode = "car";
			if (i < carcommuters) {
				mode = "bike";
				if (i > bikecommuters + carcommuters) {
					mode = "walk";
					if (i > walkcommuters + bikecommuters + carcommuters) {
						mode = "pt";
					}
				}
			}
			
			Coord homeC = findClosestCoordFromMap(drawRandomPointFromGeometry(homeCounty), this.residential);
			
			double wvWorkerOrNot = random.nextDouble();
			if (to == "WB" && wvWorkerOrNot < 0.6363) {
				vwWorkerCounter++;
				double shiftOrFlexitime = random.nextDouble();
				if (shiftOrFlexitime < 0.6666) {
					createOneVWFlexitimeWorker(commuterCounter, homeC, mode, from + "_" + to);
				} else {
					createOneVWShiftWorker(commuterCounter, homeC, mode, from + "_" + to);
				}
			} else {
				List<Map<String, Coord>> coordMaps = new ArrayList<Map<String, Coord>>();
				coordMaps.add(commercial);
				coordMaps.add(industrial);
				if (from.equals(to)) coordMaps.add(retail);
				Coord commuteDestinationC = findClosestCoordFromMap(drawRandomPointFromGeometry(commuteDestinationCounty), coordMaps);
				
				createOneWorker(commuterCounter, homeC, commuteDestinationC, mode, from + "_" + to);
			}
			workerCounter++;
			commuterCounter++;
		}
	}
	
	private void createA2TransitTruckers(int commuters) {
		
		for (int i = 0; i <= commuters; i++){
//			TODO mode="truck"?
			String mode = "car";
		
			Coord origin = this.scenario.getNetwork().getLinks().get(this.a2TruckerEastLinkID).getCoord();
			Coord destination = this.scenario.getNetwork().getLinks().get(this.a2TruckerWestLinkID).getCoord();
			
			if (random.nextBoolean()) {
				createOneTransitTrucker(i, origin, destination, mode, "eastA2west");
			} else {
				createOneTransitTrucker(i, destination, origin, mode, "westA2east");
			}
		}
	}
	
	private void createVWTruckers(int commuters) {
		
		for (int i = 0; i <= commuters; i++){
//			TODO mode="truck"?
			String mode = "car";
		
			Coord origin = this.scenario.getNetwork().getLinks().get(this.vwSourceLinkID).getCoord();
			Coord destination = this.scenario.getNetwork().getLinks().get(this.vwDeliveryGateLinkID).getCoord();
			
			createOneVWTrucker(i, origin, destination, mode, "BS_VW");
		}
	}
	
	private void createPupils(String from, String to, double commuters, double carcommuterFactor, double bikecommuterFactor, 
			double walkcommuterFactor, String origin, String destination) {
		Geometry homeCounty = this.counties.get(origin);
		Geometry commuteDestinationCounty = this.counties.get(destination);
		
		double walkcommuters = commuters * walkcommuterFactor;
		double bikecommuters = commuters * bikecommuterFactor;
		double carcommuters = commuters * carcommuterFactor;
		
		for (int i = 0; i <= commuters; i++){
			String mode = "car";
			if (i < carcommuters) {
				mode = "bike";
				if (i > bikecommuters + carcommuters) {
					mode = "walk";
					if (i > walkcommuters + bikecommuters + carcommuters) {
						mode = "pt";
					}
				}
			}
			
			Coord homeC = findClosestCoordFromMap(drawRandomPointFromGeometry(homeCounty), this.residential);
			
			Coord commuteDestinationC = findClosestCoordFromMap(drawRandomPointFromGeometry(commuteDestinationCounty), this.schools);
				
			createOnePupil(commuterCounter, homeC, commuteDestinationC, mode, from + "_" + to);
			commuterCounter++;
		}
	}
	
	private void createStudents(String from, String to, double commuters, double carcommuterFactor, double bikecommuterFactor, 
			double walkcommuterFactor, String origin, String destination) {
		Geometry homeCounty = this.counties.get(origin);
		Geometry commuteDestinationCounty = this.counties.get(destination);
		
		double walkcommuters = commuters * walkcommuterFactor;
		double bikecommuters = commuters * bikecommuterFactor;
		double carcommuters = commuters * carcommuterFactor;
		
		for (int i = 0; i <= commuters; i++){
			String mode = "car";
			if (i < carcommuters) {
				mode = "bike";
				if (i > bikecommuters + carcommuters) {
					mode = "walk";
					if (i > walkcommuters + bikecommuters + carcommuters) {
						mode = "pt";
					}
				}
			}
			
			Coord homeC = findClosestCoordFromMap(drawRandomPointFromGeometry(homeCounty), this.residential);
			
			Coord commuteDestinationC = findClosestCoordFromMap(drawRandomPointFromGeometry(commuteDestinationCounty), this.universities);
				
			createOneStudent(commuterCounter, homeC, commuteDestinationC, mode, from + "_" + to);
			commuterCounter++;
		}
	}
	
	private void createOneVWFlexitimeWorker(int i, Coord homeC, String mode, String fromToPrefix) {
		
		Id<Person> personId = Id.createPersonId(fromToPrefix + i + "vw");
		Person person = scenario.getPopulation().getFactory().createPerson(personId);
 
		Plan plan = scenario.getPopulation().getFactory().createPlan();
 
		Activity home = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
		home.setEndTime(5*60*60+30*60);
		plan.addActivity(home);
 
		Leg outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(outboundTrip);
 
		Activity work = scenario.getPopulation().getFactory().createActivityFromCoord("work_vw_flexitime", calcVWWorkCoord());
		work.setMaximumDuration(10*60*60+10*60);
		plan.addActivity(work);
 
		Leg returnTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(returnTrip);
 
		Activity home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
		plan.addActivity(home2);
 
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}
	
	private void createOneVWShiftWorker(int i, Coord homeC, String mode, String fromToPrefix) {
		
		Id<Person> personId = Id.createPersonId(fromToPrefix + i + "vw");
		Person person = scenario.getPopulation().getFactory().createPerson(personId);
 
		Plan plan = scenario.getPopulation().getFactory().createPlan();
		
		
		
		int rand = random.nextInt(3)+1;
		switch (rand) {
			case 1:
				Activity home = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
				home.setEndTime(5*60*60);
				plan.addActivity(home);
				
				Leg outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(outboundTrip);
				
				Activity shift1 = scenario.getPopulation().getFactory().createActivityFromCoord("work_vw_shift1", calcVWWorkCoord());
				shift1.setEndTime(13*60*60+40*60);
				plan.addActivity(shift1);
				
				Leg returnTrip = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(returnTrip);
		 
				Activity home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
				plan.addActivity(home2);
				break;
			case 2:
				home = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
				home.setEndTime(13*60*60);
				plan.addActivity(home);
				
				outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(outboundTrip);
				
				Activity shift2 = scenario.getPopulation().getFactory().createActivityFromCoord("work_vw_shift2", calcVWWorkCoord());
				shift2.setEndTime(21*60*60+40*60);
				plan.addActivity(shift2);
				
				returnTrip = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(returnTrip);
		 
				home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
				plan.addActivity(home2);
				break;
			case 3: 
				Activity shift3 = scenario.getPopulation().getFactory().createActivityFromCoord("work_vw_shift3", calcVWWorkCoord());
				shift3.setEndTime(5*60*60+40*60);
				plan.addActivity(shift3);
				
				outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(outboundTrip);
				
				home = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
				home.setEndTime(21*60*60);
				plan.addActivity(home);
				
				outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
				plan.addLeg(outboundTrip);
				
				shift3.setEndTime(0);
				plan.addActivity(shift3);
				break;
		}
 
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}
	
	private Coord calcVWWorkCoord() {
		
		int rand = random.nextInt(4)+1;
		switch (rand) {
			case 1:
				return this.scenario.getNetwork().getLinks().get(this.vwGate1LinkID).getCoord();
			case 2:
				return this.scenario.getNetwork().getLinks().get(this.vwGate2LinkID).getCoord();
			case 3:
				return this.scenario.getNetwork().getLinks().get(this.vwGate3LinkID).getCoord();
			case 4:
				return this.scenario.getNetwork().getLinks().get(this.vwGate4LinkID).getCoord();
		}
		return this.scenario.getNetwork().getLinks().get(this.vwGate1LinkID).getCoord();
	}

	private void createOneWorker(int i, Coord homeC, Coord coordWork, String mode, String fromToPrefix) {
		Id<Person> personId = Id.createPersonId(fromToPrefix + i);
		Person person = scenario.getPopulation().getFactory().createPerson(personId);
 
		Plan plan = scenario.getPopulation().getFactory().createPlan();
 
 
		Activity home = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
		home.setEndTime(6*60*60);
		plan.addActivity(home);
 
		Leg outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(outboundTrip);
		
		int rand = random.nextInt(121);
		Activity work = scenario.getPopulation().getFactory().createActivityFromCoord("work", coordWork);
		work.setEndTime(16*60*60 + rand*60);
		plan.addActivity(work);
		
		Leg returnTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(returnTrip);
 
		Activity home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
		plan.addActivity(home2);
 
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}
	
	private void createOnePupil(int i, Coord coord, Coord coordSchool, String mode, String fromToPrefix) {
		Id<Person> personId = Id.createPersonId(fromToPrefix + i);
		Person person = scenario.getPopulation().getFactory().createPerson(personId);
 
		Plan plan = scenario.getPopulation().getFactory().createPlan();
 
		
		Activity home = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
		home.setEndTime(7*60*60);
		plan.addActivity(home);
 
		Leg outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(outboundTrip);
 
		Activity school = scenario.getPopulation().getFactory().createActivityFromCoord("school", coordSchool);
		school.setEndTime(15*60*60);
		plan.addActivity(school);
 
		Leg returnTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(returnTrip);
 
		Activity home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
		plan.addActivity(home2);
 
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}
	
	private void createOneStudent(int i, Coord coord, Coord coordUniversity, String mode, String fromToPrefix) {
		Id<Person> personId = Id.createPersonId(fromToPrefix+i);
		Person person = scenario.getPopulation().getFactory().createPerson(personId);
 
		Plan plan = scenario.getPopulation().getFactory().createPlan();
 
		
		Activity home = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
		home.setEndTime(8*60*60);
		plan.addActivity(home);
 
		Leg outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(outboundTrip);
 
		Activity university = scenario.getPopulation().getFactory().createActivityFromCoord("university", coordUniversity);
		university.setEndTime(18*60*60);
		plan.addActivity(university);
 
		Leg returnTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(returnTrip);
 
		Activity home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
		plan.addActivity(home2);
 
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}
	
	private void createOneVWTrucker(int i, Coord origin, Coord destination, String mode, String fromToPrefix) {
		Id<Person> personId = Id.createPersonId(fromToPrefix+i);
		Person person = scenario.getPopulation().getFactory().createPerson(personId);
 
		Plan plan = scenario.getPopulation().getFactory().createPlan();
 
		Activity source = scenario.getPopulation().getFactory().createActivityFromCoord("source", origin);
		double rand = random.nextDouble()*20*60*60;
		source.setEndTime(rand);
		plan.addActivity(source);
 
		Leg outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(outboundTrip);
 
		Activity delivery = scenario.getPopulation().getFactory().createActivityFromCoord("delivery", destination);
		delivery.setEndTime(rand + 2*60*60);
		plan.addActivity(delivery);
		
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}
	
	private void createOneTransitTrucker(int i, Coord origin, Coord destination, String mode, String fromToPrefix) {
		Id<Person> personId = Id.createPersonId(fromToPrefix+i);
		Person person = scenario.getPopulation().getFactory().createPerson(personId);
 
		Plan plan = scenario.getPopulation().getFactory().createPlan();
 
		Activity cargo = scenario.getPopulation().getFactory().createActivityFromCoord("cargo", origin);
		int rand = random.nextInt(24*60*60)+1;
		cargo.setEndTime(rand);
		plan.addActivity(cargo);
 
		Leg outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(outboundTrip);
 
		cargo = scenario.getPopulation().getFactory().createActivityFromCoord("cargo", destination);
		plan.addActivity(cargo);
		
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}
	
	private  Coord drawRandomPointFromGeometry(Geometry g) {
		Point p;
		   double x, y;
		   do {
		      x = g.getEnvelopeInternal().getMinX() +  random.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
		      y = g.getEnvelopeInternal().getMinY() + random.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
		      p = MGC.xy2Point(x, y);
		   } while (!g.contains(p));
		   Coord coord = new Coord(p.getX(), p.getY());
		   return coord;
		}
	
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
	
	private Coord findClosestCoordFromMap(Coord location, Map<String,Coord> coordMap){
		Coord closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Coord coord : coordMap.values()){
			double distance = CoordUtils.calcDistance(coord, location);
			if (distance<closestDistance) {
				closestDistance = distance;
				closest = coord;
			}
		}
		return closest;
	}
	
	private Coord findClosestCoordFromMap(Coord location, List<Map<String,Coord>> coordMaps){
		Coord closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Map<String,Coord> coordMap : coordMaps) {
			for (Coord coord : coordMap.values()){
				double distance = CoordUtils.calcDistance(coord, location);
				if (distance<closestDistance) {
					closestDistance = distance;
					closest = coord;
				}
			}
		}
			
		return closest;
	}
	
	private Map<String,Coord> geometryMapToCoordMap(Map<String,Geometry> geometries) {
		Map<String,Coord> coords = new HashMap<String,Coord>();
		for (Map.Entry<String,Geometry> entry : geometries.entrySet()) {
			Coord coord = new Coord(entry.getValue().getCoordinate().x, entry.getValue().getCoordinate().y);
			coords.put(entry.getKey(), coord);
		}
		return coords;
	}
	
}

	class FacilityParser implements TabularFileHandler{
			
		private Map<String,Coord> facilityMap = new HashMap<String, Coord>();	
		CoordinateTransformation ct = new GeotoolsTransformation("EPSG:4326", "EPSG:32633");
		
		@Override
		public void startRow(String[] row) {
			try{
				System.out.println("row length: " + row.length);
				System.out.println(row[0]);
				Double x = Double.parseDouble(row[2]);
				Double y = Double.parseDouble(row[1]);
				Coord coords = new Coord(x,y);
				this.facilityMap.put(row[0],ct.transform(coords));
//			} catch (NumberFormatException e){
			} catch (Exception e){
				//skips line
			}
		}
	
		public Map<String, Coord> getFacilityMap() {
			return facilityMap;
		}
		
	}
