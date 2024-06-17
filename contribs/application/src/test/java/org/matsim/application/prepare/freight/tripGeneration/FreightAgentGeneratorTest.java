package org.matsim.application.prepare.freight.tripGeneration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.application.options.LanduseOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

class FreightAgentGeneratorTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testGenerateRoadFreightAgentsWithTripRelation() {
		String regionShpString = utils.getPackageInputDirectory() + "testRegion.shp";
		String networkXmlString = utils.getPackageInputDirectory() + "testNetwork.xml";
		String kettenString = "file:" + utils.getPackageInputDirectory() + "testKetten.csv";
		String lookupTableString = "file:" + utils.getPackageInputDirectory() + "testLookup.csv";

		/* To Test:
		1. Check if TripRelation with postRunMode != 2 (road) returns an empty list
		2. Check if numOfTripsCalc/locCal/depTimeCalc has been used properly
		3. Check if Source->Destination is correct
		4. Check if legs are "freight"
		5. Check if activities are "freight_start" and "freight_end"
		6. Check if VL/HL/NL is separated properly
		 */

		FreightAgentGenerator freightAgentGenerator;
		List<TripRelation> tripRelations;

		try{
			Network network = NetworkUtils.readNetwork(networkXmlString);
			freightAgentGenerator = new FreightAgentGenerator(network, Path.of(regionShpString), lookupTableString, new LanduseOptions(), 35, 250, 1);
			tripRelations = TripRelation.readTripRelations(kettenString);
		} catch (Exception e){
			Assertions.fail(e); // If this Assertion fails, there is an external problem in another class
			return;
		}

		// Start with second entry of example Ketten table (runMode = 0)
		// Expected behavior: Returns en empty ArrayList
		List<Person> testListRow1 = freightAgentGenerator.generateRoadFreightAgents(tripRelations.getFirst(), "test2");
		Assertions.assertTrue(testListRow1.isEmpty());

		// Generate a Person list for the first entry of the example Ketten table
		// Expected behavior: Returns a list with 12 Persons
		List<Person> testListRow2 = freightAgentGenerator.generateRoadFreightAgents(tripRelations.get(1), "test1");
		Assertions.assertEquals(12, testListRow2.size());

