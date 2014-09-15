package playground.sergioo.scheduling2013.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.geometry.Vector3D;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.sergioo.passivePlanning2012.core.population.PlaceSharer;
import playground.sergioo.scheduling2013.SchedulingNetwork;
import playground.sergioo.scheduling2013.SchedulingNetwork.SchedulingLink;
import playground.sergioo.scheduling2013.SchedulingNetwork.SchedulingNode;
import processing.core.PApplet;

public class VisualizersSet {

	static final Map<String,Color> COLORS = new HashMap<String, Color>();
	{
		COLORS.put("home", new Color(0, 255, 255));
		COLORS.put("w_0730_1000", new Color(255, 255, 0));
		COLORS.put("shop", new Color(255, 0, 255));
		COLORS.put("sport", new Color(0, 0, 255));
		COLORS.put("visit", new Color(255, 155, 0));
	}
	
	public static int NUM_YEARS = 44;
	public static int FIRST_YEAR = 1970;
	public static double FOV = Math.PI/3;
	public double[] CENTER = new double[]{370000, 150000};

	private final List<Visualizer> visualizers = new ArrayList<Visualizer>();
	private Vector3D eye = new Vector3D(CENTER[0], -25000, CENTER[1]-1);
	private Vector3D center = new Vector3D(CENTER[0], 0, CENTER[1]);
	private boolean change = true;
	private boolean moveCamera = false;
	private Vector3D[] moveEyeCameraCoeffs = new Vector3D[4];
	private Vector3D[] moveCenterCameraCoeffs = new Vector3D[4];
	private long startTime;
	private double period;
	private Boolean publicationType = false;
	private Boolean detailed = false;
	private ActivityFacility place;
	
	public VisualizersSet(ActivityFacilities facilities, PlaceSharer placeSharer, SchedulingNetwork schedulingNetwork, Network network, List<SchedulingLink> path) {
		double smallestTime = Double.MAX_VALUE;
		for(Node node:schedulingNetwork.getNodes().values())
			if(((SchedulingNode)node).getTime()<smallestTime)
				smallestTime = ((SchedulingNode)node).getTime();
		visualizers.add(new WorldVisualizer(this, placeSharer, facilities.getFacilities(), network, smallestTime));
		visualizers.add(new SchedulingNetworkVisualizer(schedulingNetwork, path, smallestTime));
		//visualizers.add(new ButtonsVisualizer(this));
	}

