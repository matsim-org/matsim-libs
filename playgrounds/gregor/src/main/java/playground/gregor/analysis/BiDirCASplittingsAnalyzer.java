/* *********************************************************************** *
 * project: org.matsim.*
 * BiDirCASplittingsAnalyzer.java
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

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.StringUtils;

import playground.gregor.analysis.BiDirCAKernelSmootherAnalyzer.Measurement;

public class BiDirCASplittingsAnalyzer {
	
	public static void main(String [] args) throws IOException {
		QuadTree<Measurement> qt = new QuadTree<BiDirCAKernelSmootherAnalyzer.Measurement>(0, 0, 5, 5);
		BufferedReader br = new BufferedReader(new FileReader(new File("/Users/laemmel/devel/bipedca/plot/qt")));

		String l = br.readLine();
		while (l != null) {
			String[] expl = StringUtils.explode(l, ' ');
			
			double v = Double.parseDouble(expl[2]);
			double rho = Double.parseDouble(expl[0]);
			double rho1 = Double.parseDouble(expl[3]);
			double rho2 = Double.parseDouble(expl[6]);
			Measurement m = new Measurement();
			m.line = l;
			m.expl = expl;
			m.v = v;
			m.rho1 = rho1;
			m.rho2 = rho2;
			m.v1 = Double.parseDouble(expl[5]);
			m.v2 = Double.parseDouble(expl[8]);
			m.q1 = m.v1*m.rho1;
			m.q2 = m.v2*m.rho2;
			m.q = m.v * rho;
			m.rho = rho;
			qt.put(rho1, rho2, m);
			l = br.readLine();
		}
		br.close();
		
		BufferedWriter p = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/bipedca/plot/splitting.p")));
		p.append("plot ");
		for (double r = 1; r >= 0.5; r -= 0.05) {
			String file = "splitting_"+r;
			p.append("\""+file+"\" u 1:2 w l,");
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/bipedca/plot/"+file)));
			for (double rho1 = 0; rho1 <= 5*r; rho1 += 0.05){
				
				
				double rho = rho1/r;
				double rho2 = (1-r)*rho;
				Measurement m = qt.getClosest(rho1, rho2);
				
				if (rho1 > 4 && m.q1 > 0.25) {
					System.out.println("err");
				}
				
				StringBuffer buf = new StringBuffer();
				buf.append(rho1);
				buf.append(' ');
				buf.append(m.q1);
				buf.append(' ');
				buf.append(m.q);
				buf.append(' ');
				buf.append(m.rho1/m.rho);
				buf.append(' ');
				buf.append('\n');
				bw.append(buf);
			}
			bw.close();
		}
		p.close();
		
	}

}
