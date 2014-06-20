package playground.mzilske.cdr;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.counts.Counts;
import org.matsim.testcases.MatsimTestUtils;
import playground.mzilske.controller.Controller;
import playground.mzilske.controller.ControllerModuleWithScenario;
import playground.mzilske.d4d.Sighting;

import java.util.List;
import java.util.Map;

public class CDREquilTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    private static class LinkIsZone implements ZoneTracker.LinkToZoneResolver {

        @Override
        public Id resolveLinkToZone(Id linkId) {
            return linkId;
        }

        public IdImpl chooseLinkInZone(String zoneId) {
            return new IdImpl(zoneId);
        }

    }

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
        public boolean makeACallAtMorningAndNight() {
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
        Map<Id, List<Sighting>> sightings = compareMain.getSightingsPerPerson();
        Counts counts = CompareMain.volumesToCounts(scenario.getNetwork(), compareMain.getGroundTruthVolumes());
        VolumesAnalyzer cdrVolumes = CompareMain.runWithTwoPlansAndCadyts(utils.getOutputDirectory() + "/output2", scenario.getNetwork(), linkToZoneResolver, sightings, counts);
        System.out.printf("%f\t%f\t%f\n", CompareMain.compareAllDay(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), CompareMain.compareTimebins(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()), CompareMain.compareEMDMassPerLink(scenario, cdrVolumes, compareMain.getGroundTruthVolumes()));
        return cdrVolumes;
    }

}
