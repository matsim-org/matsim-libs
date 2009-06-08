package playground.mmoyo.PTCase2;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactory;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.mmoyo.TransitSimulation.ToTransitScheduleConverter;
import playground.mmoyo.TransitSimulation.TransitScheduleToPTTimeTableConverter;
import playground.mmoyo.Validators.NetValidator;
import playground.mmoyo.Validators.PTLineValidator;
import playground.mmoyo.Validators.StationValidator;
import playground.mmoyo.input.PTLineAggregator;


/** 
 * Executable class to perform data input, validations and routing test according to timetable information
 */
public class PTControler2 /*extends MatsimTestCase*/ { // classes using MatsimTestCase must be in the test/src-directory!
    private static final String path = "../shared-svn/studies/schweiz-ivtch/pt-experimental/"; 
    //private static final  String path = "C://Users/manuel/Desktop/TU/scenarios/Zuerich/";
    //private static final String path = "C://Users/manuel/Desktop/TU/scenarios/5x5";
    
	private static final String CONFIG=  path  + "config.xml";
	private static final String ZURICHPTN= path + "network.xml";
	private static final String ZURICHPTTIMETABLE= path + "PTTimetable.xml";
	private static final String INPTNETFILE= path + "inptnetfile.xml";
	private static final String ZURICHPTPLANS= path + "plans.xml";
	private static final String ALLPLANS= path + "@All_plans .xml";
	private static final String OUTPUTPLANS= path + "output_plans.xml";
	private static final String INPTNEWLINES= path + "TestCase/InPTDIVA.xml";
	private static final String DIVNODES= path + "TestCase/DivNodes.xml";
	
	// disabled this test-case. Tests must be in test/src/*, otherwise the code doesn't compile.  marcel/8jun2009
//	public void test1() {
//		PTOb pt= new PTOb(CONFIG, ZURICHPTN, ZURICHPTTIMETABLE,ZURICHPTPLANS, OUTPUTPLANS); 
//		pt.readPTNet(ZURICHPTN);
//
//		// searches and shows a PT path between two coordinates 
//		Coord coord1 = new CoordImpl(747420, 262794);   
//		Coord coord2 = new CoordImpl(685862, 254136);
//		Path path2 = pt.getPtRouter2().findPTPath (coord1, coord2, 24372, 300);
//		System.out.println(path2.links.size());
//		for (Link link : path2.links){
//			System.out.println(link.getId()+ ": " + link.getFromNode().getId() + " " + link.getType() + link.getToNode().getId() );
//		}
//		
//		assertEquals( 31, path2.links.size() ) ;
//		assertEquals( "1311" , path2.links.get(10).getId().toString() ) ;
//		assertEquals( "250" , path2.links.get(20).getId().toString() ) ;
//	}
	
