/* *********************************************************************** *
 * project: org.matsim.*
 * CarTripCounter.java
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
package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * @author yu
 * 
 */
public class ModeChoiceByDistance extends AbstractPersonAlgorithm {
	private final Map<Double, Double> carLegs = new TreeMap<Double, Double>(),
			ptLegs = new TreeMap<Double, Double>();

	private final BufferedWriter out;

	public ModeChoiceByDistance(final String outputFilePath) throws Exception {
		this.out = IOUtils.getBufferedWriter(outputFilePath + ".txt");
	}

	/**
	 * @param args
	 *            [0] - netFilename
	 * @param args
	 *            [1] - planFilename
	 * @param args
	 *            [2] - outputFilepath
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = args[0];
		final String plansFilename = args[1];
		final String outputFilePath = args[2];

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		PopulationImpl ppl = new PopulationImpl();
		System.out.println("->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(ppl, network).readFile(plansFilename);
		ModeChoiceByDistance mcbd;
		try {
			mcbd = new ModeChoiceByDistance(outputFilePath);
			mcbd.run(ppl);
			mcbd.write("network :\t" + netFilename + "\n");
			mcbd.write("plansfile :\t" + plansFilename + "\n");
			mcbd.write("car : dist\tn_legs\n");
			XYLineChart chart = new XYLineChart("mode choice by distance",
					"distance [m]", "n_legs");
			Map<Double, Double> carLegs = mcbd.getCarLegs();
			double[] xCar = new double[carLegs.size()];
			double[] yCar = new double[carLegs.size()];
			int i = 0;
			for (Entry<Double, Double> e : carLegs.entrySet()) {
				double dist = e.getKey();
				double n_carlegs = e.getValue();
				mcbd.write(dist + "\t" + n_carlegs + "\n");
				xCar[i] = dist;
				yCar[i] = n_carlegs;
				i++;
			}
			chart.addSeries("car legs", xCar, yCar);

			mcbd.write("pt : dist\tn_legs\n");
			Map<Double, Double> ptLegs = mcbd.getPtLegs();
			double[] xPt = new double[ptLegs.size()];
			double[] yPt = new double[ptLegs.size()];
			int j = 0;
			for (Entry<Double, Double> e : ptLegs.entrySet()) {
				double dist = e.getKey();
				double n_ptlegs = e.getValue();
				mcbd.write(dist + "\t" + n_ptlegs + "\n");
				xPt[j] = dist;
				yPt[j] = n_ptlegs;
				j++;
			}
			chart.addSeries("pt legs", xPt, yPt);

			mcbd.close();

			chart.saveAsPng(outputFilePath + ".png", 800, 600);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("-> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

	@Override
	public void run(final Person person) {
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Leg) {
				Leg l = (Leg) pe;
				double dist = (((int) l.getRoute().getDistance()) / 1000 * 1000);
				if (dist < 320000) {
					if (l.getMode().equals(TransportMode.car)) {
						Double carLegsCounter = this.carLegs.get(dist);
						if (carLegsCounter == null) {
							carLegsCounter = 0.0;
						}
						carLegsCounter = carLegsCounter + 1.0;
						this.carLegs.put(dist, carLegsCounter);
					} else if (l.getMode().equals(TransportMode.pt)) {
						Double ptLegsCounter = this.ptLegs.get(dist);
						if (ptLegsCounter == null) {
							ptLegsCounter = 0.0;
						}
						ptLegsCounter = ptLegsCounter + 1.0;
						this.ptLegs.put(dist, ptLegsCounter);
					}
				}
			}
		}
	}

	public void write(final String args) {
		try {
			this.out.write(args);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Map<Double, Double> getCarLegs() {
		return this.carLegs;
	}

	public Map<Double, Double> getPtLegs() {
		return this.ptLegs;
	}
}
