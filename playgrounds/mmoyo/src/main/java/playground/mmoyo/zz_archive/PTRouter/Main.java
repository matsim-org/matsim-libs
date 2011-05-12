package playground.mmoyo.zz_archive.PTRouter;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

import playground.mmoyo.Validators.RepeatedStopFinder;


/**
 * This class contains the options to route with a TransitSchedule object
 */
public class Main {
	private static final String PATH = "../shared-svn/studies/schweiz-ivtch/pt-experimental/";
	//private static final String PATH = "../shared-svn/studies/schweiz-ivtch/pt-experimental/5x5/";
	private static final String CONFIG =  				PATH + "config.xml";
	private static final String PLANFILE = 				PATH + "_input_file.xml";  //"_input_file.xml"; // // "plans.xml";
	private static final String OUTPUTPLANS = 			PATH + "output_plans.xml";
	private static final String PLAINNETWORK = 			PATH + "plainNetwork.xml";
	private static final String LOGICNETWORK = 			PATH + "logicNetwork.xml";
	private static final String LOGICTRANSITSCHEDULE = 	PATH + "logicTransitSchedule.xml";
	private static String netWorkFile = 				PATH + "network.xml";
	private static String transitScheduleFile  = 		PATH + "transitSchedule.xml";

	public static void main(String[] args) {
		/** equil Network  */
		/*
		netWorkFile = "examples/equil/network.xml";
		transitScheduleFile  = 	"src/playground/marcel/pt/demo/equilnet/transitSchedule.xml";
		*/
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = scenario.getNetwork();
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule transitSchedule = builder.createTransitSchedule();
		PTActWriter ptActWriter;

		/* **************reads the transitSchedule file********* */
		new MatsimNetworkReader(scenario).readFile(netWorkFile);
		new TransitScheduleReaderV1(transitSchedule, network, scenario).readFile(transitScheduleFile);
		/*******************************************************/

		LogicFactory logicFactory = new LogicFactory(transitSchedule); // Creates logic elements: logicNetwork, logicTransitSchedule, logicToPlanConverter
		//03 dic noPlain Net NetworkLayer plainNetwork=logicFactory.getPlainNet();

		int option =10;
		switch (option){
			case 1:    //writes logicElement files
				logicFactory.writeLogicElements(PLAINNETWORK, LOGICTRANSITSCHEDULE, LOGICNETWORK);
				break;

			case 2:  //searches and shows a PT path between two coordinates or nodes */
				//03 dic no Plain net plainNetwork=logicFactory.getPlainNet();
				PTRouter ptRouter = new PTRouter(logicFactory.getLogicNet());
				Coord coord1 = new CoordImpl(686897, 250590);
				Coord coord2 = new CoordImpl(684854, 254079);
				//NodeImpl nodeA = plainNetwork.getNode("299598");
				//NodeImpl nodeB = plainNetwork.getNode("8503309");
				Path path = ptRouter.findPTPath (coord1, coord2, 37075);
				System.out.println(path.links.size());
				for (Link l : path.links){
					System.out.println(l.getId()+ ": " + l.getFromNode().getId() + " " + ((LinkImpl)l).getType() + " " + l.getToNode().getId() );
				}
				System.out.println(path.travelTime);
				break;

			case 3: //Routes a population/
				double startTime = System.currentTimeMillis();
				logicFactory.writeLogicElements(PLAINNETWORK, LOGICTRANSITSCHEDULE, LOGICNETWORK);
				ptActWriter = new PTActWriter(logicFactory, CONFIG, PLANFILE, OUTPUTPLANS);
				ptActWriter.findRouteForActivities();
				System.out.println("total process duration: " + (System.currentTimeMillis()-startTime));
				break;

			case 4:  //tests the TransitRouteFinder class with the population of PTActWriter class
				ptActWriter = new PTActWriter(logicFactory, CONFIG, PLANFILE, OUTPUTPLANS);
				ptActWriter.printPTLegs(transitSchedule);
				break;

			case 6:  //simplifies a plan
				String planToSimplify = "output_plan.xml";
				String simplifiedPlan = "simplfied_plan.xml";
				ptActWriter = new PTActWriter(logicFactory, CONFIG, PATH + planToSimplify , PATH + simplifiedPlan);
				ptActWriter.simplifyPtLegs();
				break;

			case 7:
				/*
				Map <Id, List<StaticConnection>> connectionMap = new TreeMap <Id, List<StaticConnection>>();
				final TransitSchedule transitSchedule, final NetworkLayer plainNet, Map <Id, List<StaticConnection>> connectionMap, Map <Coord, Collection<NodeImpl>> nearStopMap
				KroutesCalculator precalRoutes = new KroutesCalculator(transitSchedule, plainNetwork, connectionMap);
				//agent 1409800
				coord1 = new CoordImpl(682711, 237595);
				coord2 = new CoordImpl(682308, 242093);
				double startTime = System.currentTimeMillis();
				precalRoutes.findPTPath(coord1, coord2, 400);
				System.out.println("duration: " + (System.currentTimeMillis()-startTime));
				*/
				break;
			case 8 :
				/**counts the trains and tram routes*/
				short trains=0;
				short trams=0;
				for (TransitLine transitLine : transitSchedule.getTransitLines().values()){
					for (TransitRoute transitRoute : transitLine.getRoutes().values()){
						System.out.println(transitRoute.getId());
						if (transitRoute.getTransportMode().equals("train")) trains++;
						if (transitRoute.getTransportMode().equals("tram")) trams++;
					}
				}
				System.out.println("trains: " + trains +  "   trams: "  + trams);

				break;
			case 9:
				/**Counts nodes, transfer links*/
				NetworkImpl logicNetwork=logicFactory.getLogicNet();

				int transfers=0;
				int standard=0;
				for (Link link : logicNetwork.getLinks().values()){
					if (((LinkImpl) link).getType().equals(PTValues.TRANSFER_STR)) transfers++;
					if (((LinkImpl) link).getType().equals(PTValues.STANDARD_STR)) standard++;
				}

				System.out.println("Transferlinks: " +  transfers);
				System.out.println("Standard: " +  standard);
				System.out.println("num of logic nodes:" + logicNetwork.getNodes().size());
				//03 dic no plainNet System.out.println("num of plain nodes:" + plainNetwork.getNodes().size());

				break;

			case 11:
				new RepeatedStopFinder(transitSchedule);
				break;

				/**Try the transitRouter class on equilnet*/
			case 12:
				ptRouter = new PTRouter(logicFactory.getLogicNet());
				//x=10000.0][y=0.0] to [x=-25000.0][y=0.0] departure Time:24083.6
				Coord fromCoord = new CoordImpl(10000.0, 0.0);
				Coord toCoord = new CoordImpl(-25000.0, 0.0);
				double departureTime= 24083.6; //24083.6;  // 31283.6   //45683.6
				List<Leg> listLeg= ptRouter.calcRoute(fromCoord, toCoord, departureTime);
				for (Leg leg :listLeg){
					if (leg.getRoute()!=null){
						System.out.println(leg.getRoute());
					}
					System.out.println(leg.toString());
				}
				break;
		}
	}
}
