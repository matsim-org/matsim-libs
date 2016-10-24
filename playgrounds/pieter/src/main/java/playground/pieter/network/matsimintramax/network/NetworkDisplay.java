package playground.pieter.network.matsimintramax.network;

import org.matsim.core.network.io.NetworkReaderMatsimV2;
import playground.pieter.network.clustering.*;
import processing.core.PApplet;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkDisplay extends PApplet {
	private Network network;
	private float minX = Float.MAX_VALUE;
	private float maxX;
	private float minY = Float.MAX_VALUE;
	private float maxY;
	private boolean change = true;
	int cols = 80;
	int rows = 60;
	int lastm, fint = 1, fcount = 0;
	float frate;
	MinmizeNumberOfOutlinksNCA nca;
	Scenario scenario = ScenarioUtils
			.createScenario(ConfigUtils.createConfig());
	NetworkReaderMatsimV2 nwr = new NetworkReaderMatsimV2(scenario.getNetwork());
	int[] colorsForDisplay;
	{

		nwr.readFile("/Users/fouriep/IdeaProjects/matsim/matsim/examples/siouxfalls-2014/Siouxfalls_network_PT.xml");
		network = scenario.getNetwork();
//		nca = new IntraMinDeltaOutLinksNCA(scenario.getNetwork());
//		nca = new IntraMaxNCA(scenario.getNetwork(),"getCapacityTimesSpeed",null,null);
		nca = new MinmizeNumberOfOutlinksNCA(scenario.getNetwork(),"getCapacityTimesSpeed",null,null);
		nca.run();

//		new ClusterReader().readClusters("~/Desktop/test.txt", scenario.getNetwork(), nca);
		colorsForDisplay = new int[nca.getPointersToClusterLevels().size()];
		getRandomColors((int) nca.getClusterSteps());
	}

	// ncr.run("getCapacity", new String[] { "java.lang.Double" },
	// new Object[] { new Double(3600) });
	HashMap<Id, Float> strokeWeights = new HashMap<Id, Float>();
	float minCap = Float.MAX_VALUE;
	float maxCap = 0f;

	public void getRandomColors(int n) {
		for (int i = 0; i < colorsForDisplay.length; i++) {
			float frac = (float) i / n;
			int red = (int) (150 * sin(frac * 0.5f * PI));
			int green = (int) (150 * sin(frac * 1.1f * PI));
			int blue = (int) (150 * sin(frac * 3.3f * PI));
			int colora = color(red, green, blue);
			colorsForDisplay[i] = colora;
		}
		// colorsForDisplay[0]=color(255);
	}
    public void settings() {
        size(1600, 900);
    }
	@Override
	public void setup() {
//        size(1800,1000);
		background(255);
		strokeWeight(16);
		  textFont(createFont("Arial",12));
		  textAlign(TOP, RIGHT);
		for (Node node : network.getNodes().values()) {
			if (minX > node.getCoord().getX())
				minX = (float) node.getCoord().getX();
			if (maxX < node.getCoord().getX())
				maxX = (float) node.getCoord().getX();
			if (minY > node.getCoord().getY())
				minY = (float) node.getCoord().getY();
			if (maxY < node.getCoord().getY())
				maxY = (float) node.getCoord().getY();
		}
		for (Link link : network.getLinks().values()) {
			if (link.getCapacity() < minCap) {
				minCap = (float) link.getCapacity();

			}
			if (link.getCapacity() > maxCap)
				maxCap = (float) link.getCapacity();
		}
		for (Link link : network.getLinks().values()) {
			strokeWeights.put(link.getId(),
					map((float) link.getCapacity(), minCap, maxCap, 10f, 160f));

		}
		smooth();
		// frameRate(3);
		// ArcBall arcball = new ArcBall(this);
	}

	boolean followMouse = false;

	float xoffset = 0f;
	int currentStep = 0;

	private void checkCurrentStep() {
		while (currentStep >= colorsForDisplay.length) {
			currentStep = 0 ;

		}
		while (currentStep < 0) {
			currentStep = colorsForDisplay.length + currentStep;
		}

	}

	@Override
	public void draw() {
		// stroke(0);

		if (change) {
			println(currentStep + ":" + colorsForDisplay.length);
			background(255);
			pushMatrix();
			 if(followMouse){
			 translate(mouseX - width / 2, mouseY - height / 2);

			 }
			if (width / height < (maxX - minX) / (maxY - minY))
				scale(width / (maxX - minX), -width / (maxX - minX));
			else
				scale(height / (maxY - minY), -height / (maxY - minY));
			ArrayList<NodeCluster> clustersAtLevel = nca
					.getClustersAtLevel(currentStep);
			NodeCluster largestCluster = nca.getLargestCluster(clustersAtLevel);
			println("largest: "+largestCluster.getId());
			translate(-minX, -maxY);
			
			for (NodeCluster nc : clustersAtLevel) {
				if (nc.isLeaf()) {
					for (ClusterLink link : nc.getOutLinks().values()) {
						strokeWeight(strokeWeights.get(link.getId()));

						stroke(color(240));
						// pushMatrix();
						line((float) (link.getFromNode().getCoord().getX()),
								(float) (link.getFromNode().getCoord().getY()),
								(float) (link.getToNode().getCoord().getX()),
								(float) (link.getToNode().getCoord().getY()));

					}

				} else {

					for (ClusterLink link : nc.getInterLinks().values()) {
						strokeWeight(strokeWeights.get(link.getId()));
						int colindex = nc.getId();
						if(nc.equals(largestCluster)){
							stroke(color(255,0,0));
						}else{
							stroke(colorsForDisplay[colindex]);
							
						}
						// pushMatrix();
						line((float) (link.getFromNode().getCoord().getX()),
								(float) (link.getFromNode().getCoord().getY()),
								(float) (link.getToNode().getCoord().getX()),
								(float) (link.getToNode().getCoord().getY()));
					}
					for (ClusterLink link : nc.getOutLinks().values()) {
						strokeWeight(strokeWeights.get(link.getId()));
						stroke(color(200));
						// pushMatrix();
						line((float) (link.getFromNode().getCoord().getX()),
								(float) (link.getFromNode().getCoord().getY()),
								(float) (link.getToNode().getCoord().getX()),
								(float) (link.getToNode().getCoord().getY()));
					}
				}
			}
			// println(currentStep+":"+clustersAtLevel);

			// for (Link link : network.getLinks().values()) {
			// // popMatrix();
			// }
			// for (Node n:network.getNodes().values()){
			//
			// }
			popMatrix();
			change = false;
			// currentStep++;
			fill(0);
			text(currentStep + ":" + (colorsForDisplay.length-1), 20, 20);
		}
		 if (this.mousePressed) {
		 change = true;
		 followMouse = true;
		 }

//		 do some frame counting
//		 fcount += 1;
//		 int m = millis();
//		 if (m - lastm > 1000 * fint) {
//		 frate = (float) fcount / fint;
//		 fcount = 0;
//		 lastm = m;
//		 // println("fps: " + frate);
//		 }
//		 text(frate, 0, 20);
	}

	public void mouseClicked() {
		// super.mouseClicked();
		currentStep++;
		checkCurrentStep();
		change = true;
	}

    public static void main(String args[])
    {
      PApplet.main(new String[] { NetworkDisplay.class.getName() });
    }

	public void keyPressed() {
		if (key == 'a') {
			currentStep--;

		}
		if (key == 'd')
			currentStep++;
		if (key == 'q')
			currentStep -= 10;
		if (key == 'e')
			currentStep += 10;
		if (key == 'z')
			currentStep -= 100;
		if (key == 'c')
			currentStep += 100;
		checkCurrentStep();
		change = true;
	}

	public void keyReleased() {
//		change = true;
	}
}