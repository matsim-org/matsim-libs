package playground.mmoyo;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.transitSchedule.TransitScheduleBuilderImpl;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleBuilder;
import org.xml.sax.SAXException;

import playground.mmoyo.PTRouter.PTRouter;
import playground.mmoyo.TransitSimulation.LogicFactory;
import playground.mmoyo.TransitSimulation.LogicIntoPlainTranslator;
import playground.mmoyo.TransitSimulation.TransitRouteFinder;

/** makes tests with the transit router on the 5x5 scenario*/
public class PTRouterTest extends MatsimTestCase {

	private static final String PATH = "../shared-svn/studies/schweiz-ivtch/pt-experimental/5x5/"; 
	private static final String NETWORK= PATH + "network.xml";
	private static final String TRANSITSCHEDULE= PATH + "TransitSchedule.xml";
	private static final String PLANFILE = PATH +  "plans.xml";
	
	public void testRouter() throws SAXException, ParserConfigurationException, IOException {
		/**read transit schedule, plain net create logic elements */
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(NETWORK);
		TransitScheduleBuilder builder = new TransitScheduleBuilderImpl();
		TransitSchedule transitSchedule = builder.createTransitSchedule();
		new TransitScheduleReaderV1(transitSchedule, network).readFile(TRANSITSCHEDULE);
		LogicFactory logicFactory = new LogicFactory(transitSchedule); // Creates logic elements: logicNetwork, logicTransitSchedule, logicToPlanConverter
		NetworkLayer logicNet= logicFactory.getLogicNet();
		PTRouter ptRouter = logicFactory.getPTRouter();
		NetworkLayer plainNetwork= logicFactory.getPlainNet();
		LogicIntoPlainTranslator logicIntoPlainTranslator = logicFactory.getLogicToPlainTranslator();

		/**Tests transfer times*/
		LinkImpl transferLink = logicNet.getLink("T39");  								 // first departure: 09:00 ,  last departure is at 19:00
		double waitTime1= ptRouter.ptTravelTime.getLinkTravelTime(transferLink, 67800);  //  67800= 16:50 .  The agent should wait 10 mins 
		double waitTime2= ptRouter.ptTravelTime.getLinkTravelTime(transferLink, 72000);  //  72000= 20:00 .  The agent should wait 13 hours, until next day
		assertEquals( 600, waitTime1 ) ;                                               
		assertEquals( 46800, waitTime2 ) ;

		
		/**tests search path results*/		
		Coord coord1 = new CoordImpl(1010, 5010);
		Coord coord2 = new CoordImpl(8950, 8950);
		Path path = ptRouter.findPTPath (coord1, coord2, 2808, 400);   //07:48 , 400 walk distance
		List<Node> plainPathNodes  = logicIntoPlainTranslator.convertNodesToPlain(path.nodes);
		assertEquals( plainPathNodes.get(0).getId() , new IdImpl("10")); 
		assertEquals( plainPathNodes.get(1).getId() , new IdImpl("15"));
		assertEquals( plainPathNodes.get(2).getId() , new IdImpl("20"));  //transfer
		assertEquals( plainPathNodes.get(3).getId() , new IdImpl("20"));  //transfer
		assertEquals( plainPathNodes.get(4).getId() , new IdImpl("21"));
		assertEquals( plainPathNodes.get(5).getId() , new IdImpl("22"));
		assertEquals( plainPathNodes.get(6).getId() , new IdImpl("23"));
		assertEquals( plainPathNodes.get(7).getId() , new IdImpl("24"));
	
		/**tests TransitRouteFinder class*/
		TransitRouteFinder transitRouteFinder= new TransitRouteFinder (transitSchedule);
		PopulationImpl population = new PopulationImpl();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population, plainNetwork);
		plansReader.readFile(PLANFILE);
		PersonImpl person = population.getPersons().get(new IdImpl("1")); 
		PlanImpl plan = person.getPlans().get(0);

		ActivityImpl act1 = (ActivityImpl)plan.getPlanElements().get(0);
		ActivityImpl act2 = (ActivityImpl)plan.getPlanElements().get(2);
		List<LegImpl> legList = transitRouteFinder.calculateRoute (act1, act2, person);
		
		assertEquals( legList.get(0).getMode() , TransportMode.walk);
		assertEquals( legList.get(1).getMode() , TransportMode.pt);
		assertEquals( legList.get(2).getMode() , TransportMode.pt);
		assertEquals( legList.get(3).getMode() , TransportMode.walk);
		
	}
	
}