package playground.artemc.psim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import playground.artemc.analysis.IndividualScoreFromPopulationSQLWriter;
import playground.artemc.crowding.CrowdednessObserver;
import playground.artemc.crowding.newScoringFunctions.ScoreListener;
import playground.artemc.crowding.newScoringFunctions.ScoreTracker;
import playground.artemc.crowding.rules.SimpleRule;
import playground.artemc.plansTools.PlanRouteStripper;
import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Choice set generator based on the selected plan. Uses Psim for plans scoring.
 * Takes folder with simulation outputs and the desired output folder as parameters.
 * Created by artemc on 21/4/15.
 */
public class ChoiceSetAndCrowdingAnalysisGenerator implements ShutdownListener {

	static private String inputDirectory;
	static private String outputDirectory;
	static private String eventFilePath;

	private static String simType = "5min";
	private static String schema = "corridor_5min";

	private static String dataPath = "/Volumes/DATA 1 (WD 2 TB)/output_SelectExp1_5p_"+simType+"_1000it_Dwell/";
	private static String connectionPropertiesPath = "/Users/artemc/Workspace/playgrounds/artemc/connections/matsim2postgresLocal.properties";

	private static final Logger log = Logger.getLogger(ChoiceSetAndCrowdingAnalysisGenerator.class);

	private final ChoiceGenerationControler choiceGenerationControler;

	private Controler controler;
	private Population population;
	private HashMap<Id<Person>, Plan> initialPlans = new HashMap<Id<Person>, Plan>();
	static Integer departureTimeChoices = 3;

	public ChoiceSetAndCrowdingAnalysisGenerator(Config config, String eventsFile) {

		choiceGenerationControler = new ChoiceGenerationControler(config, eventsFile);

		this.controler = choiceGenerationControler.getControler();
		this.population = controler.getScenario().getPopulation();
		controler.addControlerListener(this);
	}

	public static void main(String[] args) {

		if (args.length == 3) {
			inputDirectory = args[0];
			outputDirectory = args[1];
			eventFilePath = args[2];

			Config config = ScenarioInitializerFromOutput.initScenario(inputDirectory, outputDirectory);
			ChoiceSetAndCrowdingAnalysisGenerator choiceSetGenerator = new ChoiceSetAndCrowdingAnalysisGenerator(config, eventFilePath);
			choiceSetGenerator.CreateChoiceSets();

			choiceSetGenerator.choiceGenerationControler.run();
		} else if (args.length == 2) {
			//String dataFolder = args[0];
			String dataFolder = dataPath;
			Integer lastIteration = Integer.valueOf(args[1]);
			File directory = new File(dataFolder);
			File[] fList = directory.listFiles();

			for (File file : fList) {
				if (file.isDirectory()) {
					inputDirectory = file.getAbsolutePath()+"/";
					outputDirectory = file.getAbsolutePath() + "_choiceSet";
					eventFilePath = file.getAbsolutePath() + "/it." + lastIteration+"/"+lastIteration+".events.xml.gz";

					Config config = ScenarioInitializerFromOutput.initScenario(inputDirectory, outputDirectory);
					ChoiceSetAndCrowdingAnalysisGenerator choiceSetGenerator = new ChoiceSetAndCrowdingAnalysisGenerator(config, eventFilePath);
					choiceSetGenerator.CreateChoiceSets();

					choiceSetGenerator.choiceGenerationControler.run();
				}
			}
		}
	}

