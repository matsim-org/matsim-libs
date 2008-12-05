package playground.mmoyo.PTCase2;

import java.util.Iterator;
import java.util.Map;

import org.matsim.network.Link;
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
		switch (option){
	    	case 0: 
	    		pt.readPTNet(ZURICHPTN);
	    		Node ptNode = pt.getPtNetworkLayer().getNode("100048");
	    		Node ptNode2 = pt.getPtNetworkLayer().getNode("101456");
	    		pt.getPtRouter2().PrintRoute(pt.getPtRouter2().findRoute(ptNode, ptNode2, 391));
	    		break;
	    	case 1:
	    		pt.readPTNet(ZURICHPTN);
	    		Coord coord1= new CoordImpl(680291,248300);
    			Coord coord2= new CoordImpl(684573,246805);
	    		Path path = pt.getPtRouter2().forceRoute(coord1, coord2, 24060,400);
	    		//ptNetworkFactory.printLinks(ptNetworkLayer);
	    		pt.getPtRouter2().PrintRoute(path);
	    		break;
	    	case 2:
	    		pt.createPTNetWithTLinks(INPTNETFILE,ZURICHPTN);
	    		break;
	    	case 3:
	    		pt.readPTNet(ZURICHPTN);
	    		PTActWriter ptActWriter = new PTActWriter(pt);
	    		ptActWriter.writePTActsLegs();
	    		break;
	    	case 4:
	    		pt.readPTNet(ZURICHPTN);
	    		PTTester pttester= new PTTester(pt);
	    		pttester.countRoutes();
	    		
	    		break;
		}//switch
	}//main
	
	public static void showMap(Map map) {
		Iterator iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			Link link= (Link)entry.getValue();
			System.out.println(entry.getKey() + " = " + link.toString());
		}
		iter = null;
	}
	
}//Class
