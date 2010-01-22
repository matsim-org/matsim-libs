package playground.mmoyo;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.xml.sax.SAXException;

import playground.mmoyo.PTRouter.LogicFactory;
import playground.mmoyo.PTRouter.LogicIntoPlainTranslator;
import playground.mmoyo.PTRouter.PTRouter;
import playground.mmoyo.TransitSimulation.TransitRouteFinder;

/** makes tests with the transit router on the 5x5 scenario*/
public class PTRouterTest extends MatsimTestCase {
	
	public void testRouter() throws SAXException, ParserConfigurationException, IOException {
	
		//final String PATH= getInputDirectory();
		final String PATH="../playgrounds/mmoyo/src/main/java/playground/mmoyo/demo/X5/transfer_det/";
		final String NETWORK = "../playgrounds/mmoyo/src/main/java/playground/mmoyo/demo/X5/network.xml";
		final String TRANSITSCHEDULE= PATH + "simple1TransitSchedule.xml";
		final String PLANFILE = PATH +  "output_plans.xml";
		
		/* read transit schedule, plain net create logic elements */
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(NETWORK);
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule transitSchedule = builder.createTransitSchedule();
		new TransitScheduleReaderV1(transitSchedule, network).readFile(TRANSITSCHEDULE);
		LogicFactory logicFactory = new LogicFactory(transitSchedule); // Creates logic elements: logicNetwork, logicTransitSchedule, logicToPlanConverter
		NetworkLayer logicNet= logicFactory.getLogicNet();
		PTRouter ptRouter = new PTRouter (logicNet);
		
		NetworkLayer plainNetwork= logicFactory.getPlainNet();
		LogicIntoPlainTranslator logicIntoPlainTranslator = logicFactory.getLogicToPlainTranslator();

		/**Tests transfer times*/
		//	System.out.println("links size of logicNet: " + logicNet.getLinks().size());
		
		LinkImpl transferLink = logicNet.getLinks().get(new IdImpl("0"));  //checck this actually the purpose is to test the transfers								 // first departure: 09:00 ,  last departure is at 19:00 
		 
		/*
		System.out.println("Printing network links");
		for (Link link :logicNet.getLinks().values()){
			System.out.println(link.getId());
		}*/
		
		double waitTime1= ptRouter.ptTravelTime.getLinkTravelTime(transferLink, 67800);  //  67800= 16:50 .  The agent should wait 10 mins 
		double waitTime2= ptRouter.ptTravelTime.getLinkTravelTime(transferLink, 72000);  //  72000= 20:00 .  The agent should wait 13 hours, until next day
		assertEquals( waitTime1, 25.0 ) ;                                               
		assertEquals( waitTime2, 25.0 ) ;
		
		/**tests search path results*/		
		Coord coord1 = new CoordImpl(1010, 5010);
		Coord coord2 = new CoordImpl(1010, 9010);
		Path path = ptRouter.findPTPath (coord1, coord2, 2808);   //07:48 , 400 walk distance
		
		List<Node> plainPathNodes  = logicIntoPlainTranslator.convertNodesToPlain(path.nodes);
		assertEquals( plainPathNodes.get(0).getId() , new IdImpl("stop2")); 
		assertEquals( plainPathNodes.get(1).getId() , new IdImpl("stop3"));
		assertEquals( plainPathNodes.get(2).getId() , new IdImpl("stop4")); 

		/**tests TransitRouteFinder class*/
		TransitRouteFinder transitRouteFinder= new TransitRouteFinder (transitSchedule);
		ScenarioImpl scenario2 = new ScenarioImpl();
		scenario2.setNetwork(network);
		PopulationImpl population = scenario2.getPopulation();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(scenario2);
		plansReader.readFile(PLANFILE);
		Person person = population.getPersons().get(new IdImpl("1")); 
		Plan plan = person.getPlans().get(0);

		ActivityImpl act1 = (ActivityImpl)plan.getPlanElements().get(0);
		ActivityImpl act2 = (ActivityImpl)plan.getPlanElements().get(2);
		
		/* TODO. repair this
		List<Leg> legList = transitRouteFinder.calculateRoute (act1, act2, person);
		
		assertEquals( legList.get(0).getMode() , TransportMode.walk);
		assertEquals( legList.get(1).getMode() , TransportMode.pt);
		assertEquals( legList.get(2).getMode() , TransportMode.pt);
		assertEquals( legList.get(3).getMode() , TransportMode.walk);
		*/
	}
	
}