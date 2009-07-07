package playground.mmoyo.TransitSimulation;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.matsim.api.basic.v01.Coord;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactory;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.xml.sax.SAXException;
import playground.marcel.pt.transitSchedule.TransitScheduleBuilderImpl;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderV1;
import playground.marcel.pt.transitSchedule.api.TransitSchedule;
import playground.marcel.pt.transitSchedule.api.TransitScheduleBuilder;
import playground.mmoyo.PTRouter.PTActWriter;
import playground.mmoyo.PTRouter.PTRouter2;

/**
 * This class contains the options to route with a TransitSchedule object 
 */
public class Main {
	//private static final String PATH = "../shared-svn/studies/schweiz-ivtch/pt-experimental/";
	private static final String PATH = "../shared-svn/studies/schweiz-ivtch/pt-experimental/5x5/";
	private static final String CONFIG =  PATH  + "config.xml";
	private static final String PLANFILE = PATH + "plans.xml";
	private static final String OUTPUTPLANS = PATH + "output_plans_with_Transit.xml";
	private static final String PLAINNETWORK = PATH + "plainNetwork.xml";
	private static final String LOGICNETWORK = PATH + "logicNetwork.xml";
	private static final String LOGICTRANSITSCHEDULE = PATH + "logicTransitSchedule.xml";
	private static final String TRANSITSCHEDULEFILE = PATH + "transitSchedule.xml";
	
	public static void main(String[] args) {
		NetworkLayer plainNet= new NetworkLayer(new NetworkFactory());
		TransitScheduleBuilder builder = new TransitScheduleBuilderImpl();
		TransitSchedule transitSchedule = builder.createTransitSchedule();
		PTActWriter ptActWriter;
		
		/***************reads the transitSchedule file**********/
		new MatsimNetworkReader(plainNet).readFile(PLAINNETWORK);   //read plain Net, it should proceed of original data source, here it is the same plain network*/
		try {
			new TransitScheduleReaderV1(transitSchedule, plainNet).readFile(TRANSITSCHEDULEFILE);   //read transitScheduleFile
		} catch (SAXException e){
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		/*******************************************************/
	
		
		LogicFactory logicFactory = new LogicFactory(transitSchedule); // Creates logic elements: logicNetwork, logicTransitSchedule, logicToPlanConverter
		
		int option =1;
		switch (option){
			case 1:    //writes logicElement files
				logicFactory.writeLogicElements(PLAINNETWORK, LOGICTRANSITSCHEDULE, LOGICNETWORK);
				break;
			
			case 2:  //searches and shows a PT path between two coordinates or nodes */  
				plainNet=logicFactory.getPlainNet();
				PTRouter2 ptRouter = logicFactory.getPTRouter();
				
				Coord coord1 = new CoordImpl(747420, 262794);
				Coord coord2 = new CoordImpl(685862, 254136);
				NodeImpl nodeA = plainNet.getNode("8506000");
				NodeImpl nodeB = plainNet.getNode("8503309");
				Path path = ptRouter.findPTPath (coord1, coord2, 24372, 300);
				System.out.println(path.links.size());
				for (LinkImpl l : path.links){
					System.out.println(l.getId()+ ": " + l.getFromNode().getId() + " " + l.getType() + l.getToNode().getId() );
				}
				break;

			case 3: //Routes a population/
	    		ptActWriter = new PTActWriter(transitSchedule, CONFIG, PLANFILE, OUTPUTPLANS);
	    		ptActWriter.findRouteForActivities();
	    		//new PopulationWriter(ptActWriter.detouredPopulation, PATH + "detouredplans.xml", "v4").write();
	    		break;

			case 4:  //tests the TransitRouteFinder class with the population of PTActWriter class
				ptActWriter = new PTActWriter(transitSchedule, CONFIG, PLANFILE, OUTPUTPLANS);
				ptActWriter.printPTLegs(transitSchedule);
				break;

			case 5: //creates GIS net from plainNetwork*/
				/*
				plainNet=logicFactory.getPlainNet();
				org.matsim.utils.gis.matsim2esri.network.Nodes2ESRIShape nodes2ESRIShape = new Nodes2ESRIShape(plainNet, PATH + "ESRISNet.xml");
				nodes2ESRIShape.write();
				*/
				break;
				
			case 6:
				//agent 35420 
				NodeImpl node1 = plainNet.getNode("299598");
				NodeImpl node2 = plainNet.getNode("8503006");
				double distance = CoordUtils.calcDistance(node1.getCoord(), node2.getCoord());
				System.out.println(distance);
				//-->check if they nodes are joined by  detTransfer link 
				break;

				
				
		}
	}
}
