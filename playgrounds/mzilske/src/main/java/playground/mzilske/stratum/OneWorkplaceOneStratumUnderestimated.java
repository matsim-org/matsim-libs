package playground.mzilske.stratum;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

class OneWorkplaceOneStratumUnderestimated implements Provider<Scenario> {

    private Scenario scenario;

    static class ConfigProvider implements Provider<Config> {
        @Override
        public Config  get() {
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
            return config;
        }
    }

    @Override
		public Scenario get() {
        int quantity = 1000;
        scenario = ScenarioUtils.createScenario(new ConfigProvider().get());
        new MatsimNetworkReader(scenario).parse(this.getClass().getResourceAsStream("one-workplace.xml"));
        Population population = scenario.getPopulation();
        for (int i=0; i<quantity; i++) {
            Person person = population.getFactory().createPerson(Id.create("9h_"+Integer.toString(i), Person.class));
            Plan plan = population.getFactory().createPlan();
            plan.addActivity(createHomeMorning(Id.create("1", Link.class), 9 * 60 * 60));
            plan.addLeg(createDriveLeg());
            plan.addActivity(createWork(Id.create("20", Link.class)));
            plan.addLeg(createDriveLeg());
            plan.addActivity(createHomeEvening(Id.create("1", Link.class)));
            plan.getCustomAttributes().put("prop", 0);
            person.addPlan(plan);
            population.addPerson(person);
        }
        for (int i=0; i<quantity; i++) {
            Person person = population.getFactory().createPerson(Id.create("7h_"+Integer.toString(i), Person.class));
            Plan plan = population.getFactory().createPlan();
            plan.addActivity(createHomeMorning(Id.create("1", Link.class), 7 * 60 * 60));
            plan.addLeg(createDriveLeg());
            plan.addActivity(createWork(Id.create("20", Link.class)));
            plan.addLeg(createDriveLeg());
            plan.addActivity(createHomeEvening(Id.create("1", Link.class)));
            if (i < quantity * 0.7) {
                plan.getCustomAttributes().put("prop", 2);
            } else {
                plan.getCustomAttributes().put("prop", 1);
            }
            person.addPlan(plan);
            population.addPerson(person);
        }
        return scenario;
    }

    private Activity createHomeMorning(Id<Link> idImpl, double time) {
        Activity act = scenario.getPopulation().getFactory().createActivityFromLinkId("home", idImpl);
        act.setEndTime(time);
        return act;
    }

    private Leg createDriveLeg() {
        return scenario.getPopulation().getFactory().createLeg(TransportMode.car);
    }

    private Activity createWork(Id<Link> idImpl) {
        Activity act = scenario.getPopulation().getFactory().createActivityFromLinkId("work", idImpl);
        act.setEndTime(17 * 60 * 60);
        return act;
    }

    private Activity createHomeEvening(Id<Link> idImpl) {
        return scenario.getPopulation().getFactory().createActivityFromLinkId("home", idImpl);
    }


}
