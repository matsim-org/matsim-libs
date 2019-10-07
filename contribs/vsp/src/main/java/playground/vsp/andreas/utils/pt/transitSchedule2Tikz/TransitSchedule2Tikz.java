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
package playground.vsp.andreas.utils.pt.transitSchedule2Tikz;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author droeder
 *
 */
public class TransitSchedule2Tikz {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(TransitSchedule2Tikz.class);
	private HashMap<Id, Id> stopId2tikzId;
	private HashMap<Id, TikzNode> tikzNodes;
	private double maxX = - Double.MAX_VALUE;
	private double maxY = - Double.MAX_VALUE;
	private double minY = Double.MAX_VALUE;
	private double minX = Double.MAX_VALUE;
	
	
	private String TIKZBEGIN = "\\begin{tikzpicture}[bend right=7, >=stealth, scale=1, transform shape]";
	
	private String USEDNODE = "usedNode";
	private String USEDNODESTYLE = "\\tikzstyle{" + USEDNODE + "} = [draw=black, fill=white, circle, inner sep = 2pt]";
	private String UNUSEDNODE = "unusedNode";
	private String UNUSEDNODESTYLE = "\\tikzstyle{" + UNUSEDNODE + "} = [draw=black!30, fill=white, circle, inner sep = 2pt]";
	private String USEDEDGE = "usdeEdge";
	private String USEDEDGESTYLE= "\\tikzstyle{" + USEDEDGE + "} = [black, ->]";
	private String UNUSEDEDGE = "unusdeEdge";
	private String UNUSEDEDGESTYLE= "\\tikzstyle{" + UNUSEDEDGE + "} = [black!30, ->]";
	private String TERMINI = "termini";
	private String TERMINISTYLE = "\\tikzstyle{" + TERMINI + "} = [black, ->>]";
	private boolean createComplDoc;
	private double width;

	/**
	 * 
	 * @param createCompleteDocument, true to create documents that ay be compiled
	 * @param widthHeight, of the desired output. Output is scaled, so that the height and width is not bigger than this... should be e.g. the width of an a4-paper
	 */
	public TransitSchedule2Tikz(boolean createCompleteDocument, double widthHeight) {
		this.createComplDoc = createCompleteDocument;
		this.width = widthHeight;
	}
	
	/**
	 * 
	 * @param schedule
	 * @param outdir
	 */
	public void createTikzPictures(TransitSchedule schedule, String outdir){
		createTikzNodes(schedule.getFacilities());
		offsetNodes();
		createAndWriteTikzlines(schedule.getTransitLines(), outdir);
	}

	/**
	 * 
	 */
	private void offsetNodes() {
		// start our coordinateSystem at (0,0)
		Double xOffset = - this.minX;
		Double yOffset = - this.minY;
		Double scale; 
		// scale, maybe use width and height here?
		if((this.maxX - this.minX) > (this.maxY- this.minY)){
			scale = new Double(this.width) / (this.maxX - this.minX) ;
		}else{
			scale = new Double(this.width) / (this.maxY - this.minY) ;
		}
		for(TikzNode n: this.tikzNodes.values()){
			n.offset(xOffset, yOffset, scale);
		}
	}

