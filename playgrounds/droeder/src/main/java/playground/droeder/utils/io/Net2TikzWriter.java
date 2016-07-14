/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.utils.io;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author droeder
 *
 */
public class Net2TikzWriter {
	
	private final static String BEGIN = "\\tikzstyle{node} = [draw=black, fill=white, circle, inner sep = 3pt]" + "\n" +
		"\\begin{tikzpicture}[bend right=7, >=stealth, scale=0.9, transform shape]" + "\n";
	private final static String END = "\\end{tikzpicture}";
	
	
//	########################################################################################################
	public static void writeTikzPictureFromNetwork(Network net, String outputfile){
		BufferedWriter w =IOUtils.getBufferedWriter(outputfile);
		
		try {
			w.write(BEGIN);
			w.write(getNet2Tik(net));
			w.write(END);
			w.flush();
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String getNet2Tik(Network net){
		StringBuffer b = new StringBuffer(); 
//		b.append(BEGIN);
		for(Node n: net.getNodes().values()){
			b.append("\t\\node [node] (" + n.getId().toString().replace(".", "") + ") at (" + 
					String.valueOf((int)n.getCoord().getX()/1000) + ", " + String.valueOf((int)n.getCoord().getY()/1000) + 
					") {}; \n");
		}
		
		for(Link l: net.getLinks().values()){
			if(!l.getToNode().getId().equals(l.getFromNode().getId())){
				b.append("\t\\draw [->] " + getLinkSequence(l));
			}
		}
		
//		b.append(END);
		return b.toString();
	}
	

	private static String getLinkSequence(Link l){
		return "(" + 
				l.getFromNode().getId().toString().replace(".", "")  + 
				") to (" + 
				l.getToNode().getId().toString().replace(".", "") + ");\n";
	}
	
//	########################################################################################################

}
