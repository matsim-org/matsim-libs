package org.matsim.application.prepare.freight.tripGeneration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;

class GenerateFreightDemandDataTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void call() {
		String kettenString = "file:" + utils.getPackageInputDirectory() + "testKetten.csv";
		String output = utils.getPackageInputDirectory() + "/outGen";

		// First run without combineSimilarEntries
		new GenerateFreightDemandData().execute(
			"--data", kettenString,
//			"--combineSimilarEntries", "false",
			"--output", output
		);

		//Check if 2 files have correct path and ending (file-name is irrelevant)
		File outputDirectory = new File(output);
		String[] files = outputDirectory.list();

		//CHeck if the population File content is correct
		Population freightPopulation = PopulationUtils.readPopulation(output + "/" + files[0]);
		Assertions.assertEquals(2, freightPopulation.getPersons().size());
		for(Person p : freightPopulation.getPersons().values()){
			if(p.getAttributes().getAttribute("destination_cell").equals("5")){
				Assertions.assertEquals("5", p.getAttributes().getAttribute("destination_cell_main_run"));
				Assertions.assertEquals("1005", p.getAttributes().getAttribute("destination_terminal"));
				Assertions.assertEquals("190", p.getAttributes().getAttribute("goods_type_main-run"));
				Assertions.assertEquals("190", p.getAttributes().getAttribute("goods_type_post-run"));
				Assertions.assertEquals("190", p.getAttributes().getAttribute("goods_type_pre-run"));
				Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.unallocated, p.getAttributes().getAttribute("mode_main-run"));
				Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.unallocated, p.getAttributes().getAttribute("mode_post-run"));
				Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.unallocated, p.getAttributes().getAttribute("mode_pre-run"));
				Assertions.assertEquals("1", p.getAttributes().getAttribute("origin_cell"));
				Assertions.assertEquals("1", p.getAttributes().getAttribute("origin_cell_main_run"));
				Assertions.assertEquals("1001", p.getAttributes().getAttribute("origin_terminal"));

				Assertions.assertEquals(1240.0, p.getAttributes().getAttribute("tonKM_per_year_main-run"));
				Assertions.assertEquals(1564.0, p.getAttributes().getAttribute("tonKM_per_year_post-run"));
				Assertions.assertEquals(0.0, p.getAttributes().getAttribute("tonKM_per_year_pre-run"));
				Assertions.assertEquals(10.0, p.getAttributes().getAttribute("tons_per_year_main-run"));
				Assertions.assertEquals(10.0, p.getAttributes().getAttribute("tons_per_year_post-run"));
				Assertions.assertEquals(0.0, p.getAttributes().getAttribute("tons_per_year_pre-run"));
				Assertions.assertEquals("FTL", p.getAttributes().getAttribute("transport_type"));
			}
			if(p.getAttributes().getAttribute("destination_cell").equals("1")){
				Assertions.assertEquals("1", p.getAttributes().getAttribute("destination_cell_main_run"));
				Assertions.assertEquals("1001", p.getAttributes().getAttribute("destination_terminal"));
				Assertions.assertEquals("190", p.getAttributes().getAttribute("goods_type_main-run"));
				Assertions.assertEquals("190", p.getAttributes().getAttribute("goods_type_post-run"));
				Assertions.assertEquals("190", p.getAttributes().getAttribute("goods_type_pre-run"));
				Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.road, p.getAttributes().getAttribute("mode_main-run"));
				Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.road, p.getAttributes().getAttribute("mode_post-run"));
				Assertions.assertEquals(LongDistanceFreightUtils.LongDistanceTravelMode.road, p.getAttributes().getAttribute("mode_pre-run"));
				Assertions.assertEquals("5", p.getAttributes().getAttribute("origin_cell"));
				Assertions.assertEquals("5", p.getAttributes().getAttribute("origin_cell_main_run"));
				Assertions.assertEquals("1005", p.getAttributes().getAttribute("origin_terminal"));

				Assertions.assertEquals(105000.0, p.getAttributes().getAttribute("tonKM_per_year_main-run"));
				Assertions.assertEquals(135000.0, p.getAttributes().getAttribute("tonKM_per_year_post-run"));
				Assertions.assertEquals(75000.0, p.getAttributes().getAttribute("tonKM_per_year_pre-run"));
				Assertions.assertEquals(35000.0, p.getAttributes().getAttribute("tons_per_year_main-run"));
				Assertions.assertEquals(45000.0, p.getAttributes().getAttribute("tons_per_year_post-run"));
				Assertions.assertEquals(25000.0, p.getAttributes().getAttribute("tons_per_year_pre-run"));
				Assertions.assertEquals("FTL", p.getAttributes().getAttribute("transport_type"));
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
