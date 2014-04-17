package playground.mzilske.stratum;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

import javax.inject.Inject;

public class OneWorkplaceOneStratumUnderestimated implements Provider<Scenario> {


    private Scenario scenario;

    @Inject Config config;

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

    public Scenario get() {
        int quantity = 1000;
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
        return scenario;
    }

    private Counts filterCounts(Counts counts) {
        Counts result = new Counts();
        for (Count count : counts.getCounts().values()) {
            Count newCount = result.createAndAddCount(count.getLocId(), count.getCsId());
            for (Volume volume : count.getVolumes().values()) {
                newCount.createVolume(volume.getHourOfDayStartingWithOne(), volume.getValue());
            }

        }
        return result;
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


}
