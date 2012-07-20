package playground.sergioo.workplaceCapacities.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

import others.sergioo.visUtils.JetColor;

import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;
import playground.sergioo.workplaceCapacities.MPAreaData;

public class WorkersAreaPainter extends NetworkPainter {
	
	
	//Attributes
	private List<MPAreaData> dataMPAreas = new ArrayList<MPAreaData>();
	private double[] wTotals;
	private Collection<Coord> stops; 
	private double minCapacity;
	private double maxCapacity;
	
	//Methods
	public WorkersAreaPainter(Network network) {
		super(network);
	}
	public WorkersAreaPainter(Network network, Color color) {
		super(network);
	}
	public void setData(double[][] matrixCapacities, SortedMap<Id, MPAreaData> dataMPAreas, Collection<Coord> stops) {
		minCapacity = Double.MAX_VALUE;
		maxCapacity = 0;
		this.stops = stops;
		wTotals = new double[matrixCapacities.length];
		for(int w=0; w<matrixCapacities.length; w++) {
			double wTotal=0;
			Iterator<MPAreaData> it = dataMPAreas.values().iterator();
			while(it.hasNext())
				this.dataMPAreas.add(it.next());
			for(int c=0; c<matrixCapacities[0].length; c++)
				wTotal += matrixCapacities[w][c];
			if(wTotal<minCapacity)
				minCapacity = wTotal;
			if(wTotal>maxCapacity)
				maxCapacity = wTotal;
			wTotals[w] = wTotal;
		}
		if(minCapacity==maxCapacity)
			maxCapacity++;
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		super.paint(g2, layersPanel);
		for(Coord stop:stops)
			paintCross(g2, layersPanel, stop, 2, Color.BLACK);
		double maxSize = 50;
		for(int w=0; w<wTotals.length; w++)
			if(wTotals[w]>0) {
				float proportion = (float) ((wTotals[w]-minCapacity)/(maxCapacity-minCapacity));
				Color color = JetColor.getJetColor(proportion);
				paintCircle(g2, layersPanel, dataMPAreas.get(w).getCoord(), proportion*maxSize, new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
			}
		JetColor.paintScale(g2, 10, 10, 300, 30, new Font("Times New Roman", Font.PLAIN, 14), Color.BLACK, minCapacity, maxCapacity, 5);
	}

}
