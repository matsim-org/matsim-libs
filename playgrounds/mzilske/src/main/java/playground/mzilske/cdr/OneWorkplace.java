package playground.mzilske.cdr;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Counts;
import playground.mzilske.d4d.Sighting;

import java.util.List;
import java.util.Map;

class OneWorkplace {

	Scenario scenario;
	private CompareMain compareMain;
    private VolumesAnalyzer cdrVolumes;

    public VolumesAnalyzer getCdrVolumes() {
        return cdrVolumes;
    }

    void run(String outputDirectory) {
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
		QSimConfigGroup tmp = config.qsim();
		tmp.setFlowCapFactor(100);
		tmp.setStorageCapFactor(100);
		tmp.setRemoveStuckVehicles(false);
		tmp.setEndTime(24*60*60);
		scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).parse(this.getClass().getResourceAsStream("one-workplace.xml"));
		Population population = scenario.getPopulation();
		for (int i=0; i<quantity; i++) {
			Person person = population.getFactory().createPerson(createId(i));
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHomeMorning(new IdImpl("1")));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createWork(new IdImpl("20")));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createHomeEvening(new IdImpl("1")));
			person.addPlan(plan);
			population.addPerson(person);
		}
		Controler controler = new Controler(scenario);
        ZoneTracker.LinkToZoneResolver linkToZoneResolver = new ZoneTracker.LinkToZoneResolver() {

            @Override
            public Id resolveLinkToZone(Id linkId) {
                return linkId;
            }

            public IdImpl chooseLinkInZone(String zoneId) {
                return new IdImpl(zoneId);
            }

        };
        compareMain = new CompareMain(scenario, controler.getEvents(), new CallBehavior() {

			@Override
			public boolean makeACall(ActivityEndEvent event) {
				return Integer.parseInt(event.getPersonId().toString()) % 2 == 0;
			}

			@Override
			public boolean makeACall(ActivityStartEvent event) {
				return Integer.parseInt(event.getPersonId().toString()) % 2 == 1;
			}

			@Override
			public boolean makeACall(Id id, double time) {
				double dailyRate = 0;
				double secondlyProbability = dailyRate / (double) (24*60*60);
				return Math.random() < secondlyProbability;
			}

			@Override
			public boolean makeACallAtMorningAndNight() {
				return true;
			}

		}, linkToZoneResolver);
		controler.run();
        compareMain.close();
        Map<Id, List<Sighting>> sightings = compareMain.getSightingsPerPerson();

        Counts counts = CompareMain.volumesToCounts(scenario.getNetwork(), compareMain.getGroundTruthVolumes());

        cdrVolumes = CompareMain.runWithTwoPlansAndCadyts(outputDirectory + "/output2", scenario.getNetwork(), linkToZoneResolver, sightings, counts);
        System.out.printf("%f\t%f\t%f\n", CompareMain.compareAllDay(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), CompareMain.compareTimebins(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), CompareMain.compareEMDMassPerLink(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()));
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
		act.setEndTime(13 * 60 * 60);
		return act;
	}

	private Activity createHomeEvening(IdImpl idImpl) {
        return scenario.getPopulation().getFactory().createActivityFromLinkId("home", idImpl);
	}

	private Id createId(int i) {
		return new IdImpl(Integer.toString(i));
	}

	CompareMain getCompare() {
		return compareMain;
	}

}
