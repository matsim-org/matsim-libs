package org.matsim.contrib.analysis.vsp.qgis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.utils.io.AbstractMatsimWriter;

import com.vividsolutions.jts.geom.Envelope;
/**
 * Writer that creates a QuantumGIs project file (*.qgs). 
 *
 * @author dhosse
 *
 */

public class QGisWriter extends AbstractMatsimWriter implements MatsimWriter {

	private static final Logger log = Logger.getLogger( QGisWriter.class );
	
	private QGisFileWriter handler;
	
	private List<QGisLayer> layers = new ArrayList<>();
	
	protected SRS srs = null;
	
	private QGisConstants.units unit;

	private String workingDirectory;
	private String title = "";
	private String projectname = "";
	
	private Envelope envelope;
	
	/**
	 * Creates a new instance of a QGis project file (*.qgs) writer.
	 * Coordinate reference system and working directory are mandatory for writing the file.
	 * Layers have to be added separately with the method {@code addLayer}. If no layers were
	 * added, the writer creates an empty project file.
	 * </p>
	 * After calling the constructor you have to set the envelope (starting view) manually.
	 * 
	 * @param crs coordinate reference system of the network / spatial data
	 * @param workingDir the directory in which all generated files (shapefiles, qgs file) are put.
	 * 
	 */
	public QGisWriter(String crs, String workingDir){

		setCrs(crs);
		this.workingDirectory = workingDir;
		this.handler = new QGisFileWriter(this);
		this.setUnit(QGisConstants.units.meters);
		
	}
	
	/**
	 * 
	 * @param srs String representation of the spatial reference system that you want to assign.
	 */
	private void setCrs(String srs){
		this.srs = SRS.createSpatialRefSys(srs);
	}
	
	/**
	 * Adds a new layer to the layers list. 
	 * 
	 * @param layer the layer you want to add
	 */
	public void addLayer(QGisLayer layer){
		
		this.layers.add(layer);
		
	}
	
	/**
	 * Adds a new layer to the layers list at the specified position.
	 * 
	 * @param position the index you want to add the layer at
	 * @param layer the layer you want to add
	 */
	public void addLayer(int position, QGisLayer layer){
		
		this.layers.add(position, layer);
		
	}
	
	@Override
	public void write(String filename) {
		
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(this.workingDirectory);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		log.info("Writing QuantumGIS project file (*.qgs) to " + this.workingDirectory + filename + "...");
		log.info("Make sure all your input files exist. This writer will do absolutely no file writing except for the project file.");
		
		this.printQGisProjectSettings();
		
		this.openFile(this.workingDirectory + filename);
		
		try {
		
			// write file header (contains document type, qgis version etc)
			this.handler.writeHeaderAndStartElement(this.writer);
			
			// writes the project title (if specified, else empty field)
			this.handler.writeTitle(this.writer);
			
			// writes the layer tree (all layers of the project in order of painting)
			this.handler.writeLayerTreeGroup(this.writer);
			
			// writes basic map information, such as envelope of view and spatial reference system
			this.handler.writeMapCanvas(this.writer);
			
			// TODO: what does this do?
			this.handler.writeLayerTreeCanvas(this.writer);
			
			if(this.layers.size() > 0){
				
				// writes the map layers inside the project file
				// order has nothing to do with drawing order
				// contains specifications of layer (id, datasource, srs, renderer etc)
				this.handler.writeProjectLayers(this.writer);
				
			}
			
			// writes project properties like gui settings and the type of paths that's used
			this.handler.writeProperties(this.writer);
			
			this.handler.endFile(this.writer);
			this.writer.flush();
			this.writer.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		log.info("...Done.");
		
		OutputDirectoryLogging.closeOutputDirLogging();
		
	}

	private void printQGisProjectSettings() {
		
		log.info("QGis project uses the following configuration:");
		
		log.info("PROJECT VERSION:\t\t\t" + QGisConstants.currentVersion);
		log.info("EXTENT (MINX, MINY, MAXX, MAXY):\t" + this.envelope.getMinX() + ", " +
														this.envelope.getMinY() + ", " +
														this.envelope.getMaxX() + ", " +
														this.envelope.getMaxY());
		log.info("SPATIAL REFERENCE SYSTEM:\t\t" + this.srs.getDescription());
		log.info("NUMBER OF LAYERS:\t\t\t" + this.layers.size());
		log.info("LAYERS (IN DRAWING ORDER):");
		
		log.info("############################################################");
		
		String srs = null;
		
		for(QGisLayer layer : this.layers){
			
			srs = layer.getSrs() != null ? layer.getSrs().getDescription() : "[project]";
			
			log.info("name:\t\t\t\t" + layer.getName());
			log.info("input type:\t\t\t" + layer.getInputType().toString());
			log.info("layer type:\t\t\t" + layer.getType().toString());
			
			if(layer.getLayerClass() != null){
				
				log.info("layer class:\t\t\t" + layer.getLayerClass().toString());
				
			}
			
			if(layer.getRenderer() != null){
				
				log.info("rendering type:\t\t\t" + layer.getRenderer().getRenderingType().toString());
				log.info("renderer:\t\t\t\t" + layer.getRenderer().getClass().getSimpleName());
				
			}
			
			log.info("spatial reference system:\t\t" + srs);
			
			log.info("############################################################");
			
		}
		
	}

	public void changeWorkingDirectory(String workingDir){
		this.workingDirectory = workingDir;
	}
	
	public List<QGisLayer> getLayers(){
		return this.layers;
	}
	
	public Envelope getEnvelope(){
		return this.envelope;
	}
	
	public String getWorkingDir(){
		return this.workingDirectory;
	}
	
	/**
	 * Sets the starting view on the map when opening the project file.
	 * 
	 * @param envelope with minx, miny, maxx and maxy coordinates of the starting view
	 */
	public void setEnvelope(Envelope envelope){
		this.envelope = envelope;
	}
	
	/**
	 * 
	 * @return The spatial reference system that was created for the project file
	 */
	public SRS getSRS(){
		return this.srs;
	}

	public QGisConstants.units getUnit() {
		return unit;
	}

	public void setUnit(QGisConstants.units unit) {
		this.unit = unit;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getProjectname() {
		return projectname;
	}

	public void setProjectname(String projectname) {
		this.projectname = projectname;
	}
	
}
