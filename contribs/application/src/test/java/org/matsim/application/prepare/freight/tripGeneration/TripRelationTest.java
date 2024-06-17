package org.matsim.application.prepare.freight.tripGeneration;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

class TripRelationTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testReadTripRelations() {
		String kettenString = "file:" + utils.getPackageInputDirectory() + "testKetten.csv";

		List<TripRelation> tripRelations;

		try{
			tripRelations = TripRelation.readTripRelations(kettenString);
		} catch (Exception e){
			Assertions.fail(e); // If this Assertion fails, there is an external problem in another class
			return;
		}

		Assertions.assertEquals(2, tripRelations.size());

		Assertions.assertEquals("1", tripRelations.getFirst().getOriginCell());
		Assertions.assertEquals("5", tripRelations.getFirst().getDestinationCell());
		Assertions.assertEquals("1", tripRelations.getFirst().getOriginCellMainRun());
		Assertions.assertEquals("5", tripRelations.getFirst().getDestinationCellMainRun());
		Assertions.assertEquals("1001", tripRelations.getFirst().getOriginTerminal());
		Assertions.assertEquals("1005", tripRelations.getFirst().getDestinationTerminal());
		Assertions.assertEquals("0", tripRelations.getFirst().getModePreRun());
		Assertions.assertEquals("0", tripRelations.getFirst().getModeMainRun());
		Assertions.assertEquals("0", tripRelations.getFirst().getModePostRun());
		Assertions.assertEquals("190", tripRelations.getFirst().getGoodsTypePreRun());
		Assertions.assertEquals("190", tripRelations.getFirst().getGoodsTypeMainRun());
		Assertions.assertEquals("190", tripRelations.getFirst().getGoodsTypePostRun());
		Assertions.assertEquals(0, tripRelations.getFirst().getTonsPerYearPreRun());
		Assertions.assertEquals(10, tripRelations.getFirst().getTonsPerYearMainRun());
		Assertions.assertEquals(10, tripRelations.getFirst().getTonsPerYearPostRun());
		Assertions.assertEquals(0, tripRelations.getFirst().getTonKMPerYearPreRun());
		Assertions.assertEquals(1240, tripRelations.getFirst().getTonKMPerYearMainRun());
		Assertions.assertEquals(1564, tripRelations.getFirst().getTonKMPerYearPostRun());

		Assertions.assertEquals("5", tripRelations.get(1).getOriginCell());
		Assertions.assertEquals("1", tripRelations.get(1).getDestinationCell());
		Assertions.assertEquals(25000, tripRelations.get(1).getTonsPerYearPreRun());
		Assertions.assertEquals(35000, tripRelations.get(1).getTonsPerYearMainRun());
		Assertions.assertEquals(45000, tripRelations.get(1).getTonsPerYearPostRun());


