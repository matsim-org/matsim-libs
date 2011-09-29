package playground.wdoering.debugvisualization.gui;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import playground.wdoering.debugvisualization.controller.Console;

import playground.wdoering.debugvisualization.model.Agent;
import playground.wdoering.debugvisualization.model.DataPoint;
import processing.core.PApplet;
import processing.core.PImage;

//import processing.core.*;
//import processing.opengl.*;

/**
 * GL Renderer
 * 
 * Extends the processing applet.
 * Currently NOT using GL.
 * 
 * @author wdoering
 *
 */
public class P3DRenderer extends PApplet
{

	private float agentSize = 20; //(meter / si units)
	private HashMap<Integer, Agent> agents;
	private HashMap<Integer, DataPoint> nodes;
	private HashMap<Integer, int[]> links;
	
	
	private Double[] extremeValues;
	private Double[] timeSteps;
	private boolean paused;
	private boolean rewind;
	
	private float factorX;
	private float factorY;
	private float i;
	private int iInt;
	private double minPosX; 
	private double maxPosX; 
	private double minPosY; 
	private double maxPosY; 
	ArrayList<int[]> colors;
	
	
	public boolean isPaused()
	{
		return paused;
	}

	public void setPaused(boolean paused)
	{
		this.paused = paused;
	}

	public void setAgentData(HashMap<Integer, Agent> agents, Double[] extremeValues, Double[] timeSteps)
	{
		//pass agents through
		this.agents = agents;

		//get extreme values, store them locally
		
		//Double[] extremeValues = {maxPosX, maxPosY, maxPosZ, minPosX, minPosY, minPosZ, maxTimeStep, minTimeStep};
		//                          0        1        2        3        4        5        6            7
		
		this.extremeValues = extremeValues;
		this.maxPosX = extremeValues[0];		
		this.maxPosY = extremeValues[1];
		this.minPosX = extremeValues[3];		
		this.minPosY = extremeValues[4];
		
		//pass time step data through
		this.timeSteps = timeSteps;		
		
		//set up color matrix to display agents
		this.colors = new ArrayList<int[]>();
		
		//give each agent a different (preattentive) color. If there are more then 10, generate the colors.  
		if (agents.size()>10)
		{
		
			for (int j = 0; j < agents.size(); j++)
			{
				//determine color
				int[] color = {(int)((((float)agents.size()-(float)j)/(float)agents.size())*255),
						       (int)((double)(((float)agents.size()-(float)j)/(float)agents.size())*255),
						       (int)(255-(double)(((float)agents.size()-(float)j)/(float)agents.size())*255)};
				
				//System.out.println(color[0] + "|" + color[1] + "|" + color[2]);
				
				//add color to the color array
				colors.add(color);
			}
		}
		else //otherwise use these 10 optimized colors (src: Colin Ware)
		{
		
			String colorsRGB[] = {  "#FB253C",
									"#F2F319",
									"#3BCF49",
									"#413FB0",
									"#B82828",
									"#AC0297",
									"#FCA147",
									"#98A192",
									"#72D2D2",
									"#FC82AF" };
			
			for (int j= 0; j < agents.size(); j++)
			{
				int[] color = { Integer.parseInt(colorsRGB[j].substring(1,3),16),
							    Integer.parseInt(colorsRGB[j].substring(3,5),16),
							    Integer.parseInt(colorsRGB[j].substring(5,7),16)};
				
				colors.add(color);
			}
		}

	}

	public void setTimeRange(Double from, Double to)
	{
		//not implemented yet

	}

	float c;

	//not implemented yet
	PImage b = loadImage("C:\\temp5\\Erdgeschoss.jpg");

	public void setup()
	{
		size(768, 768, P3D);
		frameRate(30);

		noStroke();
		
		this.i = 0;		
		paused=false;
		rewind=false;
		
	}

