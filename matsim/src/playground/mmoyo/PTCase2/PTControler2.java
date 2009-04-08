package playground.mmoyo.PTCase2;

import java.util.Collection;
import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;
import playground.mmoyo.Validators.NetValidator;
import playground.mmoyo.Validators.PlanValidator;
import playground.mmoyo.input.PTLineAggregator;

public class PTControler2 {
    private static String path = "../shared-svn/studies/schweiz-ivtch/pt-experimental/"; 
    //private static String path = "C://Users/manuel/Desktop/TU/scenarios/Zuerich/";
    //private static String path = "C://Users/manuel/Desktop/TU/scenarios/5x5";
    
	private static final String CONFIG= "../shared-svn/studies/schweiz-ivtch/pt-experimental/config.xml";
	private static final String ZURICHPTN= path + "network.xml";
	private static final String ZURICHPTTIMETABLE= path + "PTTimetable.xml";
	private static final String INPTNETFILE= path + "inptnetfile.xml";
	private static final String ZURICHPTPLANS= path + "plans.xml";
	private static final String OUTPUTPLANS= path + "output_plans.xml";
	
	public static void main(String[] args){
		PTOb pt= new PTOb(CONFIG, INPTNETFILE, ZURICHPTN, ZURICHPTTIMETABLE,ZURICHPTPLANS, OUTPUTPLANS); 
		
		int option =3;
		
		if (option>0){pt.readPTNet(ZURICHPTN);}
		switch (option){
		
			case -4:
				String plansFile = path + "@All_plans .xml";
				plansFile = path + "plans.xml";
				
				PlanValidator planValidator = new PlanValidator(); 
				int num = planValidator.PlanCounter(pt.getPtNetworkLayer(), plansFile);
				System.out.println(num);
				
				break;
			case -3:
				//pt.createPTNetWithTLinks(INPTNETFILE,ZURICHPTN);
	    		pt.readPTNet(INPTNETFILE);
				String newNodesfilePath="C://Users/manuel/Desktop/TU/ZH_Files/bus/Basic_Bus_Network.xml";
				PTLineAggregator ptLineAggregator = new PTLineAggregator(newNodesfilePath, pt.getPtNetworkLayer(), pt.getPtTimeTable());
				ptLineAggregator.AddLine();
	    		//pt.writeNet(ZURICHPTN);
				break;
			case -2:
				pt.createPTNetWithTLinks(INPTNETFILE);
				pt.writeNet(ZURICHPTN);
				pt.readPTNet(ZURICHPTN);
	    		NetValidator netValidator = new NetValidator(pt.getPtNetworkLayer(), pt.getPtTimeTable());
	    		netValidator.printNegativeTransferCosts(28800);
				break;
			case -1:
				pt.createPTNetWithTLinks(INPTNETFILE);
				pt.getPtNetworkFactory().CreateDetachedTransfers(pt.getPtNetworkLayer(), 300, pt.getPtTimeTable());
				pt.writeNet(ZURICHPTN);
	    		pt.readPTNet(ZURICHPTN);
	    		PTActWriter ptActWriter1 = new PTActWriter(pt);
	    		ptActWriter1.writePTActsLegs();
	    		
				break;
			case 0: 
	    		pt.createPTNetWithTLinks(INPTNETFILE);
	    		//Map<String, List<IdImpl>> intersecionMap = pt.getPtNetworkFactory().createIntersecionMap(pt.getPtTimeTable());
	    		//new StationValidator().validateStations(pt.getPtNetworkLayer(),intersecionMap);
	    		break;
	    	case 1:
	    		Coord coord1= new CoordImpl(708146,243607);
    			Coord coord2= new CoordImpl(709915,244793);
	    		Path path = pt.getPtRouter2().findRoute(coord1, coord2, 24060,400);
	    		//ptNetworkFactory.printLinks(ptNetworkLayer);
	    		pt.getPtRouter2().PrintRoute(path);
	    		break;
	    	case 2:
	    		Node ptNode = pt.getPtNetworkLayer().getNode("100048");
	    		Node ptNode2 = pt.getPtNetworkLayer().getNode("101456");
	    		pt.getPtRouter2().PrintRoute(pt.getPtRouter2().findRoute(ptNode, ptNode2, 391));
	    		break;
	    	case 3:
	    		double startTime = System.currentTimeMillis();
	    		PTActWriter ptActWriter = new PTActWriter(pt);
	    		ptActWriter.writePTActsLegs();
	    		double duration= System.currentTimeMillis()-startTime;
	    		System.out.println("duration total:" +  duration);	    		
	    		//ptActWriter.ptTravelTime.costValidator.printNegativeVaues();
	    		//System.out.println(ptActWriter.ptTravelTime.costValidator==null);
	    		break;
	 
	    	case 4:
	    		PTTester pttester= new PTTester(pt);
	    		pttester.countRoutes();
	    		break;
	    	case 5:
	
	    		Coord coord6= new CoordImpl(708146,243607);
	    		double distance= 4500;
	    		Collection <Node> nearNodes = pt.getPtNetworkLayer().getNearestNodes(coord6, distance);
	    		
	    		System.out.println ("Nodes near " + coord6.toString());
	    		for (Node nearNode : nearNodes){
	    			System.out.println (nearNode.getId());
	    		}
	    		break;
	    	case 6:
	    		pt.getPtNetworkFactory().CreateDetachedTransfers(pt.getPtNetworkLayer(), 200, pt.getPtTimeTable());
	    		pt.getPtNetworkFactory().writeNet(pt.getPtNetworkLayer(), ZURICHPTN);
	    		break;
	    	case 7:
	    		Node nodeA= pt.getPtNetworkLayer().getNode("~101220");     
	    		Node nodeB=pt.getPtNetworkLayer().getNode("~8587652");
	    		System.out.println(nodeA==null);
	    		System.out.println(nodeB==null);
	    		boolean connected= pt.getPtNetworkFactory().areConected(nodeA, nodeB);
	    		System.out.println(connected);
	    		break;
	    	case 8:
	    	
	    	break;
		}//switch
	}//main

	
}//Class
