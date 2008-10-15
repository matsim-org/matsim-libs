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
import playground.mmoyo.PTRouter.PTNProximity;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.population.Act;
import org.matsim.basic.v01.BasicActImpl;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.PopulationWriter;
import org.matsim.gbl.Gbl;
import org.matsim.config.Config;

public class PTControler2 {

	// Variables for Zürich
	//private static final String ZURICHPTN="C://Users/manuel/Desktop/TU/Zuerich/network.xml";

	//Only Tram lines
	private static final String ZURICHPTN="C://Users/manuel/Desktop/TU/Zuerich/TRAMnetwork.xml";
	private static final String ZURICHPTTIMETABLE="C://Users/manuel/Desktop/TU/Zuerich/PTTimetable.xml";
	private static final String ZURICHPTPLANS="C://Users/manuel/Desktop/TU/Zuerich/plans.xml";
	private static final String CONFIG="C://Users/manuel/Desktop/berlinOEV/OhneCity/config.xml";
	
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
	    		
	    		//Node ptNode = ptNetworkLayer.getNode("103041b");
	    		//Node ptNode2 = ptNetworkLayer.getNode("100872");
	    		
	    		//Node ptNode = ptNetworkLayer.getNode("0");
	    		//Node ptNode2 = ptNetworkLayer.getNode("24");
	    		
	            ptRouter2.PrintRoute(ptRouter2.findRoute(ptNode, ptNode2, 10));
	    		break;
	    	case 1:
	    		
    			Coord coord1= new CoordImpl(680407, 247013);
    			Coord coord2= new CoordImpl(682264, 248263);
	    		Route route = ptRouter2.findRoute(coord1, coord2,100);
	    		//ptNetworkFactory.printLinks(ptNetworkLayer);
	    		ptRouter2.PrintRoute(route);
	    		//System.out.println(route.getRoute().toString());
	    		
	    		break;
	    	case 2:
	    		org.matsim.config.Config config = new org.matsim.config.Config();
	    		config = Gbl.createConfig(new String[]{CONFIG, "http://www.matsim.org/files/dtd/plans_v4.dtd"});
	    		//String configfile ="C://Users/manuel/Desktop/TU/Zuerich/config.xml";
	    		Gbl.setConfig(config);
	    		Gbl.getWorld().setNetworkLayer(ptNetworkLayer);
	
	    		org.matsim.population.Population plans = new org.matsim.population.Population(false);
	    		org.matsim.population.PopulationReaderMatsimV4 plansReader = new org.matsim.population.PopulationReaderMatsimV4(plans);
	    		plansReader.readFile(ZURICHPTPLANS);
	    		//plans.printPlansCount();
	    		
	    		//Version wth one act, one plan, one person
	    		//person = plans.getPerson("1001350");
	    		//org.matsim.population.Plan plan = person.getPlans().get(0);
	    		//plan.setSelected(true);
	    		//bad performance, use interators
	    		//org.matsim.population.Act firstAct = plan.getFirstActivity();
	    		//org.matsim.population.Leg firstLeg = plan.getNextLeg(firstAct);
	    		//org.matsim.population.Act secondAct = plan.getNextActivity(firstLeg);
	    		
	    		int x=0;
	    		for (Person person: plans.getPersons().values()) {
	    			System.out.println(++x + " person: " + person.getId());
	    			
	    			Plan plan = person.getPlans().get(0);
	    			plan.setSelected(false);
	    			Plan newPlan = new Plan(person);
	    			
	    			//Iterate in plans  of the person and insert the new ptActs - ptlegs in the new ptPlan
		    		boolean val =false;
		    		Act firstAct = null;
		    		for (Iterator iter= plan.getIteratorAct(); iter.hasNext();) {
		    			Act act= (Act)iter.next();
		    			
		    			if (val) {
		    				Act secondAct = act;	
		    				Coord c1 = firstAct.getCoord();
		    	    		Coord c2 = secondAct.getCoord();
		    	    		
		    	    		Route legRoute = ptRouter2.findRoute(c1, c2, firstAct.getEndTime());
		    	    		if(legRoute!=null){
			    	    		List<Object> listLegAct = new ArrayList<Object>();
			    	    		listLegAct=ptRouter2.findLegActs(legRoute, firstAct.getEndTime());
			    	    		for (Iterator<Object> iter2 = listLegAct.iterator(); iter2.hasNext();) {
			    	    			Object legAct = iter2.next();	
			    	    			// use "instanceOf" instead of this
			    	    			if(Act.class.isInstance(legAct)){
			    	    			//if(LegAct.getClass().toString().equals("class org.matsim.population.Act")){
			    	    				newPlan.addAct((Act)legAct);
			    	    			}else{
			    	    				newPlan.addLeg((Leg)legAct);
			    	    			}
			    	    		}
		    	    		}//if legRoute
		    			}else{
		    				newPlan.addAct(act);
		    				val=true;
		    			}//if val
	    				firstAct = act;
		    		}//for iterator iter 
	    		
		    		person.exchangeSelectedPlan(newPlan, true);
		    		person.removeUnselectedPlans();

	    		}//for Person person
	    		//Write outplan XML
	    		Gbl.getConfig().plans().setOutputFile("c://@output_plans.xml"); 
	    		Gbl.getConfig().plans().setOutputVersion("v4");
	    		final PopulationWriter plansWriter = new PopulationWriter(plans);
	    		plans.addAlgorithm(plansWriter);
	    		plansWriter.write();

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
//primer leg
Route ruta= listName.get(0).getRoute();
Link[] linkRoute= ruta.getLinkRoute();
for( int x=0 ; x< linkRoute.length;x++){
	//System.out.println(linkRoute[x]);
}

//segundo leg
Route ruta2= listName.get(1).getRoute();
Link[] linkRoute2= ruta2.getLinkRoute();
for( int x=0 ; x< linkRoute2.length;x++){
	System.out.println(linkRoute2[x]);
}
*/
