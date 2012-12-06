package playground.ikaddoura.busCorridorPaper.analyze;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class PlansAnalysis {
	private final static Logger log = Logger.getLogger(PlansAnalysis.class);

	String netFile = "/Users/Ihab/ils/kaddoura/welfareBusCorridor_opt3/input_test/network_opt3.xml";
	
//	String plansFile = "/Users/Ihab/ils/kaddoura/welfareBusCorridor_opt3/output/test11_standard_50buses_fasterBuses_NTC_noCongestion/extIt0/extITERS/extIt0.0/internalIterations/output_plans.xml.gz";
	String plansFile = "/Users/Ihab/ils/kaddoura/welfareBusCorridor_opt3/output/test11_standard_50buses_fasterBuses_NTC_noCongestion/extIt0/extITERS/extIt0.1/internalIterations/output_plans.xml.gz";

	public static void main(String[] args) {
		PlansAnalysis analyse = new PlansAnalysis();
		analyse.run();
	}
	
	public void run() {
		
		Population population = getPopulation(netFile, plansFile);
		double userBenefits = 0.;		
		double userBenefitsPt = 0.;
		double userBenefitsTransitWalk = 0.;		
		double userBenefitsCar = 0.;		

		for (Person person : population.getPersons().values()){
			
			double execPlanScore = person.getSelectedPlan().getScore();
			
			if (execPlanScore <= 0.) {
				log.warn("execPlanScore <= 0");
			} else {
				
				boolean hasPtLeg = false;
				boolean hasCarLeg = false;
				boolean hasTransitWalkLeg = false;

				for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
					if (pE instanceof Leg) {
						Leg leg = (Leg) pE;
						String mode = leg.getMode();
						
						if (mode.equals(TransportMode.pt)){
							hasPtLeg = true;
						} else if (mode.equals(TransportMode.car)){
							hasCarLeg = true;
						} else if (mode.equals(TransportMode.transit_walk)){
							hasTransitWalkLeg = true;
						} else {
							log.warn("uknown leg mode");
						}
						
					}
				}
				
				if (hasPtLeg && !hasCarLeg) {
					userBenefitsPt = userBenefitsPt + execPlanScore;
				} else if (hasCarLeg && !hasPtLeg){
					userBenefitsCar = userBenefitsCar + execPlanScore;
				} else if (hasTransitWalkLeg && !hasPtLeg) {
					userBenefitsTransitWalk = userBenefitsTransitWalk + execPlanScore;
				} else {
					log.warn("uknown");
				}
				
				userBenefits = userBenefits + execPlanScore;
			}
		}
		
		log.info("UserBenefits (Sum of all executed plans): " + userBenefits);
		log.info("UserBenefits within CarMode (Sum of all executed (car-)plans): " + userBenefitsCar);
		log.info("UserBenefits within PtMode (Sum of all executed (pt-)plans): " + userBenefitsPt);
		log.info("UserBenefits within (only) TransitWalkMode (Sum of all executed (only transit_walk-)plans): " + userBenefitsTransitWalk);

	}
	
	private Population getPopulation(String netFile, String plansFile){
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();
		return population;
	}

}
