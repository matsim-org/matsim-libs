package playground.mzilske.cdr;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.general.ExpBetaPlanSelectorWithCadytsPlanRegistration;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.counts.CountControlerListener;
import org.matsim.counts.Counts;
import playground.mzilske.d4d.Sighting;

import java.util.List;
import java.util.Map;

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

		}, linkToZoneResolver);
		controler.run();
        compareMain.close();



        final Config config1 = ConfigUtils.createConfig();
        config1.planCalcScore().setWriteExperiencedPlans(true);
        config1.controler().setLastIteration(300);
        config1.qsim().setFlowCapFactor(100);
        config1.qsim().setStorageCapFactor(100);
        config1.qsim().setRemoveStuckVehicles(false);
        config1.counts().setOutputFormat("html");
        config1.counts().setWriteCountsInterval(1);
        config1.counts().setAverageCountsOverIterations(1);

        StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(new IdImpl(1));
        stratSets.setModuleName("ccc") ;
        stratSets.setProbability(1.) ;
        config1.strategy().addStrategySettings(stratSets) ;
        StrategyConfigGroup.StrategySettings random = new StrategyConfigGroup.StrategySettings(new IdImpl(2));
        random.setModuleName(PlanStrategyRegistrar.Selector.SelectRandom.toString()) ;
        random.setProbability(0.1) ;
        config1.strategy().addStrategySettings(random) ;




        final Map<Id, List<Sighting>> allSightings = compareMain.getSightingsPerPerson();

        final ScenarioImpl scenario2 = (ScenarioImpl) ScenarioUtils.createScenario(config1);
        scenario2.setNetwork(scenario.getNetwork());

        PopulationFromSightings.createPopulationWithEndTimesAtLastSightingsAndAdditionalInflationPopulation(scenario2, linkToZoneResolver, allSightings);
        PopulationFromSightings.preparePopulation(scenario2, linkToZoneResolver, allSightings);


        Counts counts = CompareMain.volumesToCounts(scenario.getNetwork(), compareMain.getGroundTruthVolumes());
        final CadytsContext context = new CadytsContext(config1, counts) ;

        Controler controler1 = new Controler(scenario2);
        controler1.setOverwriteFiles(true);
        controler1.addControlerListener(new CountControlerListener(config1.counts()));
        controler1.addControlerListener(context);

        controler1.setScoringFunctionFactory(new ScoringFunctionFactory() {

            @Override
            public ScoringFunction createNewScoringFunction(Person person1) {
                SumScoringFunction sumScoringFunction = new SumScoringFunction();
                CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person1.getSelectedPlan(), config1, context);
                scoringFunction.setWeightOfCadytsCorrection(100000000000.0);
                sumScoringFunction.addScoringFunction(scoringFunction);
                return sumScoringFunction;
            }

        });
        controler1.addPlanStrategyFactory("ccc", new PlanStrategyFactory() {
            @Override
            public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
                ExpBetaPlanSelectorWithCadytsPlanRegistration planSelector = new ExpBetaPlanSelectorWithCadytsPlanRegistration(1.0, context);
                return new PlanStrategyImpl(planSelector);
            }} ) ;
        controler1.setCreateGraphs(false);
        controler1.run();
        VolumesAnalyzer cdrVolumes = controler1.getVolumes();

        int nSelectedClones[] = new int[3];
		for (Person person : scenario2.getPopulation().getPersons().values()) {
			Id id = person.getId();
			if (id.toString().startsWith("I_")) {
				id = new IdImpl(id.toString().substring(2));
				if (person.getPlans().get(0) == person.getSelectedPlan()) {
					nSelectedClones[(Integer) scenario.getPopulation().getPersons().get(id).getPlans().get(0).getCustomAttributes().get("prop")]++;
				}
			}
			if (person.getPlans().size() == 2) {
				double score0 = CompareMain.calcCadytsScore(context, person.getPlans().get(0));
				double score1 = CompareMain.calcCadytsScore(context, person.getPlans().get(1));
				System.out.printf("%f\t%f\t%d\n", score0, score1, scenario.getPopulation().getPersons().get(id).getPlans().get(0).getCustomAttributes().get("prop"));
			} else {
				double score0 = CompareMain.calcCadytsScore(context, person.getPlans().get(0));
				System.out.printf("%f\t\t%d\n", score0, scenario.getPopulation().getPersons().get(id).getPlans().get(0).getCustomAttributes().get("prop"));
			}
		}
		System.out.printf("%f\t%f\t%f\n",
                CompareMain.compareAllDay(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()),
                CompareMain.compareTimebins(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()),
                CompareMain.compareEMDMassPerLink(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()));
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
