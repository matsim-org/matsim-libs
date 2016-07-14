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
package playground.gregor.gctpeds.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import com.fasterxml.jackson.core.JsonParseException;

import playground.gregor.casim.simulation.physics.AbstractCANetwork;

public class JPSReport {
	public static void main(String [] args) throws JsonParseException, IOException {
		Id<Link> linkId = Id.createLinkId("508");
		String link = "/Users/laemmel/devel/nyc/output_measurements/tr_link_"+linkId.toString()+".json";
		String conf = "/Users/laemmel/devel/nyc/gct_vicinity/config.xml.gz";
		String outDir = "/Users/laemmel/devel/nyc/output_measurements/jps_report/"+linkId.toString();
		
		double mL = 5;
		double mW = 6*AbstractCANetwork.PED_WIDTH;
/////////////////
		
		
		///Trajectories
		File f = new File(outDir);
		if (!f.exists()) {
			f.mkdirs();
		}
		
		Config c = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(c, conf);
		Scenario sc = ScenarioUtils.loadScenario(c);
		
		Link l = sc.getNetwork().getLinks().get(linkId);
		double w = l.getCapacity();
		double frX = l.getFromNode().getCoord().getX();
		double frY = l.getFromNode().getCoord().getY();
		double toX = l.getToNode().getCoord().getX();
		double toY = l.getToNode().getCoord().getY();
		double dx = toX-frX;
		double dy = toY-frY;
		double len = Math.sqrt(dx*dx+dy*dy);
		dx /= len;
		dy /= len;
		double x0 = frX-dy*w/2;
		double y0 = frY+dx*w/2;
		double x1 = toX-dy*w/2;
		double y1 = toY+dx*w/2;
		double x2 = toX+dy*w/2;
		double y2 = toY-dx*w/2;
		double x3 = frX+dy*w/2;
		double y3 = frY-dx*w/2;
		double minX = Math.min(Math.min(x0,x1), Math.min(x2, x3));
		double minY = Math.min(Math.min(y0,y1), Math.min(y2, y3));
		Trajectories tra = new Trajectories();
		new JSONParser(link,tra).run();
		tra.dumpAsJuPedSimTrajectories(minX,minY,3600*16+26*60+30,3600*16+28*60,outDir+"/trajectories.txt");
		
		///Geometry
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outDir+"/geometry.xml")));
		bw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
		bw.append("<geometry version =\"0.5\" caption=\"geometry\" unit=\"m\">\n");
		bw.append("\t<rooms>\n");
		bw.append("\t\t<room id=\"0\" caption=\"hall\" >\n");
		bw.append("\t\t\t<subroom id=\"0\" class=\"subroom\" >\n");
		bw.append("\t\t\t\t<polygon caption=\"wall\" >\n");	
		bw.append("\t\t\t\t\t<vertex px=\""+(x0-minX)+"\" py=\""+(y0-minY)+"\" />\n");
		bw.append("\t\t\t\t\t<vertex px=\""+(x1-minX)+"\" py=\""+(y1-minY)+"\" />\n");
		bw.append("\t\t\t\t\t<vertex px=\""+(x2-minX)+"\" py=\""+(y2-minY)+"\" />\n");
		bw.append("\t\t\t\t\t<vertex px=\""+(x3-minX)+"\" py=\""+(y3-minY)+"\" />\n");
		bw.append("\t\t\t\t\t<vertex px=\""+(x0-minX)+"\" py=\""+(y0-minY)+"\" />\n");
		bw.append("\t\t\t\t</polygon>\n");
		bw.append("\t\t\t</subroom>\n");
		bw.append("\t\t</room>\n");
		bw.append("\t</rooms>\n");
		bw.append("</geometry>\n");
		bw.close();
		
		
		///JPS ini
		
		double mX0 = frX+dx*(len/2-mL/2)-dy*mW/2-minX;
		double mY0 = frY+dy*(len/2-mL/2)+dx*mW/2-minY;
		double mX1 = frX+dx*(len/2+mL/2)-dy*mW/2-minX;
		double mY1 = frY+dy*(len/2+mL/2)+dx*mW/2-minY;
		double mX2 = frX+dx*(len/2+mL/2)+dy*mW/2-minX;
		double mY2 = frY+dy*(len/2+mL/2)-dx*mW/2-minY;
		double mX3 = frX+dx*(len/2-mL/2)+dy*mW/2-minX;
		double mY3 = frY+dy*(len/2-mL/2)-dx*mW/2-minY;
		
		String jpsIniStub = System.getProperty("user.dir") + "/src.main.resources/jpsreport/jps_ini.xml";
		BufferedWriter bw2 = new BufferedWriter(new FileWriter(new File(outDir+"/jps_ini.xml")));
		BufferedReader br = new BufferedReader(new FileReader(new File(jpsIniStub)));
		String line = br.readLine();
		while (line != null) {
			if (line.contains("<!-- insert measurement areas here -->")) {
				bw2.append("    <measurementAreas unit=\"m\">\n");
				bw2.append("    <area_B id=\"1\" type=\"BoundingBox\">\n");
				bw2.append("      <vertex x=\""+mX0+"\" y=\""+mY0+"\" />\n");
				bw2.append("      <vertex x=\""+mX1+"\" y=\""+mY1+"\" />\n");
				bw2.append("      <vertex x=\""+mX2+"\" y=\""+mY2+"\" />\n");
				bw2.append("      <vertex x=\""+mX3+"\" y=\""+mY3+"\" />\n");
				bw2.append("      <Length_in_movement_direction distance=\""+mL+"\" />\n");
				bw2.append("    </area_B>\n");
				bw2.append("    <area_L id=\"2\" type=\"Line\">\n");
				bw2.append("      <start x=\""+(x0+dx*(len/2)-minX)+"\" y=\""+(y0+dy*(len/2)-minY)+"\" />\n");
				bw2.append("      <end x=\""+(x3+dx*(len/2)-minX)+"\" y=\""+(y3+dy*(len/2)-minY)+"\" />\n");
				bw2.append("     </area_L>\n");
				bw2.append("     </measurementAreas>\n");
			} else {
				bw2.append(line);
				bw2.append('\n');
			}
			line = br.readLine();
 
		}
		bw2.close();
		br.close();
	
		
		
	}

}
