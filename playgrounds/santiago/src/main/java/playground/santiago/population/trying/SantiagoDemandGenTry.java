package playground.santiago.population.trying;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import playground.santiago.population.ActivityClassifier;


/**
 * @author leoca_000
 *
 */
public class SantiagoDemandGenTry {
	
	private final static Logger log = Logger.getLogger(SantiagoDemandGenTry.class);
	
	
	final String PATH_FOR_SIM = "../../../runs-svn/santiago/BASE10/";
	final String INPUT_FOR_SIMULATION = PATH_FOR_SIM + "input/";
	final String OUTPUT_FOR_NEW_INPUT = INPUT_FOR_SIMULATION + "new-input/";
	
	
	
	final String ORIGINAL_PLANS = INPUT_FOR_SIMULATION + "plans_final.xml.gz";
	final String ORIGINAL_CONFIG = INPUT_FOR_SIMULATION + "config_final.xml";
	
	
	final String PEOPLE_FROM_ODS = "../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/exportedFilesFromDatabase/Normal/Persona.csv";
	final double PERCENTAGE = 0.1;

	
	public SantiagoDemandGenTry(){
	}

	private void run(){
		
		clonePersons(PERCENTAGE, ORIGINAL_CONFIG, PEOPLE_FROM_ODS, OUTPUT_FOR_NEW_INPUT);

	}
	
	public static void main(String args[]){
		SantiagoDemandGenTry sdg = new SantiagoDemandGenTry();
		sdg.run();
	}
		
	private Map<String,Double> getIdsAndFactorsSantiago(String people_file){

		Map<String, Double> IdFactors = new TreeMap<String,Double>();

			try {
					
				BufferedReader bufferedReaderTwo = IOUtils.getBufferedReader(people_file);				
				String currentLineTwo = bufferedReaderTwo.readLine();				
					while ((currentLineTwo = bufferedReaderTwo.readLine()) != null) {
						String[] entries = currentLineTwo.split(",");
						IdFactors.put(entries[1], Double.parseDouble(entries[33]));
							
					}

				bufferedReaderTwo.close();
					
				} catch (IOException e) {
					
					log.error(new Exception(e));
				
				}

			return IdFactors;
	}
	
	private int getTotalPopulationSantiago(String people_file){

		double population=0;
		Map <String, Double> IdFactors=getIdsAndFactorsSantiago(people_file);
		for (Map.Entry<String, Double> entry : IdFactors.entrySet()){
			population += entry.getValue();
		}

		int totalPopulation = (int)Math.round(population);
		System.out.println("The total number of persons in Santiago is " + totalPopulation + ". Obs: this number differs from the total population stored in SantiagoScenarioConstants.java by 35 persons... ");
		return totalPopulation;
	}
	
	private Map<String,Double> getIdsAndFactorsMatsimPop(String config_file, String people_file){
		Config config = ConfigUtils.loadConfig(config_file);		
		Scenario scenarioFromBuilder = ScenarioUtils.loadScenario(config);
		Population pop = scenarioFromBuilder.getPopulation();
		List<Person> persons = new ArrayList<>(pop.getPersons().values());	
		Map<String, Double> IdsFactorsSantiago = getIdsAndFactorsSantiago(people_file);
		Map<String, Double> IdsFactorsMatsim = new LinkedHashMap <String,Double>();
		List<String> IdsMatsim = new ArrayList<>();
		
		for (Person p : persons){			
			IdsMatsim.add(p.getId().toString());	
		}
		
		for(String Ids : IdsMatsim ) {
			IdsFactorsMatsim.put(Ids, IdsFactorsSantiago.get(Ids));		
		}

		return IdsFactorsMatsim;

	
	}
	
	private double getProportionalFactor(double percentage, String config_file, String people_file){
		
		int totalPopulation = getTotalPopulationSantiago(people_file);
		Map<String,Double> infoFromMatsim = getIdsAndFactorsMatsimPop (config_file, people_file);
		double sumFactors = 0;
		
		for (Map.Entry<String,Double> entry : infoFromMatsim.entrySet()){		
			sumFactors += entry.getValue();
		}

		double pF = (percentage*totalPopulation)/sumFactors;
		return pF;
		}

