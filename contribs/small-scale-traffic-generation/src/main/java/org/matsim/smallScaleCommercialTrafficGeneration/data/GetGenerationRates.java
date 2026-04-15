package org.matsim.smallScaleCommercialTrafficGeneration.data;

import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand;
import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.StructuralAttribute;

import java.util.*;

public interface GetGenerationRates {
	/**
	 * Sets the generation rates based on the IVV 2005
	 *
	 * @param smallScaleCommercialTrafficType used trafficType (freight or business traffic)
	 * @param generationType                  start or stop rates
	 */
	static Map<Integer, Map<StructuralAttribute, Double>> setGenerationRates(
		GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType,
		String generationType) {

		Map<Integer, Map<StructuralAttribute, Double>> generationRates = new HashMap<>();
		Map<StructuralAttribute, Double> ratesPerPurpose1 = new EnumMap<>(StructuralAttribute.class);
		Map<StructuralAttribute, Double> ratesPerPurpose2 = new EnumMap<>(StructuralAttribute.class);
		Map<StructuralAttribute, Double> ratesPerPurpose3 = new EnumMap<>(StructuralAttribute.class);
		Map<StructuralAttribute, Double> ratesPerPurpose4 = new EnumMap<>(StructuralAttribute.class);
		Map<StructuralAttribute, Double> ratesPerPurpose5 = new EnumMap<>(StructuralAttribute.class);
		Map<StructuralAttribute, Double> ratesPerPurpose6 = new EnumMap<>(StructuralAttribute.class);
		if (smallScaleCommercialTrafficType.equals(
			GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.commercialPersonTraffic)) {
			if (generationType.equals("start")) {

				ratesPerPurpose1.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.059);

				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE, 0.029);
				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.045);

				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE, 0.021);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.0192);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.184);

				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE, 0.021);
				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.203);

				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE, 0.03);
				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.29);

			} else if (generationType.equals("stop")) {

				ratesPerPurpose1.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.02);

				ratesPerPurpose2.put(StructuralAttribute.INHABITANTS, 0.002);
				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.029);
				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.029);
				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.009);
				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.029);
				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.039);
				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.029);

				ratesPerPurpose3.put(StructuralAttribute.INHABITANTS, 0.025);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.0168);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.168);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.0168);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.0168);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.097);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.168);

				ratesPerPurpose4.put(StructuralAttribute.INHABITANTS, 0.002);
				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.025);
				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.025);
				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.025);
				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.025);
				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.075);
				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.025);

				ratesPerPurpose5.put(StructuralAttribute.INHABITANTS, 0.004);
				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.015);
				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.002);
				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.015);
				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.015);
				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.02);
				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.015);
			}

		} else if (smallScaleCommercialTrafficType
			.equals(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic)) {

			if (generationType.equals("start")) {

				ratesPerPurpose1.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.023);

				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE, 0.002);
				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.049);

				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE, 0.002);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.139);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.059);

				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE, 0.002);
				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.333);

				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE, 0.002);
				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.220);

				ratesPerPurpose6.put(StructuralAttribute.INHABITANTS, 0.009);

			} else if (generationType.equals("stop")) {

				ratesPerPurpose1.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.031);

				ratesPerPurpose2.put(StructuralAttribute.INHABITANTS, 0.001);
				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.001);
				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.01);
				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.011);
				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.021);
				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.001);
				ratesPerPurpose2.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.001);

				ratesPerPurpose3.put(StructuralAttribute.INHABITANTS, 0.009);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.02);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.005);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.029);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.055);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.02);
				ratesPerPurpose3.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.02);

				ratesPerPurpose4.put(StructuralAttribute.INHABITANTS, 0.014);
				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.02);
				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.002);
				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.11);
				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.154);
				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.02);
				ratesPerPurpose4.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.02);

				ratesPerPurpose5.put(StructuralAttribute.INHABITANTS, 0.002);
				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.005);
				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.002);
				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.01);
				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.01);
				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.005);
				ratesPerPurpose5.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.005);

				ratesPerPurpose6.put(StructuralAttribute.INHABITANTS, 0.002);
				ratesPerPurpose6.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.005);
				ratesPerPurpose6.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.002);
				ratesPerPurpose6.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.01);
				ratesPerPurpose6.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.01);
				ratesPerPurpose6.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.005);
				ratesPerPurpose6.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.005);
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
	 * @param commitmentType                  start or stop parameter
	 */
	static Map<String, Map<StructuralAttribute, Double>> setCommitmentRates(
		GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType,
		String commitmentType) {
		Map<String, Map<StructuralAttribute, Double>> commitmentRates = new HashMap<>();

		if (smallScaleCommercialTrafficType.equals(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.goodsTraffic)) {

			// the first number is the purpose; second number the vehicle type
			Map<StructuralAttribute, Double> ratesPerPurpose1_1 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose1_2 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose1_3 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose1_4 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose1_5 = new EnumMap<>(StructuralAttribute.class);

			Map<StructuralAttribute, Double> ratesPerPurpose2_1 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose2_2 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose2_3 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose2_4 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose2_5 = new EnumMap<>(StructuralAttribute.class);

			Map<StructuralAttribute, Double> ratesPerPurpose3_1 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose3_2 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose3_3 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose3_4 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose3_5 = new EnumMap<>(StructuralAttribute.class);

			Map<StructuralAttribute, Double> ratesPerPurpose4_1 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose4_2 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose4_3 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose4_4 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose4_5 = new EnumMap<>(StructuralAttribute.class);

			Map<StructuralAttribute, Double> ratesPerPurpose5_1 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose5_2 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose5_3 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose5_4 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose5_5 = new EnumMap<>(StructuralAttribute.class);

			Map<StructuralAttribute, Double> ratesPerPurpose6_1 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose6_2 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose6_3 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose6_4 = new EnumMap<>(StructuralAttribute.class);
			Map<StructuralAttribute, Double> ratesPerPurpose6_5 = new EnumMap<>(StructuralAttribute.class);

			if (commitmentType.equals("start")) {

				ratesPerPurpose1_1.put(StructuralAttribute.EMPLOYEE, 0.8);
				ratesPerPurpose1_1.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.44);

				ratesPerPurpose1_2.put(StructuralAttribute.EMPLOYEE, 0.1);
				ratesPerPurpose1_2.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.11);

				ratesPerPurpose1_3.put(StructuralAttribute.EMPLOYEE, 0.1);
				ratesPerPurpose1_3.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.22);

				ratesPerPurpose1_4.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.06);

				ratesPerPurpose1_5.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.16);

				ratesPerPurpose2_1.put(StructuralAttribute.EMPLOYEE, 0.8);
				ratesPerPurpose2_1.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.44);

				ratesPerPurpose2_2.put(StructuralAttribute.EMPLOYEE, 0.1);
				ratesPerPurpose2_2.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.11);

				ratesPerPurpose2_3.put(StructuralAttribute.EMPLOYEE, 0.1);
				ratesPerPurpose2_3.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.22);

				ratesPerPurpose2_4.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.06);

				ratesPerPurpose2_5.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.16);

				ratesPerPurpose3_1.put(StructuralAttribute.EMPLOYEE, 0.8);
				ratesPerPurpose3_1.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.46);
				ratesPerPurpose3_1.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.54);

				ratesPerPurpose3_2.put(StructuralAttribute.EMPLOYEE, 0.1);
				ratesPerPurpose3_2.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.1);
				ratesPerPurpose3_2.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.1);

				ratesPerPurpose3_3.put(StructuralAttribute.EMPLOYEE, 0.1);
				ratesPerPurpose3_3.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.23);
				ratesPerPurpose3_3.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.2);

				ratesPerPurpose3_4.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.06);
				ratesPerPurpose3_4.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.02);

				ratesPerPurpose3_5.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.15);
				ratesPerPurpose3_5.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.14);

				ratesPerPurpose4_1.put(StructuralAttribute.INHABITANTS, 0.009);
				ratesPerPurpose4_1.put(StructuralAttribute.EMPLOYEE, 0.8);
				ratesPerPurpose4_1.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.18);

				ratesPerPurpose4_2.put(StructuralAttribute.EMPLOYEE, 0.1);
				ratesPerPurpose4_2.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.06);

				ratesPerPurpose4_3.put(StructuralAttribute.EMPLOYEE, 0.1);
				ratesPerPurpose4_3.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.25);

				ratesPerPurpose4_4.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.08);

				ratesPerPurpose4_5.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.43);

				ratesPerPurpose5_1.put(StructuralAttribute.EMPLOYEE, 0.8);
				ratesPerPurpose5_1.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.25);

				ratesPerPurpose5_2.put(StructuralAttribute.EMPLOYEE, 0.1);
				ratesPerPurpose5_2.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.2);

				ratesPerPurpose5_3.put(StructuralAttribute.EMPLOYEE, 0.1);
				ratesPerPurpose5_3.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.25);
				ratesPerPurpose5_3.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.139);
				ratesPerPurpose5_3.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.059);

				ratesPerPurpose5_4.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.02);

				ratesPerPurpose5_5.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.28);

				ratesPerPurpose6_2.put(StructuralAttribute.INHABITANTS, 0.29);

				ratesPerPurpose6_3.put(StructuralAttribute.INHABITANTS, 0.63);

				ratesPerPurpose6_4.put(StructuralAttribute.INHABITANTS, 0.07);

				ratesPerPurpose6_5.put(StructuralAttribute.INHABITANTS, 0.001);

			} else if (commitmentType.equals("stop")) {

				ratesPerPurpose1_1.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.35);

				ratesPerPurpose1_2.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.1);

				ratesPerPurpose1_3.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.27);

				ratesPerPurpose1_4.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.01);

				ratesPerPurpose1_5.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.27);

				ratesPerPurpose2_1.put(StructuralAttribute.INHABITANTS, 0.55);
				ratesPerPurpose2_1.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.46);
				ratesPerPurpose2_1.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.46);
				ratesPerPurpose2_1.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.46);
				ratesPerPurpose2_1.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.46);
				ratesPerPurpose2_1.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.34);
				ratesPerPurpose2_1.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.46);

				ratesPerPurpose2_2.put(StructuralAttribute.INHABITANTS, 0.09);
				ratesPerPurpose2_2.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.09);
				ratesPerPurpose2_2.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.09);
				ratesPerPurpose2_2.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.09);
				ratesPerPurpose2_2.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.09);
				ratesPerPurpose2_2.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.1);
				ratesPerPurpose2_2.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.09);

				ratesPerPurpose2_3.put(StructuralAttribute.INHABITANTS, 0.21);
				ratesPerPurpose2_3.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.22);
				ratesPerPurpose2_3.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.22);
				ratesPerPurpose2_3.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.22);
				ratesPerPurpose2_3.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.22);
				ratesPerPurpose2_3.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.29);
				ratesPerPurpose2_3.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.22);

				ratesPerPurpose2_4.put(StructuralAttribute.INHABITANTS, 0.06);
				ratesPerPurpose2_4.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.06);
				ratesPerPurpose2_4.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.06);
				ratesPerPurpose2_4.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.06);
				ratesPerPurpose2_4.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.06);
				ratesPerPurpose2_4.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.06);

				ratesPerPurpose2_5.put(StructuralAttribute.INHABITANTS, 0.1);
				ratesPerPurpose2_5.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.17);
				ratesPerPurpose2_5.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.17);
				ratesPerPurpose2_5.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.17);
				ratesPerPurpose2_5.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.17);
				ratesPerPurpose2_5.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.27);
				ratesPerPurpose2_5.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.17);

				ratesPerPurpose3_1.put(StructuralAttribute.INHABITANTS, 0.489);
				ratesPerPurpose3_1.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.538);
				ratesPerPurpose3_1.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.538);
				ratesPerPurpose3_1.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.538);
				ratesPerPurpose3_1.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.538);
				ratesPerPurpose3_1.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.59);
				ratesPerPurpose3_1.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.538);

				ratesPerPurpose3_2.put(StructuralAttribute.INHABITANTS, 0.106);
				ratesPerPurpose3_2.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.092);
				ratesPerPurpose3_2.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.092);
				ratesPerPurpose3_2.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.092);
				ratesPerPurpose3_2.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.092);
				ratesPerPurpose3_2.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.03);
				ratesPerPurpose3_2.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.092);

				ratesPerPurpose3_3.put(StructuralAttribute.INHABITANTS, 0.26);
				ratesPerPurpose3_3.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.19);
				ratesPerPurpose3_3.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.19);
				ratesPerPurpose3_3.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.19);
				ratesPerPurpose3_3.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.19);
				ratesPerPurpose3_3.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.102);
				ratesPerPurpose3_3.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.19);

				ratesPerPurpose3_4.put(StructuralAttribute.INHABITANTS, 0.033);
				ratesPerPurpose3_4.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.032);
				ratesPerPurpose3_4.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.032);
				ratesPerPurpose3_4.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.032);
				ratesPerPurpose3_4.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.032);
				ratesPerPurpose3_4.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.058);
				ratesPerPurpose3_4.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.032);

				ratesPerPurpose3_5.put(StructuralAttribute.INHABITANTS, 0.112);
				ratesPerPurpose3_5.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.147);
				ratesPerPurpose3_5.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.147);
				ratesPerPurpose3_5.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.147);
				ratesPerPurpose3_5.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.147);
				ratesPerPurpose3_5.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.219);
				ratesPerPurpose3_5.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.147);

				ratesPerPurpose4_1.put(StructuralAttribute.INHABITANTS, 0.37);
				ratesPerPurpose4_1.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.14);
				ratesPerPurpose4_1.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.14);
				ratesPerPurpose4_1.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.14);
				ratesPerPurpose4_1.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.14);
				ratesPerPurpose4_1.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.06);
				ratesPerPurpose4_1.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.14);

				ratesPerPurpose4_2.put(StructuralAttribute.INHABITANTS, 0.05);
				ratesPerPurpose4_2.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.07);
				ratesPerPurpose4_2.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.07);
				ratesPerPurpose4_2.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.07);
				ratesPerPurpose4_2.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.07);
				ratesPerPurpose4_2.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.07);
				ratesPerPurpose4_2.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.07);

				ratesPerPurpose4_3.put(StructuralAttribute.INHABITANTS, 0.4);
				ratesPerPurpose4_3.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.21);
				ratesPerPurpose4_3.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.21);
				ratesPerPurpose4_3.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.21);
				ratesPerPurpose4_3.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.21);
				ratesPerPurpose4_3.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.19);
				ratesPerPurpose4_3.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.21);

				ratesPerPurpose4_4.put(StructuralAttribute.INHABITANTS, 0.13);
				ratesPerPurpose4_4.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.05);
				ratesPerPurpose4_4.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.05);
				ratesPerPurpose4_4.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.05);
				ratesPerPurpose4_4.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.05);
				ratesPerPurpose4_4.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.08);
				ratesPerPurpose4_4.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.05);

				ratesPerPurpose4_5.put(StructuralAttribute.INHABITANTS, 0.05);
				ratesPerPurpose4_5.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.54);
				ratesPerPurpose4_5.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.54);
				ratesPerPurpose4_5.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.54);
				ratesPerPurpose4_5.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.54);
				ratesPerPurpose4_5.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.61);
				ratesPerPurpose4_5.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.54);

				ratesPerPurpose5_1.put(StructuralAttribute.INHABITANTS, 0.16);
				ratesPerPurpose5_1.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.4);
				ratesPerPurpose5_1.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.4);
				ratesPerPurpose5_1.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.4);
				ratesPerPurpose5_1.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.4);
				ratesPerPurpose5_1.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.14);
				ratesPerPurpose5_1.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.4);

				ratesPerPurpose5_2.put(StructuralAttribute.INHABITANTS, 0.55);
				ratesPerPurpose5_2.put(StructuralAttribute.EMPLOYEE, 0.11);
				ratesPerPurpose5_2.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.11);
				ratesPerPurpose5_2.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.11);
				ratesPerPurpose5_2.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.11);
				ratesPerPurpose5_2.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.11);
				ratesPerPurpose5_2.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.06);
				ratesPerPurpose5_2.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.11);

				ratesPerPurpose5_3.put(StructuralAttribute.INHABITANTS, 0.22);
				ratesPerPurpose5_3.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.17);
				ratesPerPurpose5_3.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.17);
				ratesPerPurpose5_3.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.17);
				ratesPerPurpose5_3.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.17);
				ratesPerPurpose5_3.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.21);
				ratesPerPurpose5_3.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.17);

				ratesPerPurpose5_4.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.04);
				ratesPerPurpose5_4.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.04);
				ratesPerPurpose5_4.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.04);
				ratesPerPurpose5_4.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.04);
				ratesPerPurpose5_4.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.04);

				ratesPerPurpose5_5.put(StructuralAttribute.INHABITANTS, 0.06);
				ratesPerPurpose5_5.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.28);
				ratesPerPurpose5_5.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.28);
				ratesPerPurpose5_5.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.28);
				ratesPerPurpose5_5.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.28);
				ratesPerPurpose5_5.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.58);
				ratesPerPurpose5_5.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.28);

				ratesPerPurpose6_2.put(StructuralAttribute.INHABITANTS, 0.85);
				ratesPerPurpose6_2.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.21);
				ratesPerPurpose6_2.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.21);
				ratesPerPurpose6_2.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.21);
				ratesPerPurpose6_2.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.21);
				ratesPerPurpose6_2.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.09);
				ratesPerPurpose6_2.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.21);

				ratesPerPurpose6_3.put(StructuralAttribute.INHABITANTS, 0.15);
				ratesPerPurpose6_3.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.58);
				ratesPerPurpose6_3.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.58);
				ratesPerPurpose6_3.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.58);
				ratesPerPurpose6_3.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.58);
				ratesPerPurpose6_3.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.55);
				ratesPerPurpose6_3.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.58);

				ratesPerPurpose6_4.put(StructuralAttribute.EMPLOYEE_PRIMARY, 0.21);
				ratesPerPurpose6_4.put(StructuralAttribute.EMPLOYEE_CONSTRUCTION, 0.21);
				ratesPerPurpose6_4.put(StructuralAttribute.EMPLOYEE_SECONDARY, 0.21);
				ratesPerPurpose6_4.put(StructuralAttribute.EMPLOYEE_RETAIL, 0.21);
				ratesPerPurpose6_4.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.25);
				ratesPerPurpose6_4.put(StructuralAttribute.EMPLOYEE_TERTIARY, 0.21);

				ratesPerPurpose6_5.put(StructuralAttribute.EMPLOYEE_TRAFFIC, 0.11);
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
