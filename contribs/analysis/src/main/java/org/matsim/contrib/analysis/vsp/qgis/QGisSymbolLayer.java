package org.matsim.contrib.analysis.vsp.qgis;

import java.awt.Color;

/**
 * This class contains information about the symbols that are actually drawn in QGis (e.g. color,
 * transparency).
 * 
 * @author dhosse
 *
 */
public abstract class QGisSymbolLayer {
	
	private QGisConstants.symbolType symbolType;
	
	private Color color;
	
	private int id;
	
	private double layerTransparency = 0;
	private double[] offset = {0,0};
	private double[] offsetMapUnitScale = {1,1};
	
	private QGisConstants.sizeUnits sizeUnits;
	
	private QGisConstants.penstyle penStyle;
	private QGisConstants.penstyle penStyleBorder;

	public QGisConstants.symbolType getSymbolType() {
		return symbolType;
	}
	
	public int getId(){
		return this.id;
	}
	
	public void setId(int id){
		this.id = id;
	}
	
	protected void setSymbolType(QGisConstants.symbolType symbolType) {
		this.symbolType = symbolType;
	}
	
	public Color getColor() {
		return color;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	public double getLayerTransparency() {
		return layerTransparency;
	}
	
	public void setLayerTransparency(double layerTransparency) {
		this.layerTransparency = layerTransparency;
	}
	
	public double[] getOffset() {
		return offset;
	}
	
	protected void setOffset(double[] offset) {
		this.offset = offset;
	}
	
	public double[] getOffsetMapUnitScale() {
		return offsetMapUnitScale;
	}
	
	protected void setOffsetMapUnitScale(double[] offsetMapUnitScale) {
		this.offsetMapUnitScale = offsetMapUnitScale;
	}

	public QGisConstants.sizeUnits getSizeUnits() {
		return sizeUnits;
	}

	public void setSizeUnits(QGisConstants.sizeUnits sizeUnits) {
		this.sizeUnits = sizeUnits;
	}
	
	public void setPenStyle(QGisConstants.penstyle penstyle){
		this.penStyle = penstyle;
	}
	
	public QGisConstants.penstyle getPenStyle(){
		return this.penStyle;
	}

	public QGisConstants.penstyle getPenStyleBorder() {
		return penStyleBorder;
	}

	public void setPenStyleBorder(QGisConstants.penstyle penStyleBorder) {
		this.penStyleBorder = penStyleBorder;
	}
}
