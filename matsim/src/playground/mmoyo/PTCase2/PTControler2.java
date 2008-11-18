package playground.mmoyo.PTCase2;

import org.matsim.network.Node;
import java.util.Map;
import java.util.Iterator;
import org.matsim.network.Link;
import org.matsim.population.Route;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;

public class PTControler2 {

	private static final String CONFIG= "../shared-svn/studies/schweiz-ivtch/pt-experimental/config.xml";
	private static final String ZURICHPTN= "../shared-svn/studies/schweiz-ivtch/pt-experimental/network.xml";
	private static final String ZURICHPTTIMETABLE= "../shared-svn/studies/schweiz-ivtch/pt-experimental/PTTimetable.xml";
	//private static final String ZURICHPTPLANS= "../shared-svn/studies/schweiz-ivtch/pt-experimental/plans.xml";
	//private static final String OUTPUTPLANS= "../shared-svn/studies/schweiz-ivtch/pt-experimental/output_plans.xml";
	
	private static final String ZURICHPTPLANS= "C://Users/manuel/Desktop/TU/Zuerich/plans.xml";
	private static final String OUTPUTPLANS= "c://output_plans.xml";
	
	/*
	//Variables for the net 5x5
	private static final String ZURICHPTN="C://Users/manuel/Desktop/berlinOEV/OhneCity/5x5Network.xml";
	private static final String ZURICHPTTIMETABLE="C://Users/manuel/Desktop/berlinOEV/OhneCity/5x5Timetable.xml";
	private static final String ZURICHPTPLANS="C://Users/manuel/Desktop/berlinOEV/OhneCity/5x5plans.xml";
	private static final String CONFIG="C://Users/manuel/Desktop/berlinOEV/OhneCity/config.xml";
	 */	
	
	public static void main(String[] args){
		PTOb pt= new PTOb(CONFIG, ZURICHPTN, ZURICHPTTIMETABLE,ZURICHPTPLANS, OUTPUTPLANS); 
		
		int option =2;
		switch (option){
	    	case 0: 
	    		Node ptNode = pt.getPtNetworkLayer().getNode("100048");
	    		Node ptNode2 = pt.getPtNetworkLayer().getNode("101456");
	    		pt.getPtRouter2().PrintRoute(pt.getPtRouter2().findRoute(ptNode, ptNode2, 391));
	    		break;
	    	case 1:
    			Coord coord1= new CoordImpl(680291,248300);
    			Coord coord2= new CoordImpl(684573,246805);
	    		Route route = pt.getPtRouter2().forceRoute(coord1, coord2, 24060,400);
	    		//ptNetworkFactory.printLinks(ptNetworkLayer);
	    		pt.getPtRouter2().PrintRoute(route);
	    		break;
	    	case 2:
	    		PTActWriter ptActWriter = new PTActWriter(pt);
	    		ptActWriter.writePTActsLegs();
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


/*
 PTActivityCreator ptActivityCreator= new PTActivityCreator(pt.getPtNetworkLayer(), CONFIG, ZURICHPTPLANS, pt.getPtRouter2());
ptActivityCreator.createPTActs(OUTPUTPLANS,pt.getPtTimeTable());
*/