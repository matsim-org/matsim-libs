package playground.mmoyo;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import java.util.List;
import org.matsim.api.basic.v01.Coord;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

import playground.marcel.pt.transitSchedule.TransitScheduleBuilderImpl;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderV1;
import playground.marcel.pt.transitSchedule.api.TransitSchedule;
import playground.marcel.pt.transitSchedule.api.TransitScheduleBuilder;
import playground.mmoyo.PTRouter.PTRouter2;
import playground.mmoyo.TransitSimulation.LogicFactory;
import playground.mmoyo.TransitSimulation.LogicIntoPlainTranslator;

/** makes different tests with the transit router on the 5x5 scenario*/
public class PTRouterTest extends MatsimTestCase {

	private static final String PATH = "../shared-svn/studies/schweiz-ivtch/pt-experimental/5x5/"; 
	private static final String PLAINNETWORK= PATH + "plainnetwork.xml";
	private static final String TRANSITSCHEDULE= PATH + "TransitSchedule.xml";
	
	
	public void testRouter() throws SAXException, ParserConfigurationException, IOException {
		/////read transit schedule, plain net create logic elements//////////
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(PLAINNETWORK);
		TransitScheduleBuilder builder = new TransitScheduleBuilderImpl();
		TransitSchedule transitSchedule = builder.createTransitSchedule();
		new TransitScheduleReaderV1(transitSchedule, network).readFile(TRANSITSCHEDULE);
		LogicFactory logicFactory = new LogicFactory(transitSchedule); // Creates logic elements: logicNetwork, logicTransitSchedule, logicToPlanConverter
		NetworkLayer logicNet= logicFactory.getLogicNet();
		PTRouter2 ptRouter = logicFactory.getPTRouter();
		////////////////////////////////////////////////////////
		
		///////Tests transfer times
		LinkImpl transferLink = logicNet.getLink("T39");  								 // first departure: 09:00 ,  last departure is at 19:00
		double waitTime1= ptRouter.ptTravelTime.getLinkTravelTime(transferLink, 67800);  //  67800= 16:50 .  The agent should wait 10 mins 
		double waitTime2= ptRouter.ptTravelTime.getLinkTravelTime(transferLink, 72000);  //  72000= 20:00 .  The agent should wait 13 hours, until next day
		//System.out.println(waitTime1);
		//System.out.println(waitTime2);
		assertEquals( 600, waitTime1 ) ;                                               
		assertEquals( 46800, waitTime2) ;


		Coord coord1 = new CoordImpl(1010, 5010);
		Coord coord2 = new CoordImpl(8950, 8950);
		Path path = ptRouter.findPTPath (coord1, coord2, 2808, 400);   //07:48 , 400 walk distance
		
		// TODO: test correct link sequence
		
		//test the path in scenario plain net
		LogicIntoPlainTranslator logicToPlainConverter = logicFactory.getLogicToPlainConverter();
		List <LinkImpl> logicList = logicToPlainConverter.convertToPlain(path.links);
		assertEquals(new IdImpl("W1"), logicList.get(0).getId());
		assertEquals(new IdImpl("22"), logicList.get(1).getId());
		assertEquals(new IdImpl("23"), logicList.get(2).getId());
		//assertEquals(new IdImpl("T39"), path.links.get(3).getId());  <-transfer
		assertEquals(new IdImpl("16"), logicList.get(4).getId());
		assertEquals(new IdImpl("17"), logicList.get(5).getId());
		assertEquals(new IdImpl("18"), logicList.get(6).getId());
		assertEquals(new IdImpl("19"), logicList.get(7).getId());
		assertEquals(new IdImpl("W2"), logicList.get(7).getId());
		
	
		
	}
	
}