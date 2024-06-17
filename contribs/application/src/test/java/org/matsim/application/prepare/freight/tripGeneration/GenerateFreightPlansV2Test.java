package org.matsim.application.prepare.freight.tripGeneration;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class GenerateFreightPlansV2Test {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testCall() {
		String regionShpString = utils.getPackageInputDirectory() + "testRegion.shp";
		String networkXmlString = utils.getPackageInputDirectory() + "testNetwork.xml";
		String freightDemand = utils.getPackageInputDirectory() + "testFreightDemand.xml.gz";
		String lookupTableString = "file:" + utils.getPackageInputDirectory() + "testLookup.csv";
		String output = utils.getPackageInputDirectory() + "/outV2";
		System.out.println(utils.getPackageInputDirectory());

		// Since this method basically just uses te FreightAgentGenerator, we will just assure,
		// that it has been used and saved properly.

		new GenerateFreightPlansV2().execute(
			"--data", freightDemand,
			"--network", networkXmlString,
			"--lookupTable", lookupTableString,
			"--nuts", regionShpString,
			"--output", output,
			"--truck-load", "14",
			"--working-days", "260",
			"--sample", "1"
		);

		//Check if 2 files have correct path and ending (file-name is irrelevant)
		File outputDirectory = new File(output);
		String[] files = outputDirectory.list();
		Assertions.assertNotNull(files); // Make sure output data was generated
		Assertions.assertEquals(2, files.length);

		int tsvFileIndex = -1;
		int xmlFileIndex = -1;
		for(int i = 0; i < files.length; i++){
			if(files[i].endsWith(".tsv")) tsvFileIndex = i;
			if(files[i].endsWith(".plans.xml.gz")) xmlFileIndex = i;
		}
		Assertions.assertTrue(tsvFileIndex != -1 && xmlFileIndex != -1); // If this fails, one of the two files wasn't created

		//Check tsv-file content
		ArrayList<String[]> dataFile1 = new ArrayList<>();
		int fileLen = 0;
		try{
			CSVParser parser = CSVParser.parse(new File(output+"/"+files[tsvFileIndex]).toURI().toURL(), StandardCharsets.ISO_8859_1,
				CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter('\t').setHeader().setSkipHeaderRecord(true).build());
			for(CSVRecord l : parser.getRecords()){
				dataFile1.add(l.values());
				fileLen++;
			}
		} catch (Exception e){
			Assertions.fail(e); // External error, but we can't continue the test
		}

		List<String> cell5Coords = Arrays.asList("441500.0, 5424300.0", "441800.0, 5424200.0"); //B,C
		List<String> cell1Coords = Arrays.asList("442200.0, 5424300.0", "442600.0, 5424200.0"); //D,E


		//Check amount of entries in file, to make sure that formatting and quantity of agents is correct
		Assertions.assertEquals(16, fileLen);
		for(String[] s : dataFile1){
			if((s[0]).endsWith("pre")){ //pre: cell5 -> cell5
				Assertions.assertTrue(cell5Coords.contains(s[1] +", " + s[2]));
				Assertions.assertTrue(cell5Coords.contains(s[3] +", " + s[4]));
			}else if((s[0]).endsWith("main")){ //main: cell5 -> cell1
				Assertions.assertTrue(cell5Coords.contains(s[1] +", " + s[2]));
				Assertions.assertTrue(cell1Coords.contains(s[3] +", " + s[4]));
			}else if((s[0]).endsWith("post")){ //post: cell1 -> cell1
				Assertions.assertTrue(cell1Coords.contains(s[1] +", " + s[2]));
				Assertions.assertTrue(cell1Coords.contains(s[3] +", " + s[4]));
			}
		}

		//Check plans-file content by using a sample agent
		Population population = PopulationUtils.readPopulation(output + "/" + files[xmlFileIndex]);
		Person p1 = population.getPersons().get(Id.createPersonId("freight_2_0_main-run"));

		Assertions.assertEquals("1", p1.getAttributes().getAttribute("destination_cell"));
		Assertions.assertEquals("1", p1.getAttributes().getAttribute("destination_cell_main_run"));
		Assertions.assertEquals("1001", p1.getAttributes().getAttribute("destination_terminal"));
		Assertions.assertEquals("190", p1.getAttributes().getAttribute("goods_type_main-run"));
		Assertions.assertEquals("190", p1.getAttributes().getAttribute("goods_type_post-run"));
		Assertions.assertEquals("190", p1.getAttributes().getAttribute("goods_type_pre-run"));
		Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.road, p1.getAttributes().getAttribute("mode_main-run"));
		Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.road, p1.getAttributes().getAttribute("mode_post-run"));
		Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.road, p1.getAttributes().getAttribute("mode_pre-run"));
		Assertions.assertEquals("5", p1.getAttributes().getAttribute("origin_cell"));
		Assertions.assertEquals("5", p1.getAttributes().getAttribute("origin_cell_main_run"));
		Assertions.assertEquals("1005", p1.getAttributes().getAttribute("origin_terminal"));
		Assertions.assertEquals("freight", p1.getAttributes().getAttribute("subpopulation"));
		Assertions.assertEquals(105000.0, p1.getAttributes().getAttribute("tonKM_per_year_main-run"));
		Assertions.assertEquals(135000.0, p1.getAttributes().getAttribute("tonKM_per_year_post-run"));
		Assertions.assertEquals(75000.0, p1.getAttributes().getAttribute("tonKM_per_year_pre-run"));
		Assertions.assertEquals(35000.0, p1.getAttributes().getAttribute("tons_per_year_main-run"));
		Assertions.assertEquals(45000.0, p1.getAttributes().getAttribute("tons_per_year_post-run"));
		Assertions.assertEquals(25000.0, p1.getAttributes().getAttribute("tons_per_year_pre-run"));
		Assertions.assertEquals("FTL", p1.getAttributes().getAttribute("transport_type"));
		Assertions.assertEquals("main-run", p1.getAttributes().getAttribute("trip_type"));

		// Check if the Persons have correct plans
		List<String> referenceLinksCell1 = Arrays.asList("cd", "de"); // All links-names in Cell 1
		List<String> referenceLinksCell5 = Arrays.asList("ab", "bc"); // All links-names in Cell 5
		for(Person p : population.getPersons().values()){
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
			}else if(p.getId().toString().contains("post")) { // main-run
				// Destination -> Destination
				Assertions.assertTrue(referenceLinksCell1.contains(startActivity.getLinkId().toString()));
				Assertions.assertTrue(referenceLinksCell1.contains(endActivity.getLinkId().toString()));
				Assertions.assertEquals("post-run", p.getAttributes().getAttribute("trip_type"));
			}
		}

		//Delete output
		try {
			for (File toDelete : outputDirectory.listFiles()) {
				toDelete.delete();
			}
			outputDirectory.delete();
		} catch(Exception ignored){}
	}
}
