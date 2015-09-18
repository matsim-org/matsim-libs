package playground.sergioo.eventAnalysisTools2013.excessWaitingTime;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.text.DecimalFormat;

import org.matsim.api.core.v01.Coord;

import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.Painter;

public class CircleLegendPainter extends Painter {
	
	//Attributes
	private Color color;
	private double scale;
	private double[] values;
	private Coord coord;
	private double distance;
	
	//Methods
	public CircleLegendPainter(Color color, double scale, double[] values, double distance, Coord coord) {
		super();
		this.color = color;
		this.scale = scale;
		this.coord = coord;
		this.distance = distance;
		this.values = values;
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		for(int i=0; i<values.length; i++) {
			paintCircle(g2, layersPanel, new Coord(coord.getX() + Math.pow(i, 0.9) * 2 * distance, coord.getY()), (int)(Math.sqrt(values[i]*scale)), color);
			int[] screenPoint = layersPanel.getScreenXY(new double[]{coord.getX()+Math.pow(i,0.9)*2*distance, coord.getY()-distance});
			g2.setFont(new Font("Times New Roman", Font.PLAIN, 32));
			g2.drawString(DecimalFormat.getIntegerInstance().format(values[i]), screenPoint[0]-10, screenPoint[1]+20);
		}
		double x = 349000, y = 139000, d=5000;
		paintLine(g2, layersPanel, new double[]{x, y}, new double[]{x+d, y}, new BasicStroke(), Color.BLACK);
		paintLine(g2, layersPanel, new double[]{x, y+100}, new double[]{x, y-100}, new BasicStroke(), Color.BLACK);
		paintLine(g2, layersPanel, new double[]{x+d, y+100}, new double[]{x+d, y-100}, new BasicStroke(), Color.BLACK);
		int[] screenPoint = layersPanel.getScreenXY(new double[]{x, y-1000});
		g2.setFont(new Font("Times New Roman", Font.PLAIN, 24));
		g2.drawString(d/1000+" km", screenPoint[0], screenPoint[1]);
	}

}
