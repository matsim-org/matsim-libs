package org.matsim.application.prepare.network.params.ref;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.matsim.application.prepare.network.params.FeatureRegressor;
import org.matsim.application.prepare.network.params.NetworkModel;

/**
 * Reference model with a manually defined decision tree using priority, road type and speed.
 */
public final class DecisionTreeParams implements NetworkModel {

	public static final double[] DEFAULT_PARAMS = {
		0.9, 0.8, 0.9, 0.8, 0.7, 0.7, 0.7, 0.7,
		0.9, 0.8, 0.9, 0.8, 0.7, 0.7, 0.7, 0.7,
		0.9, 0.8, 0.9, 0.8, 0.7, 0.7, 0.7, 0.7
	};

	private static final FeatureRegressor INSTANCE = new Model();

	@Override
	public FeatureRegressor speedFactor(String junctionType, String highwayType) {
		return INSTANCE;
	}

	private static final class Model implements FeatureRegressor {

		@Override
		public double predict(Object2DoubleMap<String> ft) {
			return predict(ft, DEFAULT_PARAMS);
		}

		@Override
		public double predict(Object2DoubleMap<String> ft, double[] params) {
			double[] inputs = getData(ft);
			if (inputs[1] == 1)
				if (inputs[6] == 1)
					if (inputs[7] >= 27.5)
						return params[0];
					else
						return params[1];
				else if (inputs[5] == 1)
					if (inputs[7] >= 22)
						return params[2];
					else
						return params[3];
				else if (inputs[4] == 1)
					if (inputs[7] <= 10)
						return params[4];
					else
						return params[5];
				else {
					if (inputs[7] <= 10)
						return params[6];
					else
						return params[7];
				}

			else if (inputs[2] == 1)
				if (inputs[6] == 1)
					if (inputs[7] >= 27.5)
						return params[8];
					else
						return params[9];
				else if (inputs[5] == 1)
					if (inputs[7] >= 22)
						return params[10];
					else
						return params[11];
				else if (inputs[4] == 1)
					if (inputs[7] <= 10)
						return params[12];
					else
						return params[13];
				else {
					if (inputs[7] <= 10)
						return params[14];
					else
						return params[15];
				}

			else {
				if (inputs[6] == 1)
					if (inputs[7] >= 27.5)
						return params[16];
					else
						return params[17];
				else if (inputs[5] == 1)
					if (inputs[7] >= 22)
						return params[18];
					else
						return params[19];
				else if (inputs[4] == 1)
					if (inputs[7] <= 10)
						return params[20];
					else
						return params[21];
				else {
					if (inputs[7] <= 10)
						return params[22];
					else
						return params[23];
				}
			}
		}

		@Override
		public double[] getData(Object2DoubleMap<String> ft) {
			return new double[]{
				ft.getDouble("length"),
				ft.getDouble("priority_lower"),
				ft.getDouble("priority_equal"),
				ft.getDouble("priority_higher"),
				ft.getDouble("is_secondary_or_higher"),
				ft.getDouble("is_primary_or_higher"),
				ft.getDouble("is_motorway"),
				ft.getDouble("speed")
			};
		}
	}

}
