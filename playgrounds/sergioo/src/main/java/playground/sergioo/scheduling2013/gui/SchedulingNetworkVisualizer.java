package playground.sergioo.scheduling2013.gui;

import java.awt.Color;
import java.util.List;

import org.matsim.api.core.v01.network.Link;

import playground.sergioo.scheduling2013.SchedulingNetwork;
import playground.sergioo.scheduling2013.SchedulingNetwork.ActivitySchedulingLink;
import playground.sergioo.scheduling2013.SchedulingNetwork.JourneySchedulingLink;
import playground.sergioo.scheduling2013.SchedulingNetwork.SchedulingLink;
import playground.sergioo.scheduling2013.SchedulingNetwork.SchedulingNode;
import processing.core.PApplet;

public class SchedulingNetworkVisualizer implements Visualizer {

	static final float FACTOR = 2f/5;
	
	private final SchedulingNetwork network;
	private List<SchedulingLink> path;
	private final double smallestTime;
	private boolean isNetworkVisible = false;
	
	public SchedulingNetworkVisualizer(SchedulingNetwork network, List<SchedulingLink> path, double smallestTime) {
		this.network = network;
		this.path = path;
		this.smallestTime = smallestTime;
	}
	public void setNetworkVisible() {
		isNetworkVisible = !isNetworkVisible;
	}
	public void paintOnce(PApplet applet) {
		applet.strokeWeight(20);
		applet.stroke(55, 0, 55);
		applet.noFill();
		if(isNetworkVisible)
			for(Link link:network.getLinks().values())
				applet.line((float)link.getFromNode().getCoord().getX(), (float)-link.getFromNode().getCoord().getY(), (float)(((SchedulingNode)link.getFromNode()).getTime()-smallestTime)*FACTOR,
						(float)link.getToNode().getCoord().getX(), (float)-link.getToNode().getCoord().getY(), (float)(((SchedulingNode)link.getToNode()).getTime()-smallestTime)*FACTOR);
		for(SchedulingLink link:path) {
			applet.strokeWeight(100);
			if(link instanceof JourneySchedulingLink)
				if(((JourneySchedulingLink)link).getMode().equals("car"))
					applet.stroke(0, 255, 0);
				else if(((JourneySchedulingLink)link).getMode().equals("pt"))
					applet.stroke(255, 0, 0);
				else
					applet.stroke(255, 200, 255);
			else {
				applet.strokeWeight(200);
				Color color = VisualizersSet.COLORS.get(((ActivitySchedulingLink)link).getActivityType());
				applet.stroke(color.getRed(), color.getGreen(), color.getBlue());
			}
			applet.line((float)link.getFromNode().getCoord().getX(), (float)-link.getFromNode().getCoord().getY(), (float)(((SchedulingNode)link.getFromNode()).getTime()-smallestTime)*FACTOR,
					(float)link.getToNode().getCoord().getX(), (float)-link.getToNode().getCoord().getY(), (float)(((SchedulingNode)link.getToNode()).getTime()-smallestTime)*FACTOR);
		}
	}
	public void paint(PApplet applet, double time) {
		
	}

}
