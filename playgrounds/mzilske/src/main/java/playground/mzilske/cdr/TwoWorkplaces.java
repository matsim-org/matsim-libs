package playground.mzilske.cdr;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.basic.v01.IdImpl;
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
			Person person = population.getFactory().createPerson(createId(i));
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHomeMorning(new IdImpl("1")));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createWork(new IdImpl("2")));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createHomeEvening(new IdImpl("1")));
			person.addPlan(plan);
			population.addPerson(person);
		}
		for (int i=0; i<quantity; i++) {
			Person person = population.getFactory().createPerson(createId(quantity + i));
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHomeMorning(new IdImpl("1")));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createWork(new IdImpl("10")));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createHomeEvening(new IdImpl("1")));
			person.addPlan(plan);
			population.addPerson(person);
		}
		return scenario;
    }

	private Activity createHomeMorning(IdImpl idImpl) {
		Activity act = scenario.getPopulation().getFactory().createActivityFromLinkId("home", idImpl);
		act.setEndTime(9 * 60 * 60);
		return act;
	}

	private Leg createDriveLeg() {
        return scenario.getPopulation().getFactory().createLeg(TransportMode.car);
	}

	private Activity createWork(IdImpl idImpl) {
		Activity act = scenario.getPopulation().getFactory().createActivityFromLinkId("work", idImpl);
		act.setEndTime(9 * 60 * 60);
		return act;
	}

	private Activity createHomeEvening(IdImpl idImpl) {
        return scenario.getPopulation().getFactory().createActivityFromLinkId("home", idImpl);
	}


    private Id createId(int i) {
        return new IdImpl(Integer.toString(i));
    }


}
