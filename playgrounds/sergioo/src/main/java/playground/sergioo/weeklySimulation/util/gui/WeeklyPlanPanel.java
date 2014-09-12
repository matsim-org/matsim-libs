package playground.sergioo.weeklySimulation.util.gui;

import java.awt.Color;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.population.Plan;

import playground.sergioo.visualizer2D2012.Layer;
import playground.sergioo.visualizer2D2012.LayersPanel;

public class WeeklyPlanPanel extends LayersPanel {

	private static final double W = Toolkit.getDefaultToolkit().getScreenSize().width-40;
	private static final double H = Toolkit.getDefaultToolkit().getScreenSize().height-100;
	private boolean isHorizontal;
	
	public WeeklyPlanPanel(Plan plan, boolean isHorizontal) {
		super();
		this.addLayer(new Layer(new WeeklyPlanPainter(plan, isHorizontal)));
		this.isHorizontal = isHorizontal;
		this.setBackground(Color.WHITE);
		calculateBoundaries();
	}
	public void setPreferredSize(boolean isHorizontalLayout, int n) {
		double width = isHorizontal?(isHorizontalLayout?26:26*n):(isHorizontalLayout?9:n*9);
		double height = isHorizontal?(isHorizontalLayout?n*9:9):(isHorizontalLayout?26*n:26);
		if(width/height > W/H) {
			height = W*height/width;
			width = W;
		}
		else {
			width = H*width/height;
			height = H;
		}
		if(isHorizontalLayout)
			height/=n;
		else
			width/=n;
		super.setPreferredSize(width, height);
	}
	private void calculateBoundaries() {
		Collection<double[]> coords = new ArrayList<double[]>();
		if(isHorizontal) {
			coords.add(new double[]{-1, -1});
			coords.add(new double[]{25, 8});
		}
		else {
			coords.add(new double[]{-1, -1});
			coords.add(new double[]{8, 25});
		}
		super.calculateBoundaries(coords);
	}

}
