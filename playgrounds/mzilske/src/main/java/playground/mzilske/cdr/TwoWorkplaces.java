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
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

class TwoWorkplaces {
	
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
        config.controler().setOutputDirectory(outputDirectory);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		QSimConfigGroup tmp = config.qsim();
		tmp.setFlowCapFactor(100);
		tmp.setStorageCapFactor(100);
		tmp.setRemoveStuckVehicles(false);
		scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).parse(this.getClass().getResourceAsStream("two-workplaces.xml"));
		Population population = scenario.getPopulation();
		for (int i=0; i<quantity; i++) {
			Person person = population.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHomeMorning(Id.create("1", Link.class)));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createWork(Id.create("2", Link.class)));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createHomeEvening(Id.create("1", Link.class)));
			person.addPlan(plan);
			population.addPerson(person);
		}
		for (int i=0; i<quantity; i++) {
			Person person = population.getFactory().createPerson(Id.create(quantity + i, Person.class));
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHomeMorning(Id.create("1", Link.class)));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createWork(Id.create("10", Link.class)));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createHomeEvening(Id.create("1", Link.class)));
			person.addPlan(plan);
			population.addPerson(person);
		}
		return scenario;
    }

	private Activity createHomeMorning(Id<Link> idImpl) {
		Activity act = scenario.getPopulation().getFactory().createActivityFromLinkId("home", idImpl);
		act.setEndTime(9 * 60 * 60);
		return act;
	}

	private Leg createDriveLeg() {
        return scenario.getPopulation().getFactory().createLeg(TransportMode.car);
	}

	private Activity createWork(Id<Link> idImpl) {
		Activity act = scenario.getPopulation().getFactory().createActivityFromLinkId("work", idImpl);
		act.setEndTime(9 * 60 * 60);
		return act;
	}

	private Activity createHomeEvening(Id<Link> idImpl) {
        return scenario.getPopulation().getFactory().createActivityFromLinkId("home", idImpl);
	}

}
