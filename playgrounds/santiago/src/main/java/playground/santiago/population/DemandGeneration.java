package playground.santiago.population;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
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
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;



public class DemandGeneration {
	
	
	private final static Logger log = Logger.getLogger(DemandGeneration.class);
	

	final String runsWorkingDir = "../../../runs-svn/santiago/BASE10/input/";	
	
	final String svnWorkingDir = "../../../shared-svn/projects/santiago/scenario/";
	final String originalConfig = svnWorkingDir + "inputForMATSim/config_final.xml";	
	final String originalPlans = svnWorkingDir + "inputForMATSim/plans/plans_final.xml.gz";
	final String expandedAgentAttributes = svnWorkingDir + "inputForMATSim/plans/expandedAgentAttributes.xml";

	
	final String databaseFilesDir = svnWorkingDir + "inputFromElsewhere/exportedFilesFromDatabase/";
	final String Normal = databaseFilesDir + "Normal/";
	final String personasFile =  Normal + "Persona.csv";
	final String hogaresFile =  Normal + "Hogar.csv";
		


	
	final double percentage = 0.1;
	
	private ActivityClassifier activityClassifier;
	private Population originalPopulation;
	
	private Map<String,Persona> personas = new HashMap<>();
	private Map<String, Integer> hogarId2NVehicles = new HashMap<>();
	private Map<String, Coord> hogarId2Coord = new HashMap<>();
	private ObjectAttributes agentAttributes;
	private LinkedList <String> clonedAgentIds ;
	private LinkedList<String> originalAgentIds ;
	private String clonedPlans;
	private final String carUsers = "carUsers";
	private final String carAvail = "carAvail";	
	
	
	private Map<String,Double> getIdsAndFactorsSantiago(String personasFile){

		Map<String, Double> IdFactors = new TreeMap<String,Double>();

			try {
					
				BufferedReader bufferedReaderTwo = IOUtils.getBufferedReader(personasFile);				
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
	
	private int getTotalPopulationSantiago(String personasFile){

		double population=0;
		Map <String, Double> IdFactors=getIdsAndFactorsSantiago(personasFile);
		for (Map.Entry<String, Double> entry : IdFactors.entrySet()){
			population += entry.getValue();
		}

		int totalPopulation = (int)Math.round(population);
		System.out.println("The total number of persons in Santiago is " + totalPopulation + ". Obs: this number differs from the total population stored in SantiagoScenarioConstants.java by 35 persons... ");
		return totalPopulation;
	}
	
	private Map<String,Double> getIdsAndFactorsMatsimPop(String originalPlans, String personasFile){
		

		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());		
		PopulationReader pr = new PopulationReader(scenario);
		pr.readFile(originalPlans);
		this.originalPopulation = scenario.getPopulation();
		
		List<Person> persons = new ArrayList<>(originalPopulation.getPersons().values());	
		Map<String, Double> IdsFactorsSantiago = getIdsAndFactorsSantiago(personasFile);
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

	private double getProportionalFactor(double percentage, String personasFile){
		
		int totalPopulation = getTotalPopulationSantiago(personasFile);
		Map<String,Double> infoFromMatsim = getIdsAndFactorsMatsimPop (originalPlans , personasFile);
		double sumFactors = 0;
		
		for (Map.Entry<String,Double> entry : infoFromMatsim.entrySet()){		
			sumFactors += entry.getValue();
		}

		double pF = (percentage*totalPopulation)/sumFactors;
		return pF;
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
	
	private void clonePersons(double percentage, String originalConfig, String personasFile){
		

		Map<String,Double> IdsAndFactorsFromMatsimPop = getIdsAndFactorsMatsimPop(originalPlans, personasFile);
		double pF = getProportionalFactor(percentage, personasFile);

		List<Person> persons = new ArrayList<>(originalPopulation.getPersons().values());


	
		for (Person p : persons) {
			String keyId = p.getId().toString();
			int clonateFactor = (int)Math.round(pF*IdsAndFactorsFromMatsimPop.get(keyId));
			
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
		
		randomizeEndTimes(originalPopulation);

	

		new PopulationWriter(originalPopulation).write(svnWorkingDir + "inputForMATSim/plans/expanded_plans_preliminar.xml.gz");
		log.info("expanded_plans_preliminar has the entire population w/ "
				+ "randomized activity end times but WITHOUT the classification of the activities");
		
		// Re-classify the activities using the new end_times from the randomizedEndTimes method //		
		Scenario scenarioTmp = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioTmp).readFile(svnWorkingDir + "inputForMATSim/plans/expanded_plans_preliminar.xml.gz");
		this.activityClassifier = new ActivityClassifier(scenarioTmp);
		activityClassifier.run();
		new PopulationWriter(activityClassifier.getOutPop()).write(svnWorkingDir + "inputForMATSim/plans/expanded_plans.xml.gz");
		this.clonedPlans = svnWorkingDir + "inputForMATSim/plans/expanded_plans.xml.gz";
		log.info("expanded_plans has the entire population w/ randomized activity end times "
				+ "INCLUDING the classification of the activities");
		///////////////////////////////////////////////////////////////////////////////////////////
		
		
				
		
	}
	
	private void readHogares(String hogaresFile){
		

		final int idxHogarId = 0;
		final int idxNVeh = 11;
		
		BufferedReader reader = IOUtils.getBufferedReader(hogaresFile);

		
		try {
			String line = reader.readLine();
			while( (line = reader.readLine()) != null ){
				String[] splittedLine = line.split(",");
				String id = splittedLine[idxHogarId];
				int nVehicles = Integer.parseInt(splittedLine[idxNVeh]);
				this.hogarId2NVehicles.put(id, nVehicles);

			}
			reader.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}	
	
	private void readPersonas(String personasFile){
		
		final int idxHogarId = 0;
		final int idxPersonId = 1;
		final int idxAge = 2;
		final int idxSex = 3;
		final int idxNViajes = 5;
		final int idxLicence = 6;
		final int idxCoordX = 16;
		final int idxCoordY = 17;
		
		
		BufferedReader reader = IOUtils.getBufferedReader(personasFile);
		
		try {
			
			String line = reader.readLine();
			while( (line = reader.readLine()) != null ){
				String[] splittedLine = line.split(",");
				String hogarId = splittedLine[idxHogarId];
				String id = splittedLine[idxPersonId];
				int age = 2012 - Integer.valueOf(splittedLine[idxAge]);
				String sex = splittedLine[idxSex];
				String drivingLicence = splittedLine[idxLicence];
				int nCars = this.hogarId2NVehicles.get(hogarId);
				String nViajes = splittedLine[idxNViajes];
				
				Persona persona = new Persona(id, age, sex, drivingLicence, nCars, nViajes);
				persona.setHomeCoord(this.hogarId2Coord.get(hogarId));
				
				String x = splittedLine[idxCoordX];
				String y = splittedLine[idxCoordY];
				if(!x.equals("") && !y.equals("") && !x.equals("0") && !y.equals("0")){
					persona.setWorkCoord(new Coord(Double.parseDouble(x), Double.parseDouble(y)));
				}
				
				this.personas.put(id,persona);

			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private void readPlans(String clonedPlans) {
		
		this.clonedAgentIds = new LinkedList<>();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(clonedPlans);
		Population population = scenario.getPopulation();
		List<Person> persons = new ArrayList<>(population.getPersons().values());
		for (Person p : persons) {
			String keyId = p.getId().toString();
			clonedAgentIds.add(keyId);
			
		}
		
		this.originalAgentIds = new LinkedList<>();
		
		for (String agents : clonedAgentIds) {
			String[]completeIds=agents.split("_");
			originalAgentIds.add(completeIds[0]);
		}

	}
	
	private void writeNewConfigFile (String originalConfig, double percentage, String runsWorkingDir){
		
		Config oldConfig = ConfigUtils.loadConfig(originalConfig);
		
		/*QSim stuffs*/		
		QSimConfigGroup qsim = oldConfig.qsim();
		//The capacity factor is equal to the percentage used in the clonePersons method.
		qsim.setFlowCapFactor(percentage);
		//storageCapFactor obtained by expression proposed by Nicolai and Nagel, 2013.
		double storageCapFactor = Math.ceil(((0.1 / (Math.pow(percentage, 0.25))))*100)/100;
		qsim.setStorageCapFactor(storageCapFactor);
		////////////////////////////////////////////////////////////////////////
		
		/*Path to new plans file*/		
		PlansConfigGroup plans = oldConfig.plans();
		plans.setInputFile(runsWorkingDir + "expanded_plans.xml.gz");
		plans.setInputPersonAttributeFile(runsWorkingDir + "expandedAgentAttributes.xml");
		////////////////////////////////////////////////////////////////////////
		
		
		/*New group of parameters considering the new classification of the activities*/
		SortedMap<String, Double> acts = activityClassifier.getActivityType2TypicalDuration();
		setActivityParams(acts, oldConfig);
		////////////////////////////////////////////////////////////////////////

		
		/*Counts stuffs*/
		CountsConfigGroup counts = oldConfig.counts();
		counts.setCountsScaleFactor(Math.pow(percentage,-1));
		////////////////////////////////////////////////////////////////////////
		
		
		/*Write the new config_file*/	
		new ConfigWriter(oldConfig).write(svnWorkingDir + "inputForMATSim/expanded_config.xml");
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

		clonePersons(percentage, originalConfig, personasFile);

		
		readHogares(hogaresFile);
		readPersonas(personasFile);
		readPlans(clonedPlans);
		
		LinkedList<String> agentsWithCar = new LinkedList<>(); 
		
		this.agentAttributes = new ObjectAttributes();
		
		for(Persona persona : this.personas.values()){
			
			if(persona.hasCar() && persona.hasDrivingLicence()){
				String id = persona.getId();
				agentsWithCar.add(id);
				int start = originalAgentIds.indexOf(id);
				int end = originalAgentIds.lastIndexOf(id);
				/*Avoid the case in which, because of the sampling, the ID doesn't exist in the synthetic population*/
				if (start!=-1){
					for (int i = start; i<=end; i++){					
					agentAttributes.putAttribute(clonedAgentIds.get(i), carUsers, carAvail);					
					}
				}

			}		
		}
		
		
		try {
			
			PrintWriter pw = new PrintWriter (new FileWriter ( svnWorkingDir + "inputForMATSim/plans/agentsWithCar.txt" ));
			for (String agent : agentsWithCar) {
				pw.println(agent);

			}
			pw.close();
		} catch (IOException e) {
			log.error(new Exception(e));
		}
		
		new ObjectAttributesXmlWriter(agentAttributes).writeFile(expandedAgentAttributes);
		
		writeNewConfigFile (originalConfig, percentage, runsWorkingDir);
		
	}
	
	public static void main(String[] args) {

		DemandGeneration dg = new DemandGeneration();
		dg.run();

	}

}