		// Check if the Persons have correct plans
		List<String> referenceLinksCell1 = Arrays.asList("cd", "de"); // All links-names in Cell 1
		List<String> referenceLinksCell5 = Arrays.asList("ab", "bc"); // All links-names in Cell 5
		for(Person p : testListRow2){
			// Check if source link lies in correct test-Zone (5) and has activity "freight_start" and a correct departure time
			Assertions.assertInstanceOf(Activity.class, p.getPlans().getFirst().getPlanElements().getFirst());
			Activity startActivity = (Activity) p.getPlans().getFirst().getPlanElements().getFirst();
			Assertions.assertEquals("freight_start", startActivity.getType());
			Assertions.assertEquals(43200, startActivity.getEndTime().seconds(), 43200);

			// Check if leg is using "freight"-mode
			Assertions.assertInstanceOf(Leg.class, p.getPlans().getFirst().getPlanElements().get(1));
			Leg leg = (Leg) p.getPlans().getFirst().getPlanElements().get(1);
			Assertions.assertEquals("freight", leg.getMode());

			// Check if destination link lies in correct test-Zone (1) and has activity "freight_end"
			Assertions.assertInstanceOf(Activity.class, p.getPlans().getFirst().getPlanElements().get(2));
			Activity endActivity = (Activity) p.getPlans().getFirst().getPlanElements().get(2);
			Assertions.assertEquals("freight_end", endActivity.getType());

			if(p.getId().toString().contains("pre")){ //Check pre-run
				// Origin -> Origin
				Assertions.assertTrue(referenceLinksCell5.contains(startActivity.getLinkId().toString()));
				Assertions.assertTrue(referenceLinksCell5.contains(endActivity.getLinkId().toString()));
				Assertions.assertEquals("pre-run", p.getAttributes().getAttribute("trip_type"));
			}else if(p.getId().toString().contains("main")){ //Check main-run
				// Origin -> Destination
				Assertions.assertTrue(referenceLinksCell5.contains(startActivity.getLinkId().toString()));
				Assertions.assertTrue(referenceLinksCell1.contains(endActivity.getLinkId().toString()));
				Assertions.assertEquals("main-run", p.getAttributes().getAttribute("trip_type"));
			}else if(p.getId().toString().contains("post")){ //Check post-run
				// Destination -> Destination
				Assertions.assertTrue(referenceLinksCell1.contains(startActivity.getLinkId().toString()));
				Assertions.assertTrue(referenceLinksCell1.contains(endActivity.getLinkId().toString()));
				Assertions.assertEquals("post-run", p.getAttributes().getAttribute("trip_type"));
			}
		}
	}

	@Test
	void testGenerateRoadFreightAgentsWithPerson() {
		String regionShpString = utils.getPackageInputDirectory() + "testRegion.shp";
		String networkXmlString = utils.getPackageInputDirectory() + "testNetwork.xml";
		String lookupTableString = "file:" + utils.getPackageInputDirectory() + "testLookup.csv";

		// Implicitly also tests the FreightAgentGenerator.createFreightAgent() method

		Network network;
		FreightAgentGenerator freightAgentGenerator;
		PopulationFactory populationFactory;
		Person testPerson1;
		Person testPerson2;
		try{
			network = NetworkUtils.readNetwork(networkXmlString);
			freightAgentGenerator = new FreightAgentGenerator(network, Path.of(regionShpString), lookupTableString, new LanduseOptions(), 35, 250, 1);
			populationFactory = PopulationUtils.getFactory();

			//Create non-car transportation agent
			testPerson1 = populationFactory.createPerson(Id.createPersonId("test"));
			testPerson1.getAttributes().putAttribute("mode_pre-run", "unallocated");
			testPerson1.getAttributes().putAttribute("mode_main-run", "unallocated");
			testPerson1.getAttributes().putAttribute("mode_post-run", "unallocated");

			//Create car transportation agent
			testPerson2 = populationFactory.createPerson(Id.createPersonId("test"));
			testPerson2.getAttributes().putAttribute("mode_pre-run", "road");
			testPerson2.getAttributes().putAttribute("tons_per_year_pre-run", "25000");
			testPerson2.getAttributes().putAttribute("goods_type_pre-run", 10);

			testPerson2.getAttributes().putAttribute("mode_main-run", "road");
			testPerson2.getAttributes().putAttribute("tons_per_year_main-run", "35000");
			testPerson2.getAttributes().putAttribute("goods_type_main-run", 10);

			testPerson2.getAttributes().putAttribute("mode_post-run", "road");
			testPerson2.getAttributes().putAttribute("tons_per_year_post-run", "45000");
			testPerson2.getAttributes().putAttribute("goods_type_post-run", 10);


			testPerson2.getAttributes().putAttribute("origin_cell", "5");
			testPerson2.getAttributes().putAttribute("origin_cell_main_run", "5");
			//testPerson2.getAttributes().putAttribute("origin_terminal", "1001");

			testPerson2.getAttributes().putAttribute("destination_cell", "1");
			testPerson2.getAttributes().putAttribute("destination_cell_main_run", "1");
			//testPerson2.getAttributes().putAttribute("destination_terminal", "1001");


		} catch (Exception e){
			Assertions.fail(e); // If this Assertion fails, there is an external problem in another class
			return;
		}

		// Generate a person list from test-person 1
		// Expected behavior: Returns an empty list
		List<Person> testList1 = freightAgentGenerator.generateRoadFreightAgents(testPerson1, "test");
		Assertions.assertTrue(testList1.isEmpty());

		// Generate a person list from test-person 2
		// Expected behavior: Returns a list with 17 entries
		// 4 pre-run entries
		// 6 main-run entries
		// 7 post-run entries
		List<Person> testList2 = freightAgentGenerator.generateRoadFreightAgents(testPerson2, "test");
		Assertions.assertEquals(17, testList2.size());

		// Check if the Persons have correct plans
		List<String> referenceLinksCell1 = Arrays.asList("cd", "de"); // All links-names in Cell 1
		List<String> referenceLinksCell5 = Arrays.asList("ab", "bc"); // All links-names in Cell 5
		for(Person p : testList2){
			Assertions.assertInstanceOf(Activity.class, p.getSelectedPlan().getPlanElements().getFirst());
			Activity startActivity = (Activity) p.getSelectedPlan().getPlanElements().getFirst();
			Assertions.assertEquals("freight_start", startActivity.getType());
			Assertions.assertEquals(43200, startActivity.getEndTime().seconds(), 43200);

			// Check if leg is using "freight"-mode
			Assertions.assertInstanceOf(Leg.class, p.getPlans().getFirst().getPlanElements().get(1));
			Leg leg = (Leg) p.getPlans().getFirst().getPlanElements().get(1);
			Assertions.assertEquals("freight", leg.getMode());

			// Check if destination link lies in correct test-Zone (1) and has activity "freight_end"
			Assertions.assertInstanceOf(Activity.class, p.getPlans().getFirst().getPlanElements().get(2));
			Activity endActivity = (Activity) p.getPlans().getFirst().getPlanElements().get(2);
			Assertions.assertEquals("freight_end", endActivity.getType());

			if(p.getId().toString().contains("pre")) { // pre-run
				// Origin -> Origin
				Assertions.assertTrue(referenceLinksCell5.contains(startActivity.getLinkId().toString()));
				Assertions.assertTrue(referenceLinksCell5.contains(endActivity.getLinkId().toString()));
				Assertions.assertEquals("pre-run", p.getAttributes().getAttribute("trip_type"));
			}else if(p.getId().toString().contains("main")) { // main-run
				// Origin -> Destination
				Assertions.assertTrue(referenceLinksCell5.contains(startActivity.getLinkId().toString()));
				Assertions.assertTrue(referenceLinksCell1.contains(endActivity.getLinkId().toString()));
				Assertions.assertEquals("main-run", p.getAttributes().getAttribute("trip_type"));
			}else if(p.getId().toString().contains("post")) { // post-run
				// Destination -> Destination
				Assertions.assertTrue(referenceLinksCell1.contains(startActivity.getLinkId().toString()));
				Assertions.assertTrue(referenceLinksCell1.contains(endActivity.getLinkId().toString()));
				Assertions.assertEquals("post-run", p.getAttributes().getAttribute("trip_type"));
			}
		}
	}
}
