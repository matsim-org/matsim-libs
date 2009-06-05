package playground.mmoyo.PTTest;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.api.population.Population;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mmoyo.PTCase2.PTOb;
import playground.mmoyo.PTCase2.PTRouter2;
import playground.mmoyo.Validators.PathValidator;

public class PTconnectionfinder {
	private NetworkLayer net; 
	private Population population;
	private PTRouter2 ptRouter;
	
	public PTconnectionfinder(final PTOb ptOb){
		net = ptOb.getPtNetworkLayer();
		population = new PopulationImpl();
		ptRouter= ptOb.getPtRouter2();
		
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population,net);
		String plansFile = ptOb.getPlansFile();
		plansReader.readFile(plansFile);
	}
	
	public PTConnection findConnection (Coord coord1, Coord coord2, double time, double distToWalk){
		PTConnection ptConnection = new PTConnection(); 
		List<Stretch> stretches = new ArrayList<Stretch>();
		Stretch stretch;
		
		double distanceToDestination = CoordUtils.calcDistance(coord1, coord2);
		if (distanceToDestination<= distToWalk){
			double duration = distanceToDestination * 1.3;
			
			stretch = new Stretch();
			stretch.setEnd(time + duration );
			stretch.setStart(time);
			//stretch.setType();
			stretches.add(stretch);
			
			ptConnection.setDuration(distanceToDestination * 1.3);
			ptConnection.setLength(distanceToDestination);
			ptConnection.setStretches(stretches);
		}else{
			Path path = ptRouter.findPTPath(coord1, coord2, time, distToWalk);
			
			double dw1 = net.getLink("linkW1").getLength();
			double dw2 = net.getLink("linkW2").getLength();
			if ((dw1+dw2)>=(distanceToDestination)){
				
				//if( ptPathValidator.isValid(path)){valid++;}else{invalid++;}
			}
		}
		return ptConnection;
	}
	
	public void readPlans () { 
		int x=0;
		PathValidator ptPathValidator = new PathValidator ();
		int valid=0;
		int invalid=0;
		int trips=0;
		List<Double> durations = new ArrayList<Double>();  
	
		for (Person person: population.getPersons().values()) {
			//if (true){
			//Person person = population.getPersons().get(new IdImpl("3937204"));
			
			System.out.println(x + " id:" + person.getId());
			Plan plan = person.getPlans().get(0);
	
			boolean first =true;
			boolean addPerson= true;
			Activity lastAct = null;
			Activity thisAct= null;
			int legNum=0;
			double travelTime=0;
			
			double startTime=0;
			double duration=0;
			
			Plan newPlan = new PlanImpl(person);
			
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					thisAct= (Activity) pe;
					if (!first) {
						Coord lastActCoord = lastAct.getCoord();
						Coord actCoord = thisAct.getCoord();
						trips++;
						//if( ptPathValidator.isValid(path)){valid++;}else{invalid++;}
					}
				}
			}
			lastAct = thisAct;
			first=false;
			x++;
		}//for person
	}
	
	private void createConnection(Path path, double startTime) {
		String lastType = "";
		Path stretchPath;
		double endTime;
		for(Link link: path.links){
			String type = link.getType();
			if (type == lastType) {
				//stretchPath.links.add(link);
			}else{
				//Stretch stretch = new Stretch(lastType, startTime, stretchPath, endTime);
			}
			lastType=type;
		}
	} 


}
