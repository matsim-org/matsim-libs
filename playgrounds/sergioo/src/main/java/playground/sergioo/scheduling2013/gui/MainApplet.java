package playground.sergioo.scheduling2013.gui;

import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;

import playground.sergioo.passivePlanning2012.core.population.PlaceSharer;
import playground.sergioo.scheduling2013.SchedulingNetwork;
import playground.sergioo.scheduling2013.SchedulingNetwork.SchedulingLink;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

public class MainApplet extends PApplet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static int W;
	public static int H;
	
	private final VisualizersSet visualizersSet;
	private long time0;
	private int mouseYOld;
	private int mouseXOld;

	
	public MainApplet(ActivityFacilities facilities, PlaceSharer placeSharer, SchedulingNetwork schedulingNetwork, Network network, List<SchedulingLink> path) {
		visualizersSet = new VisualizersSet(facilities, placeSharer, schedulingNetwork, network, path);
	}
	@Override
	public void setup() {
		//library.generateRandomPublications(1000);
		W = displayWidth;
		H = displayHeight;
		size(displayWidth, displayHeight, P3D);
		PFont myFont = createFont("ETH Light", 64);
		textFont(myFont);
		textAlign(PConstants.CENTER, PConstants.TOP);
		rectMode(CENTER);
		time0 = System.currentTimeMillis();
		perspective((float)Math.PI/3, width/(float)height, 5, 40000);
		hint(PConstants.ENABLE_STROKE_PERSPECTIVE);
		
	}
	@Override
	public void draw() {
		pushMatrix();
		long time = System.currentTimeMillis();
		visualizersSet.refreshPoint(mouseX, mouseY, width, height); 
		visualizersSet.paint(this, (time-time0)/1000.0);
		popMatrix();		
		time0 = time;
	}
	@Override
	public void mouseDragged(MouseEvent event) {
		if(event.getButton()==PConstants.RIGHT) {
			visualizersSet.changeElevation((mouseY-mouseYOld)*Math.PI/displayHeight);
			visualizersSet.changeAzimuth(-(mouseX-mouseXOld)*Math.PI/displayHeight);
		}
		mouseYOld = mouseY;
		mouseXOld = mouseX;
	}
	@Override
	public void mouseMoved(MouseEvent event) {
		mouseYOld = mouseY;
		mouseXOld = mouseX;
	}
	@Override
	public void mousePressed(MouseEvent event) {
		/*if(!visualizersSet.isMoveCamera()) {
			visualizersSet.refreshPoint(mouseX, mouseY, width, height);
			boolean change = visualizersSet.pressButton();
			if(change)
				visualizersSet.selectPlaceBars(width, height, this);
		}*/
	}
	@Override
	public void keyTyped(KeyEvent event) {
		switch(event.getKey()) {
		case 'n':
			visualizersSet.setNetworkVisible();
			break;
		}
	}
	public void cylinder(double rBase, double rTop, int height, int slices) {
		pushMatrix();
			beginShape();
			for(int i=0; i<slices; i++) {
				double teta = 2*Math.PI*i/slices;
				vertex((float)(rBase*Math.cos(teta)), (float)(-rBase*Math.sin(teta)), 0);
			}
			endShape();
			for(int i=0; i<slices; i++) {
				double teta = 2*Math.PI*i/slices;
				double teta2 = 2*Math.PI*(i+1)/slices;
				beginShape();
				vertex((float)(rBase*Math.cos(teta)), (float)(-rBase*Math.sin(teta)), 0);
				vertex((float)(rTop*Math.cos(teta)), (float)(-rTop*Math.sin(teta)), height);
				vertex((float)(rTop*Math.cos(teta2)), (float)(-rTop*Math.sin(teta2)), height);
				vertex((float)(rBase*Math.cos(teta2)), (float)(-rBase*Math.sin(teta2)), 0);
				vertex((float)(rBase*Math.cos(teta)), (float)(-rBase*Math.sin(teta)), 0);
				endShape();
			}
			beginShape();
			for(int i=0; i<slices; i++) {
				double teta = 2*Math.PI*i/slices;
				vertex((float)(rTop*Math.cos(teta)), (float)(-rTop*Math.sin(teta)), height);
			}
			endShape();
			
		popMatrix();
	}

}
