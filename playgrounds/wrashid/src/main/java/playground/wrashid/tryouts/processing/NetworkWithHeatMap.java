package playground.wrashid.tryouts.processing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.GeneralLib;
import processing.core.*;

public class NetworkWithHeatMap extends PApplet {

	float scaler = 1;
	private Network network;
	float smallestX = Float.MAX_VALUE;
	float smallestY = Float.MAX_VALUE;
	float biggestX = Float.MIN_VALUE;
	float biggestY = Float.MIN_VALUE;

	public void setup() {
		size(1000, 1000);
		smooth();
		noStroke();

		network = GeneralLib.readNetwork("H:/data/THELMA/for psi/10pctSwitzerland-ivtch-27.02.2012/network.xml");
		// network =
		// GeneralLib.readNetwork("C:/data/workspace/playgrounds/wrashid/test/scenarios/chessboard/network.xml");

		for (Node node : network.getNodes().values()) {
			double x = node.getCoord().getX();
			double y = node.getCoord().getY();

			if (smallestX > x) {
				smallestX = (float) x;
			}

			if (smallestY > y) {
				smallestY = (float) y;
			}

			if (biggestX < x) {
				biggestX = (float) x;
			}

			if (biggestY < y) {
				biggestY = (float) y;
			}

		}

	}

	public void draw() {

		strokeWeight((float) 0.01);
		translate(width / 2, height / 2); // use translate around scale
		scale(scaler);
		translate(-width / 6, (float) (-height / 1.05)); // to scale from the
															// center

		background(255);
		// fill(0);
		// stroke(255);
		// stroke(0);
		drawNetwork();

		/*
		 * ellipse(140,140,140,140); ellipse(250,200,200,200);
		 * ellipse(400,200,70,70); ellipse(230,320,20,20);
		 * ellipse(400,400,50,50); fill(255); ellipse(width/2,height/2,30,30);
		 * fill(255,0,0); ellipse(width/2,height/2,1,1);
		 */
	}

	private void drawNetwork() {
		// program it, as you would think it is write -> need tranformation
		// function.

		CoordImpl zuerichCoord = new CoordImpl(683248, 248161);
		double maxDistanceInMeters = 500000;

		float scalingFactor = width / (biggestX - smallestX);
		// System.out.println(scalingFactor);
		for (Link link : network.getLinks().values()) {

			float xStart = (float) link.getFromNode().getCoord().getX() * scalingFactor;
			float yStart = height - (float) link.getFromNode().getCoord().getY() * scalingFactor;
			float xEnd = (float) link.getToNode().getCoord().getX() * scalingFactor;
			float yEnd = height - (float) link.getToNode().getCoord().getY() * scalingFactor;

			if (GeneralLib.getDistance(zuerichCoord, link.getFromNode().getCoord()) < maxDistanceInMeters / 2) {

				float distanceScaled = (float) ((GeneralLib.getDistance(zuerichCoord, link.getFromNode().getCoord()))
						/ (maxDistanceInMeters / 2));

				float r = 255 * (1 - distanceScaled);
				float g = 255 * distanceScaled;

				stroke(r, g, 0);
			} else if (GeneralLib.getDistance(zuerichCoord, link.getFromNode().getCoord()) < maxDistanceInMeters) {

				float distanceScaled = (float) ((GeneralLib.getDistance(zuerichCoord, link.getFromNode().getCoord()) - (maxDistanceInMeters / 2))
						/ (maxDistanceInMeters / 2));

				float g = 255 * (1 - distanceScaled);
				float b = 255 * distanceScaled;

				stroke(0, g, b);
			} else {
				stroke(0);
			}

			line(xStart, yStart, xEnd, yEnd);
		}

	}

	public void keyPressed() {
		if (key == 'z') {
			scaler -= 0.1;
		} // smaller
		if (key == 'x') {
			scaler += 0.1;
		} // bigger
		if (key == 'c') {
			scaler = 1;
		} // reset scale
	}
}