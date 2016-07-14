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
package playground.agarwalamit.templates;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.agarwalamit.utils.LoadMyScenarios;
import processing.core.PApplet;

/**
 * @author amit
 */

public class ProcessingExample extends PApplet{
	
	private static int scaleDownSize = 2;
	
	private static Network net ;
	
	public static void main(String[] args) {
		loadMatsimNetwork();
		PApplet.main(new String [] {"--present","playground.agarwalamit.templates.ProcessingExample"});
	}
	
	@Override
	public void draw() {
		this.background(255) ; // "clears" the background

		// shifting origin similar to Cartesian coordinates (+y on top, +x on right).
		this.scale(1, -1);
		this.translate(0, -this.height);
		
		this.translate(this.displayWidth/2, this.displayHeight/2); // shifting, origin in the middle of the screen.
		
		this.stroke( 255, 0, 0) ; // nodes in red circles
		this.strokeWeight(15); // Relatively bigger
		
		for (Node n : net.getNodes().values()){ 
			Coord nodeCoord = n.getCoord();
			this.point(  (float)nodeCoord.getX()/scaleDownSize, (float)nodeCoord.getY()/scaleDownSize);
		}
		
		this.stroke( 0, 0, 255, 125) ; // links (blue transparent) 
		this.strokeWeight(10); // Relatively bigger
		
		for(Link l :net.getLinks().values()){
			Coord fromNode = l.getFromNode().getCoord();
			Coord toNode = l.getToNode().getCoord();
			this.line((float)fromNode.getX()/scaleDownSize, (float)  fromNode.getY()/scaleDownSize, (float)toNode.getX()/scaleDownSize, (float) toNode.getY()/scaleDownSize);
		}
	}

	@Override
	public void settings() { // setup does not work here when not using the PDE
		size(this.displayWidth, this.displayHeight ); // full screen
	}
	
	private static void loadMatsimNetwork(){
		String networkFile = "../../../../repos/shared-svn/projects/mixedTraffic/triangularNetwork/run313/singleModes/withoutHoles/car/network.xml";
		net = LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork();
	}
}
