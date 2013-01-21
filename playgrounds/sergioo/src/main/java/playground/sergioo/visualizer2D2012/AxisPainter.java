package playground.sergioo.visualizer2D2012;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class AxisPainter extends Painter {

	//Attributes
	private double endXAxis;
	private double endYAxis;
	private double startXAxis = 0;
	private double startYAxis = 0;
	private double deltaXAxis;
	private double deltaYAxis;
	private Color color = Color.BLACK;
	
	//Constructors
	/**
	 * @param longXAxis
	 * @param longYAxis
	 * @param deltaXAxis
	 * @param deltaYAxis
	 * @param color
	 */
	public AxisPainter(double longXAxis, double longYAxis, double deltaXAxis, double deltaYAxis, Color color) {
		super();
		this.endXAxis = longXAxis;
		this.endYAxis = longYAxis;
		this.deltaXAxis = deltaXAxis;
		this.deltaYAxis = deltaYAxis;
		this.color = color;
	}
	/**
	 * @param longXAxis
	 * @param longYAxis
	 * @param startXAxis
	 * @param startYAxis
	 * @param deltaXAxis
	 * @param deltaYAxis
	 * @param color
	 */
	public AxisPainter(double endXAxis, double endYAxis, double startXAxis, double startYAxis, double deltaXAxis, double deltaYAxis, Color color) {
		super();
		this.endXAxis = endXAxis;
		this.endYAxis = endYAxis;
		this.startXAxis = startXAxis;
		this.startYAxis = startYAxis;
		this.deltaXAxis = deltaXAxis;
		this.deltaYAxis = deltaYAxis;
		this.color = color;
	}

	//Methods
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		double baseSize = layersPanel.getWorldDistance(5);
		paintArrow(g2, layersPanel, new double[]{startXAxis, startYAxis}, new double[]{endXAxis, startYAxis}, Math.PI/6, baseSize*2, new BasicStroke(), color);
		paintArrow(g2, layersPanel, new double[]{startXAxis, startYAxis}, new double[]{startXAxis, endYAxis}, Math.PI/6, baseSize*2, new BasicStroke(), color);
		int fontSize = 14;
		g2.setFont(new Font("Times New Roman", Font.PLAIN, fontSize));
		for(double x=startXAxis; x<endXAxis; x+=deltaXAxis) {
			paintLine(g2, layersPanel, new double[]{x, startYAxis}, new double[]{x, startYAxis-baseSize}, new BasicStroke(), color);
			String text = (int)(x/3600)+"";
			int[] screenPoint = layersPanel.getScreenXY(new double[]{x-text.length()*baseSize/2, startYAxis-1.5*baseSize});
			g2.drawString(text, screenPoint[0], screenPoint[1]+fontSize);
		}
		for(double y=startYAxis; y<endYAxis; y+=deltaYAxis) {
			paintLine(g2, layersPanel, new double[]{startXAxis, y}, new double[]{startXAxis-baseSize, y}, new BasicStroke(), color);
			String text = (int)(y/3600)+"";
			int[] screenPoint = layersPanel.getScreenXY(new double[]{startXAxis-1.5*baseSize, y});
			g2.drawString(text, screenPoint[0]-(fontSize/2)*text.length(), screenPoint[1]+fontSize/2);
		}
	
	}

}
