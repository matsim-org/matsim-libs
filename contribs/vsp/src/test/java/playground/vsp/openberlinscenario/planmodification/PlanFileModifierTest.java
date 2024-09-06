package playground.vsp.openberlinscenario.planmodification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author gthunig on 08.05.2018
 */
public class PlanFileModifierTest {
    private final static Logger LOG = LogManager.getLogger(PlanFileModifierTest.class);

    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

    private final static double SELECTION_PROBABILITY = 0.70;
    private final CoordinateTransformation ct = new IdentityTransformation();

    private Population originalPopulationCase;
    private Population modifiedPopulationCase1;
    private Population modifiedPopulationCase2;

    @BeforeEach
    public void initializeTestPopulations() {

        String formatedInputPlansFile = utils.getClassInputDirectory() + "testPlansFormated.xml";
        String outputplansfile1 = utils.getOutputDirectory() + "testPlansModified.xml";
        String outputPlansFile2 = utils.getOutputDirectory() + "testPlansModified2.xml";

        PlanFileModifier planFileModifier = new PlanFileModifier(
                formatedInputPlansFile, outputplansfile1,
                SELECTION_PROBABILITY, false, false,
                true, false,
                10000/* not tested*/, false, ct);
        planFileModifier.modifyPlans();

        originalPopulationCase = readPopulationFromFile(formatedInputPlansFile);
        modifiedPopulationCase1 = readPopulationFromFile(outputplansfile1);

        PlanFileModifier planFileModifier2 = new PlanFileModifier(
                formatedInputPlansFile, outputPlansFile2,
                SELECTION_PROBABILITY, true, true,
                false, true, 10000,
                true, ct);
        planFileModifier2.modifyPlans();

        modifiedPopulationCase2 = readPopulationFromFile(outputPlansFile2);
    }

	@Test
	void testSelectionProbability() {

        LOG.info("OriginalPopulationCase1 size: " + originalPopulationCase.getPersons().size());
        LOG.info("ModifiedPopulationCase1 size: " + modifiedPopulationCase1.getPersons().size());
        LOG.info("selection probability: " + SELECTION_PROBABILITY);
        LOG.info("real selection probability: " +
                ((double)modifiedPopulationCase1.getPersons().size()/ originalPopulationCase.getPersons().size()));
        Assertions.assertEquals(11, modifiedPopulationCase1.getPersons().size(),
                MatsimTestUtils.EPSILON,
                "Selection probability was not correctly applied");
    }

	@Test
	void testEveryPersonCopiedExistsInOriginal() {

        for (Person copy : modifiedPopulationCase1.getPersons().values()) {
            Person original = originalPopulationCase.getPersons().get(copy.getId());
            Assertions.assertTrue(original != null,
                    "Person " + copy.getId() + " does not exist in the original file");
        }
    }

	@Test
	void testOnlyTransferSelectedPlan() {

        for (Person copy : modifiedPopulationCase2.getPersons().values()) {
            Person original = originalPopulationCase.getPersons().get(copy.getId());
            Assertions.assertEquals(1, copy.getPlans().size(), "More than 1 plan");
            comparePlansWithoutRoutes(original.getSelectedPlan(), copy.getSelectedPlan());
        }
    }

	@Test
	void testNotOnlyTransferSelectedPlan() {
        //also tests if all plans were copied correctly

        for (Person copy : modifiedPopulationCase1.getPersons().values()) {
            Person original = originalPopulationCase.getPersons().get(copy.getId());
            Assertions.assertEquals(original.getPlans().size(), copy.getPlans().size(), "Not the same amount of plans");
            comparePlans(original.getSelectedPlan(), copy.getSelectedPlan());
            for (int i = 0; i < original.getPlans().size(); i++) {
                Plan originalPlan = original.getPlans().get(i);
                Plan modifiedPlan = copy.getPlans().get(i);
                comparePlans(originalPlan, modifiedPlan);
            }
        }
    }