	private void clonePersons(double percentage, String config_file, String people_file, String output_for_new_input){
		

		Map<String,Double> IdsAndFactorsFromMatsimPop = getIdsAndFactorsMatsimPop(config_file, people_file);
		double pF = getProportionalFactor(percentage, config_file, people_file);
		
		Config config = ConfigUtils.loadConfig(config_file);		
		Scenario scenarioFromBuilder = ScenarioUtils.loadScenario(config);
		Population populationFromPlansFinal = scenarioFromBuilder.getPopulation();
		List<Person> persons = new ArrayList<>(populationFromPlansFinal.getPersons().values());

		///////Can be omitted, just for having an observable list of factors//////
//		ArrayList<Integer> clonateFactorsList = new ArrayList<>();
//		for (Map.Entry<String,Double> entry : IdsAndFactorsFromMatsimPop.entrySet()){		
//			clonateFactorsList.add((int)Math.round(entry.getValue()*pF));			
//		}
		//////////////////////////////////////////////////////////////////////////
	
		for (Person p : persons) {
			String keyId = p.getId().toString();
			int clonateFactor = (int)Math.round(pF*IdsAndFactorsFromMatsimPop.get(keyId));
			
			for(int cf = 1; cf < clonateFactor ; cf++) {
				Id<Person> pOutId = Id.createPersonId( p.getId().toString().concat("_").concat(String.valueOf(cf)) );
				Person pOut = populationFromPlansFinal.getFactory().createPerson( pOutId  );
				populationFromPlansFinal.addPerson(pOut);
				
				for (Plan plan : p.getPlans()){
					Plan planOut = populationFromPlansFinal.getFactory().createPlan();
					List<PlanElement> pes = plan.getPlanElements();
					for ( PlanElement pe : pes){
						if(pe instanceof Leg) {
							Leg leg = (Leg) pe;
							Leg legOut = populationFromPlansFinal.getFactory().createLeg(leg.getMode());
							planOut.addLeg(legOut);
						} else { 
							Activity actIn = (Activity)pe;
							Activity actOut = populationFromPlansFinal.getFactory().createActivityFromCoord(actIn.getType(), actIn.getCoord());
							planOut.addActivity(actOut);
							actOut.setEndTime(actIn.getEndTime());
							actOut.setStartTime(actIn.getStartTime());
					}
				}
					pOut.addPlan(planOut);
			}
		}
		}
		
		randomizeEndTimes(populationFromPlansFinal);

	
		File output = new File(output_for_new_input);
		if(!output.exists()) createDir(new File(output_for_new_input));
		new PopulationWriter(populationFromPlansFinal).write(output_for_new_input + "expanded_plans_preliminar.xml.gz");
		log.info("expanded_plans_preliminar has the entire population w/ randomized activity end times but WITHOUT the classification of the activities");
		
		// Re-classify the activities using the new end_times from the randomizedEndTimes method //		
		Scenario scenarioTmp = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioTmp).readFile(output_for_new_input + "expanded_plans_preliminar.xml.gz");
		ActivityClassifier aap = new ActivityClassifier(scenarioTmp);
		aap.run();
		new PopulationWriter(aap.getOutPop()).write(output_for_new_input + "expanded_plans.xml.gz");
		log.info("expanded_plans has the entire population w/ randomized activity end times INCLUDING the classification of the activities");
		///////////////////////////////////////////////////////////////////////////////////////////
		
		
				
		
	}
	
	private void randomizeEndTimes(Population population){
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
				delta = createRandomEndTime(random);
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
		
	private double createRandomEndTime(Random random){
		//draw two random numbers [0;1] from uniform distribution
		double r1 = random.nextDouble();
		double r2 = random.nextDouble();
		
		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		double endTime = 20*60 * normal;
		
		return endTime;
	}

	private void createDir(File file) {
		log.info("Directory " + file + " created: "+ file.mkdirs());	
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}