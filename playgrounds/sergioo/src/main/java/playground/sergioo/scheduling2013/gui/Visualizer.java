package playground.sergioo.scheduling2013.gui;

import processing.core.PApplet;

public interface Visualizer {

	public void paintOnce(PApplet applet);
	public void paint(PApplet applet, double time);
	
}
