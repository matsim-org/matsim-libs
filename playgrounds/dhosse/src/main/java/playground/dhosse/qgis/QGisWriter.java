package playground.dhosse.qgis;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.AbstractMatsimWriter;

/**
 * 
 * @author dhosse
 *
 */

public class QGisWriter extends AbstractMatsimWriter implements MatsimWriter {

	private static final Logger log = Logger.getLogger( QGisWriter.class );
	
	private QGisFileWriter handler;
	
	protected Network network;
	
	protected String today;
	
	protected Map<String,QGisLayer> layers = new HashMap<String,QGisLayer>();
	
	private String workingDirectory;
	
	protected SRS srs = null;
	
	private double[] extent;
	
	/**
	 * Creates a new instance of a QGis project file (*.qgs) writer.
	 * 
	 * @param crs coordinate reference system of the network
	 * @param workingDir the directory in which all generated files (shapefiles, qgs file) are put. 
	 * 
	 */
	public QGisWriter(String crs, String workingDir){
		
		setCrs(crs);
		this.workingDirectory = workingDir;
		this.handler = new QGisFileWriter(this);
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		this.today = formatter.format(new Date());
		
	}
	
	private void setCrs(String srs){
		this.srs = SRS.createSpatialRefSys(srs);
	}
	
	public void addLayer(QGisLayer layer){
		
		this.layers.put(layer.getName(), layer);
		
	}
	
	@Override
	public void write(String filename) {
		
		log.info("Writing QuantumGIS project file (*.qgs) to " + this.workingDirectory + filename + "...");
		
		this.openFile(this.workingDirectory + filename);
		
		try {
			
			this.handler.writeHeaderAndStartElement(this.writer);
			this.handler.writeTitle(this.writer);
			this.handler.writeLayerTreeGroup(this.writer);
			this.handler.writeRelations(this.writer);
			this.handler.writeMapCanvas(this.writer);
			this.handler.writeLayerTreeCanvas(this.writer);
			
			if(this.layers.size() > 0){
				
				this.handler.writeProjectLayers(this.writer);
				
			}
			
			this.handler.writeProperties(this.writer);
			this.handler.endFile(this.writer);
			this.writer.flush();
			this.writer.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		log.info("...Done.");
		
	}

	public void changeWorkingDirectory(String workingDir){
		this.workingDirectory = workingDir;
	}
	
	public Map<String,QGisLayer> getLayers(){
		return this.layers;
	}
	
	public double[] getExtent(){
		return this.extent;
	}
	
	public String getWorkingDir(){
		return this.workingDirectory;
	}
	
	public void setExtent(double[] extent){
		this.extent = extent;
	}
	
	public SRS getSRS(){
		return this.srs;
	}
	
}
