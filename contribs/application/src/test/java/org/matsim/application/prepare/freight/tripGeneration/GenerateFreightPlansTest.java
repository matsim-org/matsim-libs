package org.matsim.application.prepare.freight.tripGeneration;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class GenerateFreightPlansTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testCall() {
		String regionShpString = utils.getPackageInputDirectory() + "testRegion.shp";
		String networkXmlString = utils.getPackageInputDirectory() + "testNetwork.xml";
		String kettenString = "file:" + utils.getPackageInputDirectory() + "testKetten.csv";
		String lookupTableString = "file:" + utils.getPackageInputDirectory() + "testLookup.csv";
		String output = utils.getPackageInputDirectory() + "/outV1";

		new GenerateFreightPlans().execute(
			"--data", kettenString,
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
		Assertions.assertEquals(30, fileLen);
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
		Person p = population.getPersons().get(Id.createPersonId("freight_1_0_main"));

		Assertions.assertEquals("1", p.getAttributes().getAttribute("destination_cell"));
		Assertions.assertEquals("1", p.getAttributes().getAttribute("destination_cell_main_run"));
//		Assertions.assertEquals("190", p.getAttributes().getAttribute("destination_cell"));
//		Assertions.assertEquals("190", p.getAttributes().getAttribute("destination_cell"));
//		Assertions.assertEquals("190", p.getAttributes().getAttribute("destination_cell"));
		Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.road, p.getAttributes().getAttribute("mode_main-run"));
		Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.road, p.getAttributes().getAttribute("mode_post-run"));
		Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.road, p.getAttributes().getAttribute("mode_pre-run"));
		Assertions.assertEquals("5", p.getAttributes().getAttribute("origin_cell"));
		Assertions.assertEquals("5", p.getAttributes().getAttribute("origin_cell_main_run"));
		Assertions.assertEquals("freight", p.getAttributes().getAttribute("subpopulation"));
		Assertions.assertEquals(35000.0, p.getAttributes().getAttribute("tons_per_year_main-run"));
		Assertions.assertEquals("main-run", p.getAttributes().getAttribute("trip_type"));

		//Delete output
		try {
			for (File toDelete : outputDirectory.listFiles()) {
				toDelete.delete();
			}
			outputDirectory.delete();
		} catch(Exception ignored){}
	}
}
