package playground.sergioo.workplaceCapacities2012.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

import others.sergioo.visUtils.JetColor;
import others.sergioo.visUtils.ScaleColor;
import playground.sergioo.visualizer2D2012.Camera3D;
import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.LinesPainter3D;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;

public class WorkersBSPainter extends NetworkPainter {
	
	
	private static final double MAX_HEIGHT = 2000;
	private static final Stroke STROKE = new BasicStroke(10);
	//Attributes
	private LinesPainter3D linesPainter3D;
	private double maxCapacityTotal;
	
	//Methods
	public WorkersBSPainter(Network network) {
		super(network);
		linesPainter3D = new LinesPainter3D(20);
	}
	public WorkersBSPainter(Network network, int size) {
		super(network, Color.LIGHT_GRAY, new BasicStroke(2f));
		linesPainter3D = new LinesPainter3D(size);
	}
	public WorkersBSPainter(Network network, Color color) {
		super(network, color);
		linesPainter3D = new LinesPainter3D(20);
	}
	public LinesPainter3D getLinesPainter3D() {
		return linesPainter3D;
	}
	public void setData(ActivityFacilities facilities, String[] schedules) {
		double minCapacity = Double.MAX_VALUE;
		double maxCapacity = 0;
		maxCapacityTotal = 0;
		Color[] colors = new Color[schedules.length];
		for(int c=0; c<schedules.length; c++)
			colors[c] = JetColor.getJetColor((c+0.5f)/schedules.length);
		for(ActivityFacility facility:facilities.getFacilities().values()) {
			double total = 0;
			for(String schedule:schedules) {
				double capacity;
				ActivityOption option = facility.getActivityOptions().get(schedule);
				if(option == null)
					capacity = 0;
				else {
					capacity = option.getCapacity();
					total+=capacity;
				}
				if(capacity<minCapacity)
					minCapacity = capacity;
				if(capacity>maxCapacity)
					maxCapacity = capacity;
			}
			if(total>maxCapacityTotal)
				maxCapacityTotal = total;
		}
		System.out.println(maxCapacityTotal);
		double maxZ=0;
		if(minCapacity==maxCapacity)
			maxCapacity++;
		//double side = 10*Math.sqrt(2);
		for(ActivityFacility facility:facilities.getFacilities().values()) {
			double z=0;
			double total = 0;
			double x = facility.getCoord().getX();
			double y = facility.getCoord().getY();
			SortedMap<Float, Color> colorsMap = new TreeMap<Float, Color>();
			colorsMap.put(0.0f, Color.YELLOW);
			colorsMap.put(0.5f, Color.GREEN.darker().darker());
			colorsMap.put(1.0f, Color.ORANGE.darker().darker().darker());
			for(int s=schedules.length; s>0; s--) {
				String schedule = schedules[s-1];
				double capacity;
				ActivityOption option = facility.getActivityOptions().get(schedule);
				if(option == null)
					capacity = 0;
				else {
					capacity = option.getCapacity();
					/*double angle = (c+0.5)*2*Math.PI/schedules.length;
					double x = x+side*Math.cos(angle);
					double y = y+side*Math.sin(angle);*/
					//linesPainter3D.addLine(new double[]{x, y, z}, new double[]{x, y, z+capacity*MAX_HEIGHT/maxCapacity}, ScaleColor.getScaleColor(colorsMap, (s-1f)/schedules.length), STROKE);
					z += capacity*MAX_HEIGHT/maxCapacity;
				}
				total += capacity;
			}
			if(z>maxZ)
				maxZ = z;
			if(total>0) {
				float proportion = (float)(Math.log(total+1)/Math.log(maxCapacity+1));
				linesPainter3D.addLine(new double[]{x, y, 0}, new double[]{x, y, total*MAX_HEIGHT/maxCapacityTotal}, ScaleColor.getScaleColor(colorsMap, proportion), STROKE);
			}
		}
		linesPainter3D.setScale(0.0, /*maxZ*/MAX_HEIGHT, 0.0, maxCapacityTotal, 5, 50, new Font("Times New Roman", Font.BOLD, 16), new DecimalFormat("######"), new BasicStroke(2), Color.BLACK);
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		super.paint(g2, layersPanel);
		if(layersPanel.getCamera() instanceof Camera3D)
			linesPainter3D.setCamera((Camera3D) layersPanel.getCamera());
		linesPainter3D.paint(g2, layersPanel);
		SortedMap<Float, Color> colorsMap = new TreeMap<Float, Color>();
		colorsMap.put(0.0f, Color.YELLOW);
		colorsMap.put(0.5f, Color.GREEN.darker().darker());
		colorsMap.put(1.0f, Color.ORANGE.darker().darker().darker());
		//ScaleColor.paintLogScale(colorsMap, g2, layersPanel.getSize().width-330, layersPanel.getSize().height-60, 300, 30, new Font("Times New Roman", Font.PLAIN, 16), Color.BLACK, 1, maxCapacityTotal, 5);
	}
	public void setData(ActivityFacilities facilities, String[] schedules, Map<Id<ActivityFacility>, String> types) {
		SortedMap<Float, Color> colorsMap = new TreeMap<Float, Color>();
		colorsMap.put(0.0f, Color.ORANGE.darker());
		colorsMap.put(0.25f, Color.RED.darker());
		colorsMap.put(0.5f, Color.BLUE.brighter());
		colorsMap.put(0.75f, Color.MAGENTA.darker().darker());
		Map<String, Color> colors = new HashMap<String, Color>();
		Set<String> diffTypes = new HashSet<String>(types.values());
		Iterator<String> diffTypesI = diffTypes.iterator();
		for(float i=0; i<diffTypes.size(); i++)
			colors.put(diffTypesI.next(), ScaleColor.getScaleColor(colorsMap, i/diffTypes.size()));
		double minCapacity = Double.MAX_VALUE;
		double maxCapacity = 0;
		maxCapacityTotal = 0;
		for(ActivityFacility facility:facilities.getFacilities().values()) {
			double total = 0;
			for(String schedule:schedules) {
				double capacity;
				ActivityOption option = facility.getActivityOptions().get(schedule);
				if(option == null)
					capacity = 0;
				else {
					capacity = option.getCapacity();
					total+=capacity;
				}
				if(capacity<minCapacity)
					minCapacity = capacity;
				if(capacity>maxCapacity)
					maxCapacity = capacity;
			}
			if(total>maxCapacityTotal)
				maxCapacityTotal = total;
		}
		System.out.println(maxCapacityTotal);
		double maxZ=0;
		if(minCapacity==maxCapacity)
			maxCapacity++;
		for(ActivityFacility facility:facilities.getFacilities().values()) {
			double z=0;
			double total = 0;
			double x = facility.getCoord().getX();
			double y = facility.getCoord().getY();
			for(int s=schedules.length; s>0; s--) {
				String schedule = schedules[s-1];
				double capacity;
				ActivityOption option = facility.getActivityOptions().get(schedule);
				if(option == null)
					capacity = 0;
				else {
					capacity = option.getCapacity();
					z += capacity*MAX_HEIGHT/maxCapacity;
				}
				total += capacity;
			}
			if(z>maxZ)
				maxZ = z;
			if(total>0)
				linesPainter3D.addLine(new double[]{x, y, 0}, new double[]{x, y, total*MAX_HEIGHT/maxCapacityTotal}, colors.get(types.get(facility.getId())), STROKE);
		}
		linesPainter3D.setScale(0.0, MAX_HEIGHT, 0.0, maxCapacityTotal, 5, 300, new Font("Times New Roman", Font.BOLD, 80), new DecimalFormat("######"), new BasicStroke(10), Color.BLACK);
	}

}
