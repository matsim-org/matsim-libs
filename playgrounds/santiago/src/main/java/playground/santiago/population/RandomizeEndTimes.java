package playground.santiago.population;

import java.util.Random;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;

public class RandomizeEndTimes {
	
	private final static Logger log = Logger.getLogger(RandomizeEndTimes.class);
	final String plansFolder = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/plans/2_10pct/";
	final String plansFile = plansFolder + "expanded_plans_0.xml.gz";
	final String configFolder = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/";
	final String configFile = configFolder + "expanded_config_0.xml";
	final String runsWorkingDir = "../../../runs-svn/santiago/TMP/input/";
	final int standardDeviation = 33; //S.D. (in minutes)
	
	public static void main(String[] args) {

		RandomizeEndTimes ret = new RandomizeEndTimes();
		ret.run();
	}
	
	
	private void run (){
		
		Config config = ConfigUtils.loadConfig(configFile);		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());		
		PopulationReader pr = new PopulationReader(scenario);
		pr.readFile(plansFile);		
		Population population = scenario.getPopulation();	
		
		
		randomizeEndTimes(population, standardDeviation);		
		ActivityClassifier activityClassifier = new ActivityClassifier(scenario);
		activityClassifier.run();
		Population randomAndClassifiedPop = activityClassifier.getOutPop();		
		SortedMap<String, Double> acts = activityClassifier.getActivityType2TypicalDuration();
		setActivityParams(acts,config);
		
		write(randomAndClassifiedPop, config);
		
	}
	
	
	
	private double createRandomEndTime(Random random, int standardDeviation){
		//draw two random numbers [0;1] from uniform distribution
		double r1 = random.nextDouble();
		double r2 = random.nextDouble();
		
		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		double endTime = standardDeviation *60 * normal;
		
		return endTime;
	}
	
	private void randomizeEndTimes(Population population, int standardDeviation){
		log.info("Randomizing activity end times...");
		Random random = MatsimRandom.getRandom();
		for(Person person : population.getPersons().values()){
			double timeShift = 0.;
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if(pe instanceof Activity){
					Activity act = (Activity) pe;
					if(act.getStartTime() != Time.UNDEFINED_TIME && act.getEndTime() != Time.UNDEFINED_TIME){
						if(act.getEndTime() - act.getStartTime() == 0){
							timeShift += 1800.;
						}
					}
				}
			}
			
			Activity firstAct = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			Activity lastAct = (Activity) person.getSelectedPlan().getPlanElements().get(person.getSelectedPlan().getPlanElements().size()-1);
			
			double delta = 0;
			while(delta == 0){
				delta = createRandomEndTime(random, standardDeviation);
				if(firstAct.getEndTime() + delta < 0){
					delta = 0;
				}
				if(lastAct.getStartTime() + delta + timeShift > 24 * 3600){
					delta = 0;
				}
				if(lastAct.getEndTime() != Time.UNDEFINED_TIME){
					// if an activity end time for last activity exists, it should be 24:00:00
					// in order to avoid zero activity durations, this check is done
					if(lastAct.getStartTime() + delta + timeShift >= lastAct.getEndTime()){
						delta = 0;
					}
				}
			}
			
			for(int i = 0; i < person.getSelectedPlan().getPlanElements().size(); i++){
				PlanElement pe = person.getSelectedPlan().getPlanElements().get(i);
				if(pe instanceof Activity){
					Activity act = (Activity)pe;
					if(!act.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
						if(person.getSelectedPlan().getPlanElements().indexOf(act) > 0){
							act.setStartTime(act.getStartTime() + delta);
						}
						if(person.getSelectedPlan().getPlanElements().indexOf(act) < person.getSelectedPlan().getPlanElements().size()-1){
							act.setEndTime(act.getEndTime() + delta);
						}
					}
//					else {
//						log.warn("This should not happen! ");
//					}
				}
			}
		}
		log.info("...Done.");
	}
	
	private void setActivityParams(SortedMap<String, Double> acts, Config config) {
		for(String act :acts.keySet()){
			if(act.equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
				//do nothing
			} else {
				ActivityParams params = new ActivityParams();
				params.setActivityType(act);
				params.setTypicalDuration(acts.get(act));
				// Minimum duration is now specified by typical duration.
//				params.setMinimalDuration(acts.get(act).getSecond());
				params.setClosingTime(Time.UNDEFINED_TIME);
				params.setEarliestEndTime(Time.UNDEFINED_TIME);
				params.setLatestStartTime(Time.UNDEFINED_TIME);
				params.setOpeningTime(Time.UNDEFINED_TIME);
				params.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
				config.planCalcScore().addActivityParams(params);
			}
		}
	}
		
	private void write (Population population, Config config){		

		
		new PopulationWriter(population).write(plansFolder + "expanded_plans_1.xml.gz");
		log.info("expanded_plans_1 has the entire population WITH "
				+ "randomized activity end times & the classification of the activities");
		
		
		
		PlansConfigGroup plans = config.plans();
		plans.setInputFile(runsWorkingDir + "expanded_plans_1.xml.gz");
		new ConfigWriter(config).write(configFolder + "expanded_config_1.xml");
		
		
	}
	
	

	
	
	
	
	
	
}
