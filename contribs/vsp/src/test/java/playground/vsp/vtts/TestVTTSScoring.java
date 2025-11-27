package playground.vsp.vtts;

import com.google.inject.Singleton;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.examples.ExamplesUtils;
import playground.vsp.analysis.modules.VTTS.VTTSHandler;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import static org.assertj.core.api.Assertions.assertThat;


public class TestVTTSScoring {

	private static final double EPSILON = 1e-6;

	@Test
	public void testWithNoIncome() {
		String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("equil-mixedTraffic"));
		//Config config = ConfigUtils.loadConfig(inputPath + "config-with-mode-vehicles.xml");
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(inputPath + "network.xml");
		config.controller().setOutputDirectory("output/VTTSTest/");
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);
		config.scoring().setExplainScores(true);
		config.scoring().setWriteExperiencedPlans(true);
		config.scoring().getOrCreateModeParams(TransportMode.car).setMarginalUtilityOfTraveling(0);

		ScoringConfigGroup.ActivityParams paramsWork = new ScoringConfigGroup.ActivityParams();
		// eight hours
		paramsWork.setTypicalDuration(8*3600);
		paramsWork.setActivityType("work");

		ScoringConfigGroup.ActivityParams paramsHome = new ScoringConfigGroup.ActivityParams();
		paramsHome.setActivityType("home");
		// twelve hours of home activity
		paramsHome.setTypicalDuration(12*3600);
		config.scoring().addActivityParams(paramsHome);
		config.scoring().addActivityParams(paramsWork);
		config.scoring().getScoringParameters(null).setLateArrival_utils_hr(0.);
		config.scoring().getScoringParameters(null).setMarginalUtlOfWaiting_utils_hr(0.);
		config.scoring().getScoringParameters(null).setMarginalUtlOfWaitingPt_utils_hr(0.);
		config.scoring().getScoringParameters(null).setEarlyDeparture_utils_hr(0.);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		createTestPopulation(scenario, DifferentIncomeForAgents.SAME_INCOME_FOR_AGENTS);
		Controler controler = new Controler(scenario);

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				//this.addEventHandlerBinding().toInstance( new VTTSHandler( new String[]{"freight"} ) );
				//this.addEventHandlerBinding().to( VTTSHandler.class ); // this would be better
				bind(VTTSHandler.class).in(Singleton.class);
				addEventHandlerBinding().to(VTTSHandler.class);
			}
		} );

		controler.run();

		VTTSHandler vttsHandler = controler.getInjector().getInstance( VTTSHandler.class );

		vttsHandler.computeFinalVTTS();
		vttsHandler.printVTTS(controler.getConfig().controller().getOutputDirectory()+"vtts.csv");
		/*
		this is the average between the work activity which is exact the typical duration --> 6.001979820359793
		and the home activity which is a lot longer then the typical duration --> 4.76729108633549
		*/
		//assertThat(vttsHandler.getAvgVTTSh(Id.createPersonId("perfectDuration"))).isEqualTo(5.3846354533476415);
		// use isCloseTo with epsilon
		assertThat(vttsHandler.getAvgVTTSh(Id.createPersonId("perfectDuration")))
			.isCloseTo(5.3846354533476415, Offset.offset(EPSILON));


		/*
		this is the average between the work activity which is below the typical duration --> 8.811606029321695 --> higher than the default of 6
		and the home activity which is a lot longer then the typical duration --> 4.0786461161928855
		*/
		//assertThat(vttsHandler.getAvgVTTSh(Id.createPersonId("timePressure"))).isEqualTo(6.44512607275729);

		assertThat(vttsHandler.getAvgVTTSh(Id.createPersonId("timePressure")))
			.isCloseTo(6.44512607275729, Offset.offset(EPSILON));
	}


	@Test
	public void testWithDifferentIncome() {
		String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("equil-mixedTraffic"));
		//Config config = ConfigUtils.loadConfig(inputPath + "config-with-mode-vehicles.xml");
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(inputPath + "network.xml");
		config.controller().setOutputDirectory("output/VTTSTestWithIncome/");
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);
		config.scoring().setExplainScores(true);
		config.scoring().setWriteExperiencedPlans(true);
		config.scoring().getOrCreateModeParams(TransportMode.car).setMarginalUtilityOfTraveling(0);

		ScoringConfigGroup.ActivityParams paramsWork = new ScoringConfigGroup.ActivityParams();
		// eight hours
		paramsWork.setTypicalDuration(8*3600);
		paramsWork.setActivityType("work");

		ScoringConfigGroup.ActivityParams paramsHome = new ScoringConfigGroup.ActivityParams();
		paramsHome.setActivityType("home");
		// twelve hours of home activity
		paramsHome.setTypicalDuration(12*3600);
		config.scoring().addActivityParams(paramsHome);
		config.scoring().addActivityParams(paramsWork);
		config.scoring().getScoringParameters(null).setLateArrival_utils_hr(0.);
		config.scoring().getScoringParameters(null).setMarginalUtlOfWaiting_utils_hr(0.);
		config.scoring().getScoringParameters(null).setMarginalUtlOfWaitingPt_utils_hr(0.);
		config.scoring().getScoringParameters(null).setEarlyDeparture_utils_hr(0.);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		createTestPopulation(scenario, DifferentIncomeForAgents.DIFFERENT_INCOME_FOR_AGENTS);
		Controler controler = new Controler(scenario);

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				bind(VTTSHandler.class).in(Singleton.class);
				addEventHandlerBinding().to(VTTSHandler.class);
				bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).asEagerSingleton();
			}
		} );

		controler.run();

		VTTSHandler vttsHandler = controler.getInjector().getInstance( VTTSHandler.class );

		vttsHandler.computeFinalVTTS();
		vttsHandler.printVTTS(controler.getConfig().controller().getOutputDirectory()+"vtts.csv");
		/*
		this is the average between the work activity which is exact the typical duration --> 6.001979820359793 /0.75 --> 8.002639760479724
		and the home activity which is a lot longer then the typical duration --> 4.76729108633549 --> 4.76729108633549 /0.75 --> 6.356388115113987
		the 0.75 is the person specific marginal utility of money
		*/
		//assertThat(vttsHandler.getAvgVTTSh(Id.createPersonId("perfectDuration"))).isEqualTo(7.179513937796855);
		assertThat(vttsHandler.getAvgVTTSh(Id.createPersonId("perfectDuration")))
			.isCloseTo(7.179513937796855, Offset.offset(EPSILON));

		/*
		this is the average between the work activity which is below the typical duration --> 8.811606029321695 / 1.5 --> 5.8744040195477965
		and the home activity which is a lot longer then the typical duration --> 4.0786461161928855 / 1.5 --> 2.719097410795257
		the 1.5 is the person specific marginal utility of money
		*/
		//assertThat(vttsHandler.getAvgVTTSh(Id.createPersonId("timePressure"))).isEqualTo(4.296750715171527);
		assertThat(vttsHandler.getAvgVTTSh(Id.createPersonId("timePressure")))
			.isCloseTo(4.296750715171527, Offset.offset(EPSILON));
	}




	private static void createTestPopulation(Scenario scenario, DifferentIncomeForAgents differentIncomeForAgents) {
		Population population = scenario.getPopulation();
		Person person = population.getFactory().createPerson(Id.createPersonId("perfectDuration"));
		if (differentIncomeForAgents == DifferentIncomeForAgents.DIFFERENT_INCOME_FOR_AGENTS) {
			PersonUtils.setIncome(person, 200.);
		} else {
			PersonUtils.setIncome(person, 10.);
		}
		Plan plan = population.getFactory().createPlan();
		Activity activityHome = scenario.getPopulation().getFactory().createActivityFromLinkId("home", Id.createLinkId("1"));
		activityHome.setEndTime(10.0);
		Activity activityWork = scenario.getPopulation().getFactory().createActivityFromLinkId("work", Id.createLinkId("6"));
		activityWork.setEndTime(29160.0);
		Activity activityHome2 = scenario.getPopulation().getFactory().createActivityFromLinkId("home", Id.createLinkId("1"));
		activityHome2.setEndTime(36360.0);

		plan.addActivity(activityHome);
		//360 seconds travel time
		plan.addLeg(population.getFactory().createLeg(TransportMode.car));
		plan.addActivity(activityWork);
		plan.addLeg(population.getFactory().createLeg(TransportMode.car));
		plan.addActivity(activityHome2);
		person.addPlan(plan);
		population.addPerson(person);

		Person personThatIsUnderTimePressure = population.getFactory().createPerson(Id.createPersonId("timePressure"));
		if (differentIncomeForAgents == DifferentIncomeForAgents.DIFFERENT_INCOME_FOR_AGENTS) {
			PersonUtils.setIncome(personThatIsUnderTimePressure, 100.);
		} else {
			PersonUtils.setIncome(personThatIsUnderTimePressure, 10.);
		}
		Plan planForTimePressureAgent = population.getFactory().createPlan();
		Activity activityHomeStartLater = scenario.getPopulation().getFactory().createActivityFromLinkId("home", Id.createLinkId("1"));
		activityHomeStartLater.setEndTime(30.0);
		//travel time still 360 seconds --> agent performs activity shorter, below typical duration --> higher vtts
		Activity activityWorkStartLater = scenario.getPopulation().getFactory().createActivityFromLinkId("work", Id.createLinkId("6"));
		activityWorkStartLater.setEndTime(20000.0);
		planForTimePressureAgent.addActivity(activityHomeStartLater);
		planForTimePressureAgent.addLeg(population.getFactory().createLeg(TransportMode.car));
		planForTimePressureAgent.addActivity(activityWorkStartLater);
		planForTimePressureAgent.addLeg(population.getFactory().createLeg(TransportMode.car));
		planForTimePressureAgent.addActivity(activityHome2);
		personThatIsUnderTimePressure.addPlan(planForTimePressureAgent);
		population.addPerson(personThatIsUnderTimePressure);
	}



	private enum DifferentIncomeForAgents {
		DIFFERENT_INCOME_FOR_AGENTS,
		SAME_INCOME_FOR_AGENTS
	}


}
