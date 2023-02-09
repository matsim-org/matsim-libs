package org.matsim.contrib.optimization.simulatedAnnealing;

import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.contrib.optimization.simulatedAnnealing.temperature.TemperatureFunction;
import org.matsim.core.config.ReflectiveConfigGroup;

public class SimulatedAnnealingConfigGroup extends ReflectiveConfigGroup {

	private final static String NAME = "simulatedAnnealing";




	public SimulatedAnnealingConfigGroup() {
		super(NAME);
	}

	@Parameter
	@Comment("Cooling Temperature schedule. Defines the shape of the cooling gradient. {linear, exponential}")
	public TemperatureFunction.DefaultFunctions coolingSchedule = TemperatureFunction.DefaultFunctions.exponentialMultiplicative;// seconds

	@Parameter
	@Comment("Cooling parameter alpha. Used to control the speed of cooling. Should be >0. " +
			"Typical values 0.8 <= alpha <= 0.9 for exponential cooling. alpha > 1 for logarithmic cooling.")
	@PositiveOrZero
	public double alpha = 0.85;

	@Parameter
	@Comment("Initial temperature T0, should be >0.")
	@PositiveOrZero
	public double initialTemperature = 1000;

	@Parameter
	@Comment("Number of cooling cycles n.")
	@PositiveOrZero
	public int nCoolingCycles = Integer.MAX_VALUE;

	@Parameter
	@Comment("Final temperature Tn of the system. Should >=0.")
	@PositiveOrZero
	public double finalTemperature = 0;

}
