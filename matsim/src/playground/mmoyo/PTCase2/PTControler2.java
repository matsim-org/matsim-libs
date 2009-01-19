package playground.mmoyo.PTCase2;

import org.matsim.network.Node;
import org.matsim.router.util.LeastCostPathCalculator.Path;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;

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
	    	case 0: 
	    		pt.createPTNetWithTLinks(INPTNETFILE,ZURICHPTN);
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
	    		PTActWriter ptActWriter = new PTActWriter(pt);
	    		ptActWriter.writePTActsLegs();
	    		break;
	    	case 4:
	    		PTTester pttester= new PTTester(pt);
	    		pttester.countRoutes();
	    		break;
	    	case 5:
	    		playground.mmoyo.PTRouter.PTNProximity ptnProximity = new playground.mmoyo.PTRouter.PTNProximity(pt.getPtNetworkLayer());  
	    		Coord coord6= new CoordImpl(708146,243607);
	    		ptnProximity.printNearestBusStops(coord6, 3500);
	    		
	    		break;
		}//switch
	}//main
	
}//Class
