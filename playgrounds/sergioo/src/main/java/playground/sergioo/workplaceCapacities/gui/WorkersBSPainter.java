package playground.sergioo.workplaceCapacities.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

import others.sergioo.visUtils.JetColor;

import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.LinesPainter3D;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;

public class WorkersBSPainter extends NetworkPainter {
	
	
	private static final double MAX_HEIGHT = 500;
	private static final Stroke STROKE = new BasicStroke(3);
	//Attributes
	private LinesPainter3D linesPainter3D;
	private double minCapacity;
	private double maxCapacity;
	
	//Methods
	public WorkersBSPainter(Network network) {
		super(network);
	}
	public WorkersBSPainter(Network network, Color color) {
		super(network, color);
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
				double capacity = facility.getActivityOptions().get(schedule).getCapacity();
				if(capacity<minCapacity)
					minCapacity = capacity;
				if(capacity>maxCapacity)
					maxCapacity = capacity;
			}
		}
		if(minCapacity==maxCapacity)
			maxCapacity++;
		for(ActivityFacility facility:facilities.getFacilities().values()) {
			int c=0;
			for(String schedule:schedules) {
				double capacity = facility.getActivityOptions().get(schedule).getCapacity();
				linesPainter3D.addLine(new double[]{facility.getCoord().getX(), facility.getCoord().getY(), 0}, new double[]{facility.getCoord().getX(), facility.getCoord().getY(), capacity*MAX_HEIGHT/maxCapacity}, colors[c], STROKE);
				c++;
			}
		}
		
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		super.paint(g2, layersPanel);
		linesPainter3D.paint(g2, layersPanel);
	}

}
