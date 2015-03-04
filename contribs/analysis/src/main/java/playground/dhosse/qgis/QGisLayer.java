package playground.dhosse.qgis;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.matsim.api.core.v01.Id;

import playground.dhosse.qgis.rendering.QGisRenderer;

public abstract class QGisLayer {
	
	private Id<QGisLayer> id;
	private String name;
	private String path;
	
	private QGisConstants.inputType inputType;
	
	private QGisConstants.layerClass layerClass;
	
	// must be specified for all layers (except for data layers)
	private QGisRenderer renderer;
	
	private QGisConstants.layerType type;
	
	private SRS srs;
	
	/**
	 * Creates a new instance of a qgis layer.
	 * For each geometry layer, a {@code QGisRenderer} must be created (for use cases look at package layerTemplates).
	 * </p>
	 * If the layer input is a csv file, you also need to specify a delimiter (e.g. , or ;) and the header names
	 * of the columns that contain the x and y coordinates (by setXField and setYField).
	 * 
	 * @param name a unique name for the layer (as it is displayed in qgis later)
	 * @param path the path to the input file
	 * @param geometryType the type of geometry (none, point, line, polygon)
	 */
	public QGisLayer(String name, String path){
		
		this.name = name;
		this.path = path;
		this.id = Id.create(name + new SimpleDateFormat("yyyyMMdd").format(new Date()), QGisLayer.class);
		
		if(this.path.contains(QGisConstants.inputType.csv.toString())){
			
			this.inputType = QGisConstants.inputType.csv;
			
		} else if(this.path.contains(QGisConstants.inputType.shp.toString())){
			
			this.inputType = QGisConstants.inputType.shp;
			
		} else if(this.path.contains(QGisConstants.inputType.xml.toString())){
			
			this.inputType = QGisConstants.inputType.xml;
			
		} else{
			
			throw new RuntimeException("Invalid input type! Cannot create layer.");
			
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

	public QGisConstants.layerClass getLayerClass() {
		return layerClass;
	}

	public void setLayerClass(QGisConstants.layerClass layerClass) {
		this.layerClass = layerClass;
	}
	
	public String getPath(){
		return this.path;
	}
	
	public QGisConstants.inputType getInputType(){
		return this.inputType;
	}

	public QGisConstants.layerType getType() {
		return type;
	}

	public void setType(QGisConstants.layerType type) {
		this.type = type;
	}

	public SRS getSrs() {
		return srs;
	}

	public void setSrs(String srs) {
		this.srs = SRS.createSpatialRefSys(srs);
	}
	
}
