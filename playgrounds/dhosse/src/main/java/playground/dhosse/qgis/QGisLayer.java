package playground.dhosse.qgis;

import org.matsim.api.core.v01.Id;

public class QGisLayer {
	
	private Id<QGisLayer> id;
	private String name;
	private String datasource;
	
	private QGisConstants.geometryType geometryType;
	private QGisConstants.layerClass layerClass;
	
	private int layerTransparency = 0;
	
	private QGisRenderer renderer;
	
	public QGisLayer(String name, String datasource, QGisConstants.geometryType geometryType){
		
		this.name = name;
		this.datasource = datasource;
		this.id = Id.create(name, QGisLayer.class);
		this.setGeometryType(geometryType);
		
		this.build();
		
	}
	
	private void build(){
		
		if(this.getGeometryType().equals(QGisConstants.geometryType.Line)){
			
			this.setLayerClass(QGisConstants.layerClass.SimpleLine);
			
		} else if(this.getGeometryType().equals(QGisConstants.geometryType.Point)){
			
			this.setLayerClass(QGisConstants.layerClass.SimpleMarker);
			
		}
		
	}
	
	public Id<QGisLayer> getId(){
		return this.id;
	}
	
	protected void setId(Id<QGisLayer> id){
		this.id = id;
	}
	
	public String getName(){
		return this.name;
	}
	
	protected void setName(String name){
		this.name = name;
	}
	
	public int getLayerTransparency(){
		return this.layerTransparency;
	}
	
	protected void setLayerTransparency(int transparency){
		this.layerTransparency = transparency;
	}
	
	public QGisRenderer getRenderer() {
		return renderer;
	}

	public void setRenderer(QGisRenderer renderer) {
		this.renderer = renderer;
	}

	public QGisConstants.geometryType getGeometryType() {
		return geometryType;
	}

	public void setGeometryType(QGisConstants.geometryType geometryType) {
		this.geometryType = geometryType;
	}

	public QGisConstants.layerClass getLayerClass() {
		return layerClass;
	}

	public void setLayerClass(QGisConstants.layerClass layerClass) {
		this.layerClass = layerClass;
	}
	
}
