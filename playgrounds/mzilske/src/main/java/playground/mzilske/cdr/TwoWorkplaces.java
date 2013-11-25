package playground.mzilske.cdr;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class TwoWorkplaces {
	
	private Scenario scenario;

	public static void main(String[] args) {
		new TwoWorkplaces().run();
	}

	private void run() {
		int quantity = 1000;
		Config config = ConfigUtils.createConfig();
		ActivityParams workParams = new ActivityParams("work");
		workParams.setTypicalDuration(60*60*8);
		config.planCalcScore().addActivityParams(workParams);
		ActivityParams homeParams = new ActivityParams("home");
		homeParams.setTypicalDuration(16*60*60);
		config.planCalcScore().addActivityParams(homeParams);
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
			Person person = population.getFactory().createPerson(new IdImpl("2_"+Integer.toString(i)));
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
			Person person = population.getFactory().createPerson(new IdImpl("10_"+Integer.toString(i)));
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHomeMorning(new IdImpl("1")));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createWork(new IdImpl("10")));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createHomeEvening(new IdImpl("1")));
			person.addPlan(plan);
			population.addPerson(person);
		}
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		CompareMain compareMain = new CompareMain(scenario, controler.getEvents());
		controler.run();
		compareMain.finish();
	}

	private Activity createHomeMorning(IdImpl idImpl) {
		Activity act = scenario.getPopulation().getFactory().createActivityFromLinkId("home", idImpl);
		act.setEndTime(9 * 60 * 60);
		return act;
	}

	private Leg createDriveLeg() {
		Leg leg = scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		return leg;
	}

	private Activity createWork(IdImpl idImpl) {
		Activity act = scenario.getPopulation().getFactory().createActivityFromLinkId("work", idImpl);
		act.setEndTime(9 * 60 * 60);
		return act;
	}

	private Activity createHomeEvening(IdImpl idImpl) {
		Activity act = scenario.getPopulation().getFactory().createActivityFromLinkId("home", idImpl);
		return act;
	}

}