	public void CreateChoiceSets() {

		PopulationFactory populationFactory = population.getFactory();
		ArrayList<Person> newPersons = new ArrayList<>();

		for (Id<Person> personId : population.getPersons().keySet()) {

			Plan plan = population.getPersons().get(personId).getSelectedPlan();
			plan.getCustomAttributes().put("toll", population.getPersonAttributes().getAttribute(personId.toString(), "selectedPlanToll"));
			initialPlans.put(personId, plan);

			population.getPersons().get(personId).createCopyOfSelectedPlanAndMakeSelected();

			Plan planTmp = population.getPersons().get(personId).getSelectedPlan();
			population.getPersons().get(personId).getPlans().clear();
			population.getPersons().get(personId).addPlan(planTmp);
		}

		/*Clean routes*/
		PlanRouteStripper planRouteStripper = new PlanRouteStripper();
		planRouteStripper.run(population);

		/*Create optimal walk plan and substitute it in the planMap*/
		OptimalWalkPlanFinder optimalWalkPlanFinder = new OptimalWalkPlanFinder(controler.getConfig());

		for (Id<Person> personId : population.getPersons().keySet()) {

			/* Plan cloning for each mode*/
			HashMap<String, Plan> planMap = new HashMap<>();

			Leg firstLeg = (Leg) population.getPersons().get(personId).getSelectedPlan().getPlanElements().get(1);
			String initialMode = firstLeg.getMode();

			//If "No PT"
			if(initialMode=="pt" && simType.equals("noPT"))
				initialMode="walk";

			planMap.put(initialMode, population.getPersons().get(personId).getSelectedPlan());

			//String[] modes = controler.getScenario().getConfig().getModule("subtourModeChoice").getValue("modes").split(",");
			//String[] modes = {"car","walk"};

			ArrayList<String> relevantModes = new ArrayList<String>();
			relevantModes.add("car");
			if(!simType.equals("carOnly") && !simType.equals("noPT"))
				relevantModes.add("pt");

			for (String mode : relevantModes) {
				if (!initialMode.equals(mode) && !mode.equals("walk")){
					population.getPersons().get(personId).createCopyOfSelectedPlanAndMakeSelected();
					Plan planForModeChange = population.getPersons().get(personId).getSelectedPlan();

					for (int j = 1; j < planForModeChange.getPlanElements().size(); j += 2) {
						LegImpl leg = (LegImpl) planForModeChange.getPlanElements().get(j);
						leg.setMode(mode);
					}

					planMap.put(mode, planForModeChange);
				}
			}

			/*Check if walk is a viable alternative and if so, add it to the choice set*/
			if(!initialMode.equals("walk") && optimalWalkPlanFinder.getWalkTravelTime(planMap.get(initialMode))<=3600.0 && !simType.equals("carOnly")) {
				population.getPersons().get(personId).createCopyOfSelectedPlanAndMakeSelected();
				Plan planForModeChange = population.getPersons().get(personId).getSelectedPlan();
				planForModeChange = optimalWalkPlanFinder.findOptimalWalkPlan(planForModeChange);
				for (int j = 1; j < planForModeChange.getPlanElements().size(); j += 2) {
					LegImpl leg = (LegImpl) planForModeChange.getPlanElements().get(j);
					leg.setMode("walk");
				}

				planMap.put("walk", planForModeChange);
			}
			;

			//String[] relevantModes = {"car","pt"};

			/* Departure time modification for each mode*/
			for (String mode :relevantModes) {
				Plan basePlan = planMap.get(mode);
				population.getPersons().get(personId).setSelectedPlan(basePlan);

				for (int i = 0; i < 6; i++) {
					population.getPersons().get(personId).createCopyOfSelectedPlanAndMakeSelected();
				}

				population.getPersons().get(personId).setSelectedPlan(basePlan);

				int planModCounter = 0;
				/* Departure time modification */
				for (Plan planToModify : population.getPersons().get(personId).getPlans()) {

					if (((Leg) planToModify.getPlanElements().get(1)).getMode().equals(mode) && !PersonUtils.isSelected(planToModify)) {

						//Home departure time + 1h
						if (planModCounter == 0) {
							Activity act = (Activity) planToModify.getPlanElements().get(0);
							act.setEndTime(act.getEndTime() + 3600.0);
						}

						//Home departure time - 1h
						if (planModCounter == 1) {
							Activity act = (Activity) planToModify.getPlanElements().get(0);
							act.setEndTime(act.getEndTime() - 3600.0);
						}

						//Work departure time + 1h
						if (planModCounter == 2) {
							Activity act = (Activity) planToModify.getPlanElements().get(2);
							act.setEndTime(act.getEndTime() + 3600.0);
						}

						//Work departure time - 1h
						if (planModCounter == 3) {
							Activity act = (Activity) planToModify.getPlanElements().get(2);
							act.setEndTime(act.getEndTime() - 3600.0);
						}

						//Home departure time + 1h, Work departure time +1h
						if (planModCounter == 4) {
							Activity act = (Activity) planToModify.getPlanElements().get(0);
							act.setEndTime(act.getEndTime() + 3600.0);

							Activity act2 = (Activity) planToModify.getPlanElements().get(2);
							act2.setEndTime(act2.getEndTime() + 3600.0);
						}

						//Home departure time - 1h, Work departure time -1h
						if (planModCounter == 5) {
							Activity act = (Activity) planToModify.getPlanElements().get(0);
							act.setEndTime(act.getEndTime() - 3600.0);

							Activity act2 = (Activity) planToModify.getPlanElements().get(2);
							act2.setEndTime(act2.getEndTime() - 3600.0);
						}


//                      NOT REALISTIC
//						//Home departure time +1h, Work departure time -1h
//						if (planModCounter == 6) {
//							Activity act = (Activity) planToModify.getPlanElements().get(0);
//							act.setEndTime(act.getEndTime() + 3600.0);
//
//							Activity act2 = (Activity) planToModify.getPlanElements().get(2);
//							act2.setEndTime(act2.getEndTime() - 3600.0);
//						}
//
//						//Home departure time - 1h, Work departure time +1h
//						if (planModCounter == 7) {
//							Activity act = (Activity) planToModify.getPlanElements().get(0);
//							act.setEndTime(act.getEndTime() - 3600.0);
//
//							Activity act2 = (Activity) planToModify.getPlanElements().get(2);
//							act2.setEndTime(act2.getEndTime() + 3600.0);
//						}

						planModCounter++;
					}
				}

			}

			/*Plan split to different persons*/
			int count = 0;
			population.getPersons().get(personId).setSelectedPlan(planMap.get(initialMode));
			for (Plan newPlan : population.getPersons().get(personId).getPlans()) {
				if (!PersonUtils.isSelected(newPlan)) {
					count++;
					Person newPerson = populationFactory.createPerson(Id.createPersonId(personId.toString() + "_" + count));
					newPerson.addPlan(newPlan);
					newPerson.setSelectedPlan(newPlan);
					newPersons.add(newPerson);
					population.getPersonAttributes().putAttribute(newPerson.getId().toString(), "income", population.getPersonAttributes().getAttribute(personId.toString(), "income"));
					population.getPersonAttributes().putAttribute(newPerson.getId().toString(), "betaFactor", population.getPersonAttributes().getAttribute(personId.toString(), "betaFactor"));
				}
			}

			/*Clear all plans from initial person*/
			population.getPersons().get(personId).getPlans().clear();
			population.getPersons().get(personId).addPlan(initialPlans.get(personId));
			population.getPersons().get(personId).setSelectedPlan(initialPlans.get(personId));
		}

		/*Add new agents to the simulation*/
		for (Person newPerson : newPersons) {
			population.addPerson(newPerson);
		}

//		/*Write out new population file*/
//		System.out.println("New number of persons: " + population.getPersons().size());
//		new org.matsim.core.population.PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(outputPopulationFile);

	}

