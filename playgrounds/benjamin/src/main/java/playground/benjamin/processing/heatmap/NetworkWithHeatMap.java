//package playground.benjamin.processing.heatmap;
//
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.network.Node;
//import org.matsim.contrib.parking.lib.GeneralLib;
//import org.matsim.core.utils.geometry.CoordImpl;
//
//// how to zoom properly + select area?
//// how to put time bar on right side
//// draw parking?
//// draw charging behaviour
//
//public class NetworkWithHeatMap extends MovableAndZoomable {
//
//	public boolean animateHeatWave=false; 
//	private Network network;
//	float smallestX = Float.MAX_VALUE;
//	float smallestY = Float.MAX_VALUE;
//	float biggestX = Float.MIN_VALUE;
//	float biggestY = Float.MIN_VALUE;
//	private double maxDistanceInMeters = 500000;
//
//	@Override
//	public void setup() {
//		super.setup();
//		//smooth();
//		//noStroke();
//
//		network = GeneralLib.readNetwork("/media/data/2_Workspaces/repos/runs-svn/detEval/latsis/output/output_baseCase_ctd_newCode/output_network.xml.gz");
//		// network =
//		// GeneralLib.readNetwork("C:/data/workspace/playgrounds/wrashid/test/scenarios/chessboard/network.xml");
//
//		for (Node node : network.getNodes().values()) {
//			double x = node.getCoord().getX();
//			double y = node.getCoord().getY();
//
//			if (smallestX > x) {
//				smallestX = (float) x;
//			}
//
//			if (smallestY > y) {
//				smallestY = (float) y;
//			}
//
//			if (biggestX < x) {
//				biggestX = (float) x;
//			}
//
//			if (biggestY < y) {
//				biggestY = (float) y;
//			}
//
//		}
//
//	}
//
//	@Override
//	public void draw() {
//		super.draw();
//
//		strokeWeight((float) 0.01);
//
//		// the following line is very important, because when moving/scaling, one sees
//		// the old locations.
//		background(255);
//		
//		if (animateHeatWave==true){
//			maxDistanceInMeters+=maxDistanceInMeters/20;
//		}
//
//		drawNetwork();
//
//	}
//
//	private void drawNetwork() {
//		// program it, as you would think it is write -> need tranformation
//		// function.
//
//		CoordImpl zuerichCoord = new CoordImpl(683248, 248161);
//
//		float scalingFactor = width / (biggestX - smallestX);
//		// System.out.println(scalingFactor);
//		for (Link link : network.getLinks().values()) {
//
//			float xStart = (float) link.getFromNode().getCoord().getX() * scalingFactor;
//			float yStart = height - (float) link.getFromNode().getCoord().getY() * scalingFactor;
//			float xEnd = (float) link.getToNode().getCoord().getX() * scalingFactor;
//			float yEnd = height - (float) link.getToNode().getCoord().getY() * scalingFactor;
//
//			if (GeneralLib.getDistance(zuerichCoord, link.getFromNode().getCoord()) < getMaxDistanceInMeters() / 2) {
//
//				float distanceScaled = (float) ((GeneralLib.getDistance(zuerichCoord, link.getFromNode().getCoord())) / (getMaxDistanceInMeters() / 2));
//
//				float r = 255 * (1 - distanceScaled);
//				float g = 255 * distanceScaled;
//
//				stroke(r, g, 0);
//			} else if (GeneralLib.getDistance(zuerichCoord, link.getFromNode().getCoord()) < getMaxDistanceInMeters()) {
//
//				float distanceScaled = (float) ((GeneralLib.getDistance(zuerichCoord, link.getFromNode().getCoord()) - (getMaxDistanceInMeters() / 2)) / (getMaxDistanceInMeters() / 2));
//
//				float g = 255 * (1 - distanceScaled);
//				float b = 255 * distanceScaled;
//
//				stroke(0, g, b);
//			} else {
//				stroke(0);
//			}
//
//			line(xStart, yStart, xEnd, yEnd);
//		}
//
//	}
//
//	public void setMaxDistanceInMeters(double maxDistanceInMeters) {
//		this.maxDistanceInMeters = maxDistanceInMeters;
//	}
//
//	public double getMaxDistanceInMeters() {
//		return maxDistanceInMeters;
//	}
//
//	public void setScaler(float scaler) {
//		this.scaler = scaler;
//	}
//
//	public float getScaler() {
//		return scaler;
//	}
//}