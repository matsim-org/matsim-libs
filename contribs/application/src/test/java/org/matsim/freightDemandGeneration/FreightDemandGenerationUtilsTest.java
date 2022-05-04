package org.matsim.freightDemandGeneration;

import java.nio.file.Path;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.testcases.MatsimTestUtils;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author Ricardo Ewert
 *
 */
public class FreightDemandGenerationUtilsTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testPreparePopulation() {
		String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";
		Population population = PopulationUtils.readPopulation(populationLocation);
		FreightDemandGenerationUtils.preparePopulation(population, 1.0, 1.0, "changeNumberOfLocationsWithDemand");
		Assert.assertEquals(8, population.getPersons().size());
		Assert.assertEquals(1.0,population.getAttributes().getAttribute("sampleSize"));
		Assert.assertEquals(1.0,population.getAttributes().getAttribute("samplingTo"));
		Assert.assertEquals("changeNumberOfLocationsWithDemand",population.getAttributes().getAttribute("samplingOption"));
		Person person = population.getPersons().get(Id.createPersonId("person1"));
		Assert.assertEquals(1200.0,person.getAttributes().getAttribute("homeX"));
		Assert.assertEquals(7700.0,person.getAttributes().getAttribute("homeY"));
		Assert.assertEquals(0, person.getPlans().size());
		Assert.assertNull(person.getSelectedPlan());
		person = population.getPersons().get(Id.createPersonId("person2"));
		Assert.assertEquals(2900.0,person.getAttributes().getAttribute("homeX"));
		Assert.assertEquals(2800.0,person.getAttributes().getAttribute("homeY"));
		Assert.assertEquals(0, person.getPlans().size());
		Assert.assertNull(person.getSelectedPlan());
		person = population.getPersons().get(Id.createPersonId("person3"));
		Assert.assertEquals(4200.0,person.getAttributes().getAttribute("homeX"));
		Assert.assertEquals(4400.0,person.getAttributes().getAttribute("homeY"));
		Assert.assertEquals(0, person.getPlans().size());
		Assert.assertNull(person.getSelectedPlan());
		person = population.getPersons().get(Id.createPersonId("person4"));
		Assert.assertEquals(5200.0,person.getAttributes().getAttribute("homeX"));
		Assert.assertEquals(2600.0,person.getAttributes().getAttribute("homeY"));
		Assert.assertEquals(0, person.getPlans().size());
		Assert.assertNull(person.getSelectedPlan());
		person = population.getPersons().get(Id.createPersonId("person5"));
		Assert.assertEquals(5200.0,person.getAttributes().getAttribute("homeX"));
		Assert.assertEquals(5500.0,person.getAttributes().getAttribute("homeY"));
		Assert.assertEquals(0, person.getPlans().size());
		Assert.assertNull(person.getSelectedPlan());
		person = population.getPersons().get(Id.createPersonId("person6"));
		Assert.assertEquals(7900.0,person.getAttributes().getAttribute("homeX"));
		Assert.assertEquals(7500.0,person.getAttributes().getAttribute("homeY"));
		Assert.assertEquals(0, person.getPlans().size());
		Assert.assertNull(person.getSelectedPlan());
		person = population.getPersons().get(Id.createPersonId("person7"));
		Assert.assertEquals(4900.0,person.getAttributes().getAttribute("homeX"));
		Assert.assertEquals(8900.0,person.getAttributes().getAttribute("homeY"));
		Assert.assertEquals(0, person.getPlans().size());
		Assert.assertNull(person.getSelectedPlan());
		person = population.getPersons().get(Id.createPersonId("person8"));
		Assert.assertEquals(8400.0,person.getAttributes().getAttribute("homeX"));
		Assert.assertEquals(5200.0,person.getAttributes().getAttribute("homeY"));
		Assert.assertEquals(0, person.getPlans().size());
		Assert.assertNull(person.getSelectedPlan());
	}
	@Test
	public void testCoordOfMiddlePointOfLink() {
		Network network = NetworkUtils.readNetwork("https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Link link = network.getLinks().get(Id.createLinkId("i(8,8)"));
		Coord middlePoint = FreightDemandGenerationUtils.getCoordOfMiddlePointOfLink(link);
		Assert.assertEquals(7500, middlePoint.getX(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(8000, middlePoint.getY(), MatsimTestUtils.EPSILON);
		Link link2 = network.getLinks().get(Id.createLinkId("j(5,8)"));
		Coord middlePoint2 = FreightDemandGenerationUtils.getCoordOfMiddlePointOfLink(link2);
		Assert.assertEquals(5000, middlePoint2.getX(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(7500, middlePoint2.getY(), MatsimTestUtils.EPSILON);
	}
	@Test
	public void testReducePopulationToShapeArea() {
		String populationLocation = utils.getPackageInputDirectory() + "testPopulation.xml";
		Population population = PopulationUtils.readPopulation(populationLocation);
		FreightDemandGenerationUtils.preparePopulation(population, 1.0, 1.0, "changeNumberOfLocationsWithDemand");
		Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
		ShpOptions shp = new ShpOptions(shapeFilePath,"WGS84", null);
		ShpOptions.Index index = shp.createIndex("WGS84", "_");
		FreightDemandGenerationUtils.reducePopulationToShapeArea(population, index);
		Assert.assertEquals(6, population.getPersons().size());
		Assert.assertFalse(population.getPersons().containsKey(Id.createPersonId("person2")));
		Assert.assertFalse(population.getPersons().containsKey(Id.createPersonId("person4")));
	}
	@Test
	public void testCheckPositionInShape_link() {
		Network network = NetworkUtils.readNetwork("https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml");
		Link link = network.getLinks().get(Id.createLinkId("i(8,8)"));
		Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
		ShpOptions shp = new ShpOptions(shapeFilePath,"WGS84", null);
		Collection<SimpleFeature> polygonsInShape = shp.readFeatures();
		Assert.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape, null, null));
		Assert.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape, new String[]{"area1"}, null));
		Assert.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape, new String[]{"area2"}, null));
		link = network.getLinks().get(Id.createLinkId("i(6,3)R"));
		Assert.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape, null, null));
		Assert.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape, new String[]{"area1"}, null));
		Assert.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(link, null, polygonsInShape, new String[]{"area2"}, null));

	}
	@Test
	public void testCheckPositionInShape_point() {
		Path shapeFilePath = Path.of(utils.getPackageInputDirectory() + "testShape/testShape.shp");
		ShpOptions shp = new ShpOptions(shapeFilePath,"WGS84", null);
		Collection<SimpleFeature> polygonsInShape = shp.readFeatures();
		Point point = MGC.xy2Point(6000, 6000);
		Assert.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(null, point, polygonsInShape, null, null));
		Assert.assertTrue(FreightDemandGenerationUtils.checkPositionInShape(null, point, polygonsInShape, new String[]{"area1"}, null));
		Assert.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(null, point, polygonsInShape, new String[]{"area2"}, null));
		point = MGC.xy2Point(2000, 2000);
		Assert.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(null, point, polygonsInShape, null, null));
		Assert.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(null, point, polygonsInShape, new String[]{"area1"}, null));
		Assert.assertFalse(FreightDemandGenerationUtils.checkPositionInShape(null, point, polygonsInShape, new String[]{"area2"}, null));

	}
}