	@Test
	void testConsiderHomeStayingAgents() {

        boolean atLeastOneHomeStayingPerson = false;
        for (Person copy : modifiedPopulationCase2.getPersons().values()) {
            if (copy.getSelectedPlan().getPlanElements().size() == 1)
                atLeastOneHomeStayingPerson = true;
        }
        Assertions.assertTrue(atLeastOneHomeStayingPerson, "No home staying person found");
    }

	@Test
	void testNotConsiderHomeStayingAgents() {

        for (Person copy : modifiedPopulationCase1.getPersons().values()) {
            Assertions.assertTrue(copy.getSelectedPlan().getPlanElements().size() > 1,
                    "No home staying agents allowed");
        }
    }

	@Test
	void testIncludeStayHomePlans() {

        boolean atLeastOneHomeStayingPlan = false;
        for (Person copy : modifiedPopulationCase1.getPersons().values()) {
            for (Plan plan : copy.getPlans())
                if (plan.getPlanElements().size() <= 1)
                    atLeastOneHomeStayingPlan = true;
        }
        Assertions.assertTrue(atLeastOneHomeStayingPlan, "No home staying plan found");
    }

	@Test
	void testNotIncludeStayHomePlans() {

        for (Person copy : modifiedPopulationCase2.getPersons().values()) {
            for (Plan plan : copy.getPlans())
                if (!plan.equals(copy.getSelectedPlan()))
                Assertions.assertTrue(plan.getPlanElements().size() > 1,
                        "No home staying plans allowed");
        }
    }

	@Test
	void testOnlyConsiderPeopleAlwaysGoingByCar() {

        for (Person copy : modifiedPopulationCase2.getPersons().values()) {
            for (Plan plan : copy.getPlans())
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Leg) {
                        Assertions.assertTrue((((Leg) planElement).getMode().equals(TransportMode.car)),
                                "No other mode than car allowed");
                    }
                }
        }
    }

	@Test
	void testNotOnlyConsiderPeopleAlwaysGoingByCar() {

        boolean otherModeThanCarConsidered = false;
        for (Person copy : modifiedPopulationCase1.getPersons().values()) {
            for (Plan plan : copy.getPlans())
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Leg && !(((Leg) planElement).getMode().equals(TransportMode.car))) {
                        otherModeThanCarConsidered = true;
                    }
                }
        }
        Assertions.assertTrue(otherModeThanCarConsidered, "There should be other modes than car");
    }

	@Test
	void testRemoveLinksAndRoutes() {

        for (Person copy : modifiedPopulationCase2.getPersons().values()) {
            for (Plan plan : copy.getPlans())
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Leg) {
                        Assertions.assertTrue((((Leg) planElement).getRoute() == null),
                                "There should not be a route left");
                    }
                }
        }
    }

	@Test
	void testNotRemoveLinksAndRoutes() {

        boolean routeFound = false;
        for (Person copy : modifiedPopulationCase1.getPersons().values()) {
            for (Plan plan : copy.getPlans())
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Leg && (((Leg) planElement).getRoute() != null)) {
                        routeFound = true;
                    }
                }
        }
        Assertions.assertTrue(routeFound, "There should be at minimum one route left");
    }

    private void comparePlans(Plan original, Plan copy) {

        Assertions.assertEquals(original.toString(), copy.toString(), "Plans are not the same");
        for (int i = 0; i < original.getPlanElements().size(); i++) {
            Assertions.assertEquals(original.getPlanElements().get(i).toString(),
                    copy.getPlanElements().get(i).toString(),
                    "PlanElements are not the same");
        }
    }

    private void comparePlansWithoutRoutes(Plan original, Plan copy) {

        Assertions.assertEquals(original.toString(), copy.toString(), "Plans are not the same");
    }

    private static Population readPopulationFromFile(String populationFile) {

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(populationFile);
        return scenario.getPopulation();
    }
}
