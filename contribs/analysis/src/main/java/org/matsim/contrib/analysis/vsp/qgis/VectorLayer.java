package org.matsim.contrib.analysis.vsp.qgis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.core.utils.io.IOUtils;

public class VectorLayer extends QGisLayer {

	private QGisConstants.geometryType geometryType;
	
	private Set<VectorJoin> vectorJoins;
	
	// transparency can be set here globally for all layer objects AND / OR
	// locally in the symbol layers (e.g. if you want the transparency of graduated
	// symbols to be different)
	private int layerTransparency;
	
	//these members are csv specific and needed for text files with geometry
	//there are two ways to set x and y fields for csv geometry files
	//1) if there is a header, you can set the members xField and yField to the name of the column headers
	//2) if there is no header, you can write the column index into the member (e.g. field_1, field_2,...), but works also if there is a header
	private String delimiter; // by default: null
	private String xField;
	private String yField;
	private String header;
	
	private final boolean useHeader;
	
	/**
	 * Instantiates a new qgis layer that contains vector data (can still be geometry OR data).
	 * After calling the constructor you have to:
	 * </p>
	 * # Case 1: shp input file (and in general)
	 * </p>
	 * 1) Specify a renderer (how a layer is drawn)
	 * </p>
	 * # Case 2: csv input and geometry
	 * </p>
	 * 2) set an x and y field (to tell QGis where the geometry information is stored)
	 * </p>
	 * Optionally: set character that delimits text fields (but causes problems if your lines end on a delimiter) 
	 * </p>
	 * If the csv file contains data used for a vector join, no x and y fields need to be specified
	 * 
	 * @param name layer name name of the layer (representation in qgis layer tree)
	 * @param path path to the input file
	 * @param geometryType type of geometry within the file
	 * @param useHeader 
	 */
	public VectorLayer(String name, String path, org.matsim.contrib.analysis.vsp.qgis.QGisConstants.geometryType geometryType, boolean useHeader) {
		
		super(name, path);
		
		this.geometryType = geometryType;
		this.useHeader = useHeader;
		this.vectorJoins = new HashSet<VectorJoin>();
		this.setType(QGisConstants.layerType.vector);
		
		if(!this.getInputType().equals(QGisConstants.inputType.xml)){
			
			if(this.getGeometryType().equals(QGisConstants.geometryType.Line)){
				
				this.setLayerClass(QGisConstants.layerClass.SimpleLine);
				
			} else if(this.getGeometryType().equals(QGisConstants.geometryType.Point)){
				
				this.setLayerClass(QGisConstants.layerClass.SimpleMarker);
				
			}
			
		}
		
		if(this.useHeader){
			this.setHeader();
		}
		
	}
	

	public VectorLayer(String name, String path, org.matsim.contrib.analysis.vsp.qgis.QGisConstants.geometryType geometryType){
		
		super(name, path);
		
		this.geometryType = geometryType;
		this.useHeader = false;
		
		this.vectorJoins = new HashSet<VectorJoin>();
		this.setType(QGisConstants.layerType.vector);
		
		if(!this.getInputType().equals(QGisConstants.inputType.xml)){
			
			if(this.getGeometryType().equals(QGisConstants.geometryType.Line)){
				
				this.setLayerClass(QGisConstants.layerClass.SimpleLine);
				
			} else if(this.getGeometryType().equals(QGisConstants.geometryType.Point)){
				
				this.setLayerClass(QGisConstants.layerClass.SimpleMarker);
				
			}
			
		}
		
	}
	
	public String getHeader(){
		return this.header;
	}
	
