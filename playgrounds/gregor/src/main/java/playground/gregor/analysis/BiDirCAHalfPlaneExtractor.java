/* *********************************************************************** *
 * project: org.matsim.*
 * BiDirCAHalfPlaneExtractor.java
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

import org.matsim.core.utils.misc.StringUtils;

import playground.gregor.analysis.BiDirCAAnalyzer.Measurement;
import playground.gregor.sim2d_v4.cgal.CGAL;

public class BiDirCAHalfPlaneExtractor {

	public static void main(String [] args) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File("/Users/laemmel/devel/bipedca/plot/unbl")));
		//		BufferedWriter rep = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/bipedca/plot/rep")));
		BufferedWriter bwG = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/bipedca/plot/gaps")));
		//half plane 
		double x0 = 0;
		double x1 = 4.5;
		double y0 = 4.5;
		double y1 = 0;
		
		
		String l = br.readLine();
		int cnt = 0;
		int mxl = 0;
		while (l != null) {
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
			
			double x =  CGAL.isLeftOfLine(rho1, rho2, x0, y0, x1, y1);
			double diff = Double.parseDouble(expl[0]) - rho1 -rho2;
			diff = Math.abs(diff);
			if (x >= 0 && (rho1 < 1.5) && (rho2 > 3) && rho2 < 4&& (rho1+rho2)>4 && diff < 0.2) {
//				System.out.println(l);
				bwG.append(l);
				bwG.append('\n');
			}
			
			Measurement m = new Measurement();
			m.line = l;
			m.expl = expl;
			m.q = q;
			m.rho1 = rho1;
			m.rho2 = rho2;
			m.v1 = Double.parseDouble(expl[5]);
			m.v2 = Double.parseDouble(expl[8]);
			//				rep.append(l + "n");
			l = br.readLine();
		}
		//		rep.close();
		br.close();
		bwG.close();
		//		System.out.println(qt.size());
	}
}
