package playground.mzilske.cdr;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.TravelDistanceStatsModule;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.analysis.VolumesAnalyzerModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.mobsim.qsim.QSimModule;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionModule;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;
import org.matsim.counts.Counts;
import org.matsim.testcases.MatsimTestUtils;

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
        public boolean makeACallAtMorningAndNight(Id<Person> id) {
            return true;
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
        Controler controler = new Controler(new OneWorkplace().run(utils.getOutputDirectory()));
        LinkIsZone linkIsZone = new LinkIsZone();
        controler.setModules(
                new EventsManagerModule(),
                new TravelDistanceStatsModule(),
                new CharyparNagelScoringFunctionModule(),
                new TripRouterModule(),
                new TravelDisutilityModule(),
                new TravelTimeCalculatorModule(),
                new StrategyManagerModule(),
                new VolumesAnalyzerModule(),
                new CollectSightingsModule(),
                new CallBehaviorModule(new AtStartOrEnd(), linkIsZone),
                new AbstractModule() {
                    @Override
                    public void install() {
                        install(new QSimModule());
                    }
                });
        controler.run();
        Scenario scenario = controler.getScenario();
        VolumesAnalyzer groundTruthVolumes = controler.getVolumes();
        VolumesAnalyzer cdrVolumes = runWithCadyts(scenario, linkIsZone, (Sightings) scenario.getScenarioElement("sightings"), groundTruthVolumes);
        Assert.assertEquals("All-day squares", 0.0, CompareMain.compareAllDay(scenario, cdrVolumes, groundTruthVolumes), 0.0);
		Assert.assertEquals("Timebin squares", 0.0, CompareMain.compareTimebins(scenario, cdrVolumes, groundTruthVolumes), 0.0);
		Assert.assertEquals("EMD", 0.0, CompareMain.compareEMD(scenario, cdrVolumes, groundTruthVolumes), 0.0);
	}

    @Test
    public void testTwoWorkplaces() {
        Controler controler = new Controler(new TwoWorkplaces().run(utils.getOutputDirectory()));
        LinkIsZone linkIsZone = new LinkIsZone();
        controler.setModules(
                new EventsManagerModule(),
                new TravelDistanceStatsModule(),
                new CharyparNagelScoringFunctionModule(),
                new TripRouterModule(),
                new TravelTimeCalculatorModule(),
                new TravelDisutilityModule(),
                new VolumesAnalyzerModule(),
                new CollectSightingsModule(),
                new StrategyManagerModule(),
                new CallBehaviorModule(new AtStartOrEnd(), linkIsZone),
                new AbstractModule() {
                    @Override
                    public void install() {
                        install(new QSimModule());
                    }
                });
        controler.run();
        Scenario scenario = controler.getScenario();
        VolumesAnalyzer groundTruthVolumes = controler.getVolumes();
        VolumesAnalyzer cdrVolumes = runWithCadyts(scenario, linkIsZone, (Sightings) scenario.getScenarioElement("sightings"), groundTruthVolumes);
        Assert.assertEquals("All-day squares", 0.0, CompareMain.compareAllDay(scenario, cdrVolumes, groundTruthVolumes), 0.0);
        Assert.assertEquals("Timebin squares", 0.0, CompareMain.compareTimebins(scenario, cdrVolumes, groundTruthVolumes), 0.0);
        Assert.assertEquals("EMD", 0.0, CompareMain.compareEMD(scenario, cdrVolumes, groundTruthVolumes), 0.0);
    }

    private VolumesAnalyzer runWithCadyts(Scenario scenario, ZoneTracker.LinkToZoneResolver linkToZoneResolver, Sightings sightings, VolumesAnalyzer groundTruthVolumes) {
        Counts counts = CompareMain.volumesToCounts(scenario.getNetwork(), groundTruthVolumes, 1.0);
        VolumesAnalyzer cdrVolumes = CompareMain.runWithTwoPlansAndCadyts(utils.getOutputDirectory() + "/output2", scenario.getNetwork(), linkToZoneResolver, sightings, counts);
        System.out.printf("%f\t%f\t%f\n", CompareMain.compareAllDay(scenario, cdrVolumes, groundTruthVolumes), CompareMain.compareTimebins(scenario, cdrVolumes, groundTruthVolumes), CompareMain.compareEMDMassPerLink(scenario, cdrVolumes, groundTruthVolumes));
        return cdrVolumes;
    }

}