		Assertions.assertEquals("5", tripRelations.getLast().getOriginCell());
		Assertions.assertEquals("1", tripRelations.getLast().getDestinationCell());
	}

	@Test
	void testCombineSimilarEntries() {
		String tripRelationString = "file:" + utils.getPackageInputDirectory() + "testTripRelation.csv";

		List<TripRelation> tripRelations;

		try{
			tripRelations = TripRelation.readTripRelations(tripRelationString);
			Assertions.assertEquals(6, tripRelations.size()); // If this fails, the readTripRelations() method is not working properly
		} catch (Exception e){
			Assertions.fail(e); // If this Assertion fails, there is an external problem in another class
			return;
		}

		// Execute method on tripRelations
		// Expected behavior: tripRelations now should have 4 entries
		TripRelation.combineSimilarEntries(tripRelations);
		Assertions.assertEquals(4, tripRelations.size());

		// Make sure, that the freight-tons amounts are correct
		// Entry 1: 0/10/0
		// Entry 2: 75000/105000/135000
		// Entry 3: 0/10/0
		// Entry 4: 0/10/0

		// Entry 1: Check all attributes to make sure they have not been changed
		Assertions.assertEquals("1", tripRelations.getFirst().getOriginCell());
		Assertions.assertEquals("5", tripRelations.getFirst().getDestinationCell());
		Assertions.assertEquals("1", tripRelations.getFirst().getOriginCellMainRun());
		Assertions.assertEquals("5", tripRelations.getFirst().getDestinationCellMainRun());
		Assertions.assertEquals("1001", tripRelations.getFirst().getOriginTerminal());
		Assertions.assertEquals("1005", tripRelations.getFirst().getDestinationTerminal());
		Assertions.assertEquals("0", tripRelations.getFirst().getModePreRun());
		Assertions.assertEquals("0", tripRelations.getFirst().getModeMainRun());
		Assertions.assertEquals("0", tripRelations.getFirst().getModePostRun());
		Assertions.assertEquals("190", tripRelations.getFirst().getGoodsTypePreRun());
		Assertions.assertEquals("190", tripRelations.getFirst().getGoodsTypeMainRun());
		Assertions.assertEquals("190", tripRelations.getFirst().getGoodsTypePostRun());
		Assertions.assertEquals(0, tripRelations.getFirst().getTonsPerYearPreRun());
		Assertions.assertEquals(10, tripRelations.getFirst().getTonsPerYearMainRun());
		Assertions.assertEquals(10, tripRelations.getFirst().getTonsPerYearPostRun());
		Assertions.assertEquals(0, tripRelations.getFirst().getTonKMPerYearPreRun());
		Assertions.assertEquals(1240, tripRelations.getFirst().getTonKMPerYearMainRun());
		Assertions.assertEquals(1564, tripRelations.getFirst().getTonKMPerYearPostRun());

		// Entry 2:
		Assertions.assertEquals("5", tripRelations.get(1).getOriginCell());
		Assertions.assertEquals("1", tripRelations.get(1).getDestinationCell());
		Assertions.assertEquals("5", tripRelations.get(1).getOriginCellMainRun());
		Assertions.assertEquals("1", tripRelations.get(1).getDestinationCellMainRun());
		Assertions.assertEquals("1005", tripRelations.get(1).getOriginTerminal());
		Assertions.assertEquals("1001", tripRelations.get(1).getDestinationTerminal());
		Assertions.assertEquals("2", tripRelations.get(1).getModePreRun());
		Assertions.assertEquals("2", tripRelations.get(1).getModeMainRun());
		Assertions.assertEquals("2", tripRelations.get(1).getModePostRun());
		Assertions.assertEquals("190", tripRelations.get(1).getGoodsTypePreRun());
		Assertions.assertEquals("190", tripRelations.get(1).getGoodsTypeMainRun());
		Assertions.assertEquals("190", tripRelations.get(1).getGoodsTypePostRun());
		Assertions.assertEquals(75000, tripRelations.get(1).getTonsPerYearPreRun());
		Assertions.assertEquals(105000, tripRelations.get(1).getTonsPerYearMainRun());
		Assertions.assertEquals(135000, tripRelations.get(1).getTonsPerYearPostRun());
		Assertions.assertEquals(225000, tripRelations.get(1).getTonKMPerYearPreRun());
		Assertions.assertEquals(315000, tripRelations.get(1).getTonKMPerYearMainRun());
		Assertions.assertEquals(405000, tripRelations.get(1).getTonKMPerYearPostRun());

		// Entry 3: Only check the most important attributes
		Assertions.assertEquals("1", tripRelations.get(2).getOriginCell());
		Assertions.assertEquals("6", tripRelations.get(2).getDestinationCell());
		Assertions.assertEquals("1", tripRelations.get(2).getOriginCellMainRun());
		Assertions.assertEquals("6", tripRelations.get(2).getDestinationCellMainRun());
		Assertions.assertEquals(0, tripRelations.get(2).getTonsPerYearPreRun());
		Assertions.assertEquals(10, tripRelations.get(2).getTonsPerYearMainRun());
		Assertions.assertEquals(10, tripRelations.get(2).getTonsPerYearPostRun());
		Assertions.assertEquals(0, tripRelations.get(2).getTonKMPerYearPreRun());
		Assertions.assertEquals(1240, tripRelations.get(2).getTonKMPerYearMainRun());
		Assertions.assertEquals(1564, tripRelations.get(2).getTonKMPerYearPostRun());

		// Entry 4: Only check the most important attributes
		Assertions.assertEquals("6", tripRelations.get(3).getOriginCell());
		Assertions.assertEquals("1", tripRelations.get(3).getDestinationCell());
		Assertions.assertEquals("6", tripRelations.get(3).getOriginCellMainRun());
		Assertions.assertEquals("1", tripRelations.get(3).getDestinationCellMainRun());
		Assertions.assertEquals(0, tripRelations.get(3).getTonsPerYearPreRun());
		Assertions.assertEquals(10, tripRelations.get(3).getTonsPerYearMainRun());
		Assertions.assertEquals(10, tripRelations.get(3).getTonsPerYearPostRun());
		Assertions.assertEquals(0, tripRelations.get(3).getTonKMPerYearPreRun());
		Assertions.assertEquals(1240, tripRelations.get(3).getTonKMPerYearMainRun());
		Assertions.assertEquals(1564, tripRelations.get(3).getTonKMPerYearPostRun());
	}

	@Test
	void testReadTripRelation() {
		String kettenString = "file:" + utils.getPackageInputDirectory() + "testKetten.csv";

		CSVParser parser;

		try {
			parser = CSVParser.parse(URI.create(kettenString).toURL(), StandardCharsets.ISO_8859_1,
				CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter(';').setHeader().setSkipHeaderRecord(true).build());
		} catch (Exception e){
			Assertions.fail(e); // If this Assertion fails, there is an external problem in another class
			return;
		}
		TripRelation tripRelation = TripRelation.readTripRelation(parser.getRecords().getFirst());

		// This method does not read all columns
		Assertions.assertEquals("1", tripRelation.getOriginCell());
		Assertions.assertEquals("5", tripRelation.getDestinationCell());
		Assertions.assertEquals("1", tripRelation.getOriginCellMainRun());
		Assertions.assertEquals("5", tripRelation.getDestinationCellMainRun());
		Assertions.assertEquals("0", tripRelation.getModePreRun());
		Assertions.assertEquals("0", tripRelation.getModeMainRun());
		Assertions.assertEquals("0", tripRelation.getModePostRun());
		Assertions.assertEquals("190", tripRelation.getGoodsTypeMainRun());
		Assertions.assertEquals(10, tripRelation.getTonsPerYearMainRun());

		// Records of parser can only be read once, thus we reinitialize it
		try {
			parser = CSVParser.parse(URI.create(kettenString).toURL(), StandardCharsets.ISO_8859_1,
				CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter(';').setHeader().setSkipHeaderRecord(true).build());
		} catch (Exception e){
			Assertions.fail(e); // If this Assertion fails, there is an external problem in another class
			return;
		}

		tripRelation = TripRelation.readTripRelation(parser.getRecords().get(1));

		Assertions.assertEquals("5", tripRelation.getOriginCell());
		Assertions.assertEquals("1", tripRelation.getDestinationCell());
		Assertions.assertEquals(35000, tripRelation.getTonsPerYearMainRun());
	}
}
