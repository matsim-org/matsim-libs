package playground.sergioo.workplaceCapacities2012.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacility;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

import others.sergioo.visUtils.JetColor;
import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;
import playground.sergioo.workplaceCapacities2012.MPAreaData;

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
	public void setData(double[][] matrixCapacities, SortedMap<Id<ActivityFacility>, MPAreaData> dataMPAreas, Collection<Coord> stops) {
		minCapacity = Double.MAX_VALUE;
		maxCapacity = 0;
		this.stops = stops;
		wTotals = new double[dataMPAreas.size()];
		for(MPAreaData area:dataMPAreas.values())
			this.dataMPAreas.add(area);
		for(int w=0; w<matrixCapacities.length; w++) {
			double wTotal=0;
			for(int c=0; c<matrixCapacities[0].length; c++)
				wTotal += matrixCapacities[w][c];
			if(wTotal<minCapacity && wTotal>0)
				minCapacity = wTotal;
			if(wTotal>maxCapacity)
				maxCapacity = wTotal;
			wTotals[w] = wTotal;
		}
		minCapacity = Math.ceil(minCapacity);
		if(minCapacity==maxCapacity)
			maxCapacity++;
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_TM, TransformationFactory.WGS84_UTM48N);
		double maxSize = 50;
		for(int w=0; w<wTotals.length; w++)
			if(wTotals[w]>0) {
				float proportion = (float) ((wTotals[w]-minCapacity)/(maxCapacity-minCapacity));
				MPAreaData area = dataMPAreas.get(w);
				Polygon polygon = area.getPolygon();
				double points[][] = new double[polygon.getNumPoints()][2];
				for(int p=0; p<points.length; p++) {
					Coordinate c = polygon.getCoordinates()[p];
					Coord coord = coordinateTransformation.transform(new CoordImpl(c.x, c.y));
					points[p][0] = coord.getX();
					points[p][1] = coord.getY();
				}
				Color color = JetColor.getJetColor(proportion);
				paintPolygon(g2, layersPanel, points, color);
				//paintCircle(g2, layersPanel, area.getCoord(), proportion*maxSize, new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
			}
		super.paint(g2, layersPanel);
		for(Coord stop:stops)
			paintCross(g2, layersPanel, stop, 2, Color.BLACK);
		JetColor.paintScale(g2, layersPanel.getSize().width-330, layersPanel.getSize().height-60, 300, 30, new Font("Times New Roman", Font.PLAIN, 14), Color.BLACK, minCapacity, maxCapacity, 5);
	}

}