	/**
	 * draw function. calculating proportions.
	 * iterating through agent data, drawing agents,
	 * agent trajectories and network.
	 * 
	 */
	public void draw()
	{
		
		if (extremeValues!=null)
		{
		
			if (!isPaused())
			{
		
				if (factorX == 0.0f)
				{
					//get max values
					float maxWidth = (float)(extremeValues[0]-extremeValues[3]);
					float maxHeight = (float)(extremeValues[1]-extremeValues[4]);
									
					//refactor min/max values of event+network file
					this.factorX = maxWidth / this.width;
					this.factorY = maxHeight / this.height;
					
					System.out.println(((this.maxPosX-this.minPosX) /factorX));
				}
				
				
				background(0);
				
				if (!isPaused())
				{
					if ((!rewind) && (i < timeSteps.length -1))
						i+=0.1;
					else if ((rewind) && (i > 0))
						i-=0.1;
				}
				
				iInt = (int)(Math.floor(i));
	
				//Draw Network
				if ((nodes!=null) && (links!=null))
				{
					//Stroke color
					stroke(100,0,100,100);
					
					//Iterate through all nodes and the draw links
					for (Iterator linksIterator = links.values().iterator(); linksIterator.hasNext();)
					{
						int[] fromTo = (int[]) linksIterator.next();
						
						DataPoint fromDataPoint = nodes.get(fromTo[0]);
						DataPoint ToDataPoint = nodes.get(fromTo[1]);
						
						//draw a line (from - to - datapoint)
						line((float)((fromDataPoint.getPosX()-minPosX)/factorX),
							(float)((fromDataPoint.getPosY()-minPosY)/factorY),
							(float)((ToDataPoint.getPosX()-minPosX)/factorX),
							(float)((ToDataPoint.getPosY()-minPosY)/factorY));
							
						
					}
					noStroke();
				}
				
				//Draw Agents and trajectories
				int currentAgent = 0;
				if (agents != null)
				{
					
					//Iterate through all agents and display the current data point + traces
					Iterator agentsIterator = agents.entrySet().iterator();
					
					while (agentsIterator.hasNext())
					{
						//Get current agent
						Map.Entry pairs = (Map.Entry) agentsIterator.next();
						Agent agent = (Agent)pairs.getValue();
						HashMap<Double,DataPoint> dataPoints = agent.getDataPoints();
						
						//Motion tween between two datapoints (-> time steps)
						int tweenCount = 0;
	
						//pick preattentive agent color
						int[] agentColor = colors.get(currentAgent);
						
						//first check if there is any datapoint stored to the current time step
						if (dataPoints.get(timeSteps[iInt]) != null) 
						{
							//get the datapoint
							DataPoint currentDataPoint = (DataPoint) dataPoints.get(timeSteps[iInt]); 
							
							float posX;
							float posY;
							
							//calculate tweened x/y position
							if ((tweenCount<dataPoints.size()-2))
							{
								DataPoint nextDataPoint = (DataPoint) dataPoints.get(timeSteps[iInt+1]);
								
								if (nextDataPoint==null)
									nextDataPoint = currentDataPoint;
									
								posX = (float)(        (  ((currentDataPoint.getPosX()-minPosX)*(1-(i-Math.floor(i))))
													   +  ((nextDataPoint.getPosX()-minPosX)*(i-Math.floor(i)))        )
													   / factorX);
								
								posY = (float)(( (  	(currentDataPoint.getPosY()-minPosY)*(1-(i-Math.floor(i))))
													   +  ((nextDataPoint.getPosY()-minPosY)*(i-Math.floor(i)))        )
													   / factorY);
									
									
							}
							else
							{
								posX = (float)((currentDataPoint.getPosX()-minPosX) / factorX);
								posY = (float)((currentDataPoint.getPosY()-minPosY) / factorY);
							}
							
							//draw trajectories
							for (int j = 1; j <= iInt; j++)
							{
								
								if ((dataPoints.get(timeSteps[j]) != null) && (dataPoints.get(timeSteps[j-1]) != null))
								{
									DataPoint trajectoryDataPoint1 = dataPoints.get(timeSteps[j]);
									DataPoint trajectoryDataPoint2 = dataPoints.get(timeSteps[j-1]);
									
									smooth();
									strokeWeight(4); 
									
									//make far away trajectories more transparent
									float jFloat = (float)j;
									float iFloat = (float)iInt;
									stroke(agentColor[0], agentColor[1],agentColor[2],255-(int)(255f*((iFloat+1f-jFloat)/iFloat)));
									
									line((float)((trajectoryDataPoint1.getPosX()-minPosX)/factorX),
										 (float)((trajectoryDataPoint1.getPosY()-minPosY)/factorY),
										 (float)((trajectoryDataPoint2.getPosX()-minPosX)/factorX),
										 (float)((trajectoryDataPoint2.getPosY()-minPosY)/factorY));
									
									
									
								}
								
							}
							
							//line from last position to tweened agent position
							line((float) ((currentDataPoint.getPosX() - minPosX) / factorX),
								 (float) ((currentDataPoint.getPosY() - minPosY) / factorY),
								 posX, posY);
							
							noStroke();
								
							//increment tween
							tweenCount++;
							
							//draw agent
							fill(color(agentColor[0], agentColor[1],agentColor[2],255));
							ellipse (posX, posY, agentSize, agentSize);
						
						}
						currentAgent++;
						
					}
		
				}
				
				
				
			}
		}
	}

	public void setPositionRange(Point max, Point min)
	{
		// TODO Auto-generated method stub

	}

	public void setAnimationSpeed(Float speed)
	{
		// TODO Auto-generated method stub
		c = 100;
		// c = speed;

	}

	public void playScenario()
	{
		if (!rewind)
			i = 0;
		else
			i = timeSteps.length-1;
		
	}
	
	public void togglePause()
	{
		paused = !paused;
		
	}

	public void rewindScenario()
	{
		rewind = !rewind;
		
	}

	public void setNetwork(HashMap<Integer, DataPoint> nodes, HashMap<Integer, int[]> links)
	{
		this.nodes = nodes;
		this.links = links;
		
	}

	public void updateAgentData(HashMap<Integer, Agent> agents)
	{
		this.agents = agents;
		
	}

	public void updateCurrentTime(double time)
	{
		//this.t
		
	}

}
