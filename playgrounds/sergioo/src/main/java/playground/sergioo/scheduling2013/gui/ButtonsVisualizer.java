package playground.sergioo.scheduling2013.gui;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math.geometry.Vector3D;

import processing.core.PApplet;
import processing.core.PConstants;

public class ButtonsVisualizer implements Visualizer {

	private final VisualizersSet visualizersSet;
	enum Label {
		DEBATE(50, MainApplet.H-100),
		CITY(50, MainApplet.H-50),
		MODE(MainApplet.W-860, MainApplet.H-110),
		ROLE(MainApplet.W-630, MainApplet.H-110);
		private int x;
		private int y;
		private Label(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
	enum Button {
		MODE(MainApplet.W-860, MainApplet.H-70, 60, 10),
		ROLE(MainApplet.W-630, MainApplet.H-70, 60, 10),
		DETAIL(MainApplet.W-400, MainApplet.H-70, 60, 10),
		BACK(MainApplet.W-170, MainApplet.H-70, 60, 10),
		BACK2(MainApplet.W-170, MainApplet.H-70, 60, 10),
		HOME(MainApplet.W-170, MainApplet.H-70, 60, 10),
		RIGHT(MainApplet.W-54, 60, 8, 8),
		CENTER(MainApplet.W-84, 60, 8, 8),
		LEFT(MainApplet.W-114, 60, 8, 8),
		UP(MainApplet.W-84, 30, 8, 8),
		DOWN(MainApplet.W-84, 90, 8, 8),
		IN(MainApplet.W-99, 130, 8, 8),
		OUT(MainApplet.W-69, 130, 8, 8);
		private int x;
		private int y;
		private int xx;
		private int yy;
		private Button(int x, int y, int xx, int yy) {
			this.x = x;
			this.y = y;
			this.xx = xx;
			this.yy = yy;
		}
	}

	private Button selected;
	private Set<Button> visibleButtons = new HashSet<Button>();
	
	public ButtonsVisualizer(VisualizersSet visualizersSet) {
		this.visualizersSet = visualizersSet;
	}
	public Button getSelected() {
		return selected;
	}
	public void paintOnce(PApplet applet) {
		visibleButtons.add(Button.MODE);
		visibleButtons.add(Button.HOME);
		visibleButtons.add(Button.ROLE);
		visibleButtons.add(Button.RIGHT);
		visibleButtons.add(Button.LEFT);
		visibleButtons.add(Button.CENTER);
		visibleButtons.add(Button.UP);
		visibleButtons.add(Button.DOWN);
		visibleButtons.add(Button.IN);
		visibleButtons.add(Button.OUT);
		paintButton(applet, Button.RIGHT, ">");
		paintButton(applet, Button.LEFT, "<");
		paintButton(applet, Button.CENTER, "O");
		paintButton(applet, Button.UP, "^");
		paintButton(applet, Button.DOWN, "v");
		paintButton(applet, Button.IN, "+");
		paintButton(applet, Button.OUT, "-");
		paintButton(applet, Button.HOME, "Home");
	}
	public void paintLabel(PApplet applet, Label label, String text, boolean centered) {
		Vector3D eye = visualizersSet.getEye();
		Vector3D center = visualizersSet.getCenter();
		Vector3D diff = center.subtract(eye).normalize();
		Vector3D position = eye.add(diff.scalarMultiply(10));
		applet.pushMatrix();
			if(centered)
				applet.textAlign(PConstants.CENTER, PConstants.CENTER);
			else
				applet.textAlign(PConstants.LEFT, PConstants.CENTER);
			translateButton(applet, label.x, label.y);
			applet.translate((float)position.getX(), (float)-position.getZ(), (float)-position.getY());
			Vector3D aux = new Vector3D(diff.getAlpha()+Math.PI/2, 0);
			applet.rotate((float)diff.getDelta(), (float)aux.getX(), (float)-aux.getZ(), (float)-aux.getY());
			applet.rotateY((float) (diff.getAlpha()-Math.PI/2));
			applet.scale(0.01f);
			applet.fill(255, 255, 200);
			applet.strokeWeight(0.02f);
			applet.text(text, 0, 0, 0);
			applet.textAlign(PConstants.CENTER, PConstants.TOP);
		applet.popMatrix();
	}
	public void paintButton(PApplet applet, Button button, String text) {
		Vector3D eye = visualizersSet.getEye();
		Vector3D center = visualizersSet.getCenter();
		Vector3D diff = center.subtract(eye).normalize();
		Vector3D position = eye.add(diff.scalarMultiply(10));
		applet.pushMatrix();
			applet.textAlign(PConstants.CENTER, PConstants.CENTER);
			translateButton(applet, button.x, button.y);
			applet.translate((float)position.getX(), (float)-position.getZ(), (float)-position.getY());
			Vector3D aux = new Vector3D(diff.getAlpha()+Math.PI/2, 0);
			applet.rotate((float)diff.getDelta(), (float)aux.getX(), (float)-aux.getZ(), (float)-aux.getY());
			applet.rotateY((float) (diff.getAlpha()-Math.PI/2));
			applet.scale(0.02f);
			if(button == selected)
				applet.fill(155, 55, 55);
			else
				applet.fill(255, 255, 255);
			applet.strokeWeight(0.02f);
			if(visibleButtons.contains(button))
				if(button.equals(Button.HOME))
					applet.stroke(50, 150, 50);
				else
					applet.stroke(150, 50, 50);
			else
				applet.stroke(50, 50, 50);
			applet.line(-button.xx, -button.yy, button.xx, -button.yy);
			applet.line(button.xx, button.yy, button.xx, -button.yy);
			applet.line(button.xx, button.yy, -button.xx, button.yy);
			applet.line(-button.xx, -button.yy, -button.xx, button.yy);
			applet.scale(0.18f);
			applet.text(text, 0, 0, 0);
			applet.textAlign(PConstants.CENTER, PConstants.TOP);
		applet.popMatrix();
	}
	private void translateButton(PApplet applet, int x, int y) {
		Vector3D eye = visualizersSet.getEye();
		Vector3D center = visualizersSet.getCenter();
		int width = applet.getWidth();
		int height = applet.getHeight();
		Vector3D d = center.subtract(eye).normalize();
		double newDelta = d.getDelta()+Math.PI/2;
		Vector3D v = newDelta>Math.PI/2?new Vector3D(d.getAlpha()>0?d.getAlpha()-Math.PI:d.getAlpha()+Math.PI, Math.PI-newDelta):new Vector3D(d.getAlpha(), newDelta);
		Vector3D u = Vector3D.crossProduct(d, v);
		double depthFactor = 10*Math.tan(VisualizersSet.FOV/2)/(height/2);
		double vParam = (height/2-y)*depthFactor; 
		double uParam = (x-width/2)*(width/height)*depthFactor;
		Vector3D point = u.scalarMultiply(uParam).add(v.scalarMultiply(vParam));
		applet.translate((float)point.getX(), (float)-point.getZ(), (float)-point.getY());	
	}
	public void setPoint(int x, int y) {
		boolean wasNull = selected==null;
		selected = null;
		for(Button button:visibleButtons)
			if(y>button.y-2*button.yy && y<button.y+2*button.yy && x>button.x-2*button.xx && x<button.x+2*button.xx) {
				selected = button;
				visualizersSet.setChange();
				return;
			}
		if(!wasNull)
			visualizersSet.setChange();
	}
	public void paint(PApplet applet, double time) {
		
	}

}
