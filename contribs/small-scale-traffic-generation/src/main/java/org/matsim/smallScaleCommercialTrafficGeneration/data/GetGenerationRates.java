package org.matsim.smallScaleCommercialTrafficGeneration.data;

import java.util.HashMap;
import java.util.Map;

public interface GetGenerationRates {
	/**
	 * Sets the generation rates based on the IVV 2005
	 *
	 * @param smallScaleCommercialTrafficType used trafficType (freight or business traffic)
	 * @param generationType start or stop rates
	 */
	static Map<Integer, Map<String, Double>> setGenerationRates(String smallScaleCommercialTrafficType,
																String generationType) {

		Map<Integer, Map<String, Double>> generationRates = new HashMap<>();
		Map<String, Double> ratesPerPurpose1 = new HashMap<>();
		Map<String, Double> ratesPerPurpose2 = new HashMap<>();
		Map<String, Double> ratesPerPurpose3 = new HashMap<>();
		Map<String, Double> ratesPerPurpose4 = new HashMap<>();
		Map<String, Double> ratesPerPurpose5 = new HashMap<>();
		Map<String, Double> ratesPerPurpose6 = new HashMap<>();
		if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
			if (generationType.equals("start")) {
				ratesPerPurpose1.put("Inhabitants", 0.0);
				ratesPerPurpose1.put("Employee", 0.0);
				ratesPerPurpose1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1.put("Employee Construction", 0.0);
				ratesPerPurpose1.put("Employee Secondary Sector Rest", 0.059);
				ratesPerPurpose1.put("Employee Retail", 0.0);
				ratesPerPurpose1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2.put("Inhabitants", 0.0);
				ratesPerPurpose2.put("Employee", 0.029);
				ratesPerPurpose2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose2.put("Employee Construction", 0.0);
				ratesPerPurpose2.put("Employee Secondary Sector Rest", 0.045);
				ratesPerPurpose2.put("Employee Retail", 0.0);
				ratesPerPurpose2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose3.put("Inhabitants", 0.0);
				ratesPerPurpose3.put("Employee", 0.021);
				ratesPerPurpose3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose3.put("Employee Construction", 0.0);
				ratesPerPurpose3.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose3.put("Employee Retail", 0.0192);
				ratesPerPurpose3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose3.put("Employee Tertiary Sector Rest", 0.184);

				ratesPerPurpose4.put("Inhabitants", 0.0);
				ratesPerPurpose4.put("Employee", 0.021);
				ratesPerPurpose4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose4.put("Employee Construction", 0.0);
				ratesPerPurpose4.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose4.put("Employee Retail", 0.0);
				ratesPerPurpose4.put("Employee Traffic/Parcels", 0.203);
				ratesPerPurpose4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose5.put("Inhabitants", 0.0);
				ratesPerPurpose5.put("Employee", 0.03);
				ratesPerPurpose5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose5.put("Employee Construction", 0.29);
				ratesPerPurpose5.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose5.put("Employee Retail", 0.0);
				ratesPerPurpose5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5.put("Employee Tertiary Sector Rest", 0.0);
			} else if (generationType.equals("stop")) {
				ratesPerPurpose1.put("Inhabitants", 0.0);
				ratesPerPurpose1.put("Employee", 0.0);
				ratesPerPurpose1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1.put("Employee Construction", 0.0);
				ratesPerPurpose1.put("Employee Secondary Sector Rest", 0.02);
				ratesPerPurpose1.put("Employee Retail", 0.0);
				ratesPerPurpose1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2.put("Inhabitants", 0.002);
				ratesPerPurpose2.put("Employee", 0.0);
				ratesPerPurpose2.put("Employee Primary Sector", 0.029);
				ratesPerPurpose2.put("Employee Construction", 0.029);
				ratesPerPurpose2.put("Employee Secondary Sector Rest", 0.009);
				ratesPerPurpose2.put("Employee Retail", 0.029);
				ratesPerPurpose2.put("Employee Traffic/Parcels", 0.039);
				ratesPerPurpose2.put("Employee Tertiary Sector Rest", 0.029);

				ratesPerPurpose3.put("Inhabitants", 0.025);
				ratesPerPurpose3.put("Employee", 0.0);
				ratesPerPurpose3.put("Employee Primary Sector", 0.0168);
				ratesPerPurpose3.put("Employee Construction", 0.168);
				ratesPerPurpose3.put("Employee Secondary Sector Rest", 0.0168);
				ratesPerPurpose3.put("Employee Retail", 0.0168);
				ratesPerPurpose3.put("Employee Traffic/Parcels", 0.097);
				ratesPerPurpose3.put("Employee Tertiary Sector Rest", 0.168);

				ratesPerPurpose4.put("Inhabitants", 0.002);
				ratesPerPurpose4.put("Employee", 0.0);
				ratesPerPurpose4.put("Employee Primary Sector", 0.025);
				ratesPerPurpose4.put("Employee Construction", 0.025);
				ratesPerPurpose4.put("Employee Secondary Sector Rest", 0.025);
				ratesPerPurpose4.put("Employee Retail", 0.025);
				ratesPerPurpose4.put("Employee Traffic/Parcels", 0.075);
				ratesPerPurpose4.put("Employee Tertiary Sector Rest", 0.025);

				ratesPerPurpose5.put("Inhabitants", 0.004);
				ratesPerPurpose5.put("Employee", 0.0);
				ratesPerPurpose5.put("Employee Primary Sector", 0.015);
				ratesPerPurpose5.put("Employee Construction", 0.002);
				ratesPerPurpose5.put("Employee Secondary Sector Rest", 0.015);
				ratesPerPurpose5.put("Employee Retail", 0.015);
				ratesPerPurpose5.put("Employee Traffic/Parcels", 0.02);
				ratesPerPurpose5.put("Employee Tertiary Sector Rest", 0.015);

			}
		} else if (smallScaleCommercialTrafficType.equals("goodsTraffic")) {
			if (generationType.equals("start")) {
				ratesPerPurpose1.put("Inhabitants", 0.0);
				ratesPerPurpose1.put("Employee", 0.0);
				ratesPerPurpose1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1.put("Employee Construction", 0.0);
				ratesPerPurpose1.put("Employee Secondary Sector Rest", 0.023);
				ratesPerPurpose1.put("Employee Retail", 0.0);
				ratesPerPurpose1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2.put("Inhabitants", 0.0);
				ratesPerPurpose2.put("Employee", 0.002);
				ratesPerPurpose2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose2.put("Employee Construction", 0.0);
				ratesPerPurpose2.put("Employee Secondary Sector Rest", 0.049);
				ratesPerPurpose2.put("Employee Retail", 0.0);
				ratesPerPurpose2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose3.put("Inhabitants", 0.0);
				ratesPerPurpose3.put("Employee", 0.002);
				ratesPerPurpose3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose3.put("Employee Construction", 0.0);
				ratesPerPurpose3.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose3.put("Employee Retail", 0.139);
				ratesPerPurpose3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose3.put("Employee Tertiary Sector Rest", 0.059);

				ratesPerPurpose4.put("Inhabitants", 0.0);
				ratesPerPurpose4.put("Employee", 0.002);
				ratesPerPurpose4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose4.put("Employee Construction", 0.0);
				ratesPerPurpose4.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose4.put("Employee Retail", 0.0);
				ratesPerPurpose4.put("Employee Traffic/Parcels", 0.333);
				ratesPerPurpose4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose5.put("Inhabitants", 0.0);
				ratesPerPurpose5.put("Employee", 0.002);
				ratesPerPurpose5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose5.put("Employee Construction", 0.220);
				ratesPerPurpose5.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose5.put("Employee Retail", 0.0);
				ratesPerPurpose5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose6.put("Inhabitants", 0.009);
				ratesPerPurpose6.put("Employee", 0.0);
				ratesPerPurpose6.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6.put("Employee Construction", 0.0);
				ratesPerPurpose6.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6.put("Employee Retail", 0.0);
				ratesPerPurpose6.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose6.put("Employee Tertiary Sector Rest", 0.0);

			} else if (generationType.equals("stop")) {
				ratesPerPurpose1.put("Inhabitants", 0.0);
				ratesPerPurpose1.put("Employee", 0.0);
				ratesPerPurpose1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1.put("Employee Construction", 0.0);
				ratesPerPurpose1.put("Employee Secondary Sector Rest", 0.031);
				ratesPerPurpose1.put("Employee Retail", 0.0);
				ratesPerPurpose1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2.put("Inhabitants", 0.001);
				ratesPerPurpose2.put("Employee", 0.0);
				ratesPerPurpose2.put("Employee Primary Sector", 0.001);
				ratesPerPurpose2.put("Employee Construction", 0.01);
				ratesPerPurpose2.put("Employee Secondary Sector Rest", 0.011);
				ratesPerPurpose2.put("Employee Retail", 0.021);
				ratesPerPurpose2.put("Employee Traffic/Parcels", 0.001);
				ratesPerPurpose2.put("Employee Tertiary Sector Rest", 0.001);

				ratesPerPurpose3.put("Inhabitants", 0.009);
				ratesPerPurpose3.put("Employee", 0.0);
				ratesPerPurpose3.put("Employee Primary Sector", 0.02);
				ratesPerPurpose3.put("Employee Construction", 0.005);
				ratesPerPurpose3.put("Employee Secondary Sector Rest", 0.029);
				ratesPerPurpose3.put("Employee Retail", 0.055);
				ratesPerPurpose3.put("Employee Traffic/Parcels", 0.02);
				ratesPerPurpose3.put("Employee Tertiary Sector Rest", 0.02);

				ratesPerPurpose4.put("Inhabitants", 0.014);
				ratesPerPurpose4.put("Employee", 0.0);
				ratesPerPurpose4.put("Employee Primary Sector", 0.02);
				ratesPerPurpose4.put("Employee Construction", 0.002);
				ratesPerPurpose4.put("Employee Secondary Sector Rest", 0.11);
				ratesPerPurpose4.put("Employee Retail", 0.154);
				ratesPerPurpose4.put("Employee Traffic/Parcels", 0.02);
				ratesPerPurpose4.put("Employee Tertiary Sector Rest", 0.02);

				ratesPerPurpose5.put("Inhabitants", 0.002);
				ratesPerPurpose5.put("Employee", 0.0);
				ratesPerPurpose5.put("Employee Primary Sector", 0.005);
				ratesPerPurpose5.put("Employee Construction", 0.002);
				ratesPerPurpose5.put("Employee Secondary Sector Rest", 0.01);
				ratesPerPurpose5.put("Employee Retail", 0.01);
				ratesPerPurpose5.put("Employee Traffic/Parcels", 0.005);
				ratesPerPurpose5.put("Employee Tertiary Sector Rest", 0.005);

				ratesPerPurpose6.put("Inhabitants", 0.002);
				ratesPerPurpose6.put("Employee", 0.0);
				ratesPerPurpose6.put("Employee Primary Sector", 0.005);
				ratesPerPurpose6.put("Employee Construction", 0.002);
				ratesPerPurpose6.put("Employee Secondary Sector Rest", 0.01);
				ratesPerPurpose6.put("Employee Retail", 0.01);
				ratesPerPurpose6.put("Employee Traffic/Parcels", 0.005);
				ratesPerPurpose6.put("Employee Tertiary Sector Rest", 0.005);
			}
			generationRates.put(6, ratesPerPurpose6);
		}
		generationRates.put(1, ratesPerPurpose1);
		generationRates.put(2, ratesPerPurpose2);
		generationRates.put(3, ratesPerPurpose3);
		generationRates.put(4, ratesPerPurpose4);
		generationRates.put(5, ratesPerPurpose5);
		return generationRates;
	}

	/**
	 * Sets the commitment rates based on the IVV 2005 for the goodsTraffic. The
	 * commitment rate for the commercialPersonTraffic is 1, because mode choice will be
	 * done in MATSim.
	 *
	 * @param smallScaleCommercialTrafficType used trafficType (freight or business traffic)
	 * @param commitmentType start or stop parameter
	 */
	static Map<String, Map<String, Double>> setCommitmentRates(String smallScaleCommercialTrafficType,
															   String commitmentType) {
		Map<String, Map<String, Double>> commitmentRates = new HashMap<>();

		if (smallScaleCommercialTrafficType.equals("goodsTraffic")) {

			// the first number is the purpose; second number the vehicle type
			Map<String, Double> ratesPerPurpose1_1 = new HashMap<>();
			Map<String, Double> ratesPerPurpose1_2 = new HashMap<>();
			Map<String, Double> ratesPerPurpose1_3 = new HashMap<>();
			Map<String, Double> ratesPerPurpose1_4 = new HashMap<>();
			Map<String, Double> ratesPerPurpose1_5 = new HashMap<>();
			Map<String, Double> ratesPerPurpose2_1 = new HashMap<>();
			Map<String, Double> ratesPerPurpose2_2 = new HashMap<>();
			Map<String, Double> ratesPerPurpose2_3 = new HashMap<>();
			Map<String, Double> ratesPerPurpose2_4 = new HashMap<>();
			Map<String, Double> ratesPerPurpose2_5 = new HashMap<>();
			Map<String, Double> ratesPerPurpose3_1 = new HashMap<>();
			Map<String, Double> ratesPerPurpose3_2 = new HashMap<>();
			Map<String, Double> ratesPerPurpose3_3 = new HashMap<>();
			Map<String, Double> ratesPerPurpose3_4 = new HashMap<>();
			Map<String, Double> ratesPerPurpose3_5 = new HashMap<>();
			Map<String, Double> ratesPerPurpose4_1 = new HashMap<>();
			Map<String, Double> ratesPerPurpose4_2 = new HashMap<>();
			Map<String, Double> ratesPerPurpose4_3 = new HashMap<>();
			Map<String, Double> ratesPerPurpose4_4 = new HashMap<>();
			Map<String, Double> ratesPerPurpose4_5 = new HashMap<>();
			Map<String, Double> ratesPerPurpose5_1 = new HashMap<>();
			Map<String, Double> ratesPerPurpose5_2 = new HashMap<>();
			Map<String, Double> ratesPerPurpose5_3 = new HashMap<>();
			Map<String, Double> ratesPerPurpose5_4 = new HashMap<>();
			Map<String, Double> ratesPerPurpose5_5 = new HashMap<>();
			Map<String, Double> ratesPerPurpose6_1 = new HashMap<>();
			Map<String, Double> ratesPerPurpose6_2 = new HashMap<>();
			Map<String, Double> ratesPerPurpose6_3 = new HashMap<>();
			Map<String, Double> ratesPerPurpose6_4 = new HashMap<>();
			Map<String, Double> ratesPerPurpose6_5 = new HashMap<>();
			if (commitmentType.equals("start")) {
				ratesPerPurpose1_1.put("Inhabitants", 0.0);
				ratesPerPurpose1_1.put("Employee", 0.8);
				ratesPerPurpose1_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_1.put("Employee Construction", 0.0);
				ratesPerPurpose1_1.put("Employee Secondary Sector Rest", 0.44);
				ratesPerPurpose1_1.put("Employee Retail", 0.0);
				ratesPerPurpose1_1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_2.put("Inhabitants", 0.0);
				ratesPerPurpose1_2.put("Employee", 0.1);
				ratesPerPurpose1_2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_2.put("Employee Construction", 0.0);
				ratesPerPurpose1_2.put("Employee Secondary Sector Rest", 0.11);
				ratesPerPurpose1_2.put("Employee Retail", 0.0);
				ratesPerPurpose1_2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_3.put("Inhabitants", 0.0);
				ratesPerPurpose1_3.put("Employee", 0.1);
				ratesPerPurpose1_3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_3.put("Employee Construction", 0.0);
				ratesPerPurpose1_3.put("Employee Secondary Sector Rest", 0.22);
				ratesPerPurpose1_3.put("Employee Retail", 0.0);
				ratesPerPurpose1_3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_3.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_4.put("Inhabitants", 0.0);
				ratesPerPurpose1_4.put("Employee", 0.0);
				ratesPerPurpose1_4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_4.put("Employee Construction", 0.0);
				ratesPerPurpose1_4.put("Employee Secondary Sector Rest", 0.06);
				ratesPerPurpose1_4.put("Employee Retail", 0.0);
				ratesPerPurpose1_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_5.put("Inhabitants", 0.0);
				ratesPerPurpose1_5.put("Employee", 0.0);
				ratesPerPurpose1_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_5.put("Employee Construction", 0.0);
				ratesPerPurpose1_5.put("Employee Secondary Sector Rest", 0.16);
				ratesPerPurpose1_5.put("Employee Retail", 0.0);
				ratesPerPurpose1_5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_5.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2_1.put("Inhabitants", 0.0);
				ratesPerPurpose2_1.put("Employee", 0.8);
				ratesPerPurpose2_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose2_1.put("Employee Construction", 0.0);
				ratesPerPurpose2_1.put("Employee Secondary Sector Rest", 0.44);
				ratesPerPurpose2_1.put("Employee Retail", 0.0);
				ratesPerPurpose2_1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2_1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2_2.put("Inhabitants", 0.0);
				ratesPerPurpose2_2.put("Employee", 0.1);
				ratesPerPurpose2_2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose2_2.put("Employee Construction", 0.0);
				ratesPerPurpose2_2.put("Employee Secondary Sector Rest", 0.11);
				ratesPerPurpose2_2.put("Employee Retail", 0.0);
				ratesPerPurpose2_2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2_2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2_3.put("Inhabitants", 0.0);
				ratesPerPurpose2_3.put("Employee", 0.1);
				ratesPerPurpose2_3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose2_3.put("Employee Construction", 0.0);
				ratesPerPurpose2_3.put("Employee Secondary Sector Rest", 0.22);
				ratesPerPurpose2_3.put("Employee Retail", 0.0);
				ratesPerPurpose2_3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2_3.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2_4.put("Inhabitants", 0.0);
				ratesPerPurpose2_4.put("Employee", 0.0);
				ratesPerPurpose2_4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose2_4.put("Employee Construction", 0.0);
				ratesPerPurpose2_4.put("Employee Secondary Sector Rest", 0.06);
				ratesPerPurpose2_4.put("Employee Retail", 0.0);
				ratesPerPurpose2_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2_4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2_5.put("Inhabitants", 0.0);
				ratesPerPurpose2_5.put("Employee", 0.0);
				ratesPerPurpose2_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose2_5.put("Employee Construction", 0.0);
				ratesPerPurpose2_5.put("Employee Secondary Sector Rest", 0.16);
				ratesPerPurpose2_5.put("Employee Retail", 0.0);
				ratesPerPurpose2_5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2_5.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose3_1.put("Inhabitants", 0.0);
				ratesPerPurpose3_1.put("Employee", 0.8);
				ratesPerPurpose3_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose3_1.put("Employee Construction", 0.0);
				ratesPerPurpose3_1.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose3_1.put("Employee Retail", 0.46);
				ratesPerPurpose3_1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose3_1.put("Employee Tertiary Sector Rest", 0.54);

				ratesPerPurpose3_2.put("Inhabitants", 0.0);
				ratesPerPurpose3_2.put("Employee", 0.1);
				ratesPerPurpose3_2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose3_2.put("Employee Construction", 0.0);
				ratesPerPurpose3_2.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose3_2.put("Employee Retail", 0.1);
				ratesPerPurpose3_2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose3_2.put("Employee Tertiary Sector Rest", 0.1);

				ratesPerPurpose3_3.put("Inhabitants", 0.0);
				ratesPerPurpose3_3.put("Employee", 0.1);
				ratesPerPurpose3_3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose3_3.put("Employee Construction", 0.0);
				ratesPerPurpose3_3.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose3_3.put("Employee Retail", 0.23);
				ratesPerPurpose3_3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose3_3.put("Employee Tertiary Sector Rest", 0.2);

				ratesPerPurpose3_4.put("Inhabitants", 0.0);
				ratesPerPurpose3_4.put("Employee", 0.0);
				ratesPerPurpose3_4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose3_4.put("Employee Construction", 0.0);
				ratesPerPurpose3_4.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose3_4.put("Employee Retail", 0.06);
				ratesPerPurpose3_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose3_4.put("Employee Tertiary Sector Rest", 0.02);

				ratesPerPurpose3_5.put("Inhabitants", 0.0);
				ratesPerPurpose3_5.put("Employee", 0.0);
				ratesPerPurpose3_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose3_5.put("Employee Construction", 0.0);
				ratesPerPurpose3_5.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose3_5.put("Employee Retail", 0.15);
				ratesPerPurpose3_5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose3_5.put("Employee Tertiary Sector Rest", 0.14);

				ratesPerPurpose4_1.put("Inhabitants", 0.009);
				ratesPerPurpose4_1.put("Employee", 0.8);
				ratesPerPurpose4_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose4_1.put("Employee Construction", 0.0);
				ratesPerPurpose4_1.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose4_1.put("Employee Retail", 0.0);
				ratesPerPurpose4_1.put("Employee Traffic/Parcels", 0.18);
				ratesPerPurpose4_1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose4_2.put("Inhabitants", 0.0);
				ratesPerPurpose4_2.put("Employee", 0.1);
				ratesPerPurpose4_2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose4_2.put("Employee Construction", 0.0);
				ratesPerPurpose4_2.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose4_2.put("Employee Retail", 0.0);
				ratesPerPurpose4_2.put("Employee Traffic/Parcels", 0.06);
				ratesPerPurpose4_2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose4_3.put("Inhabitants", 0.0);
				ratesPerPurpose4_3.put("Employee", 0.1);
				ratesPerPurpose4_3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose4_3.put("Employee Construction", 0.0);
				ratesPerPurpose4_3.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose4_3.put("Employee Retail", 0.0);
				ratesPerPurpose4_3.put("Employee Traffic/Parcels", 0.25);
				ratesPerPurpose4_3.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose4_4.put("Inhabitants", 0.0);
				ratesPerPurpose4_4.put("Employee", 0.0);
				ratesPerPurpose4_4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose4_4.put("Employee Construction", 0.0);
				ratesPerPurpose4_4.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose4_4.put("Employee Retail", 0.0);
				ratesPerPurpose4_4.put("Employee Traffic/Parcels", 0.08);
				ratesPerPurpose4_4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose4_5.put("Inhabitants", 0.0);
				ratesPerPurpose4_5.put("Employee", 0.0);
				ratesPerPurpose4_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose4_5.put("Employee Construction", 0.0);
				ratesPerPurpose4_5.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose4_5.put("Employee Retail", 0.0);
				ratesPerPurpose4_5.put("Employee Traffic/Parcels", 0.43);
				ratesPerPurpose4_5.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose5_1.put("Inhabitants", 0.0);
				ratesPerPurpose5_1.put("Employee", 0.8);
				ratesPerPurpose5_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose5_1.put("Employee Construction", 0.25);
				ratesPerPurpose5_1.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose5_1.put("Employee Retail", 0.0);
				ratesPerPurpose5_1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5_1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose5_2.put("Inhabitants", 0.0);
				ratesPerPurpose5_2.put("Employee", 0.1);
				ratesPerPurpose5_2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose5_2.put("Employee Construction", 0.2);
				ratesPerPurpose5_2.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose5_2.put("Employee Retail", 0.0);
				ratesPerPurpose5_2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5_2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose5_3.put("Inhabitants", 0.0);
				ratesPerPurpose5_3.put("Employee", 0.1);
				ratesPerPurpose5_3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose5_3.put("Employee Construction", 0.25);
				ratesPerPurpose5_3.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose5_3.put("Employee Retail", 0.139);
				ratesPerPurpose5_3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5_3.put("Employee Tertiary Sector Rest", 0.059);

				ratesPerPurpose5_4.put("Inhabitants", 0.0);
				ratesPerPurpose5_4.put("Employee", 0.0);
				ratesPerPurpose5_4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose5_4.put("Employee Construction", 0.02);
				ratesPerPurpose5_4.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose5_4.put("Employee Retail", 0.0);
				ratesPerPurpose5_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5_4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose5_5.put("Inhabitants", 0.0);
				ratesPerPurpose5_5.put("Employee", 0.0);
				ratesPerPurpose5_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose5_5.put("Employee Construction", 0.28);
				ratesPerPurpose5_5.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose5_5.put("Employee Retail", 0.0);
				ratesPerPurpose5_5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5_5.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose6_1.put("Inhabitants", 0.0);
				ratesPerPurpose6_1.put("Employee", 0.0);
				ratesPerPurpose6_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6_1.put("Employee Construction", 0.0);
				ratesPerPurpose6_1.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6_1.put("Employee Retail", 0.0);
				ratesPerPurpose6_1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose6_1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose6_2.put("Inhabitants", 0.29);
				ratesPerPurpose6_2.put("Employee", 0.0);
				ratesPerPurpose6_2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6_2.put("Employee Construction", 0.0);
				ratesPerPurpose6_2.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6_2.put("Employee Retail", 0.0);
				ratesPerPurpose6_2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose6_2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose6_3.put("Inhabitants", 0.63);
				ratesPerPurpose6_3.put("Employee", 0.0);
				ratesPerPurpose6_3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6_3.put("Employee Construction", 0.0);
				ratesPerPurpose6_3.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6_3.put("Employee Retail", 0.0);
				ratesPerPurpose6_3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose6_3.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose6_4.put("Inhabitants", 0.07);
				ratesPerPurpose6_4.put("Employee", 0.0);
				ratesPerPurpose6_4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6_4.put("Employee Construction", 0.0);
				ratesPerPurpose6_4.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6_4.put("Employee Retail", 0.0);
				ratesPerPurpose6_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose6_4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose6_5.put("Inhabitants", 0.001);
				ratesPerPurpose6_5.put("Employee", 0.0);
				ratesPerPurpose6_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6_5.put("Employee Construction", 0.2);
				ratesPerPurpose6_5.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6_5.put("Employee Retail", 0.0);
				ratesPerPurpose6_5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose6_5.put("Employee Tertiary Sector Rest", 0.0);
			} else if (commitmentType.equals("stop")) {
				ratesPerPurpose1_1.put("Inhabitants", 0.0);
				ratesPerPurpose1_1.put("Employee", 0.0);
				ratesPerPurpose1_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_1.put("Employee Construction", 0.0);
				ratesPerPurpose1_1.put("Employee Secondary Sector Rest", 0.35);
				ratesPerPurpose1_1.put("Employee Retail", 0.0);
				ratesPerPurpose1_1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_2.put("Inhabitants", 0.0);
				ratesPerPurpose1_2.put("Employee", 0.0);
				ratesPerPurpose1_2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_2.put("Employee Construction", 0.0);
				ratesPerPurpose1_2.put("Employee Secondary Sector Rest", 0.1);
				ratesPerPurpose1_2.put("Employee Retail", 0.0);
				ratesPerPurpose1_2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_3.put("Inhabitants", 0.0);
				ratesPerPurpose1_3.put("Employee", 0.0);
				ratesPerPurpose1_3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_3.put("Employee Construction", 0.0);
				ratesPerPurpose1_3.put("Employee Secondary Sector Rest", 0.27);
				ratesPerPurpose1_3.put("Employee Retail", 0.0);
				ratesPerPurpose1_3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_3.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_4.put("Inhabitants", 0.0);
				ratesPerPurpose1_4.put("Employee", 0.0);
				ratesPerPurpose1_4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_4.put("Employee Construction", 0.0);
				ratesPerPurpose1_4.put("Employee Secondary Sector Rest", 0.01);
				ratesPerPurpose1_4.put("Employee Retail", 0.0);
				ratesPerPurpose1_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_5.put("Inhabitants", 0.0);
				ratesPerPurpose1_5.put("Employee", 0.0);
				ratesPerPurpose1_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_5.put("Employee Construction", 0.0);
				ratesPerPurpose1_5.put("Employee Secondary Sector Rest", 0.27);
				ratesPerPurpose1_5.put("Employee Retail", 0.0);
				ratesPerPurpose1_5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_5.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2_1.put("Inhabitants", 0.55);
				ratesPerPurpose2_1.put("Employee", 0.0);
				ratesPerPurpose2_1.put("Employee Primary Sector", 0.46);
				ratesPerPurpose2_1.put("Employee Construction", 0.46);
				ratesPerPurpose2_1.put("Employee Secondary Sector Rest", 0.46);
				ratesPerPurpose2_1.put("Employee Retail", 0.46);
				ratesPerPurpose2_1.put("Employee Traffic/Parcels", 0.34);
				ratesPerPurpose2_1.put("Employee Tertiary Sector Rest", 0.46);

				ratesPerPurpose2_2.put("Inhabitants", 0.09);
				ratesPerPurpose2_2.put("Employee", 0.0);
				ratesPerPurpose2_2.put("Employee Primary Sector", 0.09);
				ratesPerPurpose2_2.put("Employee Construction", 0.09);
				ratesPerPurpose2_2.put("Employee Secondary Sector Rest", 0.09);
				ratesPerPurpose2_2.put("Employee Retail", 0.09);
				ratesPerPurpose2_2.put("Employee Traffic/Parcels", 0.1);
				ratesPerPurpose2_2.put("Employee Tertiary Sector Rest", 0.09);

				ratesPerPurpose2_3.put("Inhabitants", 0.21);
				ratesPerPurpose2_3.put("Employee", 0.0);
				ratesPerPurpose2_3.put("Employee Primary Sector", 0.22);
				ratesPerPurpose2_3.put("Employee Construction", 0.22);
				ratesPerPurpose2_3.put("Employee Secondary Sector Rest", 0.22);
				ratesPerPurpose2_3.put("Employee Retail", 0.22);
				ratesPerPurpose2_3.put("Employee Traffic/Parcels", 0.29);
				ratesPerPurpose2_3.put("Employee Tertiary Sector Rest", 0.22);

				ratesPerPurpose2_4.put("Inhabitants", 0.06);
				ratesPerPurpose2_4.put("Employee", 0.0);
				ratesPerPurpose2_4.put("Employee Primary Sector", 0.06);
				ratesPerPurpose2_4.put("Employee Construction", 0.06);
				ratesPerPurpose2_4.put("Employee Secondary Sector Rest", 0.06);
				ratesPerPurpose2_4.put("Employee Retail", 0.06);
				ratesPerPurpose2_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2_4.put("Employee Tertiary Sector Rest", 0.06);

				ratesPerPurpose2_5.put("Inhabitants", 0.1);
				ratesPerPurpose2_5.put("Employee", 0.0);
				ratesPerPurpose2_5.put("Employee Primary Sector", 0.17);
				ratesPerPurpose2_5.put("Employee Construction", 0.17);
				ratesPerPurpose2_5.put("Employee Secondary Sector Rest", 0.17);
				ratesPerPurpose2_5.put("Employee Retail", 0.17);
				ratesPerPurpose2_5.put("Employee Traffic/Parcels", 0.27);
				ratesPerPurpose2_5.put("Employee Tertiary Sector Rest", 0.17);

				ratesPerPurpose3_1.put("Inhabitants", 0.489);
				ratesPerPurpose3_1.put("Employee", 0.0);
				ratesPerPurpose3_1.put("Employee Primary Sector", 0.538);
				ratesPerPurpose3_1.put("Employee Construction", 0.538);
				ratesPerPurpose3_1.put("Employee Secondary Sector Rest", 0.538);
				ratesPerPurpose3_1.put("Employee Retail", 0.538);
				ratesPerPurpose3_1.put("Employee Traffic/Parcels", 0.59);
				ratesPerPurpose3_1.put("Employee Tertiary Sector Rest", 0.538);

				ratesPerPurpose3_2.put("Inhabitants", 0.106);
				ratesPerPurpose3_2.put("Employee", 0.0);
				ratesPerPurpose3_2.put("Employee Primary Sector", 0.092);
				ratesPerPurpose3_2.put("Employee Construction", 0.092);
				ratesPerPurpose3_2.put("Employee Secondary Sector Rest", 0.092);
				ratesPerPurpose3_2.put("Employee Retail", 0.092);
				ratesPerPurpose3_2.put("Employee Traffic/Parcels", 0.03);
				ratesPerPurpose3_2.put("Employee Tertiary Sector Rest", 0.092);

				ratesPerPurpose3_3.put("Inhabitants", 0.26);
				ratesPerPurpose3_3.put("Employee", 0.0);
				ratesPerPurpose3_3.put("Employee Primary Sector", 0.19);
				ratesPerPurpose3_3.put("Employee Construction", 0.19);
				ratesPerPurpose3_3.put("Employee Secondary Sector Rest", 0.19);
				ratesPerPurpose3_3.put("Employee Retail", 0.19);
				ratesPerPurpose3_3.put("Employee Traffic/Parcels", 0.102);
				ratesPerPurpose3_3.put("Employee Tertiary Sector Rest", 0.19);

				ratesPerPurpose3_4.put("Inhabitants", 0.033);
				ratesPerPurpose3_4.put("Employee", 0.0);
				ratesPerPurpose3_4.put("Employee Primary Sector", 0.032);
				ratesPerPurpose3_4.put("Employee Construction", 0.032);
				ratesPerPurpose3_4.put("Employee Secondary Sector Rest", 0.032);
				ratesPerPurpose3_4.put("Employee Retail", 0.032);
				ratesPerPurpose3_4.put("Employee Traffic/Parcels", 0.058);
				ratesPerPurpose3_4.put("Employee Tertiary Sector Rest", 0.032);

				ratesPerPurpose3_5.put("Inhabitants", 0.112);
				ratesPerPurpose3_5.put("Employee", 0.0);
				ratesPerPurpose3_5.put("Employee Primary Sector", 0.147);
				ratesPerPurpose3_5.put("Employee Construction", 0.147);
				ratesPerPurpose3_5.put("Employee Secondary Sector Rest", 0.147);
				ratesPerPurpose3_5.put("Employee Retail", 0.147);
				ratesPerPurpose3_5.put("Employee Traffic/Parcels", 0.219);
				ratesPerPurpose3_5.put("Employee Tertiary Sector Rest", 0.147);

				ratesPerPurpose4_1.put("Inhabitants", 0.37);
				ratesPerPurpose4_1.put("Employee", 0.0);
				ratesPerPurpose4_1.put("Employee Primary Sector", 0.14);
				ratesPerPurpose4_1.put("Employee Construction", 0.14);
				ratesPerPurpose4_1.put("Employee Secondary Sector Rest", 0.14);
				ratesPerPurpose4_1.put("Employee Retail", 0.14);
				ratesPerPurpose4_1.put("Employee Traffic/Parcels", 0.06);
				ratesPerPurpose4_1.put("Employee Tertiary Sector Rest", 0.14);

				ratesPerPurpose4_2.put("Inhabitants", 0.05);
				ratesPerPurpose4_2.put("Employee", 0.0);
				ratesPerPurpose4_2.put("Employee Primary Sector", 0.07);
				ratesPerPurpose4_2.put("Employee Construction", 0.07);
				ratesPerPurpose4_2.put("Employee Secondary Sector Rest", 0.07);
				ratesPerPurpose4_2.put("Employee Retail", 0.07);
				ratesPerPurpose4_2.put("Employee Traffic/Parcels", 0.07);
				ratesPerPurpose4_2.put("Employee Tertiary Sector Rest", 0.07);

				ratesPerPurpose4_3.put("Inhabitants", 0.4);
				ratesPerPurpose4_3.put("Employee", 0.0);
				ratesPerPurpose4_3.put("Employee Primary Sector", 0.21);
				ratesPerPurpose4_3.put("Employee Construction", 0.21);
				ratesPerPurpose4_3.put("Employee Secondary Sector Rest", 0.21);
				ratesPerPurpose4_3.put("Employee Retail", 0.21);
				ratesPerPurpose4_3.put("Employee Traffic/Parcels", 0.19);
				ratesPerPurpose4_3.put("Employee Tertiary Sector Rest", 0.21);

				ratesPerPurpose4_4.put("Inhabitants", 0.13);
				ratesPerPurpose4_4.put("Employee", 0.0);
				ratesPerPurpose4_4.put("Employee Primary Sector", 0.05);
				ratesPerPurpose4_4.put("Employee Construction", 0.05);
				ratesPerPurpose4_4.put("Employee Secondary Sector Rest", 0.05);
				ratesPerPurpose4_4.put("Employee Retail", 0.05);
				ratesPerPurpose4_4.put("Employee Traffic/Parcels", 0.08);
				ratesPerPurpose4_4.put("Employee Tertiary Sector Rest", 0.05);

				ratesPerPurpose4_5.put("Inhabitants", 0.05);
				ratesPerPurpose4_5.put("Employee", 0.0);
				ratesPerPurpose4_5.put("Employee Primary Sector", 0.54);
				ratesPerPurpose4_5.put("Employee Construction", 0.54);
				ratesPerPurpose4_5.put("Employee Secondary Sector Rest", 0.54);
				ratesPerPurpose4_5.put("Employee Retail", 0.54);
				ratesPerPurpose4_5.put("Employee Traffic/Parcels", 0.61);
				ratesPerPurpose4_5.put("Employee Tertiary Sector Rest", 0.54);

				ratesPerPurpose5_1.put("Inhabitants", 0.16);
				ratesPerPurpose5_1.put("Employee", 0.0);
				ratesPerPurpose5_1.put("Employee Primary Sector", 0.4);
				ratesPerPurpose5_1.put("Employee Construction", 0.4);
				ratesPerPurpose5_1.put("Employee Secondary Sector Rest", 0.4);
				ratesPerPurpose5_1.put("Employee Retail", 0.4);
				ratesPerPurpose5_1.put("Employee Traffic/Parcels", 0.14);
				ratesPerPurpose5_1.put("Employee Tertiary Sector Rest", 0.4);

				ratesPerPurpose5_2.put("Inhabitants", 0.55);
				ratesPerPurpose5_2.put("Employee", 0.11);
				ratesPerPurpose5_2.put("Employee Primary Sector", 0.11);
				ratesPerPurpose5_2.put("Employee Construction", 0.11);
				ratesPerPurpose5_2.put("Employee Secondary Sector Rest", 0.11);
				ratesPerPurpose5_2.put("Employee Retail", 0.11);
				ratesPerPurpose5_2.put("Employee Traffic/Parcels", 0.06);
				ratesPerPurpose5_2.put("Employee Tertiary Sector Rest", 0.11);

				ratesPerPurpose5_3.put("Inhabitants", 0.22);
				ratesPerPurpose5_3.put("Employee", 0.0);
				ratesPerPurpose5_3.put("Employee Primary Sector", 0.17);
				ratesPerPurpose5_3.put("Employee Construction", 0.17);
				ratesPerPurpose5_3.put("Employee Secondary Sector Rest", 0.17);
				ratesPerPurpose5_3.put("Employee Retail", 0.17);
				ratesPerPurpose5_3.put("Employee Traffic/Parcels", 0.21);
				ratesPerPurpose5_3.put("Employee Tertiary Sector Rest", 0.17);

				ratesPerPurpose5_4.put("Inhabitants", 0.0);
				ratesPerPurpose5_4.put("Employee", 0.0);
				ratesPerPurpose5_4.put("Employee Primary Sector", 0.04);
				ratesPerPurpose5_4.put("Employee Construction", 0.04);
				ratesPerPurpose5_4.put("Employee Secondary Sector Rest", 0.04);
				ratesPerPurpose5_4.put("Employee Retail", 0.04);
				ratesPerPurpose5_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5_4.put("Employee Tertiary Sector Rest", 0.04);

				ratesPerPurpose5_5.put("Inhabitants", 0.06);
				ratesPerPurpose5_5.put("Employee", 0.0);
				ratesPerPurpose5_5.put("Employee Primary Sector", 0.28);
				ratesPerPurpose5_5.put("Employee Construction", 0.28);
				ratesPerPurpose5_5.put("Employee Secondary Sector Rest", 0.28);
				ratesPerPurpose5_5.put("Employee Retail", 0.28);
				ratesPerPurpose5_5.put("Employee Traffic/Parcels", 0.58);
				ratesPerPurpose5_5.put("Employee Tertiary Sector Rest", 0.28);

				ratesPerPurpose6_1.put("Inhabitants", 0.0);
				ratesPerPurpose6_1.put("Employee", 0.0);
				ratesPerPurpose6_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6_1.put("Employee Construction", 0.0);
				ratesPerPurpose6_1.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6_1.put("Employee Retail", 0.0);
				ratesPerPurpose6_1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose6_1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose6_2.put("Inhabitants", 0.85);
				ratesPerPurpose6_2.put("Employee", 0.0);
				ratesPerPurpose6_2.put("Employee Primary Sector", 0.21);
				ratesPerPurpose6_2.put("Employee Construction", 0.21);
				ratesPerPurpose6_2.put("Employee Secondary Sector Rest", 0.21);
				ratesPerPurpose6_2.put("Employee Retail", 0.21);
				ratesPerPurpose6_2.put("Employee Traffic/Parcels", 0.09);
				ratesPerPurpose6_2.put("Employee Tertiary Sector Rest", 0.21);

				ratesPerPurpose6_3.put("Inhabitants", 0.15);
				ratesPerPurpose6_3.put("Employee", 0.0);
				ratesPerPurpose6_3.put("Employee Primary Sector", 0.58);
				ratesPerPurpose6_3.put("Employee Construction", 0.58);
				ratesPerPurpose6_3.put("Employee Secondary Sector Rest", 0.58);
				ratesPerPurpose6_3.put("Employee Retail", 0.58);
				ratesPerPurpose6_3.put("Employee Traffic/Parcels", 0.55);
				ratesPerPurpose6_3.put("Employee Tertiary Sector Rest", 0.58);

				ratesPerPurpose6_4.put("Inhabitants", 0.0);
				ratesPerPurpose6_4.put("Employee", 0.0);
				ratesPerPurpose6_4.put("Employee Primary Sector", 0.21);
				ratesPerPurpose6_4.put("Employee Construction", 0.21);
				ratesPerPurpose6_4.put("Employee Secondary Sector Rest", 0.21);
				ratesPerPurpose6_4.put("Employee Retail", 0.21);
				ratesPerPurpose6_4.put("Employee Traffic/Parcels", 0.25);
				ratesPerPurpose6_4.put("Employee Tertiary Sector Rest", 0.21);

				ratesPerPurpose6_5.put("Inhabitants", 0.0);
				ratesPerPurpose6_5.put("Employee", 0.0);
				ratesPerPurpose6_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6_5.put("Employee Construction", 0.0);
				ratesPerPurpose6_5.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6_5.put("Employee Retail", 0.0);
				ratesPerPurpose6_5.put("Employee Traffic/Parcels", 0.11);
				ratesPerPurpose6_5.put("Employee Tertiary Sector Rest", 0.0);
			}
			commitmentRates.put("1_1", ratesPerPurpose1_1);
			commitmentRates.put("1_2", ratesPerPurpose1_2);
			commitmentRates.put("1_3", ratesPerPurpose1_3);
			commitmentRates.put("1_4", ratesPerPurpose1_4);
			commitmentRates.put("1_5", ratesPerPurpose1_5);
			commitmentRates.put("2_1", ratesPerPurpose2_1);
			commitmentRates.put("2_2", ratesPerPurpose2_2);
			commitmentRates.put("2_3", ratesPerPurpose2_3);
			commitmentRates.put("2_4", ratesPerPurpose2_4);
			commitmentRates.put("2_5", ratesPerPurpose2_5);
			commitmentRates.put("3_1", ratesPerPurpose3_1);
			commitmentRates.put("3_2", ratesPerPurpose3_2);
			commitmentRates.put("3_3", ratesPerPurpose3_3);
			commitmentRates.put("3_4", ratesPerPurpose3_4);
			commitmentRates.put("3_5", ratesPerPurpose3_5);
			commitmentRates.put("4_1", ratesPerPurpose4_1);
			commitmentRates.put("4_2", ratesPerPurpose4_2);
			commitmentRates.put("4_3", ratesPerPurpose4_3);
			commitmentRates.put("4_4", ratesPerPurpose4_4);
			commitmentRates.put("4_5", ratesPerPurpose4_5);
			commitmentRates.put("5_1", ratesPerPurpose5_1);
			commitmentRates.put("5_2", ratesPerPurpose5_2);
			commitmentRates.put("5_3", ratesPerPurpose5_3);
			commitmentRates.put("5_4", ratesPerPurpose5_4);
			commitmentRates.put("5_5", ratesPerPurpose5_5);
			commitmentRates.put("6_1", ratesPerPurpose6_1);
			commitmentRates.put("6_2", ratesPerPurpose6_2);
			commitmentRates.put("6_3", ratesPerPurpose6_3);
			commitmentRates.put("6_4", ratesPerPurpose6_4);
			commitmentRates.put("6_5", ratesPerPurpose6_5);
		}
		return commitmentRates;
	}
}
