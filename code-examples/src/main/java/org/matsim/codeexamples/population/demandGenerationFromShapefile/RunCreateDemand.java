package org.matsim.codeexamples.population.demandGenerationFromShapefile;

import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;

import java.nio.file.Paths;

public class RunCreateDemand {

	public static void main(String[] args) {

		CreateDemand createDemand = new CreateDemand();
		createDemand.create();
		Population result = createDemand.getPopulation();

		new PopulationWriter(result).write(Paths.get("path to your output file").toString());
	}
}
