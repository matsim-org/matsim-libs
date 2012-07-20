package playground.sergioo.workplaceCapacities.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;
import playground.sergioo.workplaceCapacities.MPAreaData;

public class WorkersBSPainter extends NetworkPainter {
	
	
	//Attributes
	private List<MPAreaData> dataMPAreas = new ArrayList<MPAreaData>();
	private Color color = new Color(1,0,0,0.3f);
	private double[] wTotals;
	private double minCapacity;
	private double maxCapacity;
	
	//Methods
	public WorkersBSPainter(Network network) {
		super(network);
	}
	public WorkersBSPainter(Network network, Color color) {
		super(network);
		this.color = color;
	}
	public void setData(double[][][] matrixCapacities, SortedMap<Id, MPAreaData> dataMPAreas) {
		minCapacity = Double.MAX_VALUE;
		maxCapacity = 0;
		wTotals = new double[matrixCapacities[0].length];
		for(int w=0; w<matrixCapacities[0].length; w++) {
			double wTotal=0;
			Iterator<MPAreaData> it = dataMPAreas.values().iterator();
			while(it.hasNext())
				this.dataMPAreas.add(it.next());
			for(int s=0; s<matrixCapacities.length; s++)
				for(int c=0; c<matrixCapacities[0][0].length; s++)
					wTotal += matrixCapacities[s][w][c];
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
		double maxSize = 10;
		for(int w=0; w<wTotals.length; w++)
			if(wTotals[w]>0)
				paintCircle(g2, layersPanel, dataMPAreas.get(w).getCoord(), (wTotals[w]-minCapacity)*maxSize/(maxCapacity-minCapacity), color);
	}

}
