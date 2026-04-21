package org.matsim.codeexamples.population.demandGenerationFromShapefile;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.population.Population;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CreateDemandTest {

	@Test
	void testCreate() {

		CreateDemand createDemand = new CreateDemand();
		createDemand.create();

		Population population = createDemand.getPopulation();

		assertNotNull(population);
		assertEquals(73721, population.getPersons().size());
	}
}