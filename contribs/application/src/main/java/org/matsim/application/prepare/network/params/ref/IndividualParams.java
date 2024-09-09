package org.matsim.application.prepare.network.params.ref;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.matsim.application.prepare.Predictor;
import org.matsim.application.prepare.network.params.NetworkModel;

/**
 * Reference model that uses one specific speed factor for each link.
 */
public final class IndividualParams implements NetworkModel {

	private static final Predictor INSTANCE = new Model();

	@Override
	public Predictor speedFactor(String junctionType, String highwayType) {
		return INSTANCE;
	}

	private static final class Model implements Predictor {

		@Override
		public double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {
			return predict(features, categories, new double[0]);
		}

		@Override
		public double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories, double[] params) {
			if (params.length == 0)
				return 1;

			return params[(int) features.getDouble("idx")];
		}

		@Override
		public double[] getData(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {
			return new double[]{
				features.getDouble("idx")
			};
		}
	}

}
