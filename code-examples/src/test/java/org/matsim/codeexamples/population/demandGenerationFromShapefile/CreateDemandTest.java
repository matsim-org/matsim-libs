package org.matsim.codeexamples.population.demandGenerationFromShapefile;

import org.junit.Test;
import org.matsim.api.core.v01.population.Population;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CreateDemandTest {

	@Test
	public void testCreate() {

		CreateDemand createDemand = new CreateDemand();
		createDemand.create();

		Population population = createDemand.getPopulation();

		assertNotNull(population);
		assertEquals(73721, population.getPersons().size());
	}
}