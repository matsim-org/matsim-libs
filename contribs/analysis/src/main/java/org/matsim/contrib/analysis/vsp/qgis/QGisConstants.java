package org.matsim.contrib.analysis.vsp.qgis;

public class QGisConstants {
	
	public static final String currentVersion = "2.10.1-Pisa";
//	public static final String currentVersion = "2.4.0-Chugiak";
	
	//predefined geometry types that quantum gis can handle. "no geometry" means a data table used for vectorjoin
	public static enum geometryType{Line,Point,Polygon,No_geometry};
	
	public static enum inputType{shp,csv,xml};
	
	public static enum layerClass{SimpleLine,SimpleMarker,SimpleFill};
	
	public static enum layerType{raster,vector};
	
	public static enum penstyle{no,solid};
	
	public static enum pointLayerSymbol{circle,rectangle};
	
	public static enum rasterRendererType{multibandcolor};
	
	public static enum renderingType{singleSymbol,categorizedSymbol,graduatedSymbol,RuleRenderer,PolygonRenderer};
	
	public static enum sizeUnits{MM,MapUnit};
	
	public static enum symbolType{Line,Marker,Fill};
	
	public static enum units{degrees,meters};
	
}
