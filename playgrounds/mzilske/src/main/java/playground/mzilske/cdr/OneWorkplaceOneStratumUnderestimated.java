package playground.mzilske.cdr;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mzilske.cdr.CompareMain.Result;

public class OneWorkplaceOneStratumUnderestimated {

	private Scenario scenario;
	private CompareMain compareMain;

	public static void main(String[] args) {
		new OneWorkplaceOneStratumUnderestimated().run();
	}

	void run() {
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
		config.planCalcScore().setWriteExperiencedPlans(true);
		QSimConfigGroup tmp = config.qsim();
		tmp.setFlowCapFactor(100);
		tmp.setStorageCapFactor(100);
		tmp.setRemoveStuckVehicles(false);
		tmp.setEndTime(24*60*60);
		scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).parse(this.getClass().getResourceAsStream("one-workplace.xml"));
		Population population = scenario.getPopulation();
		for (int i=0; i<quantity; i++) {
			Person person = population.getFactory().createPerson(new IdImpl("9h_"+Integer.toString(i)));
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHomeMorning(new IdImpl("1"), 9 * 60 * 60));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createWork(new IdImpl("20")));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createHomeEvening(new IdImpl("1")));
			plan.getCustomAttributes().put("prop", 0);
			person.addPlan(plan);
			population.addPerson(person);
		}
		for (int i=0; i<quantity; i++) {
			Person person = population.getFactory().createPerson(new IdImpl("7h_"+Integer.toString(i)));
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHomeMorning(new IdImpl("1"), 7 * 60 * 60));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createWork(new IdImpl("20")));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createHomeEvening(new IdImpl("1")));
			if (i < quantity * 0.7) {
				plan.getCustomAttributes().put("prop", 2);
			} else {
				plan.getCustomAttributes().put("prop", 1);
			}
			person.addPlan(plan);
			population.addPerson(person);
		}
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		compareMain = new CompareMain(scenario, controler.getEvents(), new CallBehavior() {

			@Override
			public boolean makeACall(ActivityEndEvent event) {
				Plan plan = scenario.getPopulation().getPersons().get(event.getPersonId()).getSelectedPlan();
				if (event.getActType().equals("home") || plan.getCustomAttributes().get("prop").equals(0)) {
					return true;
				} else if (plan.getCustomAttributes().get("prop").equals(2)) {
					return true;
				} else {
					return false;
				}
			}

			@Override
			public boolean makeACall(ActivityStartEvent event) {
				return false;
			}

			@Override
			public boolean makeACall(Id id, double time) {
				return false;
			}

			@Override
			public boolean makeACallAtMorningAndNight() {
				return true;
			}

		});
		controler.run();
		Result result = compareMain.runWithOnePlanAndCadytsAndInflation();
		// Result result = compareMain.runWithOnePlanAndCadyts();
		int nSelectedClones[] = new int[3];
		for (Person person : result.scenario.getPopulation().getPersons().values()) {
			Id id = person.getId();
			if (id.toString().startsWith("I_")) {
				id = new IdImpl(id.toString().substring(2));
				if (person.getPlans().get(0) == person.getSelectedPlan()) {
					nSelectedClones[(Integer) scenario.getPopulation().getPersons().get(id).getPlans().get(0).getCustomAttributes().get("prop")]++;
				}
			}
			if (person.getPlans().size() == 2) {
				double score0 = CompareMain.calcCadytsScore(result.context, person.getPlans().get(0));
				double score1 = CompareMain.calcCadytsScore(result.context, person.getPlans().get(1));
				// double score1 = 0;
				System.out.printf("%f\t%f\t%d\n", score0, score1, scenario.getPopulation().getPersons().get(id).getPlans().get(0).getCustomAttributes().get("prop"));
			} else {
				double score0 = CompareMain.calcCadytsScore(result.context, person.getPlans().get(0));
				System.out.printf("%f\t\t%d\n", score0, scenario.getPopulation().getPersons().get(id).getPlans().get(0).getCustomAttributes().get("prop"));
			}
		}
		System.out.printf("%f\t%f\t%f\n",compareMain.compareAllDay(), compareMain.compareTimebins(), compareMain.compareEMDMassPerLink());
		System.out.println(nSelectedClones[0] + " " + nSelectedClones[1] + " "+ nSelectedClones[2]);

	}

	private Activity createHomeMorning(IdImpl idImpl, double time) {
		Activity act = scenario.getPopulation().getFactory().createActivityFromLinkId("home", idImpl);
		act.setEndTime(time);
		return act;
	}

	private Leg createDriveLeg() {
		Leg leg = scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		return leg;
	}

	private Activity createWork(IdImpl idImpl) {
		Activity act = scenario.getPopulation().getFactory().createActivityFromLinkId("work", idImpl);
		act.setEndTime(13 * 60 * 60);
		return act;
	}

	private Activity createHomeEvening(IdImpl idImpl) {
		Activity act = scenario.getPopulation().getFactory().createActivityFromLinkId("home", idImpl);
		return act;
	}

	CompareMain getCompare() {
		return compareMain;
	}

}
