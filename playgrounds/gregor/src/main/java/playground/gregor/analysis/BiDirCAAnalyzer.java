/* *********************************************************************** *
 * project: org.matsim.*
 * BiDirCAAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.StringUtils;

public class BiDirCAAnalyzer {


	public static void main(String [] args) throws IOException {

		QuadTree<Measurement> qt = new QuadTree<BiDirCAAnalyzer.Measurement>(0, 0, 7, 7);
		BufferedReader br = new BufferedReader(new FileReader(new File("/Users/laemmel/devel/bipedca/plot/unbl")));
		//		BufferedWriter rep = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/bipedca/plot/rep")));
		String l = br.readLine();
		int cnt = 0;
		int mxl = 0;
		while (l != null) {
			cnt++;
			String[] expl = StringUtils.explode(l, ' ');
			if (expl.length < 7) {
				l = br.readLine();
				continue;
			}
			if (expl.length > mxl) {
				mxl = expl.length;
			}
			double q = Double.parseDouble(expl[2]);
			double rho1 = Double.parseDouble(expl[3]);
			double rho2 = Double.parseDouble(expl[6]);
			Measurement m = new Measurement();
			m.line = l;
			m.expl = expl;
			m.q = q;
			m.rho1 = rho1;
			m.rho2 = rho2;
			m.v1 = Double.parseDouble(expl[5]);
			m.v2 = Double.parseDouble(expl[8]);
			qt.put(rho1, rho2, m);
			//				rep.append(l + "n");
			l = br.readLine();
		}
		//		rep.close();
		br.close();
		//		System.out.println(qt.size());

		BufferedWriter bwG = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/bipedca/plot/gaps")));
		double incr = 0.2;
		int gaps = 0;
		for (double rho1 = 0; rho1 <= 6.661; rho1 += incr) {
			for (double rho2 = 0; rho1+rho2 <= 6.661; rho2 += incr){
				//				Measurement m = qt.get(rho1, rho2);
				//				double d1 = Math.abs(m.rho1-rho1);
				//				if (d1 > incr/2 ) {
				//					continue;
				//				}else {
				//					double d2 = Math.abs(m.rho2-rho2);
				//					if (d2 > incr/2) {
				//						continue;
				//					}
				//				}
				//				
				////				bw.append(m.line +"\n");
				//				int i;
				//				for (i = 0; i < m.expl.length; i++) {
				//					if (i == 3) {
				//						bw.append(rho1+"");
				//					} else if (i == 6) {
				//						bw.append(rho2+"");
				//					} else {
				//						bw.append(m.expl[i]);
				//					}
				//					bw.append(" ");
				//				}
				//				while (i < mxl) {
				//					bw.append(" 0");
				//					i++;
				//				}
				//				bw.append("\n");

				Collection<Measurement> c = qt.getDisk(rho1, rho2, incr / 2);
				if (c.size() == 0) {
					double incr2 = incr/2+0.01;
					while (c.size() < 1) {

						c = qt.getDisk(rho1, rho2, incr2);
						incr2 += 0.01;
					}
					for (Measurement mm : c) {
						bwG.append(mm.line +"\n");
					}
					//					Measurement m = qt.get(rho1, rho2);
					//					bw.append(m.line +"\n");
					//					System.out.println(m.line);
					gaps++;
					continue;
				}
				//				double mxQ = -1;
				//				double rr1 = 0;
				//				double rr2 = 0;
				//				for (Measurement m : c) {
				//					if (m.q > mxQ) {
				//						mxQ = m.q;
				//						rr1 = m.rho1;
				//						rr2 = m.rho2;
				//					}
				//					
				//				}
				double rho = rho1+rho2;
				double v1 = 0;
				double v2 = 0;

				double [] a1 = new double [c.size()];
				double [] a2 = new double [c.size()];
				int idx = 0;
				for (Measurement m : c) {
					a1[idx] = m.v1;
					a2[idx++] = m.v2;
				}

				Arrays.sort(a1);
				Arrays.sort(a2);
				int median = c.size()/2;
				v1 = a1[median];
				v2 = a2[median];


				double q1 = v1*rho1;
				double q2 = v2*rho2;
				double v = (q1+q2)/rho;
				double q = q1+q2;
			}
		}
		bwG.close();
		System.out.println(gaps);

	}


	public static class Measurement {
		public String[] expl;
		public String line;
		double rho1;
		double rho2;
		double q;
		double q1;
		double q2;
		double v1;
		double v2;
	}
}
