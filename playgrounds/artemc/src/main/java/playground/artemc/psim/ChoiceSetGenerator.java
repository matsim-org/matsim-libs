package playground.artemc.psim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import playground.artemc.analysis.IndividualScoreFromPopulationSQLWriter;

import java.util.ArrayList;

/**
 * Choice set generator based on the selected plan. Uses Psim for plans scoring.
 * Takes folder with simulation outputs and the desired output folder as parameters.
 * Created by artemc on 21/4/15.
 */
public class ChoiceSetGenerator implements ShutdownListener
{

	private static final Logger log = Logger.getLogger(ChoiceSetGenerator.class);

	ChoiceGenerationControler choiceGenerationControler;
	Population population;
	static Integer choiceNumber = 2;

	public ChoiceSetGenerator(String[] args){
		this.choiceGenerationControler = new ChoiceGenerationControler(args[0],args[1]);
		this.population = choiceGenerationControler.getControler().getScenario().getPopulation();
	}

	public static void main(String[] args) {
		ChoiceSetGenerator choiceSetGenerator = new ChoiceSetGenerator(args);
		choiceSetGenerator.choiceGenerationControler.getControler().addControlerListener(choiceSetGenerator);
		choiceSetGenerator.run();
	}

	public void run(){

//		String inputPopulationFile = args[0];
//		String outputPopulationFile = args[1];
//
//		/*Create scenario and load population*/
//		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		log.info("Reading population...");
//		new MatsimPopulationReader(scenario).readFile(inputPopulationFile);
//		Population population = ((ScenarioImpl) scenario).getPopulation();
//
//		System.out.println("Number of persons: " + population.getPersons().size());

		PopulationFactory populationFactory = population.getFactory();
		ArrayList<Person> newPersons = new ArrayList<>();
		for (Id personId : population.getPersons().keySet()) {

			Plan plan = population.getPersons().get(personId).getSelectedPlan();
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
				}
			}

			/*Clear all plans from initial person*/
			Plan planSelected = population.getPersons().get(personId).getSelectedPlan();
			population.getPersons().get(personId).getPlans().clear();
			population.getPersons().get(personId).addPlan(planSelected);
		}

		/*Add new agents to the simulation*/
		for(Person newPerson:newPersons){
			population.addPerson(newPerson);
		}

//		/*Write out new population file*/
//		System.out.println("New number of persons: " + population.getPersons().size());
//		new org.matsim.core.population.PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(outputPopulationFile);

		choiceGenerationControler.run();
	}

	public ArrayList<Plan> generateChoices(Plan selectedPlan) {
		ArrayList<Plan> plans = new ArrayList<Plan>();
		return plans;
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population newPopulation = scenario.getPopulation();
		for(Id<Person> personId:population.getPersons().keySet()){
			Id<Person> orgPersonId = Id.create(personId.toString().split("_")[0],Person.class);
			Plan planToAdd = population.getPersons().get(personId).getSelectedPlan();

			if(newPopulation.getPersons().containsKey(orgPersonId)){
				newPopulation.getPersons().get(orgPersonId).addPlan(planToAdd);
			}
			else{
				Person newPerson = newPopulation.getFactory().createPerson(orgPersonId);
				newPerson.addPlan(planToAdd);
				newPopulation.addPerson(newPerson);
			}
		}

		String connectionPropertiesPath = "/Users/artemc/Workspace/playgrounds/artemc/connections/matsim2postgresLocal.properties";
		String schema="corridor";

		Integer relativeOutputDirectory = event.getControler().getConfig().controler().getOutputDirectory().split("/").length;

		String tableName = schema+".scores_";
		String tableSuffix = event.getControler().getConfig().controler().getOutputDirectory().split("/")[relativeOutputDirectory-1];
		tableSuffix = tableSuffix.replaceAll("\\.0x", "x");
		tableName = tableName + tableSuffix;

		IndividualScoreFromPopulationSQLWriter sqlWriter = new IndividualScoreFromPopulationSQLWriter(event.getControler().getConfig(),newPopulation);
		sqlWriter.writeToDatabase(connectionPropertiesPath, schema, tableName);

		new PopulationWriter(scenario.getPopulation()).write(choiceGenerationControler.getControler().getConfig().controler().getOutputDirectory()+"/output_plansJoin.xml");
	}
}
