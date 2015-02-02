/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.gregor.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.matsim.core.utils.misc.StringUtils;

import playground.gregor.utils.Variance;

public class VarianceAnalysisUni {

	public static void main(String[] args) throws IOException {
		String infile = "/Users/laemmel/arbeit/papers/2015/ABMTrans/rawdata/uni12";
		List<Measurement> measures = parse(infile);
		Collections.sort(measures);

		Iterator<Measurement> it = measures.iterator();
		Variance v = new Variance();
		for (double rho = .5; rho < 7; rho += .5) {
			while (it.hasNext()) {
				Measurement m = it.next();

				if (m.rho > rho) {
					System.out.println(rho + " " + v.getMean() + " "
							+ Math.sqrt(v.getVar()));
					v = new Variance();
					break;
				}

				double flow = m.rho * m.dsSpd;
				v.addVar(flow);
			}
			if (!it.hasNext()) {
				System.out.println(rho + " " + v.getMean() + " "
						+ Math.sqrt(v.getVar()));
			}

		}

	}

	private static List<Measurement> parse(String infile) throws IOException {
		List<Measurement> ret = new ArrayList<>();
		BufferedReader br = new BufferedReader(new FileReader(new File(infile)));
		String line = br.readLine();
		while (line != null) {
			String[] expl = StringUtils.explode(line, ' ');
			Measurement m = new Measurement();
			m.time = Double.parseDouble(expl[0]);
			m.dsRho = Double.parseDouble(expl[1]);
			m.dsSpd = Double.parseDouble(expl[2]);
			m.usRho = Double.parseDouble(expl[3]);
			m.usSpd = Double.parseDouble(expl[4]);
			m.rho = Double.parseDouble(expl[5]);
			m.spd = Double.parseDouble(expl[7]);
			ret.add(m);
			line = br.readLine();
		}
		br.close();
		return ret;
	}

	private static final class Measurement implements Comparable<Measurement> {
		public double usSpd;
		public double dsSpd;
		public double usRho;
		public double dsRho;
		public double spd;
		double rho;
		double time;

		@Override
		public int compareTo(Measurement o) {
			if (rho < o.rho) {
				return -1;
			}
			if (rho > o.rho) {
				return 1;
			}
			return 0;
		}
	}

}
