package playground.mzilske.cdr;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Counts;
import org.matsim.testcases.MatsimTestUtils;
import playground.mzilske.cadyts.CadytsModule;
import playground.mzilske.cadyts.CadytsScoring;
import playground.mzilske.controller.Controller;
import playground.mzilske.controller.ControllerModuleWithScenario;

import javax.inject.Inject;
import java.util.*;

public class CDREquilTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    private static class AtStartOrEnd implements CallBehavior {

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
        public boolean makeACallAtMorningAndNight(Id<Person> id) {
            return true;
        }

    }


    private static class AnyTimeAtWork implements CallBehavior {


        Map<Id, Queue<Double>> callQueues = new HashMap<Id, Queue<Double>>();

        @Inject
        AnyTimeAtWork(Scenario scenario) {
            for (Person person : scenario.getPopulation().getPersons().values()) {
                Id id = person.getId();
                List<Double> calls = getCalls(scenario, id);
                callQueues.put(id, new PriorityQueue<Double>(calls));
            }
        }

        @Override
        public boolean makeACall(ActivityEndEvent event) {
            return false;
        }

        @Override
        public boolean makeACall(ActivityStartEvent event) {
            return false;
        }

        @Override
        public boolean makeACall(Id id, double time) {
            Queue<Double> callQueue = callQueues.get(id);
            while (callQueue.peek() != null && callQueue.peek() <= time) {
                // Whoops - I can make at most one call per time step anyway.
                callQueue.remove();
                return true;
            }
            return false;
        }

        @Override
        public boolean makeACallAtMorningAndNight(Id<Person> id) {
            return false;
        }

    }

    private static List<Double> getCalls(Scenario scenario, Id id) {
        List<Double> result = new ArrayList<Double>();
        double time = 0.0;
        for (PlanElement planElement : scenario.getPopulation().getPersons().get(id).getSelectedPlan().getPlanElements()) {
            if (planElement instanceof Activity) {
                Activity activity = (Activity) planElement;
                double endTime = activity.getEndTime() != Time.UNDEFINED_TIME ? activity.getEndTime() : 30.0 * 60.0 * 60.0;
                double callTime = time + (endTime - time) * Math.random();
                result.add(callTime);
                time = endTime;
            }
        }
        return result;
    }


    private static class CadytsScoringFunctionFactory implements ScoringFunctionFactory {

        @Inject
        Config config;

        @Inject
        AnalyticalCalibrator<Link> cadyts;

        @Inject
        PlansTranslator<Link> ptStep;

        @Override
        public ScoringFunction createNewScoringFunction(final Person person) {
            SumScoringFunction sumScoringFunction = new SumScoringFunction();
            CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(), config, ptStep, cadyts);
            sumScoringFunction.addScoringFunction(scoringFunction);
            return sumScoringFunction;
        }
    }
	
	
	/*
	 * Run a simple uncongested loop. One half of the population habitually phones at the beginning of activities, the other at
	 * the ends of activities.
	 * All of the population get two plans each, one which stays at activities as long as possible to reach the next sighting
	 * (assuming an uncongested network), and one (simpler) which departs each location as soon as the agent has been seen 
	 * there for the last consecutive time.
	 * 
	 * We expect Cadyts to find the 100% correct fit.
	 *  
	 */
	@Test
	public void testOneWorkplace() {
		final OneWorkplace oneWorkplace = new OneWorkplace();
        Injector injector = Guice.createInjector(
                new ControllerModuleWithScenario(oneWorkplace.run(utils.getOutputDirectory())),
                new CDRModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ZoneTracker.LinkToZoneResolver.class).to(LinkIsZone.class);
                        bind(CallBehavior.class).to(AtStartOrEnd.class);
                        bind(ScoringFunctionFactory.class).to(CharyparNagelScoringFunctionFactory.class);
                    }
                }
        );

        Controller controller = injector.getInstance(Controller.class);
        controller.run();

        CompareMain compareMain = injector.getInstance(CompareMain.class);
        ZoneTracker.LinkToZoneResolver linkToZoneResolver = injector.getInstance(ZoneTracker.LinkToZoneResolver.class);
        Scenario scenario = injector.getInstance(Scenario.class);

        VolumesAnalyzer cdrVolumes = runWithCadyts(scenario, compareMain, linkToZoneResolver);
        Assert.assertEquals("All-day squares", 0.0, CompareMain.compareAllDay(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), 0.0);
		Assert.assertEquals("Timebin squares", 0.0, CompareMain.compareTimebins(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), 0.0);
		Assert.assertEquals("EMD", 0.0, CompareMain.compareEMD(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), 0.0);
	}

    @Test @Ignore
    public void testOneWorkplaceAnytime() {
        final OneWorkplace oneWorkplace = new OneWorkplace();
        Injector injector = Guice.createInjector(
                new ControllerModuleWithScenario(oneWorkplace.run(utils.getOutputDirectory())),
                new CDRModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ZoneTracker.LinkToZoneResolver.class).to(LinkIsZone.class);
                        bind(CallBehavior.class).to(AnyTimeAtWork.class);
                        bind(ScoringFunctionFactory.class).to(CharyparNagelScoringFunctionFactory.class);
                    }
                }
        );

        Controller controller = injector.getInstance(Controller.class);
        controller.run();

        CompareMain compareMain = injector.getInstance(CompareMain.class);
        ZoneTracker.LinkToZoneResolver linkToZoneResolver = injector.getInstance(ZoneTracker.LinkToZoneResolver.class);
        Scenario scenario = injector.getInstance(Scenario.class);

        VolumesAnalyzer cdrVolumes = runWithCadyts(scenario, compareMain, linkToZoneResolver);
        Assert.assertEquals("All-day squares", 0.0, CompareMain.compareAllDay(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), 0.0);
        Assert.assertEquals("Timebin squares", 0.0, CompareMain.compareTimebins(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), 0.0);
        Assert.assertEquals("EMD", 0.0, CompareMain.compareEMD(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), 0.0);
    }

    @Test @Ignore
    public void testOneWorkplaceAnytimeWithReplanning() {
        final OneWorkplace oneWorkplace = new OneWorkplace();
        Injector injector = Guice.createInjector(
                new ControllerModuleWithScenario(oneWorkplace.run(utils.getOutputDirectory())),
                new CDRModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ZoneTracker.LinkToZoneResolver.class).to(LinkIsZone.class);
                        bind(CallBehavior.class).to(AnyTimeAtWork.class);
                        bind(ScoringFunctionFactory.class).to(CharyparNagelScoringFunctionFactory.class);
                    }
                }
        );

        Controller controller = injector.getInstance(Controller.class);
        controller.run();

        CompareMain compareMain = injector.getInstance(CompareMain.class);
        final Scenario scenario = injector.getInstance(Scenario.class);

        final Sightings sightings = new SightingsImpl(compareMain.getSightingsPerPerson());
        final Counts counts = CompareMain.volumesToCounts(scenario.getNetwork(), compareMain.getGroundTruthVolumes());

        Config phoneConfig = ConfigUtils.createConfig();
        PlanCalcScoreConfigGroup.ActivityParams sightingParam = new PlanCalcScoreConfigGroup.ActivityParams("sighting");
        sightingParam.setTypicalDuration(30.0 * 60);
        phoneConfig.planCalcScore().addActivityParams(sightingParam);
        phoneConfig.planCalcScore().setTraveling_utils_hr(0);
        phoneConfig.planCalcScore().setPerforming_utils_hr(0);
        phoneConfig.planCalcScore().setTravelingOther_utils_hr(0);
        phoneConfig.planCalcScore().setConstantCar(0);
        phoneConfig.planCalcScore().setMonetaryDistanceCostRateCar(0);
        phoneConfig.planCalcScore().setWriteExperiencedPlans(true);
        phoneConfig.controler().setOutputDirectory(utils.getOutputDirectory() + "/output2");
        phoneConfig.controler().setLastIteration(100);
        QSimConfigGroup tmp = phoneConfig.qsim();
        tmp.setFlowCapFactor(100);
        tmp.setStorageCapFactor(100);
        tmp.setRemoveStuckVehicles(false);
        {
            StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(new IdImpl(1));
            stratSets.setModuleName("SelectExpBeta");
            stratSets.setProbability(0.7);
            phoneConfig.strategy().addStrategySettings(stratSets);
        }
        {
            StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(new IdImpl(2));
            stratSets.setModuleName("ReRealize");
            stratSets.setProbability(0.3);
            stratSets.setDisableAfter(90);
            phoneConfig.strategy().addStrategySettings(stratSets);
        }

        final ScenarioImpl phoneScenario = (ScenarioImpl) ScenarioUtils.createScenario(phoneConfig);
        phoneScenario.setNetwork(scenario.getNetwork());


        List<Module> modules = new ArrayList<Module>();
        modules.add(new ControllerModuleWithScenario(phoneScenario));
        modules.add(new CadytsModule());
        modules.add(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Sightings.class).toInstance(sightings);
                bind(ZoneTracker.LinkToZoneResolver.class).to(LinkIsZone.class);
                bind(ScoringFunctionFactory.class).to(CadytsScoringFunctionFactory.class);
                bind(Counts.class).annotatedWith(Names.named("allCounts")).toInstance(counts);
                bind(Counts.class).annotatedWith(Names.named("calibrationCounts")).toInstance(counts);
                MapBinder<String, PlanStrategyFactory> planStrategyFactoryBinder
                        = MapBinder.newMapBinder(binder(), String.class, PlanStrategyFactory.class);
                planStrategyFactoryBinder.addBinding("ReRealize").to(TrajectoryReRealizerFactory.class);
            }
        });

        Injector injector2 = Guice.createInjector(modules);
        PopulationFromSightings.createPopulationWithRandomRealization(phoneScenario, sightings, injector2.getInstance(ZoneTracker.LinkToZoneResolver.class));
        Controller controler2 = injector2.getInstance(Controller.class);
        controler2.run();


        VolumesAnalyzer cdrVolumes = injector2.getInstance(VolumesAnalyzer.class);
        System.out.printf("%f\t%f\t%f\n", CompareMain.compareAllDay(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), CompareMain.compareTimebins(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), CompareMain.compareEMDMassPerLink(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()));

        Assert.assertEquals("All-day squares", 0.0, CompareMain.compareAllDay(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), 0.0);
        Assert.assertEquals("Timebin squares", 0.0, CompareMain.compareTimebins(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), 0.0);
        Assert.assertEquals("EMD", 0.0, CompareMain.compareEMD(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), 0.0);
    }

    @Test
    public void testTwoWorkplaces() {
        TwoWorkplaces twoWorkplaces = new TwoWorkplaces();
        Injector injector = Guice.createInjector(
                new ControllerModuleWithScenario(twoWorkplaces.run(utils.getOutputDirectory())),
                new CDRModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ZoneTracker.LinkToZoneResolver.class).to(LinkIsZone.class);
                        bind(CallBehavior.class).to(AtStartOrEnd.class);
                        bind(ScoringFunctionFactory.class).to(CharyparNagelScoringFunctionFactory.class);
                    }
                }
        );

        Controller controller = injector.getInstance(Controller.class);
        controller.run();

        CompareMain compareMain = injector.getInstance(CompareMain.class);
        ZoneTracker.LinkToZoneResolver linkToZoneResolver = injector.getInstance(ZoneTracker.LinkToZoneResolver.class);
        Scenario scenario = injector.getInstance(Scenario.class);

        VolumesAnalyzer cdrVolumes = runWithCadyts(scenario, compareMain, linkToZoneResolver);
        Assert.assertEquals("All-day squares", 0.0, CompareMain.compareAllDay(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), 0.0);
        Assert.assertEquals("Timebin squares", 0.0, CompareMain.compareTimebins(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), 0.0);
        Assert.assertEquals("EMD", 0.0, CompareMain.compareEMD(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), 0.0);
    }

    private VolumesAnalyzer runWithCadyts(Scenario scenario, CompareMain compareMain, ZoneTracker.LinkToZoneResolver linkToZoneResolver) {
        Sightings sightings = new SightingsImpl(compareMain.getSightingsPerPerson());
        Counts counts = CompareMain.volumesToCounts(scenario.getNetwork(), compareMain.getGroundTruthVolumes());
        VolumesAnalyzer cdrVolumes = CompareMain.runWithTwoPlansAndCadyts(utils.getOutputDirectory() + "/output2", scenario.getNetwork(), linkToZoneResolver, sightings, counts);
        System.out.printf("%f\t%f\t%f\n", CompareMain.compareAllDay(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), CompareMain.compareTimebins(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), CompareMain.compareEMDMassPerLink(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()));
        return cdrVolumes;
    }

}
