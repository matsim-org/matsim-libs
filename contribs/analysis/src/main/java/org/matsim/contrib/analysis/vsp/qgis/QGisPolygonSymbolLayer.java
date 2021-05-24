package org.matsim.contrib.analysis.vsp.qgis;

import java.awt.*;

public class QGisPolygonSymbolLayer extends QGisSymbolLayer {

	private double[] sizeMapUnitScale;
	private QGisConstants.symbolType symbolType;
	private Color outlineColor;
	private String outlineWidth;

	public QGisPolygonSymbolLayer(){
		this.symbolType = QGisConstants.symbolType.Fill;
		this.setOffsetMapUnitScale(new double[]{0,0,0,0,0,0});
		this.setSizeUnits(QGisConstants.sizeUnits.MM);
		this.setPenStyle(QGisConstants.penstyle.solid);
		this.outlineWidth = "0.26";
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

	public Color getOutlineColor() {
		return outlineColor;
	}

	public void setOutlineColor(Color outlineColor) {
		this.outlineColor = outlineColor;
	}

	public String getOutlineWidth() {
		return outlineWidth;
	}

	public void setOutlineWidth(String outlineWidth) {
		this.outlineWidth = outlineWidth;
	}
}
