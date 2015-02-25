package playground.dhosse.qgis;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;

public class QGisLayer {
	
	private Id<QGisLayer> id;
	private String name;
	private String path;
	
	private QGisConstants.inputType inputType;
	
	private QGisConstants.geometryType geometryType;
	private QGisConstants.layerClass layerClass;
	
	private QGisRenderer renderer;
	
	private int layerTransparency;
	
	private Set<VectorJoin> vectorJoins;
	
	//these members are csv specific and needed for text files with geometry
	private String delimiter;
	private String xField;
	private String yField;
	
	/**
	 * Creates a new instance of a qgis layer.
	 * For each layer, a {@code QGisRenderer} must be created (for use cases look at package layerTemplates).
	 * 
	 * If the layer input is a csv file, you also need to specify a delimiter (e.g. , or ;) and the header names
	 * of the columns that contain the x and y coordinates. 
	 * 
	 * @param name a unique name for the layer (as it is displayed in qgis later)
	 * @param path the path to the input file
	 * @param geometryType the type of geometry (none, point, line, polygon)
	 */
	public QGisLayer(String name, String path, QGisConstants.geometryType geometryType){
		
		this.name = name;
		this.path = path;
		this.id = Id.create(name + new SimpleDateFormat("yyyyMMdd").format(new Date()), QGisLayer.class);
		this.setGeometryType(geometryType);
		this.layerTransparency = 0;
		this.vectorJoins = new HashSet<VectorJoin>();
		
		this.build();
		
	}
	
	private void build(){
		
		if(this.getGeometryType().equals(QGisConstants.geometryType.Line)){
			
			this.setLayerClass(QGisConstants.layerClass.SimpleLine);
			
		} else if(this.getGeometryType().equals(QGisConstants.geometryType.Point)){
			
			this.setLayerClass(QGisConstants.layerClass.SimpleMarker);
			
		}
		
		if(this.path.contains("csv")){
			this.inputType = QGisConstants.inputType.csv;
		} else if(this.path.contains("shp")){
			this.inputType = QGisConstants.inputType.shp;
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
	
	public String getPath(){
		return this.path;
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
	
	/**
	 *
	 * @param layer the layer that contains the data (not the geometry)
	 * @param joinFieldName name of the attribute of the join layer
	 * @param targetFieldName
	 */
	public void addVectorJoin(QGisLayer layer, String joinFieldName, String targetFieldName ){
		this.vectorJoins.add(new VectorJoin(layer.getId(), joinFieldName, targetFieldName));
	}
	
	public QGisConstants.inputType getInputType(){
		return this.inputType;
	}
	
}
