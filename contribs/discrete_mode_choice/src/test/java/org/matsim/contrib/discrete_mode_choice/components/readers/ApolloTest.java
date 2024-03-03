package org.matsim.contrib.discrete_mode_choice.components.readers;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.matsim.contribs.discrete_mode_choice.components.readers.ApolloParameterReader;
import org.matsim.contribs.discrete_mode_choice.components.readers.ApolloParameters;

public class ApolloTest {
	@Test
	void testApolloReader() throws IOException {
		URL fixtureUrl = getClass().getClassLoader().getResource("Model_13_12_Zurich_output.txt");
		ApolloParameters parameters = new ApolloParameterReader().read(fixtureUrl);

		assertEquals(-0.0608, parameters.getParameter("asc_sav"), 1e-6);
		assertEquals(-0.0888, parameters.getParameter("B_Cost"), 1e-6);
		assertEquals(-0.4590, parameters.getParameter("b_trip_zurich_car"), 1e-6);
	}
}
