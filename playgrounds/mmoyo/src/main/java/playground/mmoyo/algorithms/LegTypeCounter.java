package playground.mmoyo.algorithms;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;

import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.utils.DataLoader;

/**
 *Counts the number of legs per Transport Mode
 */
 public class LegTypeCounter implements PersonAlgorithm{
	Map <String, Integer> mode_num_map = new TreeMap <String, Integer>();

	
	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()){
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					String mode = leg.getMode();
					if (!mode_num_map.keySet().contains( mode )){
						mode_num_map.put( mode , 0);
					}
					mode_num_map.put(mode , (mode_num_map.get(mode) + 1) );
				}
			}
		}
		
	}

	private void printModes(){
		for(Map.Entry <String,Integer> entry: mode_num_map.entrySet() ){
			String mode = entry.getKey(); 
			Integer value = entry.getValue();
			System.out.println(mode + " " + value );
		}
	}
	
//	public void run(final Population pop, String mode ){
//		int numAgentWcarlegs=0;
//		for (Person person : pop.getPersons().values()){
//			boolean hasCarLeg = false;
//			for (Plan plan : person.getPlans()){
//				for (PlanElement pe : plan.getPlanElements()){
//					if (pe instanceof LegImpl) {
//						Leg leg = (LegImpl)pe;
//						if(leg.getMode().equals(mode)){
//							 hasCarLeg = (hasCarLeg || true);
//						}
//					}
//				}
//			}
//			if (hasCarLeg){numAgentWcarlegs++;}
//		}
//		
//		log.info("agents = " + pop.getPersons().size());
//		log.info("agents with car leg = " + numAgentWcarlegs);
//	}
	
	public static void main(String[] args) {
		String netFilePath;
		String popFilePath;
		if (args.length>0){
			popFilePath = args[0];
			netFilePath = args[1];
		}else{
			popFilePath = "../../runs_manuel/CalibLineM44/automCalib10xTimeMutated/10xrun/poponlyM44.xml.gz";
			netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		}
		
		DataLoader dataLoader = new DataLoader ();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario(); 
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scn);
		matsimNetReader.readFile(netFilePath);
			
		LegTypeCounter legTypeCounter = new LegTypeCounter();
		new PopSecReader(scn, legTypeCounter).readFile(popFilePath);
		legTypeCounter.printModes();
		
	}



}