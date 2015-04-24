package playground.artemc.psim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import playground.artemc.analysis.AnalysisControlerListener;
import playground.artemc.analysis.IndividualScoreFromPopulationSQLWriter;
import playground.artemc.heterogeneity.HeterogeneityConfigGroup;
import playground.artemc.heterogeneity.IncomeHeterogeneityWithoutTravelDisutilityModule;
import playground.artemc.heterogeneity.TravelDisutilityIncomeHeterogeneityProviderWrapper;
import playground.artemc.heterogeneity.scoring.HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory;
import playground.artemc.heterogeneityWithToll.TravelDisutilityTollAndIncomeHeterogeneityProviderWrapper;
import playground.artemc.pricing.LinkOccupancyAnalyzerModule;
import playground.artemc.pricing.RoadPricingWithoutTravelDisutilityModule;
import playground.artemc.pricing.UpdateSocialCostPricingSchemeWithSpillOverModule;
import playground.artemc.scenarios.scenarioInitializer;
import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Choice set generator based on the selected plan. Uses Psim for plans scoring.
 * Takes folder with simulation outputs and the desired output folder as parameters.
 * Created by artemc on 21/4/15.
 */
public class ChoiceSetGenerator implements ShutdownListener
{

	private static final Logger log = Logger.getLogger(ChoiceSetGenerator.class);

	private final ChoiceGenerationControler choiceGenerationControler;

	private Controler controler;
	private Population population;
	private HashMap<Id<Person>,Plan> initialPlans = new HashMap<Id<Person>,Plan>();
	private MoneyEventHandler moneyHandler;
	static Integer choiceNumber = 2;

	public ChoiceSetGenerator(Config config, String eventsFile){

		choiceGenerationControler = new ChoiceGenerationControler(config, eventsFile);

		this.controler = choiceGenerationControler.getControler();
		this.population = controler.getScenario().getPopulation();

		//Add self for after run plan join and output writing
		controler.addControlerListener(this);

	}

	public static void main(String[] args) {
		String inputDirectory = args[0];
		String outputDirectory = args[1];
		String eventFilePath = args[2];

		Config config = scenarioInitializer.initScenario(inputDirectory, outputDirectory);
		ChoiceSetGenerator choiceSetGenerator = new ChoiceSetGenerator(config, eventFilePath);

		choiceSetGenerator.IncomeHeterogeneityAndTollInitializer();
		choiceSetGenerator.CreateChoiceSets();

		choiceSetGenerator.choiceGenerationControler.run();
	}

	public void IncomeHeterogeneityAndTollInitializer() {

		Integer inputDirectoryDepth = controler.getConfig().plans().getInputFile().split("/").length;
		boolean roadpricing = controler.getConfig().plans().getInputFile().split("/")[inputDirectoryDepth-2].contains("toll");

		if(roadpricing==true) {
			log.info("First-best roadpricing enabled!");
//			controler.setModules(new ControlerDefaultsModule(), new IncomeHeterogeneityWithoutTravelDisutilityModule(), new RoadPricingWithoutTravelDisutilityModule(),new UpdateSocialCostPricingSchemeModule());
			controler.setModules(new ControlerDefaultsModule(), new IncomeHeterogeneityWithoutTravelDisutilityModule(), new RoadPricingWithoutTravelDisutilityModule(), new LinkOccupancyAnalyzerModule(), new UpdateSocialCostPricingSchemeWithSpillOverModule());
			controler.addOverridingModule( new AbstractModule() {
				@Override
				public void install() {
					bind(TravelDisutilityFactory.class).toProvider(TravelDisutilityTollAndIncomeHeterogeneityProviderWrapper.TravelDisutilityWithPricingAndHeterogeneityProvider.class);
				}});
		}else{
			log.info("No roadpricing!");
			controler.setModules(new ControlerDefaultsModule(), new IncomeHeterogeneityWithoutTravelDisutilityModule());
			controler.addOverridingModule( new AbstractModule() {
				@Override
				public void install() {
					bind(TravelDisutilityFactory.class).toProvider(TravelDisutilityIncomeHeterogeneityProviderWrapper.TravelDisutilityIncludingIncomeHeterogeneityFactoryProvider.class);
				}});
		}

		//Scoring
		controler.setScoringFunctionFactory(new HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory(controler.getConfig().planCalcScore(), controler.getScenario().getNetwork()));

		// Additional analysis
		AnalysisControlerListener analysisControlerListener = new AnalysisControlerListener((ScenarioImpl) controler.getScenario());
		controler.addControlerListener(analysisControlerListener);

		//Money payment analysis
		moneyHandler = new MoneyEventHandler();
		controler.getEvents().addHandler(moneyHandler);

		controler.getScenario().getConfig().controler().setLastIteration(0);
	}

