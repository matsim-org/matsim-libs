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
		private double dailyMoneyConstant = 0;
		private double dailyUtilityConstant = 0;

		public Builder() {}

		public Builder( PlanCalcScoreConfigGroup.ModeParams params ) {
			this.marginalUtilityOfTraveling_s = params.getMarginalUtilityOfTraveling() / 3600.0;
			this.marginalUtilityOfDistance_m = params.getMarginalUtilityOfDistance();
			this.monetaryDistanceRate = params.getMonetaryDistanceRate();
			this.constant = params.getConstant();
			this.dailyMoneyConstant = params.getDailyMonetaryConstant();
			this.dailyUtilityConstant = params.getDailyUtilityConstant();
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

		public Builder setDailyMoneyConstant(double dailyMoneyConstant) {
			this.dailyMoneyConstant = dailyMoneyConstant;
			return this;
		}
		
		public Builder setDailyUtilityConstant(double dailyUtilityConstant) {
			this.dailyUtilityConstant = dailyUtilityConstant;
			return this;
		}

		public ModeUtilityParameters build() {
			return new ModeUtilityParameters(
					marginalUtilityOfTraveling_s,
					marginalUtilityOfDistance_m,
					monetaryDistanceRate,
					constant,
					dailyMoneyConstant,
					dailyUtilityConstant);
		}
	}

	public ModeUtilityParameters(
			double marginalUtilityOfTraveling_s,
			double marginalUtilityOfDistance_m,
			double monetaryDistanceCostRate,
			double constant,
			double dailyMoneyConstant,
			double dailyUtilityConstant) {
		this.marginalUtilityOfTraveling_s = marginalUtilityOfTraveling_s;
		this.marginalUtilityOfDistance_m = marginalUtilityOfDistance_m;
		this.monetaryDistanceCostRate = monetaryDistanceCostRate;
		this.constant = constant;
		this.dailyMoneyConstant = dailyMoneyConstant;
		this.dailyUtilityConstant = dailyUtilityConstant;
	}

	public final double marginalUtilityOfTraveling_s;
	public final double marginalUtilityOfDistance_m;
	public final double monetaryDistanceCostRate;
	public final double constant;
	public final double dailyMoneyConstant;
	public final double dailyUtilityConstant;
}
