package playground.santiago.population;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
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




public class DemandGeneration {
	
	
	private final static Logger log = Logger.getLogger(DemandGeneration.class);
	

	final String runsWorkingDir = "../../../runs-svn/santiago/TMP/input/";	
	
	final String svnWorkingDir = "../../../shared-svn/projects/santiago/scenario/";
	final String originalConfig = svnWorkingDir + "inputForMATSim/config_final.xml";	
	final String originalPlans = svnWorkingDir + "inputForMATSim/plans/1_initial/workDaysOnly/plans_final.xml.gz";


	
	final String databaseFilesDir = svnWorkingDir + "inputFromElsewhere/exportedFilesFromDatabase/";
	final String Normal = databaseFilesDir + "Normal/";
	final String personasFile =  Normal + "Persona.csv";

	
	final double percentage = 0.1;
	
	private ActivityClassifier activityClassifier;
	private Population originalPopulation;
	private Map<String,Double> idsFactorsSantiago;
	private Map<String,Double> idsFactorsMatsim;
	private int totalPopulation;
	private double proportionalFactor;
	

	private void getIdsAndFactorsSantiago(){

		this.idsFactorsSantiago = new TreeMap<String,Double>();

			try {
					
				BufferedReader bufferedReader = IOUtils.getBufferedReader(personasFile);				
				String currentLine = bufferedReader.readLine();				
					while ((currentLine = bufferedReader.readLine()) != null) {
						String[] entries = currentLine.split(",");
						idsFactorsSantiago.put(entries[1], Double.parseDouble(entries[33]));
							
					}

				bufferedReader.close();
					
				} catch (IOException e) {
					
					log.error(new Exception(e));
				
				}


	}
	
	private void getTotalPopulationSantiago(){

		double population=0;

		for (Map.Entry<String, Double> entry : idsFactorsSantiago.entrySet()){
			population += entry.getValue();
		}

		this.totalPopulation = (int)Math.round(population);
		System.out.println("The total number of persons in Santiago is " + totalPopulation + ". Obs: this number differs from the total population stored in SantiagoScenarioConstants.java by 35 persons... ");

	}
	
	private void getIdsAndFactorsMatsimPop(){
	
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());		
		PopulationReader pr = new PopulationReader(scenario);
		pr.readFile(originalPlans);
		this.originalPopulation = scenario.getPopulation();
		
		List<Person> persons = new ArrayList<>(originalPopulation.getPersons().values());		

		List<String> IdsMatsim = new ArrayList<>();
		

		for (Person p : persons){			
			IdsMatsim.add(p.getId().toString());	
		}
		
		this.idsFactorsMatsim = new TreeMap <String,Double>();
		
