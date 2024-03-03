package org.matsim.contrib.discrete_mode_choice.models.nested;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel.NoFeasibleChoiceException;
import org.matsim.contribs.discrete_mode_choice.model.nested.DefaultNest;
import org.matsim.contribs.discrete_mode_choice.model.nested.DefaultNestStructure;
import org.matsim.contribs.discrete_mode_choice.model.nested.DefaultNestedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.nested.NestCalculator;
import org.matsim.contribs.discrete_mode_choice.model.nested.NestedUtilityCandidate;

public class NestCalculatorTest {
	@Test
	void testNoNesting() throws NoFeasibleChoiceException {
		DefaultNestStructure structure = new DefaultNestStructure();

		NestCalculator calculator = new NestCalculator(structure);

		NestedUtilityCandidate carCandidate = new DefaultNestedTripCandidate(-1.0, "car", 0.0, structure.getRoot());
		NestedUtilityCandidate busCandidate = new DefaultNestedTripCandidate(-2.0, "bus", 0.0, structure.getRoot());

		calculator.addCandidate(carCandidate);
		calculator.addCandidate(busCandidate);

		assertEquals(0.7310585786300049, calculator.calculateProbability(carCandidate), 1e-6);
		assertEquals(0.2689414213699951, calculator.calculateProbability(busCandidate), 1e-6);
	}

	@Test
	void testNoNestingRedBlue() throws NoFeasibleChoiceException {
		DefaultNestStructure structure = new DefaultNestStructure();

		NestCalculator calculator = new NestCalculator(structure);

		NestedUtilityCandidate carCandidate = new DefaultNestedTripCandidate(-1.0, "car", 0.0, structure.getRoot());
		NestedUtilityCandidate redbusCandidate = new DefaultNestedTripCandidate(-1.0, "redbus", 0.0,
				structure.getRoot());
		NestedUtilityCandidate bluebusCandidate = new DefaultNestedTripCandidate(-1.0, "bluebus", 0.0,
				structure.getRoot());

		calculator.addCandidate(carCandidate);
		calculator.addCandidate(redbusCandidate);
		calculator.addCandidate(bluebusCandidate);

		assertEquals(0.333333333333333, calculator.calculateProbability(carCandidate), 1e-6);
		assertEquals(0.333333333333333, calculator.calculateProbability(redbusCandidate), 1e-6);
		assertEquals(0.333333333333333, calculator.calculateProbability(bluebusCandidate), 1e-6);
	}

	@Test
	void testNestedBalancedRedBlue() throws NoFeasibleChoiceException {
		DefaultNestStructure structure = new DefaultNestStructure();
		DefaultNest busNest = new DefaultNest("bus", 1.0);
		structure.addNest(structure.getRoot(), busNest);

		NestCalculator calculator = new NestCalculator(structure);

		NestedUtilityCandidate carCandidate = new DefaultNestedTripCandidate(-1.0, "car", 0.0, structure.getRoot());
		NestedUtilityCandidate redbusCandidate = new DefaultNestedTripCandidate(-1.0, "redbus", 0.0, busNest);
		NestedUtilityCandidate bluebusCandidate = new DefaultNestedTripCandidate(-1.0, "bluebus", 0.0, busNest);

		calculator.addCandidate(carCandidate);
		calculator.addCandidate(redbusCandidate);
		calculator.addCandidate(bluebusCandidate);

		busNest.setScaleParameter(1.0);
		assertEquals(Math.log(2.0 * Math.exp(-1.0)), calculator.calculateExpectedUtility(busNest), 1e-6);
		assertEquals(0.5, calculator.calculateConditionalProbability(redbusCandidate), 1e-6);
		assertEquals(0.5, calculator.calculateConditionalProbability(bluebusCandidate), 1e-6);
		assertEquals(0.33333333333333, calculator.calculateConditionalProbability(carCandidate), 1e-6);
		assertEquals(0.66666666666666, calculator.calculateConditionalProbability(busNest), 1e-6);
		assertEquals(0.33333333333333, calculator.calculateProbability(carCandidate), 1e-6);
		assertEquals(0.33333333333333, calculator.calculateProbability(redbusCandidate), 1e-6);
		assertEquals(0.33333333333333, calculator.calculateProbability(bluebusCandidate), 1e-6);

		busNest.setScaleParameter(10.0);
		assertEquals(-0.9306852819440055, calculator.calculateExpectedUtility(busNest), 1e-6);
		assertEquals(0.5, calculator.calculateConditionalProbability(redbusCandidate), 1e-6);
		assertEquals(0.5, calculator.calculateConditionalProbability(bluebusCandidate), 1e-6);
		assertEquals(0.48267825516781476, calculator.calculateConditionalProbability(carCandidate), 1e-6);
		assertEquals(1.0 - 0.48267825516781476, calculator.calculateConditionalProbability(busNest), 1e-6);
		assertEquals(0.48267825516781476, calculator.calculateProbability(carCandidate), 1e-6);
		assertEquals(0.5 * (1.0 - 0.48267825516781476), calculator.calculateProbability(redbusCandidate), 1e-6);
		assertEquals(0.5 * (1.0 - 0.48267825516781476), calculator.calculateProbability(bluebusCandidate), 1e-6);

		busNest.setScaleParameter(100.0);
		assertEquals(-0.9930685281944005, calculator.calculateExpectedUtility(busNest), 1e-6);
		assertEquals(0.5, calculator.calculateConditionalProbability(redbusCandidate), 1e-6);
		assertEquals(0.5, calculator.calculateConditionalProbability(bluebusCandidate), 1e-6);
		assertEquals(0.4982671389865804, calculator.calculateConditionalProbability(carCandidate), 1e-6);
		assertEquals(1.0 - 0.4982671389865804, calculator.calculateConditionalProbability(busNest), 1e-6);
		assertEquals(0.4982671389865804, calculator.calculateProbability(carCandidate), 1e-6);
		assertEquals(0.5 * (1.0 - 0.4982671389865804), calculator.calculateProbability(redbusCandidate), 1e-6);
		assertEquals(0.5 * (1.0 - 0.4982671389865804), calculator.calculateProbability(bluebusCandidate), 1e-6);
	}

	@Test
	void testNestedUnbalancedRedBlue() throws NoFeasibleChoiceException {
		DefaultNestStructure structure = new DefaultNestStructure();
		DefaultNest busNest = new DefaultNest("bus", 1.0);
		structure.addNest(structure.getRoot(), busNest);

		NestCalculator calculator = new NestCalculator(structure);

		NestedUtilityCandidate carCandidate = new DefaultNestedTripCandidate(-1.0, "car", 0.0, structure.getRoot());
		NestedUtilityCandidate redbusCandidate = new DefaultNestedTripCandidate(-0.5, "redbus", 0.0, busNest);
		NestedUtilityCandidate bluebusCandidate = new DefaultNestedTripCandidate(-1.0, "bluebus", 0.0, busNest);

		calculator.addCandidate(carCandidate);
		calculator.addCandidate(redbusCandidate);
		calculator.addCandidate(bluebusCandidate);

		busNest.setScaleParameter(1.0);
		assertEquals(0.6224593312018546, calculator.calculateConditionalProbability(redbusCandidate), 1e-6);
		assertEquals(1.0 - 0.6224593312018546, calculator.calculateConditionalProbability(bluebusCandidate), 1e-6);
	}
}