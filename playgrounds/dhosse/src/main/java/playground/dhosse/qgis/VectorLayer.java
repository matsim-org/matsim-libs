package playground.dhosse.qgis;

import java.util.HashSet;
import java.util.Set;

public class VectorLayer extends QGisLayer {

	private QGisConstants.geometryType geometryType;
	
	private Set<VectorJoin> vectorJoins;
	
	private int layerTransparency;
	
	//these members are csv specific and needed for text files with geometry
	private String delimiter;
	private String xField;
	private String yField;
	
	public VectorLayer(String name, String path, playground.dhosse.qgis.QGisConstants.geometryType geometryType) {
		
		super(name, path);
		
		this.geometryType = geometryType;
		this.vectorJoins = new HashSet<VectorJoin>();
		this.setType(QGisConstants.layerType.vector);
		
		build();
		
	}
	
	private void build(){
		
		if(!this.getInputType().equals(QGisConstants.inputType.xml)){
			
			if(this.getGeometryType().equals(QGisConstants.geometryType.Line)){
				
				this.setLayerClass(QGisConstants.layerClass.SimpleLine);
				
			} else if(this.getGeometryType().equals(QGisConstants.geometryType.Point)){
				
				this.setLayerClass(QGisConstants.layerClass.SimpleMarker);
				
			}
			
		}
		
	}

	public QGisConstants.geometryType getGeometryType() {
		return geometryType;
	}

	public void setGeometryType(QGisConstants.geometryType geometryType) {
		this.geometryType = geometryType;
	}
	
	public void setDelimiter(String delimiter){
		this.delimiter = delimiter;
	}
	
	public String getDelimiter(){
		return this.delimiter;
	}
	
	public void setXField(String xField){
		this.xField = xField;
	}
	
	public String getXField(){
		return this.xField;
	}
	
	public void setYField(String yField){
		this.yField = yField;
	}
	
	public String getYField(){
		return this.yField;
	}
	
	public int getLayerTransparency(){
		return this.layerTransparency;
	}
	
	public void setLayerTransparency(int transparency){
		this.layerTransparency = transparency;
	}
	
	public Set<VectorJoin> getVectorJoins(){
		return this.vectorJoins;
	}
	
	/**
	 *
	 * @param layer the layer that contains the data (not the geometry)
	 * @param joinFieldName name of the attribute of the join layer
	 * @param targetFieldName
	 */
	public void addVectorJoin(QGisLayer layer, String joinFieldName, String targetFieldName ){
		this.vectorJoins.add(new VectorJoin(layer.getId(), joinFieldName, targetFieldName));
	}

}
