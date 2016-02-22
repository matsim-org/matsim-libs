package org.matsim.core.scoring.functions;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

/**
 * Class that stores parameters used from the scoring functions
 * @author thibautd
 */
public class ModeUtilityParameters {
	public static class Builder {
		private double marginalUtilityOfTraveling_s = 0;
		private double marginalUtilityOfDistance_m = 0;
		private double monetaryDistanceRate = 0;
		private double constant = 0;

		public Builder() {}

		public Builder( PlanCalcScoreConfigGroup.ModeParams params ) {
			this.marginalUtilityOfTraveling_s = params.getMarginalUtilityOfTraveling() / 3600.0;
			this.marginalUtilityOfDistance_m = params.getMarginalUtilityOfDistance();
			this.monetaryDistanceRate = params.getMonetaryDistanceRate();
			this.constant = params.getConstant();
		}

		public Builder setMarginalUtilityOfTraveling_s(double marginalUtilityOfTraveling_s) {
			this.marginalUtilityOfTraveling_s = marginalUtilityOfTraveling_s;
			return this;
		}

		public Builder setMarginalUtilityOfDistance_m(double marginalUtilityOfDistance_m) {
			this.marginalUtilityOfDistance_m = marginalUtilityOfDistance_m;
			return this;
		}

		public Builder setMonetaryDistanceRate(double monetaryDistanceRate) {
			this.monetaryDistanceRate = monetaryDistanceRate;
			return this;
		}

		public Builder setConstant(double constant) {
			this.constant = constant;
			return this;
		}

		public ModeUtilityParameters build() {
			return new ModeUtilityParameters(
					marginalUtilityOfTraveling_s,
					marginalUtilityOfDistance_m,
					monetaryDistanceRate,
					constant );
		}
	}

	public ModeUtilityParameters(
			double marginalUtilityOfTraveling_s,
			double marginalUtilityOfDistance_m,
			double monetaryDistanceCostRate,
			double constant) {
		this.marginalUtilityOfTraveling_s = marginalUtilityOfTraveling_s;
		this.marginalUtilityOfDistance_m = marginalUtilityOfDistance_m;
		this.monetaryDistanceCostRate = monetaryDistanceCostRate;
		this.constant = constant;
	}

	public final double marginalUtilityOfTraveling_s;
	public final double marginalUtilityOfDistance_m;
	public final double monetaryDistanceCostRate;
	public final double constant;
}
