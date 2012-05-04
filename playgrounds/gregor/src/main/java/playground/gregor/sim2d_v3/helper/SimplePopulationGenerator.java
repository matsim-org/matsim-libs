package playground.gregor.sim2d_v3.helper;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.ConfigUtils;

public class SimplePopulationGenerator {



	private static final int NUM_PERSONS = 100;
	private static final Id fromLinkId = new IdImpl(38);
	private static final Id toLinkId = new IdImpl(123);
	private static final int RATE = 5;

	private final Scenario sc;

	public SimplePopulationGenerator(Scenario sc) {
		this.sc = sc;
	}

	private void generate() {

		Population pop = this.sc.getPopulation();
		PopulationFactory pb = pop.getFactory();


		for (int i = 0; i < NUM_PERSONS; i++) {
			Person pers = pb.createPerson(this.sc.createId("g"+Integer.toString(i)));
			pop.addPerson(pers);
			Plan plan = pb.createPlan();
			Activity act = pb.createActivityFromLinkId("pre-evac", fromLinkId);
			act.setEndTime(i/RATE);
			plan.addActivity(act);
			Leg leg = pb.createLeg("walk2d");
			plan.addLeg(leg);
			Activity act2 = pb.createActivityFromLinkId("post-evac", toLinkId);
			act2.setEndTime(0);
			plan.addActivity(act2);
			plan.setScore(0.);
			pers.addPlan(plan);
		}

	}

	public static void main(String[] args) {

		String config = "/Users/laemmel/devel/sim2DDemo/config2d.xml";
		Config c = ConfigUtils.loadConfig(config);
		Scenario sc = ScenarioUtils.loadScenario(c);

		new SimplePopulationGenerator(sc).generate();
		new PopulationWriter(sc.getPopulation(), sc.getNetwork()).write(c.plans().getInputFile());

	}


}
