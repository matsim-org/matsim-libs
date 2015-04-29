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
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import playground.artemc.analysis.IndividualScoreFromPopulationSQLWriter;
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
public class ChoiceSetGenerator implements ShutdownListener {

	static private String inputDirectory;
	static private String outputDirectory;
	static private String eventFilePath;

	private static final Logger log = Logger.getLogger(ChoiceSetGenerator.class);

	private final ChoiceGenerationControler choiceGenerationControler;

	private Controler controler;
	private Population population;
	private HashMap<Id<Person>, Plan> initialPlans = new HashMap<Id<Person>, Plan>();
	static Integer departureTimeChoices = 3;

	public ChoiceSetGenerator(Config config, String eventsFile) {

		choiceGenerationControler = new ChoiceGenerationControler(config, eventsFile);

		this.controler = choiceGenerationControler.getControler();
		this.population = controler.getScenario().getPopulation();

		//Add self for after run plan9 join and output writing
		controler.addControlerListener(this);

	}

	public static void main(String[] args) {

		if (args.length == 3) {
			inputDirectory = args[0];
			outputDirectory = args[1];
			eventFilePath = args[2];

			Config config = ScenarioInitializerFromOutput.initScenario(inputDirectory, outputDirectory);
			ChoiceSetGenerator choiceSetGenerator = new ChoiceSetGenerator(config, eventFilePath);
			choiceSetGenerator.CreateChoiceSets();

			choiceSetGenerator.choiceGenerationControler.run();
		} else if (args.length == 2) {
			String outputFolder = args[0];
			Integer lastIteration = Integer.valueOf(args[1]);
			File directory = new File(outputFolder);
			File[] fList = directory.listFiles();

			for (File file : fList) {
				if (file.isDirectory()) {
					inputDirectory = file.getAbsolutePath()+"/";
					outputDirectory = file.getAbsolutePath() + "_choiceSet";
					eventFilePath = file.getAbsolutePath() + "/it." + lastIteration+"/"+lastIteration+".events.xml.gz";

					Config config = ScenarioInitializerFromOutput.initScenario(inputDirectory, outputDirectory);
					ChoiceSetGenerator choiceSetGenerator = new ChoiceSetGenerator(config, eventFilePath);
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

		for (Id<Person> personId : population.getPersons().keySet()) {

			/* Plan cloning for each mode*/
			HashMap<String, Plan> planMap = new HashMap<>();
			String[] modes = controler.getScenario().getConfig().getModule("subtourModeChoice").getValue("modes").split(",");

			Leg firstLeg = (Leg) population.getPersons().get(personId).getSelectedPlan().getPlanElements().get(1);
			planMap.put(firstLeg.getMode(), population.getPersons().get(personId).getSelectedPlan());
			String initialMode = firstLeg.getMode();

			for (String mode : modes) {
				if (!firstLeg.getMode().equals(mode)) {
					population.getPersons().get(personId).createCopyOfSelectedPlanAndMakeSelected();

					Plan planForModeChange = population.getPersons().get(personId).getSelectedPlan();

					for (int j = 1; j < planForModeChange.getPlanElements().size(); j += 2) {
						LegImpl leg = (LegImpl) planForModeChange.getPlanElements().get(j);
						leg.setMode(mode);
					}

					planMap.put(mode, planForModeChange);
				}
			}

			String[] relevantModes = {"car","pt"};
			/* Departure time modification for each mode*/
//			for (String mode :relevantModes) {
//				Plan basePlan = planMap.get(mode);
//				population.getPersons().get(personId).setSelectedPlan(basePlan);
//
//				for (int i = 1; i < (departureTimeChoices * departureTimeChoices  - 1); i++) {
//					population.getPersons().get(personId).createCopyOfSelectedPlanAndMakeSelected();
//				}
//
//				int planModCounter = 0;
//				/* Departure time modification */
//				for (Plan planToModify : population.getPersons().get(personId).getPlans()) {
//
//					if (((Leg) planToModify.getPlanElements().get(1)).getMode().equals(mode)) {
//
//						//Home departure time + 1h
//						if (planModCounter == 0) {
//							Activity act = (Activity) planToModify.getPlanElements().get(0);
//							act.setEndTime(act.getEndTime() + 3600.0);
//						}
//
//						//Home departure time - 1h
//						if (planModCounter == 1) {
//							Activity act = (Activity) planToModify.getPlanElements().get(0);
//							act.setEndTime(act.getEndTime() - 3600.0);
//						}
//
//						//Work departure time + 1h
//						if (planModCounter == 2) {
//							Activity act = (Activity) planToModify.getPlanElements().get(2);
//							act.setEndTime(act.getEndTime() + 3600.0);
//						}
//
//						//Work departure time - 1h
//						if (planModCounter == 3) {
//							Activity act = (Activity) planToModify.getPlanElements().get(2);
//							act.setEndTime(act.getEndTime() - 3600.0);
//						}
//
//						//Home departure time + 1h, Work departure time +1h
//						if (planModCounter == 4) {
//							Activity act = (Activity) planToModify.getPlanElements().get(0);
//							act.setEndTime(act.getEndTime() + 3600.0);
//
//							Activity act2 = (Activity) planToModify.getPlanElements().get(2);
//							act2.setEndTime(act2.getEndTime() + 3600.0);
//						}
//
//						//Home departure time - 1h, Work departure time -1h
//						if (planModCounter == 5) {
//							Activity act = (Activity) planToModify.getPlanElements().get(0);
//							act.setEndTime(act.getEndTime() - 3600.0);
//
//							Activity act2 = (Activity) planToModify.getPlanElements().get(2);
//							act2.setEndTime(act2.getEndTime() - 3600.0);
//						}
//
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
//
//						planModCounter++;
//					}
//				}
//
//			}

			/*Plan split to different persons*/
			int count = 0;
			population.getPersons().get(personId).setSelectedPlan(planMap.get(initialMode));
			for (Plan newPlan : population.getPersons().get(personId).getPlans()) {
				if (!newPlan.isSelected()) {
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
		String connectionPropertiesPath = "/Users/artemc/Workspace/playgrounds/artemc/connections/matsim2postgresLocal.properties";
		String schema = "corridor";

		Integer relativeOutputDirectory = event.getControler().getConfig().controler().getOutputDirectory().split("/").length;

		String tableName = schema + ".scores_";
		String tableSuffix = event.getControler().getConfig().controler().getOutputDirectory().split("/")[relativeOutputDirectory - 1];
		tableSuffix = tableSuffix.replaceAll("\\.0x", "x");
		tableSuffix = tableSuffix.replaceAll("-", "_");
		tableName = tableName + tableSuffix;

		IndividualScoreFromPopulationSQLWriter sqlWriter = new IndividualScoreFromPopulationSQLWriter(event.getControler().getConfig(), newPopulation);
		sqlWriter.writeToDatabase(connectionPropertiesPath, schema, tableName);

		new PopulationWriter(newScenario.getPopulation()).write(choiceGenerationControler.getControler().getConfig().controler().getOutputDirectory() + "/output_plansJoin.xml");
	}
}
