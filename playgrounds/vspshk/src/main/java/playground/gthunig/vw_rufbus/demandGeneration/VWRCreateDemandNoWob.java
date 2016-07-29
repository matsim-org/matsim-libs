package playground.gthunig.vw_rufbus.demandGeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.util.random.WeightedRandomSelection;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class VWRCreateDemandNoWob {

	private VWRConfig config;

	private Scenario scenario;
	private Map<String, Geometry> counties;

	private Map<String, Coord> commercial;
	private Map<String, Coord> industrial;
	private Map<String, Coord> residential;
	private Map<String, Coord> retail;
	private Map<String, Coord> schools;
	private Map<String, Coord> universities;
	private Map<String, Geometry> bs;
	private Map<String, Geometry> wb;
    private final WeightedRandomSelection<String> wrs;


	private Id<Link> vwGateFE1LinkID = Id.createLinkId("vw2");
	private Id<Link> vwGateFE2LinkID = Id.createLinkId("vw222");
	private Id<Link> vwGateNHSLinkID = Id.createLinkId("vw10");
	private Id<Link> vwGateWestLinkID = Id.createLinkId("vw7");
	private Id<Link> vwGateEastID = Id.createLinkId("vw14");
	private Id<Link> vwGateNorthID = Id.createLinkId("vwno");
	private Id<Link> vwGateNorthITVID = Id.createLinkId("46193");
	
	
	private Id<Link> vwSourceLinkID = Id.createLinkId(32575);
	private Id<Link> vwDeliveryGateLinkID = Id.createLinkId("vw24");
	private Id<Link> a2TruckerEastLinkID = Id.createLinkId(38773);
	private Id<Link> a2TruckerEastLinkIDEnd = Id.createLinkId(57868);
	private Id<Link> a2TruckerWestLinkID = Id.createLinkId(10799);
	private Id<Link> a2TruckerWestLinkIDEnd = Id.createLinkId(10865);
	private Set<Id<Person>> teleportPtUsers = new HashSet<>();
	private Random random = MatsimRandom.getRandom();

	private int commuterCounter = 0;
	private int vwWorkerCounter = 0;
	private int workerCounter = 0;

	public VWRCreateDemandNoWob(VWRConfig config) {
		this.config = config;
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.getNetworkFileString());

		this.counties = readShapeFile(config.getCountiesFileString(), "RS");

		this.commercial = geometryMapToCoordMap(readShapeFile(config.getCommercialFileString(), "osm_id"));
		this.industrial = geometryMapToCoordMap(readShapeFile(config.getIndustrialFileString(), "osm_id"));
		this.residential = geometryMapToCoordMap(readShapeFile(config.getResidentialFileString(), "osm_id"));
		this.retail = geometryMapToCoordMap(readShapeFile(config.getRetailFileString(), "osm_id"));
		this.schools = geometryMapToCoordMap(readShapeFile(config.getSchoolsFileString(), "osm_id"));
		this.universities = geometryMapToCoordMap(readShapeFile(config.getUniversitiesFileString(), "osm_id"));
		this.bs = readShapeFile(config.getBs(), "bez");
		this.wb = readShapeFile(config.getWb(), "bez");
		this.wrs = new WeightedRandomSelection<>();
        readPopulationData();

		
		
		
		

	}

	public static void main(String[] args) {

		// String basedir = "C:/Users/Gabriel/Desktop/VSP/SVN
		// VSP/shared-svn/projects/vw_rufbus/scenario/network/generation/";
		String basedir = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/generation/";
		String network = "../versions/network_nopt.xml";
		String counties = "landkreise/landkreise.shp";
		String commercial = "landuse/commercial.shp";
		String industrial = "landuse/industrial.shp";
		String residential = "landuse/residential.shp";
		String retail = "landuse/retail.shp";
		String schools = "landuse/schools.shp";
		String universities = "landuse/universities.shp";
		String bs = "zones/bs.shp";
		String bsb = "zones/bev_bs.txt";
		
		String wb = "zones/wb.shp";
//		double scalefactor = 0.05;
		double scalefactor = 0.1;
//		double scalefactor = 1.0;
//		double scalefactor = 0.01;
		String plansOutputComplete = basedir + "../../input/initial_plans_bsonly"+scalefactor+".xml.gz";
		String objectAttributesFile = basedir + "../../input/initial_plans_bsonly_oA"+scalefactor+".xml.gz";
		String transitSchedule = basedir+"../../input/transitschedule.xml";
		VWRConfig config = new VWRConfig(basedir, network, counties, commercial, industrial, residential, retail,
				schools, universities, plansOutputComplete, scalefactor,transitSchedule,objectAttributesFile,bs,bsb,wb);

		VWRCreateDemandNoWob cd = new VWRCreateDemandNoWob(config);
		cd.run();
	}

	private void run() {

		// Braunschweig, Stadt - BS
		// Wolfsburg, Stadt - WB
		// Gifhorn - GH
		// Wolfenbüttel - WL
		// Helmstedt - HS
		// Peine - PE
		// Salzgitter, Stadt - SG 
		// Börde - BR
		// Altmarkkreis Salzwedel - AS
		// Region Hannover - RH
		// Goslar - GL
		// Harz - HZ
		// Hildesheim - HH
		// Magdeburg, Landeshauptstadt - MB
		// Celle - CL
		// Uelzen - UL
		// Stendal - SD
		// Göttingen - GG
		// Salzlandkreis - SL
		// Jerochower Land - JL
		// Osnabrück, Stadt - OB
		// Osterode am Harz - OR

		// Braunschweig, Stadt - Braunschweig, Stadt | work
		createWorkers("BS", "BS", 62158 * config.getScalefactor(), 0.6, 0.15, 0.15, "03101", "03101");

		// Braunschweig, Stadt - Braunschweig, Stadt | school
		createPupils("BS", "BS", 40000 * config.getScalefactor(), 0.25, 0.25, 0.25, "03101", "03101");

		// Braunschweig, Stadt - Braunschweig, Stadt | university
		createStudents("BS", "BS", 15000 * config.getScalefactor(), 0.45, 0.2, 0.2, "03101", "03101");

		// Wolfsburg, Stadt - Wolfsburg, Stadt | work
//		createWorkers("WB", "WB", (41470-18313) * config.getScalefactor(), 0.55, 0.2, 0.12, "03103", "03103");
//		createVWWorkers("WB", "03103", 11207, 1260+5328, 658, 0.55, 0.2, 0.12);
		//		3103	11207	1260	5328	518	18313
//		
//		createWorkers("WB", "BS", 2310 * config.getScalefactor(), 0.8, 0.2, 0.0, "03103", "03101");
//
//		
//		// Wolfsburg, Stadt - Wolfsburg, Stadt | school
//		createPupils("WB", "WB", 13867 * config.getScalefactor(), 0.05, 0.13, 0.27, "03103", "03103");
//
//	
//		// Gifhorn - Wolfsburg, Stadt | work
//		createWorkers("GH", "WB", (26484-15152-3000) * config.getScalefactor(), 0.65, 0.0, 0.0, "03151", "03103");
//		createVWWorkers("GH", "03151", 9156, 1050+4758, 518, 0.65, 0.0, 0.0);
//		//3151	9156	1050	4758	188	15152
//
//		// Gifhorn - Gifhorn | work
////		createWorkers("GH", "GH", 10000 * config.getScalefactor(), 0.65, 0.1, 0.1, "03151", "03151");
//
//		// Gifhorn - Gifhorn | school
////		createPupils("GH", "GH", 15000 * config.getScalefactor(), 0.05, 0.3, 0.2, "03151", "03151");
//
//		// Wolfenbüttel - Braunschweig, Stadt | work
//		createWorkers("WL", "BS", 6907 * config.getScalefactor(), 0.85, 0.0, 0.0, "03158", "03101");
//
//		// Helmstedt - Wolfsburg, Stadt | work
////		createWorkers("HS", "WB", (12731-7908) * config.getScalefactor(), 0.65, 0.0, 0.0, "03154", "03103");
////		createVWWorkers("HS", "03154", 4704, 536+2668, 188, 0.65, 0.0, 0.0);
////		3154	4704	536	2668	0	7908
//
//		
//		// Braunschweig, Stadt - Wolfsburg, Stadt | work
//		createWorkers("BS", "WB", (10273-5006) * config.getScalefactor(), 0.65, 0.01, 0.0, "03101", "03103");
//		createVWWorkers("BS", "03101", 4085, 127+693, 101, 0.65,0.01,0.0);
//		//3101	4085	127	693	101	8107
//
//		
////		// Peine - Braunschweig, Stadt | work
//		createWorkers("PE", "BS", 9089 * config.getScalefactor(), 0.85, 0.0, 0.0, "03157", "03101");
//
//		// Gifhorn - Braunschweig, Stadt | work
//		createWorkers("GH", "BS", 7586 * config.getScalefactor(), 0.65, 0.0, 0.0, "03151", "03101");
//
//		// Salzgitter, Stadt - Braunschweig, Stadt | work
//		createWorkers("SG", "BS", 5572 * config.getScalefactor(), 0.65, 0.0, 0.0, "03102", "03101");
//
////		// Helmstedt - Braunschweig, Stadt | work
//		createWorkers("HS", "BS", 4618 * config.getScalefactor(), 0.85, 0.0, 0.0, "03154", "03101");
//
//		// Börde - Wolfsburg, Stadt | work
////		createWorkers("BR", "WB", (3685-856) * config.getScalefactor(), 0.65, 0.0, 0.0, "15085", "03103");
////		createVWWorkers("BR", "15085", 396, 460, 0, 0.65, 0.0, 0.0);
//		//		15362	396	0	460	0	856
//
//		// Altmarkkreis Salzwedel - Wolfsburg, Stadt | work
//		createWorkers("AS", "WB", (3305-740) * config.getScalefactor(), 0.65, 0.0, 0.0, "15081", "03103");
//		createVWWorkers("AS", "15081", 290, 450, 0, .65, .0, .0);
//		//		15370	290	0	450	0	740
//
//		// Region Hannover - Wolfsburg, Stadt | work
////		createWorkers("RH", "WB", (2850-1148) * config.getScalefactor(), 0.65, 0.0, 0.0, "03241", "03103");
////		createVWWorkers("RH", "03241", 1004, 144, 0, 0.65, 0.0, .0);
////		3241	1004	0	144	0	1148
//
//		
//		// Region Hannover - Braunschweig, Stadt | work
//		createWorkers("RH", "BS", 2833 * config.getScalefactor(), 0.8, 0.0, 0.0, "03241", "03101");
//
//		// Wolfenbüttel - Wolfsburg, Stadt | work
//		createWorkers("WL", "WB", (2676-1191) * config.getScalefactor(), 0.65, 0.0, 0.0, "03158", "03103");
//		createVWWorkers("WL", "03158", 943, 248, 0 , .65, .0, .0);
////		3158	943	0	248	0	1191
//
//		// Braunschweig, Stadt - Gifhorn | work
//		createWorkers("BS", "GH", 2635 * config.getScalefactor(), 0.65, 0.0, 0.0, "03101", "03151");
//
//
//		// Braunschweig, Stadt - Goslar | work
//		createWorkers("BS", "GL", 1960 * config.getScalefactor(), 0.65, 0.0, 0.0, "03101", "03153");
//
//		// Wolfsburg, Stadt - Gifhorn | work
////		createWorkers("WB", "GH", 1844 * config.getScalefactor(), 0.65, 0.0, 0.0, "03103", "03151");
//
//		// Peine - Wolfburg, Stadt | work
////		createWorkers("PE", "WB", (1756-782) * config.getScalefactor(), 0.65, 0.0, 0.0, "03157", "03103");
////		createVWWorkers("PE", "03157", 551, 231, 0, .65, .0, .0);
//		//		3157	551	0	231	0	782
//
//		
//		// Altmarkkreis Salzwedel - Gifhorn | work
////		createWorkers("AS", "GH", 1649 * config.getScalefactor(), 0.65, 0.0, 0.0, "15081", "03151");
//
//		// Börde - Braunschweig, Stadt | work
//		createWorkers("BR", "BS", 1260 * config.getScalefactor(), 0.85, 0.0, 0.0, "15085", "03101");
//
//		// Hildesheim - Braunschweig, Stadt | work
//		createWorkers("HH", "BS", 1195 * config.getScalefactor(), 0.8, 0.0, 0.0, "03254", "03101");
//
//		// Salzgitter, Stadt - Wolfburg, Stadt | work
////		createWorkers("SG", "WB", 0 * config.getScalefactor(), 0.65, 0.0, 0.0, "03102", "03103");
////		createVWWorkers("SG", "03102", 180, 106, 0, .65, .0, .0);
//		//3102	180	0	106	658	944
//
//		// Magdeburg, Landeshauptstadt - Wolfburg, Stadt | work
////		createWorkers("MB", "WB", 847 * config.getScalefactor(), 0.65, 0.0, 0.0, "15003", "03103");
////		createVWWorkers("MB", "15003", 256, 140, 0, .65, .0, .0);
//	
//		//15003	256	0	140	0	396
////		createWorkers("HS", "GH", 735 * config.getScalefactor(), 0.65, 0.0, 0.0, "03154", "03151");
//
//		// Region Hannover - Gifhorn | work
////		createWorkers("RH", "GH", 680 * config.getScalefactor(), 0.65, 0.0, 0.0, "03241", "03151");
//
//		// Celle - Wolfburg, Stadt | work
////		createWorkers("CL", "WB", (607-315) * config.getScalefactor(), 0.65, 0.0, 0.0, "03351", "03103");
////		createVWWorkers("CL", "03351", 155, 160, 0, .65, .0, .0);
//		//		3351	155	0	160	0	315
//
//		// Magdeburg, Landeshauptstadt - Braunschweig, Stadt | work
//		createWorkers("MB", "BS", 511 * config.getScalefactor(), 0.65, 0.0, 0.0, "15003", "03101");
//
//		// Wolfenbüttel - Gifhorn | work
//		createWorkers("WB", "GH", 496 * config.getScalefactor(), 0.65, 0.0, 0.0, "03158", "03151");
//
//		// Harz - Wolfburg, Stadt | work
////		createWorkers("HZ", "WB", 476 * config.getScalefactor(), 0.65, 0.0, 0.0, "15085", "03103");
//
//		// Stendal - Wolfburg, Stadt | work
////		createWorkers("SD", "WB", 456 * config.getScalefactor(), 0.65, 0.0, 0.0, "15090", "03103");
//
//		// Celle - Braunschweig, Stadt | work
//		createWorkers("CL", "BS", 440 * config.getScalefactor(), 0.8, 0.0, 0.0, "03351", "03101");
//
//		// Börde - Gifhorn | work
////		createWorkers("BR", "GH", 400 * config.getScalefactor(), 0.8, 0.0, 0.0, "15083", "03151");
//
//
//		// Goslar - Wolfburg, Stadt | work
////		createWorkers("GL", "WB", 384 * config.getScalefactor(), 0.65, 0.0, 0.0, "03153", "03103");
//
//
//		// Altmarkkreis Salzwedel - Braunschweig, Stadt | work
//		createWorkers("AS", "BS", 245 * config.getScalefactor(), 0.8, 0.0, 0.0, "15081", "03101");
//
//		// Salzlandkreis - Wolfburg, Stadt | work
////		createWorkers("SL", "WB", 214 * config.getScalefactor(), 0.65, 0.0, 0.0, "15089", "03103");
//
//		// Salzlandkreis - Braunschweig, Stadt | work
//		createWorkers("SL", "BS", 185 * config.getScalefactor(), 0.65, 0.0, 0.0, "15089", "03101");
//
//		// Salzgitter, Stadt - Gifhorn | work
////		createWorkers("SG", "GH", 174 * config.getScalefactor(), 0.65, 0.0, 0.0, "03102", "03151");
//
//
//		// Jerichower Land - Braunschweig, Stadt | work
//		createWorkers("JL", "BS", 117 * config.getScalefactor(), 0.8, 0.0, 0.0, "15086", "03101");
//
//		// Magdeburg, Landeshauptstadt - Gifhorn | work
////		createWorkers("MB", "GH", 114 * config.getScalefactor(), 0.65, 0.0, 0.0, "15003", "03151");
//
//		// Stendal - Braunschweig, Stadt | work
//		createWorkers("SD", "BS", 108 * config.getScalefactor(), 0.65, 0.0, 0.0, "15090", "03101");
//
//	
//		createA2TransitTruckers(Math.round(10000 * config.getScalefactor()));
//		createVWTruckers(Math.round(900* config.getScalefactor()));
//		replaceDoubtfulLegsByOtherMode();
		
		System.out.println("generated Agents: " + commuterCounter);
		System.out.println("VW Workers: " + vwWorkerCounter);
		System.out.println("Workers: " + workerCounter);
//		createAgentGroupNearTransitstrops(scenario, 1500,config.getTransitSchedule() );
//		replaceSptByPtp();
		PopulationWriter pw = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		pw.write(config.getPlansOutputString());
		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes()).writeFile(config.getObjectAttributes());
	}

	

	private void createVWWorkers(String from, String origin, double flex, double threeshift, double partTime, double carcommuterFactor,
			double bikecommuterFactor, double walkcommuterFactor){
		
			for (int i = 0; i<=flex*config.getScalefactor(); i++){
				Geometry homeCounty = getCounty(origin);

				Coord homeC = (origin.equals("03101"))? drawRandomPointFromGeometry(homeCounty): findClosestCoordFromMap(drawRandomPointFromGeometry(homeCounty), this.residential);

				vwWorkerCounter++;
				commuterCounter++;
				String mode = drawMode(carcommuterFactor,bikecommuterFactor,walkcommuterFactor);
				createOneVWFlexitimeWorker(commuterCounter, homeC, mode, from + "_WB", 6.0, 9.0 );
			
			}
			for (int i = 0; i<=partTime*config.getScalefactor(); i++){
				Geometry homeCounty = getCounty(origin);

				Coord homeC = (origin.equals("03101"))? drawRandomPointFromGeometry(homeCounty): findClosestCoordFromMap(drawRandomPointFromGeometry(homeCounty), this.residential);

				vwWorkerCounter++;
				commuterCounter++;
				String mode = drawMode(carcommuterFactor,bikecommuterFactor,walkcommuterFactor);
				createOneVWFlexitimeWorker(commuterCounter, homeC, mode, from + "_WB", 3.5, 5.5 );
				
			}
			for (int i = 0; i<=threeshift*config.getScalefactor(); i++){
				Geometry homeCounty = getCounty(origin);

				Coord homeC = (origin.equals("03101"))? drawRandomPointFromGeometry(homeCounty): findClosestCoordFromMap(drawRandomPointFromGeometry(homeCounty), this.residential);

				vwWorkerCounter++;
				commuterCounter++;
				String mode = drawMode(carcommuterFactor,bikecommuterFactor,walkcommuterFactor);
				createOneVWShiftWorker(commuterCounter, homeC, mode, from + "_WB" );
			
			}
			
			
					
	}
	private String drawMode(double car, double bike, double walk){
		double d = random.nextDouble();
		if (d<car) return "car";
		else if (d<car+walk) return "walk";
		else if (d<car+walk+bike) return "bike";
		else return "pt";
		
	}
	
	private void createWorkers(String from, String to, double commuters, double carcommuterFactor,
			double bikecommuterFactor, double walkcommuterFactor, String origin, String destination) {
		
		
		Geometry commuteDestinationCounty = this.counties.get(destination);

		double walkcommuters = commuters * walkcommuterFactor;
		double bikecommuters = commuters * bikecommuterFactor;
		double carcommuters = commuters * carcommuterFactor;

		for (int i = 0; i <= commuters; i++) {
			String mode = "car";
			if (i > carcommuters) {
				mode = "bike";
				if (i > bikecommuters + carcommuters) {
					mode = "walk";
					if (i > walkcommuters + bikecommuters + carcommuters) {
						mode = "pt";
					}
				}
			}
			Geometry homeCounty = getCounty(origin);

			Coord homeC = (origin.equals("03101"))? findClosestCoordFromMapRandomized(drawRandomPointFromGeometry(homeCounty),this.residential,4): findClosestCoordFromMap(drawRandomPointFromGeometry(homeCounty), this.residential);

//			double wvWorkerOrNot = random.nextDouble();
//			if (to == "WB" && wvWorkerOrNot < 0.6363) {
//				vwWorkerCounter++;
//				double shiftOrFlexitime = random.nextDouble();
//				if (shiftOrFlexitime < 0.6666) {
//					createOneVWFlexitimeWorker(commuterCounter, homeC, mode, from + "_" + to);
//				} else {
//					createOneVWShiftWorker(commuterCounter, homeC, mode, from + "_" + to);
//				}
//			} else {
				List<Map<String, Coord>> coordMaps = new ArrayList<Map<String, Coord>>();
				coordMaps.add(commercial);
				coordMaps.add(industrial);
				if (from.equals(to))
					coordMaps.add(retail);
				Coord commuteDestinationC = findClosestCoordFromMap(
						drawRandomPointFromGeometry(commuteDestinationCounty), coordMaps);

				createOneWorker(commuterCounter, homeC, commuteDestinationC, mode, from + "_" + to);
//			}
			workerCounter++;
			commuterCounter++;
		}
	}

	private void createA2TransitTruckers(long commuters) {

		for (int i = 0; i <= commuters; i++) {
			String mode = "car";


			if (random.nextBoolean()) {
				Coord origin = this.scenario.getNetwork().getLinks().get(this.a2TruckerEastLinkID).getCoord();
				Coord destination = this.scenario.getNetwork().getLinks().get(this.a2TruckerWestLinkIDEnd).getCoord();
				createOneTransitTrucker(i, origin, destination, mode, "eastA2west");
			} else {
				Coord origin = this.scenario.getNetwork().getLinks().get(this.a2TruckerWestLinkID).getCoord();
				Coord destination = this.scenario.getNetwork().getLinks().get(this.a2TruckerEastLinkIDEnd).getCoord();
				createOneTransitTrucker(i, origin,destination, mode, "westA2east");
			}
		}
	}

	private void createVWTruckers(long l) {

		for (int i = 0; i <= l; i++) {
			String mode = "car";

			double r = random.nextDouble();
			Coord origin;
			if (r < 0.33) {
				origin = this.scenario.getNetwork().getLinks().get(this.vwSourceLinkID).getCoord();
			} else if (r < 0.67) {
				origin = this.scenario.getNetwork().getLinks().get(this.a2TruckerEastLinkID).getCoord();
			} else {
				origin = this.scenario.getNetwork().getLinks().get(this.a2TruckerWestLinkID).getCoord();
			}

			Coord destination = this.scenario.getNetwork().getLinks().get(this.vwDeliveryGateLinkID).getCoord();

			createOneVWTrucker(i, origin, destination, mode, "BS_VW");
		}
	}

	private void createPupils(String from, String to, double commuters, double carcommuterFactor,
			double bikecommuterFactor, double walkcommuterFactor, String origin, String destination) {
		Geometry commuteDestinationCounty = this.counties.get(destination);

		double walkcommuters = commuters * walkcommuterFactor;
		double bikecommuters = commuters * bikecommuterFactor;
		double carcommuters = commuters * carcommuterFactor;

		for (int i = 0; i <= commuters; i++) {
			String mode = "car";
			if (i > carcommuters) {
				mode = "bike";
				if (i > bikecommuters + carcommuters) {
					mode = "walk";
					if (i > walkcommuters + bikecommuters + carcommuters) {
						mode = "pt";
					}
				}
			}
			Geometry homeCounty = getCounty(origin);

			Coord homeC = (origin.equals("03101"))? findClosestCoordFromMapRandomized(drawRandomPointFromGeometry(homeCounty),this.residential,4): findClosestCoordFromMap(drawRandomPointFromGeometry(homeCounty), this.residential);

			Coord commuteDestinationC = findClosestCoordFromMap(drawRandomPointFromGeometry(commuteDestinationCounty),
					this.schools);

			createOnePupil(commuterCounter, homeC, commuteDestinationC, mode, from + "_" + to);
			commuterCounter++;
		}
	}

	private void createStudents(String from, String to, double commuters, double carcommuterFactor,
			double bikecommuterFactor, double walkcommuterFactor, String origin, String destination) {
		Geometry commuteDestinationCounty = this.counties.get(destination);

		double walkcommuters = commuters * walkcommuterFactor;
		double bikecommuters = commuters * bikecommuterFactor;
		double carcommuters = commuters * carcommuterFactor;

		for (int i = 0; i <= commuters; i++) {
			String mode = "car";
			if (i > carcommuters) {
				mode = "bike";
				if (i > bikecommuters + carcommuters) {
					mode = "walk";
					if (i > walkcommuters + bikecommuters + carcommuters) {
						mode = "pt";
					}
				}
			}
			Geometry homeCounty = getCounty(origin);

			Coord homeC = (origin.equals("03101"))? findClosestCoordFromMapRandomized(drawRandomPointFromGeometry(homeCounty),this.residential,4): findClosestCoordFromMap(drawRandomPointFromGeometry(homeCounty), this.residential);
			
			Coord commuteDestinationC = findClosestCoordFromMap(drawRandomPointFromGeometry(commuteDestinationCounty),
					this.universities);

			createOneStudent(commuterCounter, homeC, commuteDestinationC, mode, from + "_" + to);
			commuterCounter++;
		}
	}

	private Geometry getCounty(String origin) {
		if (origin == "03151"){
			if (random.nextDouble()<0.15) {origin ="99999";}
		}
		if (origin == "03101"){
			String bez = this.wrs.select();
			return this.bs.get(bez);
		}
		if (origin == "03103"){
			if (random.nextBoolean()){
				return this.wb.get("0");
			}else {
				return this.wb.get("111");
			}
			
		}
		
		
		
		return this.counties.get(origin);
	}

	private void createOneVWFlexitimeWorker(int i, Coord homeC, String mode, String fromToPrefix) {
		createOneVWFlexitimeWorker(i, homeC, mode, fromToPrefix, 6.0, 9.0);
	}

	private void createOneVWFlexitimeWorker(int i, Coord homeC, String mode, String fromToPrefix, double minHrs, double maxHrs) {
		int additionalTrips = random.nextInt(4);

		Id<Person> personId = Id.createPersonId(fromToPrefix + i);
		Person person = scenario.getPopulation().getFactory().createPerson(personId);

		Plan plan = scenario.getPopulation().getFactory().createPlan();

		Activity home = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);

		home.setEndTime(5 * 60 * 60 + 3 * 3600 * random.nextDouble());
		plan.addActivity(home);

		Leg outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(outboundTrip);

		Coord coord = scenario.getNetwork().getLinks().get(calcVWWorkLinkId()).getCoord();
		Activity work = scenario.getPopulation().getFactory().createActivityFromCoord("work",
				coord);
		double spread = (maxHrs-minHrs)*3600;
		work.setMaximumDuration(minHrs*3600 + random.nextDouble()*spread  );
		plan.addActivity(work);
		if ( additionalTrips == 1 || additionalTrips == 3){
			enrichPlanBySingleLegAndActivity(coord, plan,mode, 4800, false);
		}

		Leg returnTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(returnTrip);

		Activity home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
		plan.addActivity(home2);

		person.addPlan(plan);
		if (additionalTrips>1){
			home2.setMaximumDuration(random.nextInt(5400));

			enrichPlanByReturnLegAndActivity(home2, plan,mode, 4800);

		}
		scenario.getPopulation().addPerson(person);
	}

	private void createOneVWShiftWorker(int i, Coord homeC, String mode, String fromToPrefix) {
		int additionalTrips = random.nextInt(4);

		Id<Person> personId = Id.createPersonId(fromToPrefix+i);
		Person person = scenario.getPopulation().getFactory().createPerson(personId);

		Plan plan = scenario.getPopulation().getFactory().createPlan();

		int rand = random.nextInt(3) + 1;
		switch (rand) {
		case 1:
			Activity home = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
			home.setEndTime(5 * 60 * 60);
			plan.addActivity(home);

			Leg outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
			plan.addLeg(outboundTrip);

			Coord coord3 = scenario.getNetwork().getLinks().get(calcVWWorkLinkId()).getCoord();
			Activity shift1 = scenario.getPopulation().getFactory().createActivityFromCoord("work",
					coord3);
			shift1.setEndTime(14 * 60 * 60 + 5 * 60);
			plan.addActivity(shift1);
			if ( additionalTrips == 1 || additionalTrips == 3){
				enrichPlanBySingleLegAndActivity(coord3, plan,mode, 4800, false);
			}

			Leg returnTrip = scenario.getPopulation().getFactory().createLeg(mode);
			plan.addLeg(returnTrip);

			Activity home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
			plan.addActivity(home2);
			if (additionalTrips>1){
				home2.setMaximumDuration(random.nextInt(3600));

				enrichPlanByReturnLegAndActivity(home2, plan,mode, 4800);

			}
			break;
		case 2:
			home = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
			home.setEndTime(13 * 60 * 60);
			
			
			plan.addActivity(home);
			
			if (additionalTrips>1){

				enrichPlanByReturnLegAndActivity(home, plan,mode, 5400);
				home.setEndTime(8*3600+random.nextInt(7200));;

			}
			
			if ( additionalTrips == 1 || additionalTrips == 3){
				enrichPlanBySingleLegAndActivity(homeC, plan,mode, 5400, false);
			}
			outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
			plan.addLeg(outboundTrip);

			Coord coord = scenario.getNetwork().getLinks().get(calcVWWorkLinkId()).getCoord();
			Activity shift2 = scenario.getPopulation().getFactory().createActivityFromCoord("work",
					coord);
			shift2.setEndTime(22 * 60 * 60 + 5 * 60);
			plan.addActivity(shift2);
			
			returnTrip = scenario.getPopulation().getFactory().createLeg(mode);
			plan.addLeg(returnTrip);

			home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
			plan.addActivity(home2);
			
			break;
		case 3:
			Id<Link> workLink = calcVWWorkLinkId();
			Coord coord2 = scenario.getNetwork().getLinks().get(workLink).getCoord();
			Activity shift3 = scenario.getPopulation().getFactory().createActivityFromCoord("workN",coord2
					);
			shift3.setEndTime(6 * 60 * 60 + 5 * 60);
			plan.addActivity(shift3);
			
			if ( additionalTrips == 1 || additionalTrips == 3){
				enrichPlanBySingleLegAndActivity(coord2, plan,mode, 5400, false);
			}
			outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
			plan.addLeg(outboundTrip);

			home = scenario.getPopulation().getFactory().createActivityFromCoord("homeD", homeC);
			home.setEndTime(21 * 60 * 60);
			plan.addActivity(home);
			
			if (additionalTrips>1){

				enrichPlanByReturnLegAndActivity(home, plan,mode, 5400);
				home.setEndTime(8*3600+random.nextInt(7200));;

			}

			outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
			plan.addLeg(outboundTrip);

			Activity shift32 = scenario.getPopulation().getFactory().createActivityFromCoord("workN",
					scenario.getNetwork().getLinks().get(workLink).getCoord());
			
			plan.addActivity(shift32);
			break;
		}

		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}

	private Id<Link> calcVWWorkLinkId() {

//		Parkplatzverteilung
//		FE1	6709	21.60%
//		FE2	4000	12.88%
//		West	5084	16.37%
//		Nordhoff	5221	16.81%
//		Ost	2950	9.50%
//		Nord	4100	13.20%
//		NordostITV	3000	9.66%
//		sum	31064	
//
//		
		
		double rand = random.nextDouble();
		if (rand<0.166) return this.vwGateFE1LinkID;
		else if  (rand<0.3447)	return this.vwGateFE2LinkID;
		else if  (rand<0.5084)	return this.vwGateWestLinkID;
		else if  (rand<0.6765)	return this.vwGateNHSLinkID;
		else if  (rand<0.7714)	return this.vwGateEastID;
		else if  (rand<0.9034)	return this.vwGateNorthID;
		else return this.vwGateNorthITVID;
		
	}

	private void createOneWorker(int i, Coord homeC, Coord coordWork, String mode, String fromToPrefix) {
		int additionalTrips = random.nextInt(2)+random.nextInt(3);

		Id<Person> personId = Id.createPersonId(fromToPrefix + i);
		Person person = scenario.getPopulation().getFactory().createPerson(personId);

		Plan plan = scenario.getPopulation().getFactory().createPlan();

		Activity home = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
		home.setEndTime(6 * 60 * 60 + 3 * 3600 * random.nextDouble());
		plan.addActivity(home);

		Leg outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(outboundTrip);

		Activity work = scenario.getPopulation().getFactory().createActivityFromCoord("work", coordWork);
		work.setEndTime(16 * 60 * 60 + 3 * 3600 * random.nextDouble());
		plan.addActivity(work);
		if ( additionalTrips == 1 || additionalTrips == 3){
			enrichPlanBySingleLegAndActivity(coordWork, plan,mode, 5400, false);
		}

		Leg returnTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(returnTrip);

		Activity home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", homeC);
		plan.addActivity(home2);
		if (additionalTrips>1){
			home2.setMaximumDuration(random.nextInt(5400));

			enrichPlanByReturnLegAndActivity(home2, plan,mode, 4800);

		}

		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}

	private void createOnePupil(int i, Coord coord, Coord coordSchool, String mode, String fromToPrefix) {
		int additionalTrips = random.nextInt(3);

		Id<Person> personId = Id.createPersonId(fromToPrefix + i);
		Person person = scenario.getPopulation().getFactory().createPerson(personId);

		Plan plan = scenario.getPopulation().getFactory().createPlan();

		Activity home = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
		home.setEndTime(7 * 60 * 60 + 2 * 3600 * random.nextDouble());
		plan.addActivity(home);

		Leg outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(outboundTrip);

		Activity school = scenario.getPopulation().getFactory().createActivityFromCoord("school", coordSchool);
		school.setEndTime(14 * 60 * 60 + 2 * 3600 * random.nextDouble());
		plan.addActivity(school);

		if ( additionalTrips == 1 || additionalTrips == 3){
			enrichPlanBySingleLegAndActivity(coordSchool, plan,mode, 5400, false);
		}
		
		Leg returnTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(returnTrip);

		Activity home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
		plan.addActivity(home2);

		if (additionalTrips>1){
			home2.setMaximumDuration(random.nextInt(5400));

			enrichPlanByReturnLegAndActivity(home2, plan,mode, 4800);

		}
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}

	private void createOneStudent(int i, Coord coord, Coord coordUniversity, String mode, String fromToPrefix) {
		int additionalTrips = random.nextInt(4);
		Id<Person> personId = Id.createPersonId(fromToPrefix + i);
		Person person = scenario.getPopulation().getFactory().createPerson(personId);
		
		Plan plan = scenario.getPopulation().getFactory().createPlan();

		Activity home = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
		home.setEndTime(8 * 60 * 60 + 2 * 3600 * random.nextDouble());
		plan.addActivity(home);

		Leg outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(outboundTrip);
		
		Activity university = scenario.getPopulation().getFactory().createActivityFromCoord("university",
				coordUniversity);
		university.setEndTime(16 * 60 * 60 + 2 * 3600 * random.nextDouble());
		plan.addActivity(university);

		if ( additionalTrips == 1 || additionalTrips == 3){
			enrichPlanBySingleLegAndActivity(coordUniversity, plan,mode, 5400, false);
		}
		
		Leg returnTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(returnTrip);

		Activity home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
		plan.addActivity(home2);
		if (additionalTrips>1){
			home2.setMaximumDuration(random.nextInt(5400));
			enrichPlanByReturnLegAndActivity(home2, plan,mode, 4800);

		}
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}

	private void enrichPlanByReturnLegAndActivity(Activity last, Plan plan, String mode, int maxDur) {

		double lastEnd = 0.0;
		if (last.getEndTime() != Time.UNDEFINED_TIME){
			lastEnd = last.getEndTime();
		}
		String newMode  = enrichPlanBySingleLegAndActivity(last.getCoord(), plan, mode, maxDur, true);
		Leg returnTrip = scenario.getPopulation().getFactory().createLeg(newMode);
		plan.addLeg(returnTrip);
		Activity home = scenario.getPopulation().getFactory().createActivityFromCoord(last.getType(), last.getCoord());
		if (lastEnd!=0.0) {
			home.setEndTime(lastEnd);
		}
		plan.addActivity(home);
	}

	private String enrichPlanBySingleLegAndActivity(Coord lastActivityCoord, Plan plan, String oldmode, int maxDur, boolean canChangeMode) {
		String mode = oldmode;
		Coord nextDestination;
		String nextActivity;
		double duration;
		double r = random.nextDouble();
		if ( r<0.25){
			nextActivity = "private";
			nextDestination = findClosestCoordFromMapRandomized(lastActivityCoord, residential, 20);
			duration = 600 +random.nextInt(maxDur);
		}
		else if (r< 0.5){
			nextActivity = "leisure";
			nextDestination = findClosestCoordFromMapRandomized(lastActivityCoord, schools, 5);
			duration = 600 +random.nextInt(maxDur);

		}
		else{
			nextActivity = "shopping";
			nextDestination = findClosestCoordFromMapRandomized(lastActivityCoord, retail, 10);
			duration = 600 +random.nextInt(maxDur);
			
		}
		Activity next = scenario.getPopulation().getFactory().createActivityFromCoord(nextActivity, nextDestination);
		next.setMaximumDuration(duration);
		double distance = CoordUtils.calcEuclideanDistance(nextDestination, lastActivityCoord);
		if (canChangeMode){
			if (distance<500){
				mode = "walk";
				
			}
			else if (distance < 3000){
				if (random.nextBoolean()) mode = "car";
				else if (random.nextBoolean()) mode = "bike";
				else mode = "pt";
			}
			else {if (random.nextBoolean()) mode = "car";
				else mode = "pt"; 
			}
		}
		plan.addLeg(scenario.getPopulation().getFactory().createLeg(mode));
		plan.addActivity(next);
		return mode;
	}

	private void createOneVWTrucker(int i, Coord origin, Coord destination, String mode, String fromToPrefix) {
		Id<Person> personId = Id.createPersonId(fromToPrefix + i);
		Person person = scenario.getPopulation().getFactory().createPerson(personId);

		Plan plan = scenario.getPopulation().getFactory().createPlan();

		Activity source = scenario.getPopulation().getFactory().createActivityFromCoord("source", origin);
		double rand = random.nextDouble() * 18 * 60 * 60;
		source.setEndTime(rand);
		plan.addActivity(source);

		Leg outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(outboundTrip);

		Activity delivery = scenario.getPopulation().getFactory().createActivityFromCoord("delivery", destination);
		delivery.setMaximumDuration(3600);
		plan.addActivity(delivery);

		Leg inboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(inboundTrip);
		Activity source2 = scenario.getPopulation().getFactory().createActivityFromCoord("source", origin);
		plan.addActivity(source2);
		
		person.addPlan(plan);
		scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "subpopulation", "noRep");

		scenario.getPopulation().addPerson(person);
	}

	private void createOneTransitTrucker(int i, Coord origin, Coord destination, String mode, String fromToPrefix) {
		Id<Person> personId = Id.createPersonId(fromToPrefix + i);
		Person person = scenario.getPopulation().getFactory().createPerson(personId);

		Plan plan = scenario.getPopulation().getFactory().createPlan();

		Activity cargo = scenario.getPopulation().getFactory().createActivityFromCoord("cargo", origin);
		int rand = random.nextInt(18 * 60 * 60) + 1;
		cargo.setEndTime(rand);
		plan.addActivity(cargo);

		Leg outboundTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(outboundTrip);

		Activity cargod = scenario.getPopulation().getFactory().createActivityFromCoord("cargoD", destination);
		cargod.setMaximumDuration(3600);
		plan.addActivity(cargod);
		Leg inBundTrip = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(inBundTrip);
		
		Activity cargo2 = scenario.getPopulation().getFactory().createActivityFromCoord("cargo", origin);
		plan.addActivity(cargo2);

		person.addPlan(plan);
		scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "subpopulation", "noRep");
		scenario.getPopulation().addPerson(person);
	}

	private Coord drawRandomPointFromGeometry(Geometry g) {
		Random rnd = MatsimRandom.getLocalInstance();
		Point p;
		double x, y;
		do {
			x = g.getEnvelopeInternal().getMinX()
					+ rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY()
					+ random.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!g.contains(p));
		Coord coord = new Coord(p.getX(), p.getY());
		return coord;
	}

	public Map<String, Geometry> readShapeFile(String filename, String attrString) {

		Map<String, Geometry> shapeMap = new HashMap<String, Geometry>();

		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {

			GeometryFactory geometryFactory = new GeometryFactory();
			WKTReader wktReader = new WKTReader(geometryFactory);
			Geometry geometry;

			try {
				geometry = wktReader.read((ft.getAttribute("the_geom")).toString());
				shapeMap.put(ft.getAttribute(attrString).toString(), geometry);

			} catch (ParseException e) {
				e.printStackTrace();
			}

		}
		return shapeMap;
	}



	private Coord findClosestCoordFromMap(Coord location, Map<String, Coord> coordMap) {
		Coord closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Coord coord : coordMap.values()) {
			double distance = CoordUtils.calcEuclideanDistance(coord, location);
			if (distance < closestDistance) {
				closestDistance = distance;
				closest = coord;
			}
		}
		return closest;
	}
	
	
	private Coord findClosestCoordFromMapRandomized(Coord location, Map<String, Coord> coordMap, int scope) {
		TreeMap<Double,Coord> closestCoords = new TreeMap<>();
		Coord closest = null;
		for (Coord coord : coordMap.values()) {
			double distance = CoordUtils.calcEuclideanDistance(coord, location);
			closestCoords.put(distance, coord);
		}
		int drawscope = scope;
		if (scope>closestCoords.size()){
			drawscope = closestCoords.size();
		}
		int draw = random.nextInt(drawscope);
		int i = 0;
		
		for (Coord c : closestCoords.values()	){
			if (i == draw){ 
				closest = c;
				break;
			}
			i++;
		}
		
		return closest;
	}

	private Coord findClosestCoordFromMap(Coord location, List<Map<String, Coord>> coordMaps) {
		Coord closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Map<String, Coord> coordMap : coordMaps) {
			for (Coord coord : coordMap.values()) {
				double distance = CoordUtils.calcEuclideanDistance(coord, location);
				if (distance < closestDistance) {
					closestDistance = distance;
					closest = coord;
				}
			}
		}

		return closest;
	}

	private Map<String, Coord> geometryMapToCoordMap(Map<String, Geometry> geometries) {
		Map<String, Coord> coords = new HashMap<String, Coord>();
		for (Map.Entry<String, Geometry> entry : geometries.entrySet()) {
			Coord coord = new Coord(entry.getValue().getCoordinate().x, entry.getValue().getCoordinate().y);
			coords.put(entry.getKey(), coord);
		}
		return coords;
	}
	
	void createAgentGroupNearTransitstrops(Scenario scenario,double distance, String transitScheduleFile ){
		new TransitScheduleReader(scenario).readFile(transitScheduleFile);
		for(Person p : scenario.getPopulation().getPersons().values()){
			if (scenario.getPopulation().getPersonAttributes().getAttribute(p.getId().toString(), "subpopulation")!=null){
				return;
			}
			ArrayList<Boolean> isIt = new ArrayList<>();
			for (Plan plan : p.getPlans()){
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Activity){
						boolean setAct = false;
						Coord ac = ((Activity) pe).getCoord();
						for (TransitStopFacility stop : scenario.getTransitSchedule().getFacilities().values()){
							double dist = CoordUtils.calcEuclideanDistance(stop.getCoord(), ac);
							if (dist<=distance){ 
								setAct =true;
								break;
								}
						}
						isIt.add(setAct);
					}
				}
			}
			boolean truth = true;
		for (Boolean t : isIt){
			if (!t) truth=false;
		}	
		if (truth){
			scenario.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), "subpopulation", "schedulePt");
		}else {
			scenario.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), "subpopulation", "teleportPt");
			this.teleportPtUsers.add(p.getId());
		}
		}
		
	}

	private void replaceSptByPtp() {
		for (Id<Person> pid : this.teleportPtUsers){
			Person p = scenario.getPopulation().getPersons().get(pid);
			for (Plan plan : p.getPlans()){
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Leg){
						Leg leg = (Leg) pe;
						if (leg.getMode().equals("pt")){
							leg.setMode("tpt");
						}
						
					}
				}
			}
		}
	}
	


private void readPopulationData() {
	
	TabularFileParserConfig cfg = new TabularFileParserConfig();
    cfg.setDelimiterTags(new String[] {"\t"});
    cfg.setFileName(config.getBsb());
    cfg.setCommentTags(new String[] { "#" });
    new TabularFileParser().parse(cfg, new TabularFileHandler() {
		
		@Override
		public void startRow(String[] row) {

			wrs.add(row[0], Double.parseDouble(row[2]));
		}
	});
    
}	
}




