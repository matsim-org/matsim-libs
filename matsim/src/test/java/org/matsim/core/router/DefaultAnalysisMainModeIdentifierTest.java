package org.matsim.core.router;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;


public class DefaultAnalysisMainModeIdentifierTest {

	private AnalysisMainModeIdentifier mmi = new DefaultAnalysisMainModeIdentifier();

	private static List<? extends PlanElement> legs(String... modes) {
		return Arrays.stream(modes).map(PopulationUtils::createLeg).toList();
	}

	@Test
	void correctModes() {

		assertThat(mmi.identifyMainMode(legs(TransportMode.walk, TransportMode.car, TransportMode.walk)))
			.isEqualTo(TransportMode.car);

		assertThat(mmi.identifyMainMode(legs(TransportMode.pt, TransportMode.car)))
			.isEqualTo(TransportMode.pt);

		assertThat(mmi.identifyMainMode(legs(TransportMode.walk, TransportMode.walk)))
			.isEqualTo(TransportMode.walk);

		assertThat(mmi.identifyMainMode(legs("new_mode", TransportMode.walk)))
			.isEqualTo("new_mode");

		assertThat(mmi.identifyMainMode(legs(TransportMode.non_network_walk, "new_mode")))
			.isEqualTo("new_mode");

		assertThat(mmi.identifyMainMode(legs("new_mode", "new_mode", TransportMode.walk, TransportMode.non_network_walk)))
			.isEqualTo("new_mode");

	}

	@Test
	void failingModes() {

		assertThatThrownBy(() -> mmi.identifyMainMode(legs("new_mode", TransportMode.walk, "other_new_mode"))).
			isInstanceOf(IllegalStateException.class);

		assertThatThrownBy(() -> mmi.identifyMainMode(legs("new_mode", "other_new_mode"))).
			isInstanceOf(IllegalStateException.class);

		assertThatThrownBy(() -> mmi.identifyMainMode(legs("new_mode", TransportMode.car))).
			isInstanceOf(IllegalStateException.class);

		assertThatThrownBy(() -> mmi.identifyMainMode(legs(TransportMode.car, "new_mode"))).
			isInstanceOf(IllegalStateException.class);

	}

	@Test
	final void testIntermodalPtDrtTrip() {

		assertThat(mmi.identifyMainMode(legs(TransportMode.non_network_walk, TransportMode.walk, TransportMode.non_network_walk,
			TransportMode.drt, TransportMode.walk, TransportMode.pt, TransportMode.walk, TransportMode.pt, TransportMode.walk)))
			.isEqualTo(TransportMode.pt);

	}

}
