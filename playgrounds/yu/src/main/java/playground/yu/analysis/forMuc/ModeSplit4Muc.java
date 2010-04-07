/* *********************************************************************** *
 * project: org.matsim.*
 * ModeSplit4Muc.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.yu.analysis.forMuc;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.analysis.ModeSplit;
import playground.yu.utils.TollTools;
import playground.yu.utils.charts.PieChart;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class ModeSplit4Muc extends ModeSplit implements Analysis4Muc {
	private int rideLegs = 0,//
			tollRideLegs = 0;

	/**
	 * @param toll
	 */
	public ModeSplit4Muc(RoadPricingScheme toll) {
		super(toll);
	}

	public void run(final Plan plan) {
		boolean inRange = false;
		if (toll != null)
			inRange = TollTools.isInRange(((PlanImpl) plan).getFirstActivity()
					.getLinkId(), toll);
		for (PlanElement pe : plan.getPlanElements())
			if (pe instanceof LegImpl) {
				TransportMode m = ((LegImpl) pe).getMode();
				switch (m) {
				case car:
					carLegs++;
					if (inRange)
						tollCarLegs++;
					break;
				case pt:
					ptLegs++;
					if (inRange)
						tollPtLegs++;
					break;
				case walk:
					wlkLegs++;
					if (inRange)
						tollWlkLegs++;
					break;
				case bike:
					bikeLegs++;
					if (inRange)
						tollBikeLegs++;
					break;
				case ride:
					rideLegs++;
					if (inRange)
						tollRideLegs++;
				default:
					othersLegs++;
					if (inRange)
						tollOthersLegs++;
					break;
				}
			}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(total)mode\tnumber\tfraction[%]\n");
		double sum = carLegs + ptLegs + wlkLegs + bikeLegs + othersLegs;
		sb.append("car\t" + carLegs + "\t" + carLegs / sum * 100.0 + "\n");
		sb.append("pt\t" + ptLegs + "\t" + ptLegs / sum * 100.0 + "\n");
		sb.append("walk\t" + wlkLegs + "\t" + wlkLegs / sum * 100.0 + "\n");
		sb.append("bike\t" + bikeLegs + "\t" + bikeLegs / sum * 100.0 + "\n");
		sb.append("ride\t" + rideLegs + "\t" + rideLegs / sum * 100.0 + "\n");
		sb.append("others\t" + othersLegs + "\t" + othersLegs / sum * 100.0
				+ "\n");

		if (toll != null) {
			sum = tollCarLegs + tollPtLegs + tollWlkLegs + tollBikeLegs
					+ tollOthersLegs;
			sb.append("(toll area)mode\tnumber\tfraction[%]\n");
			sb.append("car\t" + tollCarLegs + "\t" + tollCarLegs / sum * 100.0
					+ "\n");
			sb.append("pt\t" + tollPtLegs + "\t" + tollPtLegs / sum * 100.0
					+ "\n");
			sb.append("walk\t" + tollWlkLegs + "\t" + tollWlkLegs / sum * 100.0
					+ "\n");
			sb.append("bike\t" + tollBikeLegs + "\t" + tollBikeLegs / sum
					* 100.0 + "\n");
			sb.append("ride\t" + tollRideLegs + "\t" + tollRideLegs / sum
					* 100.0 + "\n");
			sb.append("other\t" + tollOthersLegs + "\t" + tollOthersLegs / sum
					* 100.0 + "\n");
		}

		return sb.toString();
	}

	public void write(final String outputPath) {
		SimpleWriter sw = new SimpleWriter(outputPath + "modalSplitLegs.txt");
		sw.write(toString());
		sw.close();

		PieChart chart = new PieChart("ModalSplit -- Legs");
		chart.addSeries(new String[] { CAR, PT, WALK, BIKE, RIDE, OTHERS },
				new double[] { carLegs, ptLegs, wlkLegs, bikeLegs, rideLegs,
						othersLegs });
		chart.saveAsPng(outputPath + "modalSplitLegs.png", 800, 600);

		if (toll != null) {
			PieChart chart2 = new PieChart(
					"ModalSplit Center (toll area) -- Legs");
			chart2.addSeries(
					new String[] { CAR, PT, WALK, BIKE, RIDE, OTHERS },
					new double[] { tollCarLegs, tollPtLegs, tollWlkLegs,
							tollBikeLegs, tollRideLegs, tollOthersLegs });
			chart2.saveAsPng(outputPath + "modalSplitTollLegs.png", 800, 600);
		}
	}
}
