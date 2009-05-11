package playground.mmoyo.Pedestrian;

import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.api.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactory;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

public class Main {

	private static final String PATH = "../shared-svn/studies/schweiz-ivtch/pt-experimental/";
	
	private static final String LOGICNETFILE = PATH + "network.xml";
	private static final String NETFILE = PATH + "pedestrian/ivtch-osm.xml";
	private static final String INTPUTPLAN = PATH + "output_plans.xml";
	private static final String OUTPUTPLAN = PATH + "pedestrian/PedestrianOutput.xml";
	private static final String CONFIGFILE = PATH + "pedestrian/config.xml";
	private Config config = Gbl.createConfig(new String[]{CONFIGFILE, "http://www.matsim.org/files/dtd/plans_v4.dtd"});
	
	public static void main(String[] args){

		//Gbl.getConfig().plans().get
		System.out.println("ROUTING TO THE PT STATIONS");
		
		NetworkFactory networkFactory = new NetworkFactory();
		
		NetworkLayer logicNet= new NetworkLayer(networkFactory);
		MatsimNetworkReader logicMatsimNetworkReader = new MatsimNetworkReader(logicNet);
		logicMatsimNetworkReader.readFile(LOGICNETFILE);
		
		NetworkLayer net= new NetworkLayer(networkFactory);
		MatsimNetworkReader matsimNetworkReader2 = new MatsimNetworkReader(net);
		matsimNetworkReader2.readFile(NETFILE);
		
		Dijkstra dijkstra = new Dijkstra(logicNet, new PedTravelCost(), new PedTravelTime());
		
		Population population = new PopulationImpl();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population,logicNet);
		plansReader.readFile(INTPUTPLAN);
		
		int found =0;
		int notFound=0;
		int walking =0 ;
		for (Person person: population.getPersons().values()) {
		
			Plan plan = person.getPlans().get(0);
			Plan newPlan = new PlanImpl(person); 
			
			boolean first= true;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg= (Leg) pe;
					leg.setRoute(null);
					
					Activity prevAct = plan.getPreviousActivity(leg);
					Activity nextAct = plan.getNextActivity(leg);

					if (leg.getMode().equals(TransportMode.walk)){

				    	Node node1 = prevAct.getLink().getToNode();
				    	Node node2 = nextAct.getLink().getToNode();
				    	double time= prevAct.getStartTime();
				    		
				    	Path path = dijkstra.calcLeastCostPath(node1, node2, time);
				    	if(path!=null){
				    		NetworkRoute legRoute = new LinkNetworkRoute(null, null); 
				    		legRoute.setLinks(null, path.links, null);
				    		leg.setRoute(legRoute);
				    		found++;
						}else {
							notFound++;
						}
					}
			    	
					if (first){
			    		newPlan.addActivity(cloneAct(prevAct));
			    		first=false;
			    	}
			    	newPlan.addLeg(leg);
			    	newPlan.addActivity(cloneAct(nextAct));
				}
			}
			person.exchangeSelectedPlan(newPlan, true);
		}

		/*
		System.out.println("writing output plan file...");
		System.out.println(OUTPUTPLAN==null);
		new PopulationWriter(population, OUTPUTPLAN, "v4").write();
		System.out.println("Done");
		
		System.out.println("Found:" + found + "not found:"+ notFound);
		System.out.println("walk:" + walking);
		*/
	}

	private static Activity cloneAct(Activity act){
		Activity newAct= new ActivityImpl(act.getType(), act.getCoord());
		newAct.setStartTime(act.getStartTime());
		newAct.setEndTime(act.getEndTime());
		return newAct;
	}
	
	
}
