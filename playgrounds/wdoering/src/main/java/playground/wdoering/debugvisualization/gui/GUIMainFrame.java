package playground.wdoering.debugvisualization.gui;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import playground.wdoering.debugvisualization.controller.Console;
import playground.wdoering.debugvisualization.controller.Controller;

//import processing.core.PApplet;


public class GUIMainFrame extends JFrame {
	GUIToolbar guiToolbar;
	P3DRenderer renderer;
	Console console;

	public GUIMainFrame(Controller controller) {
		
		super("Embedded PApplet");
		this.guiToolbar = new GUIToolbar(controller);
		
		this.console = controller.console;
		

		setSize(768, 768);

		setLayout(new BorderLayout());
		renderer = new P3DRenderer();

		add(renderer, BorderLayout.CENTER);
		add(guiToolbar, BorderLayout.SOUTH);

		renderer.init();
	}

	public void setTimeRange(Double from, Double to) {
		renderer.setTimeRange(from,to);
		guiToolbar.setTimeRange(from, to);
		
	}

	public void setPositionRange(Point min, Point max) {
		renderer.setPositionRange(max,min);
		guiToolbar.setPositionRange(max,min);

	}
	
	 
}
