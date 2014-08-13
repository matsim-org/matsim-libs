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

import org.matsim.core.utils.misc.StringUtils;

public class BiDirCARRFilter {


	public static void main(String [] args) throws IOException {

		BufferedReader br = new BufferedReader(new FileReader(new File("/Users/laemmel/devel/bipedca/plot/unbl")));
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/bipedca/plot/filtered")));

		String l = br.readLine();
		int line  =0;
		int filtered = 0;
		while (l != null) {
			line++;
			String[] expl = StringUtils.explode(l, ' ');
			if (expl.length < 7) {
				l = br.readLine();
				continue;
			}
		
			

			
			double rho1 = Double.parseDouble(expl[3]);
			double rho2 = Double.parseDouble(expl[6]);
			double q2 = Double.parseDouble(expl[7]);
			double q1 = Double.parseDouble(expl[4]);
			if (rho2 >= 1.5 && rho2 < 2 && rho1 < 0.5 && q2 < 1) {
				System.err.println(l);
				l = br.readLine();
				continue;
			}
			if (expl.length < 14) {
				bw.append(l);
				bw.append('\n');
				l = br.readLine();
				continue;
			}
			double bL = Double.parseDouble(expl[9]);
			double bR = Double.parseDouble(expl[10]);
			double exR= Double.parseDouble(expl[11]);
			double exL= Double.parseDouble(expl[12]);
			
			if (bL > exR && bR > exL) {
				l = br.readLine();
				filtered++;
				continue;
			}

			bw.append(l);
			bw.append('\n');
			l = br.readLine();
		}

		br.close();
		bw.close();
		System.out.println(filtered);

	}


}
