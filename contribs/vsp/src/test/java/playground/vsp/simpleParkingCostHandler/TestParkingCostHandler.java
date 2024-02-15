package playground.vsp.simpleParkingCostHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.population.Population;
import org.matsim.testcases.MatsimTestUtils;

public class TestParkingCostHandler {
	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();
	private Population population;

	@BeforeEach
	public void setUp() {
		// TODO: Network, Persons, Event construction (PT drivers, other)

	}

}