	public void paint(PApplet applet, double time) {
		applet.camera((float)eye.getX(), -(float)eye.getZ(), -(float)eye.getY(), (float)center.getX(), -(float)center.getZ(), -(float)center.getY(), 0, 0, -1);
		if(change) {
			applet.background(255);
			for(Visualizer visualizer:visualizers) {
				applet.pushMatrix();
				visualizer.paintOnce(applet);
				applet.popMatrix();
			}
			change = false;
		}
		for(Visualizer visualizer:visualizers) {
			applet.pushMatrix();
			visualizer.paint(applet, time);
			applet.popMatrix();
		}
	}
	public void changeElevation(double d) {
		eye = eye.subtract(center);
		Vector3D eye2 = new Vector3D(eye.getX(), eye.getZ(), eye.getY());
		double newDelta = eye2.getDelta()-d;
		if(newDelta>Math.PI/2-Math.PI/1800)
			newDelta = Math.PI/2-Math.PI/1800;
		else if(newDelta<-Math.PI/2+Math.PI/1800)
			newDelta = -Math.PI/2+Math.PI/1800;
		eye2 = (new Vector3D(eye2.getAlpha(), newDelta)).scalarMultiply(eye2.getNorm());
		eye = new Vector3D(eye2.getX(), eye2.getZ(), eye2.getY());
		eye = eye.add(center);
		change = true;
	}
	public void changeAzimuth(double a) {
		eye = eye.subtract(center);
		Vector3D eye2 = new Vector3D(eye.getX(), eye.getZ(), eye.getY());
		double newAlpha = eye2.getAlpha()+a;
		if(newAlpha>2*Math.PI)
			newAlpha -= 2*Math.PI;
		else if(newAlpha<0)
			newAlpha += 2*Math.PI;
		eye2 = (new Vector3D(newAlpha, eye2.getDelta())).scalarMultiply(eye2.getNorm());
		eye = new Vector3D(eye2.getX(), eye2.getZ(), eye2.getY());
		eye = eye.add(center);
		change = true;
	}
	public void startMoveCamera(Vector3D eyeDestination, Vector3D centerDestination, double period) {
		if(!moveCamera && !(eye.equals(eyeDestination) && center.equals(centerDestination))) {
			moveCamera = true;
			this.startTime = System.currentTimeMillis();
			this.period = period;
			moveEyeCameraCoeffs[0] = eyeDestination.subtract(eye).scalarMultiply(-2/Math.pow(period, 3));
			moveEyeCameraCoeffs[1] = eyeDestination.subtract(eye).scalarMultiply(3/Math.pow(period, 2));
			moveEyeCameraCoeffs[2] = new Vector3D(0, 0, 0);
			moveEyeCameraCoeffs[3] = eye;
			moveCenterCameraCoeffs[0] = centerDestination.subtract(center).scalarMultiply(-2/Math.pow(period, 3));
			moveCenterCameraCoeffs[1] = centerDestination.subtract(center).scalarMultiply(3/Math.pow(period, 2));
			moveCenterCameraCoeffs[2] = new Vector3D(0, 0, 0);
			moveCenterCameraCoeffs[3] = center;
		}
	}
	Boolean isPublicationType() {
		return publicationType;
	}
	void setPublicationType(Boolean publicationType) {
		this.publicationType = publicationType;
		setChange();
	}
	Boolean isDetailed() {
		return this.detailed;
	}
	void setDetailed(Boolean detailed) {
		this.detailed = detailed;
		setChange();
	}
	Vector3D getEye() {
		return eye;
	}
	Vector3D getCenter() {
		return center;
	}
	public boolean isMoveCamera() {
		return moveCamera;
	}
	public void moveCamera(long time) {
		double deltaTime = time - startTime;
		if(deltaTime>period) {
			moveCamera = false;
			deltaTime = period;
		}
		eye = moveEyeCameraCoeffs[0].scalarMultiply(Math.pow(deltaTime, 3)).add(moveEyeCameraCoeffs[1].scalarMultiply(Math.pow(deltaTime, 2))).add(moveEyeCameraCoeffs[2].scalarMultiply(deltaTime)).add(moveEyeCameraCoeffs[3]);
		center = moveCenterCameraCoeffs[0].scalarMultiply(Math.pow(deltaTime, 3)).add(moveCenterCameraCoeffs[1].scalarMultiply(Math.pow(deltaTime, 2))).add(moveCenterCameraCoeffs[2].scalarMultiply(deltaTime)).add(moveCenterCameraCoeffs[3]);
		change = true;
	}
	public Vector3D getMapPoint(int x, int y, int width, int height) {
		Vector3D d = center.subtract(eye).normalize();
		double newDelta = d.getDelta()+Math.PI/2;
		Vector3D v = newDelta>Math.PI/2?new Vector3D(d.getAlpha()>0?d.getAlpha()-Math.PI:d.getAlpha()+Math.PI, Math.PI-newDelta):new Vector3D(d.getAlpha(), newDelta);
		Vector3D u = Vector3D.crossProduct(d, v);
		double depthFactor = center.subtract(eye).getNorm()*Math.tan(FOV/2)/(height/2);
		double vParam = (height/2-y)*depthFactor; 
		double uParam = (x-width/2)*(width/height)*depthFactor;
		Vector3D point = center.add(u.scalarMultiply(uParam)).add(v.scalarMultiply(vParam));
		d = point.subtract(eye).normalize();
		//In the map
		return point.add(d.scalarMultiply(-point.getY()/d.getY()));
	}
	public Vector3D getGraphPoint(int x, int y, int width, int height, double xC) {
		Vector3D d = center.subtract(eye).normalize();
		double newDelta = d.getDelta()+Math.PI/2;
		Vector3D v = newDelta>Math.PI/2?new Vector3D(d.getAlpha()>0?d.getAlpha()-Math.PI:d.getAlpha()+Math.PI, Math.PI-newDelta):new Vector3D(d.getAlpha(), newDelta);
		Vector3D u = Vector3D.crossProduct(d, v);
		double depthFactor = center.subtract(eye).getNorm()*Math.tan(FOV/2)/(height/2);
		double vParam = (height/2-y)*depthFactor; 
		double uParam = (x-width/2)*(width/height)*depthFactor;
		Vector3D point = center.add(u.scalarMultiply(uParam)).add(v.scalarMultiply(vParam));
		d = point.subtract(eye).normalize();
		//In the graph
		return  point.add(d.scalarMultiply((xC-point.getX())/d.getX()));
	}
	public void refreshPoint(int x, int y, int width, int height) {
		((WorldVisualizer)visualizers.get(0)).setPoint(getMapPoint(x, y, width, height));
		/*((ButtonsVisualizer)visualizers.get(5)).setPoint(x, y);
		if(((TimeBarsVisualizer)visualizers.get(1)).getCity()!=null)
			((TimeBarsVisualizer)visualizers.get(1)).setPoint(getGraphPoint(x, y, width, height, ((TimeBarsVisualizer)visualizers.get(1)).getCity().x));*/
	}
	public boolean isPlace() {
		return place!=null;
	}
	public ActivityFacility getPlace() {
		return place;
	}
	public void setChange() {
		change = true;
	}
	public void setNetworkVisible() {
		((SchedulingNetworkVisualizer)visualizers.get(1)).setNetworkVisible();
		setChange();
	}
	public boolean pressButton() {
		ButtonsVisualizer.Button button = ((ButtonsVisualizer)visualizers.get(5)).getSelected();
		if(button != null) {
			switch(button) {
			case BACK:
				startMoveCamera(new Vector3D(CENTER[0], -200, CENTER[1]), new Vector3D(CENTER[0], 0, CENTER[1]), 3000);
				detailed = false;
				place = null;
				return false;
			case HOME:
				return false;
			case RIGHT:
				if(eye.getX()-eye.getY()/5<=180)
					startMoveCamera(eye.add(new Vector3D(-eye.getY()/5, 0, 0)), center.add(new Vector3D(-eye.getY()/5, 0, 0)), 1000);
				return false;
			case LEFT:
				if(eye.getX()+eye.getY()/5>=-180)
					startMoveCamera(eye.add(new Vector3D(eye.getY()/5, 0, 0)), center.add(new Vector3D(eye.getY()/5, 0, 0)), 1000);
				return false;
			case CENTER:
				startMoveCamera(new Vector3D(CENTER[0], -200, CENTER[1]), new Vector3D(CENTER[0], 0, CENTER[1]), 1500);
				return false;
			case UP:
				if(eye.getZ()-eye.getY()/5<=90)
					startMoveCamera(eye.add(new Vector3D(0, 0, -eye.getY()/5)), center.add(new Vector3D(0, 0, -eye.getY()/5)), 1000);
				return false;
			case DOWN:
				if(eye.getZ()+eye.getY()/5>=-90)
					startMoveCamera(eye.add(new Vector3D(0, 0, eye.getY()/5)), center.add(new Vector3D(0, 0, eye.getY()/5)), 1000);
				return false;
			case IN:
				if(eye.getY()+30<0)
					startMoveCamera(eye.add(new Vector3D(0, 30, 0)), center.add(new Vector3D(0, 30, 0)), 1000);
				return false;
			case OUT:
				if(eye.getY()-30>-300)
					startMoveCamera(eye.add(new Vector3D(0, -30, 0)), center.add(new Vector3D(0, -30, 0)), 1000);
				return false;
			}
		}
		return true;
	}

}
