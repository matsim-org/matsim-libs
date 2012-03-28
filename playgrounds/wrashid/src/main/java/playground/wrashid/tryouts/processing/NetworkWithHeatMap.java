package playground.wrashid.tryouts.processing;

import java.awt.Event;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.GeneralLib;
import processing.core.*;

// how to zoom properly + select area?
// how to put time bar on right side
// draw parking?
// draw charging behaviour

public class NetworkWithHeatMap extends PApplet {

	private float scaler = 1;
	private Network network;
	float smallestX = Float.MAX_VALUE;
	float smallestY = Float.MAX_VALUE;
	float biggestX = Float.MIN_VALUE;
	float biggestY = Float.MIN_VALUE;
	private double maxDistanceInMeters = 500000;
	private float moveTranslateX=0;
	private float moveTranslateY=0;
	
	
	public void setup() {

		addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				adjustScaleFactor(e);
			}
		});

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

	addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent e) {
				mouseX = e.getX();
				mouseY = e.getY();
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				//System.out.println(moveTranslateX);
				//System.out.println(moveTranslateY);
				moveTranslateX+=e.getX()-mouseX;
				moveTranslateY+=e.getY()-mouseY;
				mouseX = e.getX();
				mouseY = e.getY();
			
			}
		});
	
	addMouseListener(new MouseListener() {
		
		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			if (mouseEvent.getClickCount()==2) println("<double click>");
			
		}
	});
		
	}

	public void adjustScaleFactor(MouseWheelEvent e) {

		if (e.getWheelRotation() < 0) {
			setScaler(getScaler() + 0.1f);
		}

		if (e.getWheelRotation() > 0) {
			setScaler(getScaler() - 0.1f);
		}
		
	}

	public void draw() {

		
		strokeWeight((float) 0.01);
		

		translate(moveTranslateX,moveTranslateY);
		translate(-scaler*width/2,-scaler*height/2);
		scale(scaler);
	

		background(255);
	
		drawNetwork();

	
	}

	private void drawNetwork() {
		// program it, as you would think it is write -> need tranformation
		// function.

		CoordImpl zuerichCoord = new CoordImpl(683248, 248161);

		float scalingFactor = width / (biggestX - smallestX);
		// System.out.println(scalingFactor);
		for (Link link : network.getLinks().values()) {

			float xStart = (float) link.getFromNode().getCoord().getX() * scalingFactor;
			float yStart = height - (float) link.getFromNode().getCoord().getY() * scalingFactor;
			float xEnd = (float) link.getToNode().getCoord().getX() * scalingFactor;
			float yEnd = height - (float) link.getToNode().getCoord().getY() * scalingFactor;

			if (GeneralLib.getDistance(zuerichCoord, link.getFromNode().getCoord()) < getMaxDistanceInMeters() / 2) {

				float distanceScaled = (float) ((GeneralLib.getDistance(zuerichCoord, link.getFromNode().getCoord())) / (getMaxDistanceInMeters() / 2));

				float r = 255 * (1 - distanceScaled);
				float g = 255 * distanceScaled;

				stroke(r, g, 0);
			} else if (GeneralLib.getDistance(zuerichCoord, link.getFromNode().getCoord()) < getMaxDistanceInMeters()) {

				float distanceScaled = (float) ((GeneralLib.getDistance(zuerichCoord, link.getFromNode().getCoord()) - (getMaxDistanceInMeters() / 2)) / (getMaxDistanceInMeters() / 2));

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
			setScaler(getScaler() - 0.2f);
		} // smaller
		if (key == 'x') {
			setScaler(getScaler() + 0.2f);
		} // bigger
		if (key == 'c') {
			setScaler(1);
		} // reset scale
	}

	public void setMaxDistanceInMeters(double maxDistanceInMeters) {
		this.maxDistanceInMeters = maxDistanceInMeters;
	}

	public double getMaxDistanceInMeters() {
		return maxDistanceInMeters;
	}

	public void setScaler(float scaler) {
		this.scaler = scaler;
	}

	public float getScaler() {
		return scaler;
	}
}