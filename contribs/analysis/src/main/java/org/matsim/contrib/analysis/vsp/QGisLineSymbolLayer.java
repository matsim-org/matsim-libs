package org.matsim.contrib.analysis.vsp;

public class QGisLineSymbolLayer extends QGisSymbolLayer {
	
	private double width;
	private QGisConstants.symbolType symbolType;
	
	public QGisLineSymbolLayer(){
		this.symbolType = QGisConstants.symbolType.Line;
	}
	
	public double getWidth(){
		return this.width;
	}
	
	public void setWidth(double width){
		this.width = width;
	}
	
	public QGisConstants.symbolType getSymbolType(){
		return this.symbolType;
	}

}
