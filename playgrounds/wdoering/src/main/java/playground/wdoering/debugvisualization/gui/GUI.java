package playground.wdoering.debugvisualization.gui;

import java.awt.BorderLayout;
import java.awt.Point;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.JFrame;

import playground.wdoering.debugvisualization.controller.Controller;
import playground.wdoering.debugvisualization.model.Agent;
import playground.wdoering.debugvisualization.model.DataPoint;
import playground.wdoering.debugvisualization.model.Scene;

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
	GUIInfo guiInfo;
	P3DRenderer renderer;
	Thread rendererThread = null;
	

	public GUI(Controller controller, int traceTimeRange, int width, int height, int visualizationMode) {
		
		super("Debug Visualization");
		
		this.controller = controller;
		setSize(width+512, height + 64);
		setLayout(new BorderLayout());
		
		//get toolbar and renderer (processing applet)
		guiToolbar = new GUIToolbar(controller);
		
		//this.rendererThread = new Thread(renderer = new P3DRenderer(controller, traceTimeRange, width, (int)(height), visualizationMode), "readerthread"); 
		//renderer = rendererThread.getClass();
		renderer = new P3DRenderer(controller, traceTimeRange, width, (height), visualizationMode);
		guiInfo = new GUIInfo(controller);
		
		//guiInfo.disableUpdate(true);
		
		//add elements to the jframe
		add(renderer, BorderLayout.CENTER);
		add(guiToolbar, BorderLayout.SOUTH);
		add(guiInfo,BorderLayout.EAST);
		
		//rendererThread.run();
	
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		renderer.init();
	}

	public void setTimeRange(Double from, Double to) {
		renderer.setTimeRange(from,to);
		guiToolbar.setTimeRange(from, to);
		
	}

	public void setPositionRange(Point min, Point max)
	{
		renderer.setPositionRange(max,min);
		guiToolbar.setPositionRange(max,min);

	}


	public void setAgentData(HashMap<String,Agent> agents, Double[] extremeValues, LinkedList<Double> timeSteps)
	{
		//load agent data into GUI.
		renderer.setAgentData(agents, extremeValues, timeSteps);

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

	public void updateAgentData(HashMap<String, Agent> agents)
	{
		renderer.updateAgentData(agents);
		
	}

	public void updateCurrentTime(double time)
	{
		renderer.updateCurrentTime(time);
		
	}

	public void updateAgentData(String ID, Double X, Double Y, Double time)
	{
		renderer.updateAgentData(ID, X, Y, time);
		
	}

	public void updateSceneView(LinkedList<Scene> scenes)
	{
		renderer.updateSceneView(scenes);
	}

	public void updateView(LinkedList<Double> timeSteps, HashMap<String, Agent> agents)
	{
		Double time = timeSteps.getLast();
		renderer.updateView(time, agents);
		guiInfo.updateView(time);
		
	}

	public void updateExtremeValues(Double maxPosX, Double minPosX, Double maxPosY, Double minPosY)
	{
		renderer.updateExtremeValues(maxPosX, minPosX, maxPosY, minPosY);		
	}

	public void suspendRenderThread() {
		if (rendererThread != null)
			rendererThread.suspend();
		
		
	}

	public void resumeRenderThread() {
		if (rendererThread != null)
		{
			rendererThread.resume();
//			try {
//				System.out.println("before sleep");
//				Thread.sleep(100);
//				System.out.println("after sleep");
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		
	}

	public void setOffset(int x, int y)
	{
		renderer.setOffset(x,y);
		guiToolbar.setOffsetText(x,y);
		
	}

	public void updateView(Double time, HashMap<String, Agent> agents)
	{
		renderer.updateView(time, agents);
		
		//guiInfo.updateView(time);
		
	}
	
	
}
