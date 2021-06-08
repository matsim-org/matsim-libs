package org.matsim.analysis;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.CompressionType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Map;

/**
 *
 * Tests ModeChoiceCoverageControlerListener
 *
 * @author jakobrehmann
 * adapted from Aravind "ModeStatsControlerListenerTest"
 *
 */
public class ModeChoiceCoverageControlerListenerTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();


    @Test
    public void testChangePlanModes() {

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        PlanCalcScoreConfigGroup scoreConfig = new PlanCalcScoreConfigGroup();
        TransportPlanningMainModeIdentifier transportId = new TransportPlanningMainModeIdentifier();

        ControlerConfigGroup controlerConfigGroup = new ControlerConfigGroup();
        OutputDirectoryHierarchy controlerIO = new OutputDirectoryHierarchy(utils.getOutputDirectory() + "/ModeChoiceCoverageControlerListener",
                OverwriteFileSetting.overwriteExistingFiles, CompressionType.gzip);

        Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
        population.addPerson(person);

        ModeChoiceCoverageControlerListener modeCC = new ModeChoiceCoverageControlerListener(controlerConfigGroup, population, controlerIO, scoreConfig, transportId);
        modeCC.notifyStartup(new StartupEvent(null));

        // Iteration 0: walk - walk
        Plan plan1 = makePlan(person, TransportMode.walk, TransportMode.walk);
        person.addPlan(plan1);
        person.setSelectedPlan(plan1);

        modeCC.notifyIterationEnds(new IterationEndsEvent(null, 0,false));
        Map<Integer, Map<String, Map<Integer, Double>>> modeChoiceCoverageHistory = modeCC.getModeChoiceCoverageHistory();
        Assert.assertEquals( "There should only be one mode in the mode modeCCHistory",
                1, modeChoiceCoverageHistory.get(1).keySet().size());
        Assert.assertEquals( "Since all trips were completed with walk, mcc for walk should be 1.0 (or 100%)",
                (Double) 1.0, modeChoiceCoverageHistory.get(1).get(TransportMode.walk).get(0));

        // Iteration 1: walk - bike
        Plan plan2 = makePlan(person, TransportMode.walk, TransportMode.bike);
        person.addPlan(plan2);
        person.setSelectedPlan(plan2);

        modeCC.notifyIterationEnds(new IterationEndsEvent(null, 1,false));
        Assert.assertEquals( "There should now be two modes in modeCCHistory",
                2, modeChoiceCoverageHistory.get(1).keySet().size());
        Assert.assertEquals( "Since all trips were completed with walk (in previous iterations), mcc for walk should be 1.0 (or 100%)",
                (Double) 1.0, modeChoiceCoverageHistory.get(1).get(TransportMode.walk).get(1));
        Assert.assertEquals( "The mcc for bike from the 0th iteration should be 0.0, since bike only showed up in this iteration",
                (Double) 0.0, modeChoiceCoverageHistory.get(1).get(TransportMode.bike).get(0));
        Assert.assertEquals( "Since 1 of 2 trips were completed with bike, mcc for bike should be 0.5 (or 50%)",
                (Double) 0.5, modeChoiceCoverageHistory.get(1).get(TransportMode.bike).get(1));

        // Iteration 3: bike - walk
        Plan plan3 = makePlan(person, TransportMode.bike, TransportMode.walk);
        person.addPlan(plan3);
        person.setSelectedPlan(plan3);

        modeCC.notifyIterationEnds(new IterationEndsEvent(null, 2,false));
        Assert.assertEquals( "There should still be two modes in modeCCHistory",
                2, modeChoiceCoverageHistory.get(1).keySet().size());
        Assert.assertEquals( "Since all trips were completed with walk (in previous iterations), mcc for walk should be 1.0 (or 100%)",
                (Double) 1.0, modeChoiceCoverageHistory.get(1).get(TransportMode.walk).get(2));
        Assert.assertEquals( "Since all trips were completed with bike (in previous iterations), mcc for bike should be 1.0 (or 100%)",
                (Double) 1.0, modeChoiceCoverageHistory.get(1).get(TransportMode.bike).get(2));
    }

    @Test
    public void testTwoAgents() {
        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        PlanCalcScoreConfigGroup scoreConfig = new PlanCalcScoreConfigGroup();
        TransportPlanningMainModeIdentifier transportId = new TransportPlanningMainModeIdentifier();

        ControlerConfigGroup controlerConfigGroup = new ControlerConfigGroup();
        OutputDirectoryHierarchy controlerIO = new OutputDirectoryHierarchy(utils.getOutputDirectory() + "/ModeChoiceCoverageControlerListener",
                OverwriteFileSetting.overwriteExistingFiles, CompressionType.gzip);

        Person person1 = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
        Person person2 = PopulationUtils.getFactory().createPerson(Id.create(2, Person.class));
        population.addPerson(person1);
        population.addPerson(person2);


        ModeChoiceCoverageControlerListener modeCC = new ModeChoiceCoverageControlerListener(controlerConfigGroup, population, controlerIO, scoreConfig, transportId);
        modeCC.notifyStartup(new StartupEvent(null));

        // Iteration 0: walk - walk
        Plan person1plan1 = makePlan(person1, TransportMode.walk, TransportMode.walk);
        person1.addPlan(person1plan1);
        person1.setSelectedPlan(person1plan1);

        Plan person2plan1 = makePlan(person2, TransportMode.walk, TransportMode.walk);
        person2.addPlan(person2plan1);
        person2.setSelectedPlan(person2plan1);

        modeCC.notifyIterationEnds(new IterationEndsEvent(null, 0,false));
        Map<Integer, Map<String, Map<Integer, Double>>> modeChoiceCoverageHistory = modeCC.getModeChoiceCoverageHistory();
        Assert.assertEquals( "There should only be one mode in the mode modeCCHistory",
                1, modeChoiceCoverageHistory.get(1).keySet().size());
        Assert.assertEquals( "Since all trips were completed with walk, mcc for walk should be 1.0 (or 100%)",
                (Double) 1.0, modeChoiceCoverageHistory.get(1).get(TransportMode.walk).get(0));

        // Iteration 1: p1: walk - bike, p2: walk - walk
        Plan person1plan2 = makePlan(person1, TransportMode.walk, TransportMode.bike);
        person1.addPlan(person1plan2);
        person1.setSelectedPlan(person1plan2);

        Plan person2plan2 = makePlan(person2, TransportMode.walk, TransportMode.walk);
        person2.addPlan(person2plan2);
        person2.setSelectedPlan(person2plan2);

        modeCC.notifyIterationEnds(new IterationEndsEvent(null, 1,false));
        Assert.assertEquals( "There should now be two modes in modeCCHistory",
                2, modeChoiceCoverageHistory.get(1).keySet().size());
        Assert.assertEquals( "Since all trips were completed with walk (in previous iterations), mcc for walk should be 1.0 (or 100%)",
                (Double) 1.0, modeChoiceCoverageHistory.get(1).get(TransportMode.walk).get(1));
        Assert.assertEquals( "Since 1 of 4 trips were completed with bike, mcc for bike should be 0.25 (or 25%)",
                (Double) 0.25, modeChoiceCoverageHistory.get(1).get(TransportMode.bike).get(1));

        // Iteration 3: p1: walk - walk, p2: bike - bike
        Plan person1plan3 = makePlan(person1, TransportMode.walk, TransportMode.walk);
        person1.addPlan(person1plan3);
        person1.setSelectedPlan(person1plan3);

        Plan person2plan3 = makePlan(person1, TransportMode.bike, TransportMode.bike);
        person2.addPlan(person2plan3);
        person2.setSelectedPlan(person2plan3);

        modeCC.notifyIterationEnds(new IterationEndsEvent(null, 2,false));
        Assert.assertEquals( "There should still be two modes in modeCCHistory",
                2, modeChoiceCoverageHistory.get(1).keySet().size());
        Assert.assertEquals( "Since all trips were completed with walk (in previous iterations), mcc for walk should be 1.0 (or 100%)",
                (Double) 1.0, modeChoiceCoverageHistory.get(1).get(TransportMode.walk).get(2));
        Assert.assertEquals( "Since 3 of 4 trips were completed with bike (in previous iterations), mcc for bike should be 0.75 (or 75%)",
                (Double) 0.75, modeChoiceCoverageHistory.get(1).get(TransportMode.bike).get(2));
    }

    @Test
    public void testDifferentLevels() {

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        PlanCalcScoreConfigGroup scoreConfig = new PlanCalcScoreConfigGroup();
        TransportPlanningMainModeIdentifier transportId = new TransportPlanningMainModeIdentifier();

        ControlerConfigGroup controlerConfigGroup = new ControlerConfigGroup();
        OutputDirectoryHierarchy controlerIO = new OutputDirectoryHierarchy(utils.getOutputDirectory() + "/ModeChoiceCoverageControlerListener",
                OverwriteFileSetting.overwriteExistingFiles, CompressionType.gzip);

        Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
        population.addPerson(person);

        ModeChoiceCoverageControlerListener modeCC = new ModeChoiceCoverageControlerListener(controlerConfigGroup, population, controlerIO, scoreConfig, transportId);
        modeCC.notifyStartup(new StartupEvent(null));

        // After 1 iteration
        Plan plan1 = makePlan(person, TransportMode.walk, TransportMode.walk);
        person.addPlan(plan1);
        person.setSelectedPlan(plan1);

        modeCC.notifyIterationEnds(new IterationEndsEvent(null, 0,false));
        Map<Integer, Map<String, Map<Integer, Double>>> modeChoiceCoverageHistory = modeCC.getModeChoiceCoverageHistory();
        Assert.assertEquals("1x threshold should be met after 1 iteration",
                (Double) 1.0, modeChoiceCoverageHistory.get(1).get("walk").get(0));
        Assert.assertEquals("5x threshold should NOT be met after 1 iteration",
                (Double) 0.0, modeChoiceCoverageHistory.get(5).get("walk").get(0));
        Assert.assertEquals("10x threshold should NOT be met after 1 iteration",
                (Double) 0.0, modeChoiceCoverageHistory.get(10).get("walk").get(0));


        // After 5 iterations
        for (int i = 1; i < 5; i++) {
            modeCC.notifyIterationEnds(new IterationEndsEvent(null, i,false));
        }

        Assert.assertEquals("1x threshold should be met after 5 iterations",
                (Double) 1.0, modeChoiceCoverageHistory.get(1).get("walk").get(4));
        Assert.assertEquals("5x threshold should be met after 5 iterations",
                (Double) 1.0, modeChoiceCoverageHistory.get(5).get("walk").get(4));
        Assert.assertEquals("10x threshold should NOT be met after 5 iterations",
                (Double) 0.0, modeChoiceCoverageHistory.get(10).get("walk").get(4));

        // After 10 iterations
        for (int i = 5; i < 10; i++) {
            modeCC.notifyIterationEnds(new IterationEndsEvent(null, i,false));
        }

        Assert.assertEquals("1x threshold should be met after 10 iterations",
                (Double) 1.0, modeChoiceCoverageHistory.get(1).get("walk").get(9));
        Assert.assertEquals("5x threshold should be met after 10 iterations",
                (Double) 1.0, modeChoiceCoverageHistory.get(5).get("walk").get(9));
        Assert.assertEquals("10x threshold should be met after 10 iterations",
                (Double) 1.0, modeChoiceCoverageHistory.get(10).get("walk").get(9));

    }


    private Plan makePlan( Person person, String modeLeg1, String modeLeg2) {
        Plan plan = PopulationUtils.createPlan(person);

        final Id<Link> link1 = Id.create(10723, Link.class);
        final Id<Link> link2 = Id.create(123160, Link.class);

        Activity act1 = PopulationUtils.createActivityFromLinkId("home", link1);
        plan.addActivity(act1);

        Leg leg1 = PopulationUtils.createLeg(modeLeg1);
        plan.addLeg(leg1);

        Activity act2 = PopulationUtils.createActivityFromLinkId("work", link2);
        plan.addActivity(act2);

        Leg leg2 = PopulationUtils.createLeg(modeLeg2);
        plan.addLeg(leg2);

        Activity act3 = PopulationUtils.createActivityFromLinkId("home", link1);
        plan.addActivity(act3);
        return plan;
    }
}