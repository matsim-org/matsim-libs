/* *********************************************************************** *
 * project: org.matsim.*
 * GnuplotScript.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.bidirpeds.ca;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import playground.gregor.bidirpeds.ca.BiDirPedsCA.Cell;

public class GnuplotScript {
	
	public GnuplotScript(String datFile,Cell [] cells,int numPeds, int nrCells) throws IOException {
		BufferedWriter bf = new BufferedWriter(new FileWriter(new File(datFile+".gnuplot")));
		bf.append("\nset terminal pdfcairo color solid size 7in,4in font \"Bitstream, 16\"\n");
		bf.append("set output \""+datFile+".pdf\"\n");
		bf.append("set xrange [0:500]\n");
		bf.append("set yrange [1:");
		bf.append(nrCells+"]\n");
		bf.append("set ylabel \"space\"\n");
		bf.append("set xlabel \"time\"\n");
		bf.append("set pointsize 1\n");
//		bf.append("set grid\n");
		bf.append("set title \"\"\n");
		bf.append("set datafile missing '-1'\n");
		bf.append("plot");
		
		for (Cell c : cells) {
			if (c.ped != null) {
				bf.append("\"");
				bf.append(datFile);
				bf.append("\" using 1:($");
				bf.append((c.ped.nr+2)+"");
				bf.append(") title '' with lines ");
//				bf.append("lt rgb \"#");
//				int pr = c.ped.hashCode()%128;
//				int r,g,b;
//				if (c.ped.dir == 1) {
//					r = 0;
//					g = 255;
//					b = pr;
//				} else {
//					r = pr;
//					g = 0;
//					b = 255-pr;
//				}
////				int rgb = ((r)<<16)|((g)<<8)|(b);
//				String hr = Integer.toHexString(r);
//				String hg = Integer.toHexString(g);
//				String hb = Integer.toHexString(b);
//				if (hr.length() == 1) {
//					bf.append('0');
//				}
//				bf.append(hr);
//				if (hg.length() == 1) {
//					bf.append('0');
//				}
//				bf.append(hg);
//				if (hb.length() == 1) {
//					bf.append('0');
//				}
//				bf.append(hb+"\"");
//				bf.append("000000\"");
				bf.append(" lw 1,\\\n");
			}
		}
//		bf.append("\nset terminal pdfcairo color solid size 7in,4in font \"Bitstream, 16\"\n");
//		bf.append("set output \"space_time.pdf\"\n");
//		bf.append("replot\n");
//		bf.append("set terminal aqua\n");
//		bf.append("replot\n");
		bf.close();
	}

}
