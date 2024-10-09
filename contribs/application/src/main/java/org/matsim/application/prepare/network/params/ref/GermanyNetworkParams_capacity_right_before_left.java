package org.matsim.application.prepare.network.params.ref;
import org.matsim.application.prepare.network.params.FeatureRegressor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
* Generated model, do not modify.
*/
final class GermanyNetworkParams_capacity_right_before_left implements FeatureRegressor {

    public static GermanyNetworkParams_capacity_right_before_left INSTANCE = new GermanyNetworkParams_capacity_right_before_left();
    public static final double[] DEFAULT_PARAMS = {};

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        return predict(ft, DEFAULT_PARAMS);
    }

    @Override
    public double[] getData(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 126.5781093424676) / 82.93604475431307;
		data[1] = (ft.getDouble("speed") - 8.347924642353416) / 0.2530674029724574;
		data[2] = (ft.getDouble("num_lanes") - 1.0161192826919203) / 0.1410293306032409;
		data[3] = ft.getDouble("change_speed");
		data[4] = ft.getDouble("change_num_lanes");
		data[5] = ft.getDouble("num_to_links");
		data[6] = ft.getDouble("junction_inc_lanes");
		data[7] = ft.getDouble("priority_lower");
		data[8] = ft.getDouble("priority_equal");
		data[9] = ft.getDouble("priority_higher");
		data[10] = ft.getDouble("is_secondary_or_higher");
		data[11] = ft.getDouble("is_primary_or_higher");
		data[12] = ft.getDouble("is_motorway");
		data[13] = ft.getDouble("is_link");

        return data;
    }

    @Override
    public double predict(Object2DoubleMap<String> ft, double[] params) {

        double[] data = getData(ft);
        for (int i = 0; i < data.length; i++)
            if (Double.isNaN(data[i])) throw new IllegalArgumentException("Invalid data at index: " + i);

        return score(data, params);
    }
    public static double score(double[] input, double[] params) {
        return 820.7862981813687 + input[0] * -2.2407160935169324 + input[1] * -1.0312478382897772 + input[2] * -65.10322048874464 + input[3] * -3.7672954706609025 + input[4] * 91.56185003046016 + input[5] * 8.354883935056996 + input[6] * -16.573320689701585 + input[7] * 1.3872614339054785 + input[8] * 798.9462847274731 + input[9] * 20.45275201994965 + input[10] * 12.054157458777055 + input[11] * -11.583879746981143 + input[12] * -1.4911083760151145 + input[13] * 15.111478746337356;
    }
}
