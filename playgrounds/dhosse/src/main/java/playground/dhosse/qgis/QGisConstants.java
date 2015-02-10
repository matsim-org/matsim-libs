package playground.dhosse.qgis;

public class QGisConstants {
	
	public static final String currentVersion = "2.4.0-Chugiak";
	
	public static enum units{degrees,meters};
	
	public static enum geometryType{Line,Point};
	
	public static enum symbolType{Line,Marker};
	
	public static enum layerClass{SimpleLine,SimpleMarker};
	
	public static enum inputType{shp,csv,xml};
	
	public static enum pointLayerSymbol{circle};
	
	public static enum penstyle{solid};
	
	public static enum renderingType{singleSymbol,categorizedSymbol,graduatedSymbol,RuleRenderer};
	
}
