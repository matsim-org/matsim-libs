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
	private double cost = -1.0;

	@Parameter
	@Comment("scoring utility applied per minute waited")
	private double waitTime_min = 0.0;

	@Parameter
	@Comment("scoring utility applied per detour minutes induced for charging (during routing)")
	private double detourTime_min = 0.0;

	@Parameter
	@Comment("scoring utility applied per detour kilometres induced for charging (during routing)")
	private double detourDistance_km = 0.0;

	@Parameter
	@Comment("scoring utility applied every time the SoC goes to zero")
	private double zeroSoc = -100.0;

	@Parameter
	@Comment("scoring utility applied every time a charging attempt is unsuccessful (going to next charger)")
	private double failedChargingAttempt = -10.0;

	@Parameter
	@Comment("scoring utility applied every time a charging process (multiple retries) is unsuccessful")
	private double failedChargingProcess = -100.0;

	@Parameter
	@Comment("scoring utility applied every time the SoC goes from above to below the per-person minium soc (person attriute "
			+ ChargingPlanScoring.MINIMUM_SOC_PERSON_ATTRIBUTE + ")")
	private double belowMinimumSoc = 0.0;

	@Parameter
	@Comment("scoring utility applied at the end of the day if the SoC is below the per-person requirement (person attriute "
			+ ChargingPlanScoring.MINIMUM_END_SOC_PERSON_ATTRIBUTE + ")")
	private double belowMinimumEndSoc = 0.0;

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public double getWaitTime_min() {
		return waitTime_min;
	}

	public void setWaitTime_min(double waitTime_min) {
		this.waitTime_min = waitTime_min;
	}

	public double getDetourTime_min() {
		return detourTime_min;
	}

	public void setDetourTime_min(double detourTime_min) {
		this.detourTime_min = detourTime_min;
	}

	public double getDetourDistance_km() {
		return detourDistance_km;
	}

	public void setDetourDistance_km(double detourDistance_km) {
		this.detourDistance_km = detourDistance_km;
	}

	public double getZeroSoc() {
		return zeroSoc;
	}

	public void setZeroSoc(double zeroSoc) {
		this.zeroSoc = zeroSoc;
	}

	public double getFailedChargingAttempt() {
		return failedChargingAttempt;
	}

	public void setFailedChargingAttempt(double failedChargingAttempt) {
		this.failedChargingAttempt = failedChargingAttempt;
	}

	public double getFailedChargingProcess() {
		return failedChargingProcess;
	}

	public void setFailedChargingProcess(double failedChargingProcess) {
		this.failedChargingProcess = failedChargingProcess;
	}

	public double getBelowMinimumSoc() {
		return belowMinimumSoc;
	}

	public void setBelowMinimumSoc(double belowMinimumSoc) {
		this.belowMinimumSoc = belowMinimumSoc;
	}

	public double getBelowMinimumEndSoc() {
		return belowMinimumEndSoc;
	}

	public void setBelowMinimumEndSoc(double belowMinimumEndSoc) {
		this.belowMinimumEndSoc = belowMinimumEndSoc;
	}
}