	public ArrayList<Plan> generateChoices(Plan selectedPlan) {
		ArrayList<Plan> plans = new ArrayList<Plan>();
		return plans;
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		Scenario newScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population newPopulation = newScenario.getPopulation();
		MoneyEventHandler moneyHandler = choiceGenerationControler.getMoneyEventHandler();

		for (Id<Person> personId : population.getPersons().keySet()) {

			Id<Person> orgPersonId = Id.create(personId.toString().split("_")[0], Person.class);

			Double monetaryPayments = moneyHandler.getPersonId2amount().get(personId);
			if (monetaryPayments == null) {
				monetaryPayments = 0.0;
			}

			Plan planToAdd = null;
			if (personId.toString().contains("_")) {
				planToAdd = population.getPersons().get(personId).getSelectedPlan();
				planToAdd.getCustomAttributes().put("toll", monetaryPayments.toString());
			} else {
				planToAdd = initialPlans.get(personId);
			}

			if (newPopulation.getPersons().containsKey(orgPersonId)) {
				newPopulation.getPersons().get(orgPersonId).addPlan(planToAdd);
			} else {
				Person newPerson = newPopulation.getFactory().createPerson(orgPersonId);
				newPerson.addPlan(planToAdd);
				newPopulation.addPerson(newPerson);
			}
		}

		/*Write score table to SQL*/
		Integer relativeOutputDirectory = event.getControler().getConfig().controler().getOutputDirectory().split("/").length;

		String tableName = schema + ".scores_";
		String tableSuffix = event.getControler().getConfig().controler().getOutputDirectory().split("/")[relativeOutputDirectory - 1];
		tableSuffix = tableSuffix.replaceAll("\\.0x", "x");
		tableSuffix = tableSuffix.replaceAll("-", "_");
		tableSuffix = tableSuffix.replaceAll("\\.5", "5");
		tableSuffix = tableSuffix.replaceAll("\\.1", "1");
		tableName = tableName + tableSuffix;

		IndividualScoreFromPopulationSQLWriter sqlWriter = new IndividualScoreFromPopulationSQLWriter(event.getControler().getConfig(), newPopulation);
		sqlWriter.writeToDatabase(connectionPropertiesPath, schema, tableName);

		new PopulationWriter(newScenario.getPopulation()).write(choiceGenerationControler.getControler().getConfig().controler().getOutputDirectory() + "/output_plansJoin.xml");
	}
}