	private void setHeader(){
		
		File file = new File(this.getPath());
		
		if(file.exists()){
			
			if(this.getInputType().equals(QGisConstants.inputType.csv)){
				
				try {
					
					BufferedReader reader = IOUtils.getBufferedReader(this.getPath());
					
					this.header = reader.readLine();
					
				} catch (FileNotFoundException e) {
					
					e.printStackTrace();
					
				} catch (IOException e) {
					
					e.printStackTrace();
					
				}
				
			}
			
		} else{
			
			throw new RuntimeException("File " + this.getPath() + " cannot be found! Please check input path!");
			
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
	
	/**
	 * Sets the x field to the specified column index.
	 * </p>
	 * This method is used if no file header exists.
	 * 
	 * @param columnIndex the column that contains the x coordinates of the geometries
	 */
	public void setXField(int columnIndex){
		
		if(!this.useHeader){
			
			this.xField = "field_" + Integer.valueOf(columnIndex);
			
		} else{
			
			throw new RuntimeException("The file " + this.getPath() + " has a header! Use the method \"setXField(String xField)\" instead!");
			
		}
		
	}
	
	/**
	 * Sets the x field to the specified column name.
	 * </p>
	 * This method is used if a file header exists.
	 * 
	 * @param xField the header field name of the column that contains the x coordinates of the geometries
	 */
	public void setXField(String xField){
		
		if(this.useHeader){
			
			if(this.header.contains(xField)){
				
				this.xField = xField;
				
			} else{
			
				throw new RuntimeException("Field " + xField + " cannot be found in the file header.");
				
			}
			
		} else{
			
			throw new RuntimeException("The file " + this.getPath() + " has no header! Use the method \"setXField(int xField)\" instead!");
			
		}
		
	}
	
	/**
	 * Sets the y field to the specified column index.
	 * </p>
	 * This method is used if no file header exists.
	 * 
	 * @param columnIndex the column that contains the y coordinates of the geometries
	 */
	public String getXField(){
		return this.xField;
	}
	
	/**
	 * Sets the y field to the specified column name.
	 * </p>
	 * This method is used if a file header exists.
	 * 
	 * @param yField the header field name of the column that contains the x coordinates of the geometries
	 */
	public void setYField(int columnIndex){
		
		if(!this.useHeader){
			
				this.yField = "field_" + Integer.valueOf(columnIndex);
			
		} else{
			
			throw new RuntimeException("The file " + this.getPath() + " has a header! Use the method \"setYField(String yField)\" instead!");
			
		}
		
	}
	
	public void setYField(String yField){
		
		if(this.useHeader){
			
			if(this.header.contains(yField)){
				
				this.yField = yField;
				
			} else{
				
				throw new RuntimeException("Field " + yField + " cannot be found in file header.");
				
			}
			
		} else{
			
			throw new RuntimeException("The file " + this.getPath() + " has no header! Use the method \"setYField(int yField)\" instead!");
			
		}
		
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
	 * Creates a vector join between a geometry layer (e.g. points) and a data layer (e.g. accessibility data, immissions,...).
	 * This method must be called by the geometry layer because the vector joins are stored there.
	 * You need to specify the name of the data layer and the field names of the geometry and the data layer so
	 * QGis can merge objects with same field contents (e.g. same Id). The field names of the layers don't have to be equal. 
	 *
	 * @param layer the layer that contains the data (not the geometry)
	 * @param joinFieldName attribute name of the data layer
	 * @param targetFieldName attribute name of the geometry layer
	 * @param zField 
	 */
	public void addVectorJoin(QGisLayer layer, String joinFieldName, String targetFieldName, String zField){
		
		this.vectorJoins.add(new VectorJoin(layer.getId(), joinFieldName, targetFieldName));
		this.header += this.delimiter + layer.getName() + "_" + zField;
		
	}
	
	public boolean isUsingHeader(){
		return this.useHeader;
	}
	
	/**
	 * This method is only for the {@code QGisFileWriter}.
	 * 
	 * @return true is the header is not equal to null
	 */
	protected String headerUsed(){
		if(!this.useHeader){
			return "No";
		} else{
			return "Yes";
		}
	}
	
}