	public void CreateChoiceSets(){

		PopulationFactory populationFactory = population.getFactory();
		ArrayList<Person> newPersons = new ArrayList<>();

		/*Read original personalAttributes file*/
		HeterogeneityConfigGroup heterogeneityConfig = ConfigUtils.addOrGetModule(controler.getConfig(), HeterogeneityConfigGroup.GROUP_NAME, HeterogeneityConfigGroup.class);
		String personalAttributesFile = heterogeneityConfig.getIncomeFile();
		new ObjectAttributesXmlReader(population.getPersonAttributes()).parse(personalAttributesFile);

		for (Id<Person> personId : population.getPersons().keySet()) {

			Plan plan = population.getPersons().get(personId).getSelectedPlan();
			plan.getCustomAttributes().put("toll", population.getPersonAttributes().getAttribute(personId.toString(), "selectedPlanToll"));
			initialPlans.put(personId, plan);

			population.getPersons().get(personId).createCopyOfSelectedPlanAndMakeSelected();
			plan = population.getPersons().get(personId).getSelectedPlan();
			population.getPersons().get(personId).getPlans().clear();
			population.getPersons().get(personId).addPlan(plan);

			/* Plan cloning*/
			for (int i = 1; i < choiceNumber; i++) {
				population.getPersons().get(personId).createCopyOfSelectedPlanAndMakeSelected();
			}

			/* Plan modification*/
			for (Plan planToModify : population.getPersons().get(personId).getPlans()) {
				if (!planToModify.isSelected()) {
					Activity act = (Activity) planToModify.getPlanElements().get(0);
					act.setEndTime(act.getEndTime() + 3600.0);

					Leg leg = (Leg) planToModify.getPlanElements().get(1);
					leg = populationFactory.createLeg(leg.getMode());
					planToModify.getPlanElements().set(1, leg);

				}
			}

			/*Plan split to different persons*/
			int count = 0;
			for (Plan newPlan : population.getPersons().get(personId).getPlans()) {
				if (!newPlan.isSelected()){
					count++;
					Person newPerson = populationFactory.createPerson(Id.createPersonId(personId.toString()+"_"+count));
					newPerson.addPlan(newPlan);
					newPersons.add(newPerson);
					population.getPersonAttributes().putAttribute(newPerson.getId().toString(), "income", population.getPersonAttributes().getAttribute(personId.toString(), "income"));
				}
			}

			/*Clear all plans from initial person*/
			Plan selectedPlan = population.getPersons().get(personId).getSelectedPlan();
			population.getPersons().get(personId).getPlans().clear();
			population.getPersons().get(personId).addPlan(selectedPlan);
		}

		/*Add new agents to the simulation*/
		for(Person newPerson:newPersons){
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

		for(Id<Person> personId:population.getPersons().keySet()){

			Id<Person> orgPersonId = Id.create(personId.toString().split("_")[0], Person.class);

			Double monetaryPayments = moneyHandler.getPersonId2amount().get(personId);
			if(monetaryPayments==null) {
				monetaryPayments=0.0;
			}

			Plan planToAdd = null;
			if(personId.toString().contains("_")){
				planToAdd = population.getPersons().get(personId).getSelectedPlan();
				planToAdd.getCustomAttributes().put("toll", monetaryPayments.toString());
			}
			else {
				planToAdd = initialPlans.get(personId);
			}

			if(newPopulation.getPersons().containsKey(orgPersonId)){
				newPopulation.getPersons().get(orgPersonId).addPlan(planToAdd);
			}
			else{
				Person newPerson = newPopulation.getFactory().createPerson(orgPersonId);
				newPerson.addPlan(planToAdd);
				newPopulation.addPerson(newPerson);
			}
		}

		/*Write score table to SQL*/
		String connectionPropertiesPath = "/Users/artemc/Workspace/playgrounds/artemc/connections/matsim2postgresLocal.properties";
		String schema="corridor";

		Integer relativeOutputDirectory = event.getControler().getConfig().controler().getOutputDirectory().split("/").length;

		String tableName = schema+".scores_";
		String tableSuffix = event.getControler().getConfig().controler().getOutputDirectory().split("/")[relativeOutputDirectory-1];
		tableSuffix = tableSuffix.replaceAll("\\.0x", "x");
		tableSuffix = tableSuffix.replaceAll("-", "_");
		tableName = tableName + tableSuffix;

		IndividualScoreFromPopulationSQLWriter sqlWriter = new IndividualScoreFromPopulationSQLWriter(event.getControler().getConfig(),newPopulation);
		sqlWriter.writeToDatabase(connectionPropertiesPath, schema, tableName);

		new PopulationWriter(newScenario.getPopulation()).write(choiceGenerationControler.getControler().getConfig().controler().getOutputDirectory()+"/output_plansJoin.xml");
	}
}