	/**
	 * @param facilities
	 */
	private void createTikzNodes(Map<Id<TransitStopFacility>, TransitStopFacility> facilities) {
		int i = 0;
		this.tikzNodes = new HashMap<Id, TikzNode>();
		this.stopId2tikzId = new HashMap<Id, Id>();
		// we only one node per coordinate. That is, the stopfacilities need to be preprocessed
		QuadTree<TikzNode> quadTree = new QuadTree<TikzNode>(-Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
		TikzNode tikzNode;
		for(TransitStopFacility f: facilities.values()){
			tikzNode = null;
			// find all very close nodes
			Collection<TikzNode> temp = quadTree.getDisk(f.getCoord().getX(), f.getCoord().getY(), 0.1);
			// check if one got the same coords as the current facility
			for(TikzNode n: temp){
				if(n.getCoord().equals(f.getCoord())){
					// there is one, store it
					tikzNode = n;
					break;
				}
			}
			// otherwise create a new node
			if(tikzNode == null){
				tikzNode = new TikzNode(f, i++);
				quadTree.put(tikzNode.getCoord().getX(), tikzNode.getCoord().getY(), tikzNode);
				this.tikzNodes.put(tikzNode.getId(), tikzNode);
				// necessary for scaling later
				findMinMaxValues(f.getCoord());
			}
			// map the stopId to the node id
			this.stopId2tikzId.put(f.getId(), tikzNode.getId());
		}
	}
	

	/**
	 * @param coord
	 */
	private void findMinMaxValues(Coord coord) {
		if(coord.getX() < this.minX) this.minX = coord.getX();
		if(coord.getY() < this.minY) this.minY = coord.getY();
		if(coord.getX() > this.maxX) this.maxX = coord.getX();
		if(coord.getY() > this.maxY) this.maxY = coord.getY();
	}

	/**
	 * @param transitLines
	 */
	private void createAndWriteTikzlines(Map<Id<TransitLine>, TransitLine> transitLines, String outdir) {
		for(TransitLine l: transitLines.values()){
			for(TransitRoute r: l.getRoutes().values()){
				write(outdir, r);
			}
		}
	}

	/**
	 * @param outdir
	 */
	private void write(String outdir, TransitRoute r) {
		BufferedWriter w = IOUtils.getBufferedWriter(outdir + r.getId().toString() + ".tex");
		Map<Id, String> preprocessedNodeStrings = new HashMap<Id, String>();
		Set<String> links = new HashSet<String>();
		// create the ouputstrings for the nodes
		for(TikzNode n: tikzNodes.values()){
			preprocessedNodeStrings.put(n.getId(), n.getTikzString(UNUSEDNODE));
		}
		Id stopId, tikzId;
		// overwritte the outputs for used Nodes and create usedLinks
		Id oldStopId = r.getStops().get(0).getStopFacility().getId();
		Id oldTikzId = this.stopId2tikzId.get(oldStopId);
		preprocessedNodeStrings.put(oldTikzId, tikzNodes.get(oldTikzId).getTikzString(USEDNODE));
		for(int i = 1; i < (r.getStops().size() -1 ); i++){
			stopId = r.getStops().get(i).getStopFacility().getId();
			tikzId = this.stopId2tikzId.get(stopId);
			preprocessedNodeStrings.put(tikzId, tikzNodes.get(tikzId).getTikzString(USEDNODE));
			links.add(new String("\\draw [" + USEDEDGE + "] (" + oldTikzId.toString() + ") to (" + tikzId.toString() + ");"));
			oldTikzId = tikzId;
		}
		// add the last node and the termini
		stopId = r.getStops().get(r.getStops().size() -1 ).getStopFacility().getId();
		tikzId = this.stopId2tikzId.get(stopId);
		preprocessedNodeStrings.put(tikzId, tikzNodes.get(tikzId).getTikzString(USEDNODE));
		links.add(new String("\\draw [" + TERMINI + "] (" + oldTikzId.toString() + ") to (" + tikzId.toString() + ");"));
		// write
		try {
			// you need a complete tex-file that compiles
			if(createComplDoc){
				w.write("\\documentclass{article}\n" +
						"\\usepackage{tikz}\n" +
						"\\begin{document}\n\n");
			}
			// add the styles
			w.write("\t%" + r.getDescription() + "\n");
			w.write("\t" + USEDNODESTYLE + "\n");
			w.write("\t" + UNUSEDNODESTYLE +"\n");
			w.write("\t" + USEDEDGESTYLE + "\n");
			w.write("\t" + UNUSEDEDGESTYLE + "\n");
			w.write("\t" + TERMINISTYLE + "\n");
			w.write("\t" + TIKZBEGIN + "\n");
			// add the nodes
			for(String s: preprocessedNodeStrings.values()){
				w.write("\t\t" + s + "\n");
			}
			// add the links (or better to say the connections between served stops)
			for(String s: links){
				w.write("\t\t" + s + "\n");
			}
			// finish the tikzpicture
			w.write("\t\\end{tikzpicture}\n\n");
			// maybe finish the document
			if(createComplDoc){
			w.write("\\end{document}");
			}
			w.flush();
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		String dir = "C:/Users/Daniel/Desktop/test/";
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().transit().setUseTransit(true);
//		new TransitScheduleReader(sc).readFile(dir + "schedule.xml");
		new TransitScheduleReader(sc).readFile(dir + "schedule.xml.gz");
		new TransitSchedule2Tikz(true, 10).createTikzPictures(sc.getTransitSchedule(), dir);
	}
}


