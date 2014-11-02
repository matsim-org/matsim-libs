package playground.mzilske.cdr;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

class OneWorkplace {

	Scenario scenario;

    Scenario run(String outputDirectory) {
		int quantity = 1000;
		Config config = ConfigUtils.createConfig();
		ActivityParams workParams = new ActivityParams("work");
		workParams.setTypicalDuration(60*60*8);
		config.planCalcScore().addActivityParams(workParams);
		ActivityParams homeParams = new ActivityParams("home");
		homeParams.setTypicalDuration(16*60*60);
		config.planCalcScore().addActivityParams(homeParams);
        config.controler().setOutputDirectory(outputDirectory+"/output");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.planCalcScore().setWriteExperiencedPlans(true);
        config.qsim().setFlowCapFactor(100);
		config.qsim().setStorageCapFactor(100);
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setEndTime(30 * 60 * 60);
		scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).parse(this.getClass().getResourceAsStream("one-workplace.xml"));
		Population population = scenario.getPopulation();
		for (int i=0; i<quantity; i++) {
			Person person = population.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHomeMorning(Id.create("1", Link.class)));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createWork(Id.create("20", Link.class)));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createHomeEvening(Id.create("1", Link.class)));
            plan.addActivity(createHomeOvernight(Id.create("1", Link.class)));
			person.addPlan(plan);
			population.addPerson(person);
		}
        return scenario;
    }

    private Activity createHomeMorning(Id<Link> Id) {
		Activity act = scenario.getPopulation().getFactory().createActivityFromLinkId("home", Id);
		act.setEndTime(9 * 60 * 60);
		return act;
	}

	private Leg createDriveLeg() {
        return scenario.getPopulation().getFactory().createLeg(TransportMode.car);
	}

	private Activity createWork(Id<Link> id) {
		Activity act = scenario.getPopulation().getFactory().createActivityFromLinkId("work", id);
		act.setEndTime(13 * 60 * 60);
		return act;
	}

	private Activity createHomeEvening(Id<Link> id) {
        Activity activity = scenario.getPopulation().getFactory().createActivityFromLinkId("home", id);
        activity.setEndTime(30 * 60 * 60);
        return activity;

	}

    private Activity createHomeOvernight(Id<Link> id) {
        return scenario.getPopulation().getFactory().createActivityFromLinkId("home", id);
    }

}
