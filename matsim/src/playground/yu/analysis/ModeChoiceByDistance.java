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

import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg.Mode;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.io.IOUtils;

/**
 * @author yu
 * 
 */
public class ModeChoiceByDistance extends AbstractPersonAlgorithm {
	private Map<Double, Double> carLegs = new TreeMap<Double, Double>(),
			ptLegs = new TreeMap<Double, Double>();

	private String outputFilePath;

	private BufferedWriter out;

	public ModeChoiceByDistance(String outputFilePath) throws Exception {
		out = IOUtils.getBufferedWriter(outputFilePath + ".txt");
		this.outputFilePath = outputFilePath;
	}

	/**
	 * @param args
	 *            [0] - netFilename
	 * @param args
	 *            [1] - planFilename
	 * @param args
	 *            [2] - outputFilepath
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = args[0];
		final String plansFilename = args[1];
		final String outputFilePath = args[2];

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		Gbl.getWorld().setNetworkLayer(network);
		Gbl.getWorld().complete();

		Population ppl = new Population();
		System.out.println("->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(ppl).readFile(plansFilename);
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
	public void run(Person person) {
		for (LegIterator li = person.getSelectedPlan().getIteratorLeg(); li
				.hasNext();) {
			Leg l = (Leg) li.next();
			double dist = (double) (((int) l.getRoute().getDist()) / 1000 * 1000);
			if (dist < 320000) {
				if (l.getMode().equals(Mode.car)) {
					Double carLegsCounter = carLegs.get(dist);
					if (carLegsCounter == null) {
						carLegsCounter = new Double(0.0);
					}
					carLegsCounter = new Double(
							carLegsCounter.doubleValue() + 1.0);
					carLegs.put(dist, carLegsCounter);
				} else if (l.getMode().equals(Mode.pt)) {
					Double ptLegsCounter = ptLegs.get(dist);
					if (ptLegsCounter == null) {
						ptLegsCounter = new Double(0.0);
					}
					ptLegsCounter = new Double(
							ptLegsCounter.doubleValue() + 1.0);
					ptLegs.put(dist, ptLegsCounter);
				}
			}
		}
	}

	public void write(String args) {
		try {
			out.write(args);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			out.close();
		} catch (IOException e) {
		}
	}

	public Map<Double, Double> getCarLegs() {
		return carLegs;
	}

	public Map<Double, Double> getPtLegs() {
		return ptLegs;
	}
}
