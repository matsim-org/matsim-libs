package playground.wdoering.debugvisualization.gui;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFrame;

import playground.wdoering.debugvisualization.controller.Controller;
import playground.wdoering.debugvisualization.model.Agent;
import playground.wdoering.debugvisualization.model.DataPoint;

//import processing.core.*; 

/**
 * DebugSim GUI
 * 
 * Containing processing applet and control elements
 * 
 * @author wdoering
 */
public class GUI extends JFrame { 
	
	private static final long serialVersionUID = 1L;
	float x, y;
	Controller controller;
	//private HashMap<Integer,Agent> agents;
	private Double[] extremeValues;
	
	GUIMainFrame guiMainFrame;
	GUIToolbar guiToolbar;
	P3DRenderer renderer;

	public GUI(Controller controller) {
		
		super("Debug Visualization");
		
		this.controller = controller;

		setSize(768, 800);
		setLayout(new BorderLayout());
		
		//get toolbar and renderer (processing applet)
		guiToolbar = new GUIToolbar(controller);
		renderer = new P3DRenderer();

		//add elements to the jframe
		add(renderer, BorderLayout.CENTER);
		add(guiToolbar, BorderLayout.SOUTH);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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


	public void setAgentData(HashMap<Integer,Agent> agents, Double[] extremeValues, Double[] timeSteps)
	{
		//load agent data into GUI.
		this.controller.console.print("loading agent data into GUI...");
		renderer.setAgentData(agents, extremeValues, timeSteps);
		this.controller.console.println("done.");

	}
	
	public void init()
	{
		
		
	}

	public void setAnimationSpeed(Float speed)
	{
		renderer.setAnimationSpeed(speed);
		
	}

	public void play()
	{
		
		renderer.playScenario();
		
	}

	public void pause()
	{
		renderer.togglePause();
		
	}

	public void rewind()
	{
		renderer.rewindScenario();
		
	}

	public void setNetwork(HashMap<Integer, DataPoint> nodes, HashMap<Integer, int[]> links)
	{
		renderer.setNetwork(nodes,links);
		
	}

	public void updateAgentData(HashMap<Integer, Agent> agents)
	{
		renderer.updateAgentData(agents);
		
	}

	public void updateCurrentTime(double time)
	{
		renderer.updateCurrentTime(time);
		
	}

	
	
}
