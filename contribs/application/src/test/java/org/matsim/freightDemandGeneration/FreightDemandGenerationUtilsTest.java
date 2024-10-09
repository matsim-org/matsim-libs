package org.matsim.freightDemandGeneration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;

/**
 * @author Ricardo Ewert
 *
 */
public class FreightDemandGenerationUtilsTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testPreparePopulation() {
		String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";
		Population population = PopulationUtils.readPopulation(populationLocation);
		FreightDemandGenerationUtils.preparePopulation(population, 1.0, 1.0, "changeNumberOfLocationsWithDemand");
		Assertions.assertEquals(8, population.getPersons().size());
		Assertions.assertEquals(1.0,population.getAttributes().getAttribute("sampleSize"));
		Assertions.assertEquals(1.0,population.getAttributes().getAttribute("samplingTo"));
		Assertions.assertEquals("changeNumberOfLocationsWithDemand",population.getAttributes().getAttribute("samplingOption"));
		Person person = population.getPersons().get(Id.createPersonId("person1"));
		Assertions.assertEquals(1200.0,person.getAttributes().getAttribute("homeX"));
		Assertions.assertEquals(7700.0,person.getAttributes().getAttribute("homeY"));
		Assertions.assertEquals(0, person.getPlans().size());
		Assertions.assertNull(person.getSelectedPlan());
		person = population.getPersons().get(Id.createPersonId("person2"));
		Assertions.assertEquals(2900.0,person.getAttributes().getAttribute("homeX"));
		Assertions.assertEquals(2800.0,person.getAttributes().getAttribute("homeY"));
		Assertions.assertEquals(0, person.getPlans().size());
		Assertions.assertNull(person.getSelectedPlan());
		person = population.getPersons().get(Id.createPersonId("person3"));
		Assertions.assertEquals(4200.0,person.getAttributes().getAttribute("homeX"));
		Assertions.assertEquals(4400.0,person.getAttributes().getAttribute("homeY"));
		Assertions.assertEquals(0, person.getPlans().size());
		Assertions.assertNull(person.getSelectedPlan());
		person = population.getPersons().get(Id.createPersonId("person4"));
		Assertions.assertEquals(5200.0,person.getAttributes().getAttribute("homeX"));
		Assertions.assertEquals(2600.0,person.getAttributes().getAttribute("homeY"));
		Assertions.assertEquals(0, person.getPlans().size());
		Assertions.assertNull(person.getSelectedPlan());
		person = population.getPersons().get(Id.createPersonId("person5"));
		Assertions.assertEquals(5200.0,person.getAttributes().getAttribute("homeX"));
		Assertions.assertEquals(5500.0,person.getAttributes().getAttribute("homeY"));
		Assertions.assertEquals(0, person.getPlans().size());
		Assertions.assertNull(person.getSelectedPlan());
		person = population.getPersons().get(Id.createPersonId("person6"));
		Assertions.assertEquals(7900.0,person.getAttributes().getAttribute("homeX"));
		Assertions.assertEquals(7500.0,person.getAttributes().getAttribute("homeY"));
		Assertions.assertEquals(0, person.getPlans().size());
		Assertions.assertNull(person.getSelectedPlan());
		person = population.getPersons().get(Id.createPersonId("person7"));
		Assertions.assertEquals(4900.0,person.getAttributes().getAttribute("homeX"));
		Assertions.assertEquals(8900.0,person.getAttributes().getAttribute("homeY"));
		Assertions.assertEquals(0, person.getPlans().size());
		Assertions.assertNull(person.getSelectedPlan());
		person = population.getPersons().get(Id.createPersonId("person8"));
		Assertions.assertEquals(8400.0,person.getAttributes().getAttribute("homeX"));
		Assertions.assertEquals(5200.0,person.getAttributes().getAttribute("homeY"));
		Assertions.assertEquals(0, person.getPlans().size());
		Assertions.assertNull(person.getSelectedPlan());
	}

	@Test
	void testCoordOfMiddlePointOfLink() {
		Network network = NetworkUtils.readNetwork("https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Link link = network.getLinks().get(Id.createLinkId("i(8,8)"));
		Coord middlePoint = FreightDemandGenerationUtils.getCoordOfMiddlePointOfLink(link);
		Assertions.assertEquals(7500, middlePoint.getX(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(8000, middlePoint.getY(), MatsimTestUtils.EPSILON);
		Link link2 = network.getLinks().get(Id.createLinkId("j(5,8)"));
		Coord middlePoint2 = FreightDemandGenerationUtils.getCoordOfMiddlePointOfLink(link2);
		Assertions.assertEquals(5000, middlePoint2.getX(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(7500, middlePoint2.getY(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testReducePopulationToShapeArea() {
		String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";
		Population population = PopulationUtils.readPopulation(populationLocation);
		FreightDemandGenerationUtils.preparePopulation(population, 1.0, 1.0, "changeNumberOfLocationsWithDemand");
		Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
		ShpOptions shp = new ShpOptions(shapeFilePath,"WGS84", null);
		ShpOptions.Index index = shp.createIndex("WGS84", "_");
		FreightDemandGenerationUtils.reducePopulationToShapeArea(population, index);
		Assertions.assertEquals(6, population.getPersons().size());
		Assertions.assertFalse(population.getPersons().containsKey(Id.createPersonId("person2")));
		Assertions.assertFalse(population.getPersons().containsKey(Id.createPersonId("person4")));
	}

	@Test
	void testCheckPositionInShape_link() {
		Network network = NetworkUtils.readNetwork("https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Link link = network.getLinks().get(Id.createLinkId("i(8,8)"));
		Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
		ShpOptions shp = new ShpOptions(shapeFilePath,"WGS84", null);
		ShpOptions.Index indexShape = shp.createIndex("Ortsteil");
		Assertions.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape, null, null));
		Assertions.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape, new String[]{"area1"}, null));
		Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape, new String[]{"area2"}, null));
		link = network.getLinks().get(Id.createLinkId("i(6,3)R"));
		Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape, null, null));
		Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape, new String[]{"area1"}, null));
		Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, indexShape, new String[]{"area2"}, null));

	}

	@Test
	void testCheckPositionInShape_point() {
		Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
		ShpOptions shp = new ShpOptions(shapeFilePath,"WGS84", null);
		ShpOptions.Index indexShape = shp.createIndex("Ortsteil");
		Coord coord = new Coord(6000, 6000);
		Assertions.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(null, coord, indexShape, null, null));
		Assertions.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(null, coord, indexShape, new String[]{"area1"}, null));
		Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(null, coord, indexShape, new String[]{"area2"}, null));
		coord = new Coord(2000, 2000);
		Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(null, coord, indexShape, null, null));
		Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(null, coord, indexShape, new String[]{"area1"}, null));
		Assertions.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(null, coord, indexShape, new String[]{"area2"}, null));

	}
}
