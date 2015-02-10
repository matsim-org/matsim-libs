package playground.dhosse.qgis;

import java.awt.Color;

public abstract class QGisSymbolLayer {
	
	private QGisConstants.symbolType symbolType;
	
	private Color color;
	
	private int layerTransparency = 0;
	private double[] offset = {0,0};
	private double[] offsetMapUnitScale = {1,1};
	
	public QGisConstants.symbolType getSymbolType() {
		return symbolType;
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
	
	public int getLayerTransparency() {
		return layerTransparency;
	}
	
	public void setLayerTransparency(int layerTransparency) {
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
	
}
