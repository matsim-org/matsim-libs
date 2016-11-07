package playground.pieter.network.matsimintramax.network;



import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV2;
import org.matsim.core.scenario.ScenarioUtils;

//import com.processinghacks.arcball.ArcBall;

import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import processing.core.PApplet;


public class LinksSelector extends PApplet {
	
	private static final float HEIGHT_CAMERA = 10000;
	private static final float MAX_HEIGHT = 300;
	private static final float MAX_CAPACITY = 8000;
	private Network network;
	private ActivityFacilities facilities;
	private String[] schedules = {"w_0645_0815", "w_0730_1000", "w_0730_1145", "w_0730_1345", "w_0830_0915", "w_0900_1015", "w_0945_0800", "w_0945_1145", "w_1345_0845", "w_2015_0945"};
	private float minX=Float.MAX_VALUE;
	private float maxX=-Float.MAX_VALUE;
	private float minY=Float.MAX_VALUE;
	private float maxY=-Float.MAX_VALUE;
	private SortedMap<Float, Color> colorsMap;
	{
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV2(scenario.getNetwork()).readFile("");
		network = scenario.getNetwork();
		for(Node node:network.getNodes().values()) {
			if(minX>node.getCoord().getX())
				minX=(float) node.getCoord().getX();
			if(maxX<node.getCoord().getX())
				maxX=(float) node.getCoord().getX();
			if(minY>node.getCoord().getY())
				minY=(float) node.getCoord().getY();
			if(maxY<node.getCoord().getY())
				maxY=(float) node.getCoord().getY();
		}
		new FacilitiesReaderMatsimV1(scenario).readFile("f:/matsimWorkspace/playgrounds/pieter/data/matsimSG2DemandGen/workFacilitiesO.xml");
		facilities = scenario.getActivityFacilities();
		colorsMap = new TreeMap<Float, Color>();
		colorsMap.put(0.0f, Color.YELLOW);
		colorsMap.put(0.5f, Color.GREEN.darker().darker());
		colorsMap.put(1.0f, Color.ORANGE.darker().darker().darker());
		
	}
	private boolean change = true;
	private int w;
	private int h;

	@Override
	public void setup() {
		size(displayWidth, displayHeight, P3D);
		background(255);
		//strokeWeight(16);
		smooth();
//		ArcBall arcball = new ArcBall(this);
	}
	
	@Override
	public void draw() {
		if(change) {
			background(255);
			pushMatrix();
//			camera(0,0,-500,width/2,height/2,0,0,0,1);
			//translate(mouseX-width/2, mouseY-height/2);
			if(width/height<(maxX-minX)/(maxY-minY))
				scale(width/(maxX-minX),width/(maxX-minX));
			else
				scale(height/(maxY-minY),height/(maxY-minY));
			translate(-minX, -minY);
			//camera((minX+maxX)/2,(minY+maxY)/2,1,(minX+maxX)/2,(minY+maxY)/2,0,0,1,0);
			stroke(0);
			for(Link link:network.getLinks().values()) {
				//strokeWeight((float)(link.getCapacity()*16/2000));
				line((float)(link.getFromNode().getCoord().getX()),
						(float)(link.getFromNode().getCoord().getY()),0,
						(float)(link.getToNode().getCoord().getX()),
						(float)(link.getToNode().getCoord().getY()),0);
			}
			noStroke();
			for(ActivityFacility facility:facilities.getFacilities().values()) {
				float buildingHeight = 0;
				for(String schedule:schedules)
					buildingHeight+=facility.getActivityOptions().get(schedule)==null?0:facility.getActivityOptions().get(schedule).getCapacity();
				pushMatrix();
				translate((float)facility.getCoord().getX(), (float)facility.getCoord().getY(), 0);
				Color c = ScaleColor.getScaleColor(colorsMap, (float)(Math.log(buildingHeight+1)/Math.log(MAX_CAPACITY+1)));
				fill(c.getRGB());
				box(100, 100, buildingHeight*MAX_HEIGHT/MAX_CAPACITY);
				popMatrix();
			}
			popMatrix();
			change = false;
//			System.out.println("painted");
		}
		if(this.mousePressed)
			change = true;
		w = width;
		h = height;
	}
	public void mousePressed() {
	}
 
}
