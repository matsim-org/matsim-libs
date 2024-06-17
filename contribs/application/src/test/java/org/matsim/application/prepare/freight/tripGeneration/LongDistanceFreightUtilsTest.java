package org.matsim.application.prepare.freight.tripGeneration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;

class LongDistanceFreightUtilsTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testFindTransportType() {
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(10));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(21));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(22));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(23));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(24));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(25));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(26));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(27));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(28));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(29));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(30));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(31));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(71));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(72));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(32));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(33));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(80));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(90));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(100));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(110));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(120));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(160));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(170));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(190));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.FTL, LongDistanceFreightUtils.findTransportType(200));

		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.LTL, LongDistanceFreightUtils.findTransportType(40));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.LTL, LongDistanceFreightUtils.findTransportType(50));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.LTL, LongDistanceFreightUtils.findTransportType(60));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.LTL, LongDistanceFreightUtils.findTransportType(130));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.LTL, LongDistanceFreightUtils.findTransportType(140));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.LTL, LongDistanceFreightUtils.findTransportType(150));
		Assertions.assertEquals(LongDistanceFreightUtils.TransportType.LTL, LongDistanceFreightUtils.findTransportType(180));
	}

	@Test
	void testWriteCommonAttributesV1() {
		String kettenString = "file:" + utils.getPackageInputDirectory() + "testKetten.csv";

		List<TripRelation> tripRelations;
		PopulationFactory populationFactory;
		Person testPerson;

		try{
			tripRelations = TripRelation.readTripRelations(kettenString);
			populationFactory = PopulationUtils.getFactory();
			testPerson = populationFactory.createPerson(Id.createPersonId("test"));
		} catch (Exception e){
			Assertions.fail(e); // If this Assertion fails, there is an external problem in another class
			return;
		}

		LongDistanceFreightUtils.writeCommonAttributesV1(testPerson, tripRelations.getFirst(), "a");

		//Test the attribute
		Assertions.assertEquals("5", testPerson.getAttributes().getAttribute("destination_cell"));
		Assertions.assertEquals("5", testPerson.getAttributes().getAttribute("destination_cell_main_run"));
		Assertions.assertEquals("190", testPerson.getAttributes().getAttribute("goods_type_main-run"));
		Assertions.assertEquals("190", testPerson.getAttributes().getAttribute("goods_type_post-run"));
		Assertions.assertEquals("190", testPerson.getAttributes().getAttribute("goods_type_pre-run"));
		Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.unallocated, testPerson.getAttributes().getAttribute("mode_main-run"));
		Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.unallocated, testPerson.getAttributes().getAttribute("mode_post-run"));
		Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.unallocated, testPerson.getAttributes().getAttribute("mode_pre-run"));
		Assertions.assertEquals("1", testPerson.getAttributes().getAttribute("origin_cell"));
		Assertions.assertEquals("1", testPerson.getAttributes().getAttribute("origin_cell_main_run"));
		Assertions.assertEquals("freight", testPerson.getAttributes().getAttribute("subpopulation"));
		Assertions.assertEquals(10.0, testPerson.getAttributes().getAttribute("tons_per_year_main-run"));
		Assertions.assertEquals("a", testPerson.getAttributes().getAttribute("trip_relation_index"));
	}

	@Test
	void testWriteCommonAttributesV2() {
		String kettenString = "file:" + utils.getPackageInputDirectory() + "testKetten.csv";

		List<TripRelation> tripRelations;
		PopulationFactory populationFactory;
		Person testPerson;

		try{
			tripRelations = TripRelation.readTripRelations(kettenString);
			populationFactory = PopulationUtils.getFactory();
			testPerson = populationFactory.createPerson(Id.createPersonId("test"));
		} catch (Exception e){
			Assertions.fail(e); // If this Assertion fails, there is an external problem in another class
			return;
		}

		LongDistanceFreightUtils.writeCommonAttributesV2(testPerson, tripRelations.getFirst(), "a");

		//Test the attribute
		Assertions.assertEquals("5", testPerson.getAttributes().getAttribute("destination_cell"));
		Assertions.assertEquals("5", testPerson.getAttributes().getAttribute("destination_cell_main_run"));
		Assertions.assertEquals("1005", testPerson.getAttributes().getAttribute("destination_terminal"));
		Assertions.assertEquals("190", testPerson.getAttributes().getAttribute("goods_type_main-run"));
		Assertions.assertEquals("190", testPerson.getAttributes().getAttribute("goods_type_post-run"));
		Assertions.assertEquals("190", testPerson.getAttributes().getAttribute("goods_type_pre-run"));
		Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.unallocated, testPerson.getAttributes().getAttribute("mode_main-run"));
		Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.unallocated, testPerson.getAttributes().getAttribute("mode_post-run"));
		Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.unallocated, testPerson.getAttributes().getAttribute("mode_pre-run"));
		Assertions.assertEquals("1", testPerson.getAttributes().getAttribute("origin_cell"));
		Assertions.assertEquals("1", testPerson.getAttributes().getAttribute("origin_cell_main_run"));
		Assertions.assertEquals("1001", testPerson.getAttributes().getAttribute("origin_terminal"));
		Assertions.assertEquals("freight", testPerson.getAttributes().getAttribute("subpopulation"));
		Assertions.assertEquals(1240.0, testPerson.getAttributes().getAttribute("tonKM_per_year_main-run"));
		Assertions.assertEquals(1564.0, testPerson.getAttributes().getAttribute("tonKM_per_year_post-run"));
		Assertions.assertEquals(0.0, testPerson.getAttributes().getAttribute("tonKM_per_year_pre-run"));
		Assertions.assertEquals(10.0, testPerson.getAttributes().getAttribute("tons_per_year_main-run"));
		Assertions.assertEquals(10.0, testPerson.getAttributes().getAttribute("tons_per_year_post-run"));
		Assertions.assertEquals(0.0, testPerson.getAttributes().getAttribute("tons_per_year_pre-run"));
		Assertions.assertEquals("FTL", testPerson.getAttributes().getAttribute("transport_type"));
		Assertions.assertEquals("a", testPerson.getAttributes().getAttribute("trip_relation_index"));
	}

	@Test
	void testSetTripType() {
		String kettenString = "file:" + utils.getPackageInputDirectory() + "testKetten.csv";

		List<TripRelation> tripRelations;
		PopulationFactory populationFactory;
		Person testPerson;

		try{
			tripRelations = TripRelation.readTripRelations(kettenString);
			populationFactory = PopulationUtils.getFactory();
			testPerson = populationFactory.createPerson(Id.createPersonId("test"));
		} catch (Exception e){
			Assertions.fail(e); // If this Assertion fails, there is an external problem in another class
			return;
		}

		LongDistanceFreightUtils.writeCommonAttributesV2(testPerson, tripRelations.getFirst(), "a");

		LongDistanceFreightUtils.setTripType(testPerson, "car");
		Assertions.assertEquals("car", testPerson.getAttributes().getAttribute("trip_type"));
	}
}
