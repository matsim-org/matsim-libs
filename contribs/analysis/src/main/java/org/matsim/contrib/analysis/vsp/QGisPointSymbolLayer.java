package org.matsim.contrib.analysis.vsp;

import java.awt.Color;

public class QGisPointSymbolLayer extends QGisSymbolLayer {
	
	private Color colorBorder;
	private QGisConstants.pointLayerSymbol pointLayerSymbol;
	private double size;
	private double[] sizeMapUnitScale;
	private QGisConstants.symbolType symbolType;
	
	public QGisPointSymbolLayer(){
		this.symbolType = QGisConstants.symbolType.Marker;
	}
	
	public Color getColorBorder() {
		return colorBorder;
	}
	
	public void setColorBorder(Color colorBorder) {
		this.colorBorder = colorBorder;
	}
	
	public QGisConstants.pointLayerSymbol getPointLayerSymbol() {
		return pointLayerSymbol;
	}
	
	public void setPointLayerSymbol(QGisConstants.pointLayerSymbol pointLayerSymbol) {
		this.pointLayerSymbol = pointLayerSymbol;
	}
	
	public double getSize() {
		return size;
	}
	
	public void setSize(double size) {
		this.size = size;
	}
	
	public double[] getSizeMapUnitScale() {
		return sizeMapUnitScale;
	}
	
	public void setSizeMapUnitScale(double[] sizeMapUnitScale) {
		this.sizeMapUnitScale = sizeMapUnitScale;
	}
	
	public QGisConstants.symbolType getSymbolType(){
		return this.symbolType;
	}
	
}
