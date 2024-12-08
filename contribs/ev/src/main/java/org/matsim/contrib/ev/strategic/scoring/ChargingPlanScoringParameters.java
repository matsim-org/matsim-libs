package org.matsim.contrib.ev.strategic.scoring;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * This parameter set represents the weights that are used in charging plan
 * scoring. Some defaults are given.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ChargingPlanScoringParameters extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "scoring";

	public ChargingPlanScoringParameters() {
		super(GROUP_NAME);
	}

	@Parameter
	@Comment("scoring utility applied per money paid")
	public double cost = -1.0;

	@Parameter
	@Comment("scoring utility applied per minute waited")
	public double waitTime_min = 0.0;

	@Parameter
	@Comment("scoring utility applied per detour minutes induced for charging (during routing)")
	public double detourTime_min = 0.0;

	@Parameter
	@Comment("scoring utility applied per detour kilometres induced for charging (during routing)")
	public double detourDistance_km = 0.0;

	@Parameter
	@Comment("scorign utility applied every time the SoC goes to zero")
	public double zeroSoc = -100.0;

	@Parameter
	@Comment("scoring utility applied every time a charging attempt is unsuccessful (going to next charger)")
	public double failedChargingAttempt = -10.0;

	@Parameter
	@Comment("scoring utility applied every time a charging process (multiple retries) is unsuccessful")
	public double failedChargingProcess = -100.0;

	@Parameter
	@Comment("scoring utility applied every time the SoC goes from above to below the per-person minium soc (person attriute "
			+ ChargingPlanScoring.MINIMUM_SOC_PERSON_ATTRIBUTE + ")")
	public double belowMinimumSoc = 0.0;

	@Parameter
	@Comment("scoring utility applied at the end of the day if the SoC is below the per-person requirement (person attriute "
			+ ChargingPlanScoring.MINIMUM_END_SOC_PERSON_ATTRIBUTE + ")")
	public double belowMinimumEndSoc = 0.0;
}
