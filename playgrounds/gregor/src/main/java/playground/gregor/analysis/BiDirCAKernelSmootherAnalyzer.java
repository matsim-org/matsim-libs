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
import java.util.Collection;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.StringUtils;

public class BiDirCAKernelSmootherAnalyzer {


	private static final double LAMBDA = 1;
	private static final double MX_DELTA = .1;

	public static void main(String [] args) throws IOException {

		QuadTree<Measurement> qt = new QuadTree<BiDirCAKernelSmootherAnalyzer.Measurement>(0, 0,7, 7);
		BufferedReader br = new BufferedReader(new FileReader(new File("/Users/laemmel/devel/bipedca/plot/unbl")));

		String l = br.readLine();
		int cnt = 0;
		int filtered = 0;
		while (l != null) {
			String[] expl = StringUtils.explode(l, ' ');
			if (expl.length < 7) {
				l = br.readLine();
				continue;
			}
			//			if (expl.length < mxl) {
			//				mxl = expl.length;
			//			}

			if (expl.length >= 13) {
				double bL = Double.parseDouble(expl[9]);
				double bR = Double.parseDouble(expl[10]);
				double exR= Double.parseDouble(expl[11]);
				double exL= Double.parseDouble(expl[12]);

				if (bL > exR && bR > exL) {
					l = br.readLine();
					filtered++;
					continue;
				}
			}
			double rho = Double.parseDouble(expl[0]);
//			double q = Double.parseDouble(expl[1]);
			double rho1 = Double.parseDouble(expl[3]);
			double rho2 = Double.parseDouble(expl[6]);
			
			Measurement m = new Measurement();
			m.line = l;
			m.expl = expl;
			m.rho1 = rho1;
			m.rho2 = rho2;
			m.v1 = Double.parseDouble(expl[5]);
			m.v2 = Double.parseDouble(expl[8]);
			m.rho = rho1+rho2;
			m.q1 = m.rho1*m.v1;
			m.q2 = m.rho2*m.v2;
			m.q = m.q1+m.q2;
			m.v = m.q/m.rho;
			
			
			
			
			
			double delta = Math.abs(rho-rho1-rho2);
			if (delta > 0.2) {
				filtered++;
				l = br.readLine();
				continue;
			} 
//			
//			if (m.v1*m.rho1 > 0.3 && m.rho1 > 4.4) {
//				System.out.println(l);
//				System.out.println(l);
//				filtered++;
//				l = br.readLine();
//				continue;
//			}
			
			
			qt.put(rho1, rho2, m);
			
			cnt++;
			l = br.readLine();
		}
		System.out.println(cnt + " " + filtered);
		br.close();
		//		minDist(qt);
		//		System.out.println(qt.size());

		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/bipedca/plot/qt")));
		double incr = 0.03333;
		int gaps = 0;
		for (double rho1 = 0; rho1 < 6.661; rho1 += incr) {
			for (double rho2 = 0; rho1+rho2 <= 6.661; rho2 += incr){


				double rho = rho1+rho2;
				double v1 = 0;
				double v2 = 0;

				double lambda = 0.2;
				Collection<Measurement> nb = qt.getDisk(rho1, rho2, lambda);
				double total = 0;
				for (Measurement el : nb) {
					double weight = compContribution(el,rho1,rho2,lambda);
					total += weight;
					v1+= weight * el.v1;
				}
				if (total > 0) {
					v1 /= total;
				}
				total = 0;
				for (Measurement el : nb) {
					double weight = compContribution(el,rho1,rho2,lambda);
					total += weight;
					v2+= weight * el.v2;
				}
				if (total > 0) {
					v2 /= total;
				}
				//				v1;
				//				v2;
				//				if (Double.isNaN(v1)){
				//					System.out.println("stop");
				//				}
				double q1 = v1*rho1;
				double q2 = v2*rho2;
				double v = (q1+q2)/rho;
				double q = q1+q2;
				bw.append(rho +" " + q + " " + v +" " + rho1 + " " + q1 + " " + v1 + " " + rho2 + " " + q2 + " " + v2 + "\n");
				if (++gaps % 100 == 0) {
					System.out.println(gaps);
				}
			}
		}
		bw.close();

	}


	//
	//	private static void minDist(QuadTree<Measurement> qt) {
	//		int minDists  = 0;
	//		for (Measurement el : qt.values()) {
	//			double searchRadius = 0.01;
	//			Collection<Measurement> c = qt.get(el.rho1, el.rho2,searchRadius);
	//			while (c.size() <= 1) {
	//				searchRadius+=0.01;
	//				c = qt.get(el.rho1, el.rho2,searchRadius);
	//			}
	//			
	//			double minDist = Double.POSITIVE_INFINITY;
	//			for (Measurement tmp : c) {
	//				if (tmp != el) {
	//					double dRho1 = el.rho1-tmp.rho1;
	//					double dRho2 = el.rho2-tmp.rho2;
	//					double dist =Math.sqrt(dRho1*dRho1+dRho2*dRho2);
	//					if (dist < minDist) {
	//						minDist = dist;
	//					}
	//				}
	//				
	//			}
	//			el.minDist = minDist;
	//			if (++minDists % 100 == 0) {
	//				System.out.println(minDists);
	//			}
	//		}
	//		
	//	}

	private static double compContribution(Measurement el, double rho1,
			double rho2, double lambda) {
		double dRho1 = el.rho1-rho1;
		double dRho2 = el.rho2-rho2;
		double sqrDist =dRho1*dRho1+dRho2*dRho2;
		return Math.exp(-sqrDist/lambda);
	}


	public static class Measurement {
		public double minDist;
		public String[] expl;
		public String line;
		double rho1;
		double rho2;
		double q;
		double q1;
		double q2;
		double v1;
		double v2;
		double v;
		double rho;
	}
}