		for(String Ids : IdsMatsim ) {
			idsFactorsMatsim.put(Ids, idsFactorsSantiago.get(Ids));		
		}



	
	}

	private void getProportionalFactor(){
		

		double sumFactors = 0;
		
		for (Map.Entry<String,Double> entry : idsFactorsMatsim.entrySet()){		
			sumFactors += entry.getValue();
		}

		this.proportionalFactor = (percentage*totalPopulation)/sumFactors;

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
	
	private void clonePersons(){
		

		List<Person> persons = new ArrayList<>(originalPopulation.getPersons().values());
	
		for (Person p : persons) {
			String keyId = p.getId().toString();
			int clonateFactor = (int)Math.round(proportionalFactor*idsFactorsMatsim.get(keyId));
			
			for(int cf = 1; cf < clonateFactor ; cf++) {
				Id<Person> pOutId = Id.createPersonId( p.getId().toString().concat("_").concat(String.valueOf(cf)) );
				Person pOut = originalPopulation.getFactory().createPerson( pOutId  );
				originalPopulation.addPerson(pOut);
				
				for (Plan plan : p.getPlans()){
					Plan planOut = originalPopulation.getFactory().createPlan();
					List<PlanElement> pes = plan.getPlanElements();
					for ( PlanElement pe : pes){
						if(pe instanceof Leg) {
							Leg leg = (Leg) pe;
							Leg legOut = originalPopulation.getFactory().createLeg(leg.getMode());
							planOut.addLeg(legOut);
						} else { 
							Activity actIn = (Activity)pe;
							Activity actOut = originalPopulation.getFactory().createActivityFromCoord(actIn.getType(), actIn.getCoord());
							planOut.addActivity(actOut);
							actOut.setEndTime(actIn.getEndTime());
							actOut.setStartTime(actIn.getStartTime());
					}
				}
					pOut.addPlan(planOut);
			}
		}
		}
		
		//randomizeEndTimes(originalPopulation);

	

		new PopulationWriter(originalPopulation).write(svnWorkingDir + "inputForMATSim/plans/2_10pct/expanded_plans_0.xml.gz");
		log.info("expanded_plans_0 has the entire population WITHOUT "
				+ "randomized activity end times and WITHOUT the classification of the activities");
		
		// Re-classify the activities using the new end_times from the randomizedEndTimes method //		
//		Scenario scenarioTmp = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new PopulationReader(scenarioTmp).readFile(svnWorkingDir + "inputForMATSim/plans/expanded_plans_preliminar.xml.gz");
//		this.activityClassifier = new ActivityClassifier(scenarioTmp);
//		activityClassifier.run();
//		new PopulationWriter(activityClassifier.getOutPop()).write(svnWorkingDir + "inputForMATSim/plans/expanded_plans.xml.gz");
//
//		log.info("expanded_plans has the entire population w/ randomized activity end times "
//				+ "INCLUDING the classification of the activities");
		///////////////////////////////////////////////////////////////////////////////////////////
		
		
				
		
	}
		
	private void writeNewConfigFile (){
		
		Config oldConfig = ConfigUtils.loadConfig(originalConfig);
		
		/*QSim stuffs*/		
		QSimConfigGroup qsim = oldConfig.qsim();
		//The capacity factor is equal to the percentage used in the clonePersons method.
		qsim.setFlowCapFactor(percentage);
		//storageCapFactor obtained by expression proposed by Nicolai and Nagel, 2013.
		double storageCapFactor = Math.ceil(((percentage / (Math.pow(percentage, 0.25))))*100)/100;
		qsim.setStorageCapFactor(storageCapFactor);
		////////////////////////////////////////////////////////////////////////
		
		/*Path to new plans file*/		
		PlansConfigGroup plans = oldConfig.plans();
		plans.setInputFile(runsWorkingDir + "expanded_plans_0.xml.gz");
		plans.setInputPersonAttributeFile(runsWorkingDir + "expandedAgentAttributes.xml");
		////////////////////////////////////////////////////////////////////////
		
		
		/*New group of parameters considering the new classification of the activities*/
//		SortedMap<String, Double> acts = activityClassifier.getActivityType2TypicalDuration();
//		setActivityParams(acts, oldConfig);
		////////////////////////////////////////////////////////////////////////

		
		/*Counts stuffs*/
		CountsConfigGroup counts = oldConfig.counts();
		counts.setCountsScaleFactor(Math.pow(percentage,-1));
		////////////////////////////////////////////////////////////////////////
		
		
		/*Write the new config_file*/	
		new ConfigWriter(oldConfig).write(svnWorkingDir + "inputForMATSim/expanded_config_0.xml");
		////////////////////////////////////////////////////////////////////////
		
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
	
	private void run (){

		getIdsAndFactorsSantiago();
		getTotalPopulationSantiago();
		getIdsAndFactorsMatsimPop();
		getProportionalFactor();
		clonePersons();
		writeNewConfigFile();
		
	}
	
	public static void main(String[] args) {

		DemandGeneration dg = new DemandGeneration();
		dg.run();

	}

}
