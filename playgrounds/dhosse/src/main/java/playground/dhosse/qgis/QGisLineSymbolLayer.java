package playground.dhosse.qgis;

public class QGisLineSymbolLayer extends QGisSymbolLayer {
	
	private double width = 0.25;
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
