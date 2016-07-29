package playground.singapore.springcalibration.preprocess;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;


public class PlanChecker {
	
	private final static Logger log = Logger.getLogger(PlanChecker.class);

	public static void main(String[] args) {
		PlanChecker corrector = new PlanChecker();
		corrector.run(args[0]);

	}
	
	public void run(String inputFile) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(inputFile);
		this.checkWork(scenario.getPopulation());
		
		log.info("finished ##############################################");
		
	}
	
	// there are many absurdly long walk legs, which do NOT diminish quick enough -> 
	// convert them to passenger legs (not car legs as passenger is a teleportation mode)
	private void check(Population population) {
		for (Person p : population.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			
			String planStr = "";
			boolean print = false;
			for (PlanElement pe : plan.getPlanElements()){	
				if(pe instanceof Leg){
					String mode = ((Leg) pe).getMode();
					planStr += "_" + mode;
					
 				}
				if(pe instanceof Activity){
					String type = ((Activity) pe).getType();
					planStr += "_" + type;
					String coord = ((Activity) pe).getCoord().toString();
					planStr += "_" + coord;
					String facility = null;
					
					if (((Activity) pe).getFacilityId() != null) facility =  ((Activity) pe).getFacilityId().toString();
					planStr += "_" + facility;	
					
					if (facility == null && !type.contains("transit") && !type.contains("interaction")) {
						planStr += "_ERROR_";
						print = true;
					}
					
 				}
 			}
			if (print) log.error("Person " + p.getId().toString() + " plan " + planStr);
		}
	}
	
	private void checkWork(Population population) {
		int cnt_1700_bulk = 0;
		int cnt_work = 0;
		for (Person p : population.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){	
				if(pe instanceof Activity){
					
					String type = ((Activity) pe).getType();
					if (type.equals("w_1030_0630") || type.equals("w_0800_1000") || type.equals("w_0900_0845")) {
						cnt_1700_bulk++;
					}
					if (type.startsWith("w_")) {
						cnt_work++;
					} 
					
 				}
 			}
			
		}
		log.info(100.0 * cnt_1700_bulk / cnt_work);
	}
	
	private void checkFreight(Population population) {
		int freightCnt = 0;
		int totalCnt = 0;
		for (Person p : population.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			boolean freight = false;
			for (PlanElement pe : plan.getPlanElements()){	
				if(pe instanceof Leg){
					String mode = ((Leg) pe).getMode();
					if (mode.equals("freight")) freight = true;
					
 				}
			}
			if (freight) freightCnt++;
			totalCnt++;
		}
		
		log.error(freightCnt + " freight agents, of " + totalCnt + " agents overall");
		}
	}