	public static void main(String[] args){
		PTOb pt= new PTOb(CONFIG, ZURICHPTN, ZURICHPTTIMETABLE,ZURICHPTPLANS, OUTPUTPLANS); 
		// seems that INPTNETFILE is never used in PTOb and should be removed as a parameter so that it 
		// becomes clear that all conversions are done somewhere else.  kai, may'09 //  DONE
		
		int option =5;
		
		if (option>0){pt.readPTNet(ZURICHPTN);}
		switch (option){
			case -5: 
				/**creates net from ZVV diva2Web database, periodic departures as timetable and route agents */  
				NetworkLayer net = new NetworkLayer();
				pt.setPtNetworkLayer(net);
				PTLineAggregator ptLineAggregator = new PTLineAggregator(INPTNEWLINES, pt.getPtNetworkLayer(), pt.getPtTimeTable());
				ptLineAggregator.AddLines();
	
				pt.getPtNetworkFactory().createTransferLinks(pt.getPtNetworkLayer(), pt.getPtTimeTable());
				pt.getPtNetworkFactory().CreateDetachedTransfers(pt.getPtNetworkLayer(), 300);
				
				pt.writeNet(ZURICHPTN);
				pt.createRouter();
				PTActWriter ptActWriter1 = new PTActWriter(pt);
	    		ptActWriter1.findRouteForActivities();
				break;
			
			case-4:
	    		/** validates the id assignation for nodes of ZVV diva2Web database*/
				NetworkLayer netDiv= new NetworkLayer(new NetworkFactory());
	    		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(netDiv);
	    		matsimNetworkReader.readFile(DIVNODES);
	    		
	    		StationValidator StationValidator = new StationValidator(pt.getPtNetworkLayer());
	    		StationValidator.validateIds(netDiv);
				break;

			case -3: 
				/** adds in memory extra PTlines from a separate file creating alias nodes but 
				does not write the new network*/
				pt.createPTNetWithTLinks(INPTNETFILE);
	    		pt.readPTNet(INPTNETFILE);
				ptLineAggregator = new PTLineAggregator(INPTNEWLINES, pt.getPtNetworkLayer(), pt.getPtTimeTable());
				ptLineAggregator.AddLine();
	    		pt.writeNet(ZURICHPTN);
	    		// Is it true that this does not write the network as the comment says?  What does "writeNet" do? kai, jun'09
	    		
				break;
				
			case -2:  
				/** validates that the network does not have standard links with negative values. 
				 * This is possible when a PTL has two or more profiles and a intermediate station of a profile 
				 * has a greater value than the same stretch in other profile*/
				pt.createPTNetWithTLinks(INPTNETFILE);
				pt.writeNet(ZURICHPTN);
				pt.readPTNet(ZURICHPTN);
	    		NetValidator netValidator = new NetValidator(pt.getPtNetworkLayer(), pt.getPtTimeTable());
	    		netValidator.printNegativeTransferCosts(28800);
				break;
			
			case -1:
				/* Creates and writes detached transfer links. This is necessary before routing to get better results */  
				pt.createPTNetWithTLinks(INPTNETFILE);
				pt.getPtNetworkFactory().CreateDetachedTransfers(pt.getPtNetworkLayer(), 300);
				pt.writeNet(ZURICHPTN);
	    		pt.readPTNet(ZURICHPTN);
	    		PTActWriter ptActWriter2 = new PTActWriter(pt);
	    		ptActWriter2.findRouteForActivities();
				break;
			
		/************the positive cases need the network already loaded in memory*/
			case 0: 
	    		pt.createPTNetWithTLinks(INPTNETFILE);
	    		//Map<String, List<IdImpl>> intersecionMap = pt.getPtNetworkFactory().createIntersecionMap(pt.getPtTimeTable());
	    		//new StationValidator().validateStations(pt.getPtNetworkLayer(),intersecionMap);
	    		break;
	    	
			case 1:
				/**searches and shows a PT path between two coordinates or nodes */
				Coord coord1 = new CoordImpl(747420, 262794);   
				Coord coord2 = new CoordImpl(685862, 254136);
				Node nodeA = pt.getPtNetworkLayer().getNode("_8506000");
				Node nodeB = pt.getPtNetworkLayer().getNode("_8503309");
				//Path path2 = pt.getPtRouter2().findRoute(nodeA, nodeB, 45386);
				Path path2 = pt.getPtRouter2().findPTPath (coord1, coord2, 24372, 300);
				System.out.println(path2.links.size());
				for (Link l : path2.links){
					System.out.println(l.getId()+ ": " + l.getFromNode().getId() + " " + l.getType() + l.getToNode().getId() );
				}
				break;

			case 2:
				/**invokes the ptActWriter to create a out_plan file with pt_legs and pt_acts and tries a simulation run*/
				//pt.getPtNetworkFactory().setDetNextLinks(pt.getPtNetworkLayer(), pt.getPtTimeTable());
	    		PTActWriter ptActWriter = new PTActWriter(pt);
	    		ptActWriter.findRouteForActivities();
	    		//ptActWriter.SimplifyPtLegs();
	    		//ptActWriter.printPTLegs();
	    		//ptActWriter.ptTravelTime.costValidator.printNegativeVaues();
	    		//System.out.println(ptActWriter.ptTravelTime.costValidator==null);
	    		break;
	 
	    	case 3:
	    		/**counts the number of routes found for a population */
	    		PTTester pttester= new PTTester(pt);
	    		pttester.countRoutes();
	    		break;

	    	case 4:
	    		/**Converter: from mmoyo.PTCase2.PTTimeTable2 to marcel.pt.transitSchedule.transitSchedule */
	    		ToTransitScheduleConverter converter = new ToTransitScheduleConverter();	    		
	    		//converter.createTransitSchedule(pt.getPtTimeTable(), path + "TransitSim/PtFacilities.xml", path + "TransitSim/transitSchedule.xml");
	    		//converter.createFacilities("../shared-svn/studies/schweiz-ivtch/pt-experimental/PtFacilities.xml");
	    		//converter.createFacilities(network);
	    		converter.createTransitSchedule(pt.getPtTimeTable(), pt.getPtNetworkLayer(), path + "transitSchedule.xml");
	    		break;
	    	case 5:
	    		/**reads a transitSchedule file to create a PTTimeTable and routes a population */
	    		TransitScheduleToPTTimeTableConverter transitScheduleToPTTimeTableConverter = new TransitScheduleToPTTimeTableConverter();
	    		PTTimeTable2 ptTimeTable = transitScheduleToPTTimeTableConverter.getPTTimeTable(path + "transitSchedule.xml", pt.getPtNetworkLayer());
	    		pt.setPTTimeTable(ptTimeTable); 
	    		PTActWriter ptActWriter3 = new PTActWriter(pt);
	    		ptActWriter3.findRouteForActivities();
	    		break;
	    	case 6:
	    		PTLineValidator ptLineValidator = new PTLineValidator(); 
	    		ptLineValidator.getIsolatedPTLines(pt.getPtTimeTable(), pt.getPtNetworkLayer());
	    		break;
	   
	    	
		}
	}
}






