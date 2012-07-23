package playground.sergioo.workplaceCapacities.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;

import others.sergioo.visUtils.JetColor;

import playground.sergioo.Visualizer2D.Camera3DOrtho1;
import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.LinesPainter3D;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;

public class WorkersBSPainter extends NetworkPainter {
	
	
	private static final double MAX_HEIGHT = 200;
	private static final Stroke STROKE = new BasicStroke(5);
	//Attributes
	private LinesPainter3D linesPainter3D;
	private double minCapacity;
	private double maxCapacity;
	
	//Methods
	public WorkersBSPainter(Network network) {
		super(network);
		linesPainter3D = new LinesPainter3D();
	}
	public WorkersBSPainter(Network network, Color color) {
		super(network, color);
		linesPainter3D = new LinesPainter3D();
	}
	public LinesPainter3D getLinesPainter3D() {
		return linesPainter3D;
	}
	public void setData(ActivityFacilities facilities, String[] schedules) {
		minCapacity = Double.MAX_VALUE;
		maxCapacity = 0;
		Color[] colors = new Color[schedules.length];
		for(int c=0; c<schedules.length; c++)
			colors[c] = JetColor.getJetColor((c+0.5f)/schedules.length);
		for(ActivityFacility facility:facilities.getFacilities().values()) {
			for(String schedule:schedules) {
				double capacity;
				ActivityOption option = facility.getActivityOptions().get(schedule);
				if(option == null)
					capacity = 0;
				else
					capacity = option.getCapacity();
				if(capacity<minCapacity)
					minCapacity = capacity;
				if(capacity>maxCapacity)
					maxCapacity = capacity;
			}
		}
		if(minCapacity==maxCapacity)
			maxCapacity++;
		double side = 10*Math.sqrt(2);
		for(ActivityFacility facility:facilities.getFacilities().values()) {
			int c=0;
			double z=0;
			for(String schedule:schedules) {
				double capacity;
				ActivityOption option = facility.getActivityOptions().get(schedule);
				if(option == null)
					capacity = 0;
				else {
					capacity = option.getCapacity();
					/*double angle = (c+0.5)*2*Math.PI/schedules.length;
					double x = facility.getCoord().getX()+side*Math.cos(angle);
					double y = facility.getCoord().getY()+side*Math.sin(angle);*/
					double x = facility.getCoord().getX();
					double y = facility.getCoord().getY();
					linesPainter3D.addLine(new double[]{x, y, z}, new double[]{x, y, z+capacity*MAX_HEIGHT/maxCapacity}, colors[c], STROKE);
					z += capacity*MAX_HEIGHT/maxCapacity;
				}
				c++;
			}
		}
		
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		super.paint(g2, layersPanel);
		if(layersPanel.getCamera() instanceof Camera3DOrtho1)
			linesPainter3D.setCamera((Camera3DOrtho1)layersPanel.getCamera());
		linesPainter3D.paint(g2, layersPanel);
	}

}
