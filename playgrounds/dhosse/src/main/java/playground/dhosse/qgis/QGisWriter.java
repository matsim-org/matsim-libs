package playground.dhosse.qgis;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.AbstractMatsimWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.dhosse.qgis.layerTemplates.networkSimple.SingleSymbolRenderer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

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
	
	private void setCrs(String crs){
		
		if(crs.equals(TransformationFactory.DHDN_GK4)){
			this.srs = new SRS("+proj=tmerc +lat_0=0 +lon_0=12 +k=1 +x_0=4500000 +y_0=0 +ellps=bessel +towgs84=598.1,73.7,418.2,0.202,0.045,-2.455,6.7 +units=m +no_defs",
					"2648", "31468", "EPSG:31468", "DHDN / Gauss-Kruger zone 4", "tmerc", "bessel");
		} else if(crs.equals(TransformationFactory.WGS84)){
			this.srs = new SRS("+proj=longlat +datum=WGS84 +no_defs",
					"3452", "4326", "EPSG:4326", "WGS 84", "longlat", "WGS84");
		}
		
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
			this.handler.writeLegend(this.writer);
			
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
	
	public static class SRS{
		
		private String proj4;
		private String srsid;
		private String srid;
		private String authid;
		private String description;
		private String projectionacronym;
		private String ellipsoidacronym;

		public SRS(String proj4, String srsid, String srid, String authid, String description, String projectionacronym, String ellipsoidacronym){
			this.proj4 = proj4;
			this.srsid = srsid;
			this.srid = srid;
			this.authid = authid;
			this.description = description;
			this.projectionacronym = projectionacronym;
			this.ellipsoidacronym = ellipsoidacronym;
		}
		
		public String getProj4() {
			return proj4;
		}

		public String getSrsid() {
			return srsid;
		}

		public String getSrid() {
			return srid;
		}

		public String getAuthid() {
			return authid;
		}

		public String getDescription() {
			return description;
		}

		public String getProjectionacronym() {
			return projectionacronym;
		}

		public String getEllipsoidacronym() {
			return ellipsoidacronym;
		}
		
	}
	
}
