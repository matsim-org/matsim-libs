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
package playground.droeder.P2.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.droeder.DaFileReader;

/**
 * @author droeder
 *
 */
public class Output2TikzPicture {
	
	private final static String BEGIN = "\\tikzstyle{node} = [circle, draw, fill=white]" + "\n" +
		"\\begin{tikzpicture}[node distance=2.0cm, bend right=7, >=stealth, scale=0.9, transform shape]" + "\n";
	private final static String END = "\\end{tikzpicture}";
	
	
	public static void net2tik(Network net, String outputfile){
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
			b.append("\t\\draw [->] (" + 
					l.getFromNode().getId().toString().replace(".", "")  + 
					") to (" + 
					l.getToNode().getId().toString().replace(".", "") + ");\n");
		}
		
//		b.append(END);
		return b.toString();
	}
	
	public static void coopLogger2tik(Network net, String coopLoggerFile, String outDir, List<Integer> iterations){
		Set<String[]> lines = DaFileReader.readFileContent(coopLoggerFile, "\t", true);
		BufferedWriter w ;
		
		Map<Integer, Set<String[]>> iteration2line = new HashMap<Integer, Set<String[]>>();
		
		Set<String[]> temp;
		Integer iteration;
		
//		read and sort by iteration
		for(String[] l : lines){
			iteration = Integer.parseInt(l[0]);
			if(!iterations.contains(iteration)) continue;
			if(!iteration2line.containsKey(iteration)){
				temp = new HashSet<String[]>();
			}else{
				temp = iteration2line.get(iteration);
			}
			
			temp.add(l);
			iteration2line.put(iteration, temp);
		}
		
		for(Entry<Integer, Set<String[]>> e: iteration2line.entrySet()){
			String dir = outDir + "coop2tik/" + e.getKey().toString() + "/";
			File f = new File(dir);
			if(!f.exists()) f.mkdirs();
			
			String[] links;
			Link l;
			for(String[] s : e.getValue()){
				StringBuffer b = new StringBuffer();
				b.append("%status: " + s[2] + "\n");
				b.append("%#veh: " + s[3] + "\n");
				b.append("%#pax: " + s[4] + "\n");
				b.append("%score: " + s[5] + "\n");
				b.append("%budget: " + s[6] + "\n");
				b.append("%from: " + s[7] + "\n");
				b.append("%to: " + s[8] + "\n");
				b.append("%stops2BeServed: " + s[9] + "\n");
				b.append(BEGIN);
				b.append(getNet2Tik(net));
				links = s[10].replace("[", "").replace("]", "").split(", ");
				l = net.getLinks().get(new IdImpl(links[0]));
				b.append("\t\\draw [->>, red, thick] (" + 
						l.getFromNode().getId().toString().replace(".", "")  + 
						") to (" + 
						l.getToNode().getId().toString().replace(".", "") + ");\n");
				for(int i = 1; i< links.length; i++){
					l = net.getLinks().get(new IdImpl(links[i]));
					b.append("\t\\draw [->, red, thick] (" + 
							l.getFromNode().getId().toString().replace(".", "")  + 
							") to (" + 
							l.getToNode().getId().toString().replace(".", "") + ");\n");
				}
				b.append(END);
				w = IOUtils.getBufferedWriter(dir + s[1].replace("_", "-") + ".tex");
				try {
					w.write(b.toString());
					w.flush();
					w.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
		}
	}

	
	public static void main(String[] args){
		final String NET = "D:/VSP/net/ils/roeder/11x6/network.xml.gz";
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(NET);
		net2tik(sc.getNetwork(), "D:/VSP/net/ils/roeder/11x6/network.tex");
		List<Integer> i = new ArrayList<Integer>();
		i.add(10000);
		coopLogger2tik(sc.getNetwork(), "D:/VSP/net/ils/roeder/11x6/ExtendAndReduce/extendAndReduce.pCoopLogger.txt", "D:/VSP/net/ils/roeder/11x6/ExtendAndReduce/", i);
	}
}
