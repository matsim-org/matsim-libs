package playground.mmoyo.PTCase2;

import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

import java.util.List;
import java.util.Map;
import java.util.Iterator;
import org.matsim.network.Link;
import org.matsim.population.Leg;
import org.matsim.population.Route;
import org.matsim.utils.geometry.Coord;
import org.matsim.gbl.Gbl;
import java.util.ArrayList;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.PopulationWriter;

public class PTControler2 {

	// Variables for Zürich
	//private static final String ZURICHPTN="C://Users/manuel/Desktop/@TU/Zuerich/network.xml";

	//Only Tram lines
	private static final String ZURICHPTN="C://Users/manuel/Desktop/TU/Zuerich/TRAMnetwork.xml";
	private static final String ZURICHPTTIMETABLE="C://Users/manuel/Desktop/TU/Zuerich/PTTimetable.xml";
	private static final String ZURICHPTPLANS="C://Users/manuel/Desktop/TU/Zuerich/plans.xml";
	private static final String CONFIG="C://Users/manuel/Desktop/berlinOEV/OhneCity/config.xml";
	
	/*
	private static final String ZURICHPTN="../TRAMnetwork.xml";
	private static final String ZURICHPTTIMETABLE="../PTTimetable.xml";
	private static final String ZURICHPTPLANS="../plans.xml";
	private static final String CONFIG="../config.xml";
	*/
	
	/*
	//Variables for the net 5x5
	private static final String ZURICHPTN="C://Users/manuel/Desktop/berlinOEV/OhneCity/5x5Network.xml";
	private static final String ZURICHPTTIMETABLE="C://Users/manuel/Desktop/berlinOEV/OhneCity/5x5Timetable.xml";
	private static final String ZURICHPTPLANS="C://Users/manuel/Desktop/berlinOEV/OhneCity/5x5plans.xml";
	private static final String CONFIG="C://Users/manuel/Desktop/berlinOEV/OhneCity/config.xml";
	 */	
	
	//Case ivtch
	//private static final String ZURICHPTN="C://Users/manuel/Eclipseworkspace/schweiz-ivtch/baseCase/network/ivtch-osm.xml";
	
	public static void main(String[] args){
		PTTimeTable2 ptTimeTable = new PTTimeTable2(ZURICHPTTIMETABLE);
		PTNetworkFactory2 ptNetworkFactory =new PTNetworkFactory2();
		NetworkLayer ptNetworkLayer = ptNetworkFactory.createNetwork(ZURICHPTN, ptTimeTable);
		PTRouter2 ptRouter2 = new PTRouter2(ptNetworkLayer, ptTimeTable);

		int option =2;
		switch (option){
	    	case 0: 
	    		Node ptNode = ptNetworkLayer.getNode("100821");
	    		Node ptNode2 = ptNetworkLayer.getNode("100917");
	            ptRouter2.PrintRoute(ptRouter2.findRoute(ptNode, ptNode2, 10));
	    		break;
	    	case 1:
    			Coord coord1= new CoordImpl(682275,263203);
    			Coord coord2= new CoordImpl(683064,262261);
	    		Route route = ptRouter2.findRoute(coord1, coord2,50700);
	    		//ptNetworkFactory.printLinks(ptNetworkLayer);
	    		ptRouter2.PrintRoute(route);
	    		//System.out.println(route.getRoute().toString());
	    		
	    		break;
	    	case 2:
	    		PTActivityCreator ptActivityCreator= new PTActivityCreator(ptNetworkLayer, CONFIG, ZURICHPTPLANS, ptRouter2);
	    		ptActivityCreator.createPTActs("c://@output_plans.xml");
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


