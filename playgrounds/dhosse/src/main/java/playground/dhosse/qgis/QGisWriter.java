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
	
	private QGisWriterHandler handler;
	
	protected Network network;
	
	protected String nodesFileName;
	protected String linksFileName;
	protected String today;
	
	private QGisConstants.inputType source;
	
	protected Map<String,QGisLayer> layers = new HashMap<String,QGisLayer>();
	
	protected String crs = null;
	
	private double[] extent;
	
	/**
	 * Creates a new instance of a QGis project file writer. Layers for network links and nodes are added automatically.
	 * 
	 * @param network matsim network
	 * @param crs coordinate reference system of the network
	 */
	public QGisWriter(String crs){
		
		this.crs = crs;
		this.handler = new QGisWriterHandler(this);
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		this.today = formatter.format(new Date());
		
	}
	
//	public QGisWriter(final String nodesFile, final String linksFile, final String crs){
//		
//		this.network = null;
//		this.crs = crs != null ? crs : "WGS84";
//		this.source = QGisConstants.inputType.shp;
//		this.linksFileName = linksFile;
//		this.nodesFileName = nodesFile;
//		this.handler = new QGisWriterHandler(this);
//		
//		this.init();
//		
//	}
	
	public void addNetworkLayer(Network network){
		
		this.network = network;
		this.source = QGisConstants.inputType.xml;
		
		this.extent = NetworkUtils.getBoundingBox(this.network.getNodes().values());
		
		QGisLayer nodesLayer = new QGisLayer("nodes", this.nodesFileName, QGisConstants.geometryType.Point);
		nodesLayer.setRenderer(new SingleSymbolRenderer(nodesLayer.getGeometryType()));
		this.layers.put(nodesLayer.getName(),nodesLayer);
		
		QGisLayer linksLayer = new QGisLayer("links", this.linksFileName, QGisConstants.geometryType.Line);
		linksLayer.setRenderer(new SingleSymbolRenderer(linksLayer.getGeometryType()));
		this.layers.put(linksLayer.getName(),linksLayer);
		
		writeLinkAndNodeShapeFiles();
		
	}
	
	@Override
	public void write(String filename) {
		
		log.info("writing QuantumGIS project file (*.qgs) to " + filename + "...");
		
		this.openFile(filename);
		
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

	private void writeLinkAndNodeShapeFiles() {
		
		log.info("Writing link and node shape files...");
		
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("link",LineString.class);
		typeBuilder.add("id",String.class);
		typeBuilder.add("length",Double.class);
		typeBuilder.add("freespeed",Double.class);
		typeBuilder.add("capacity",Double.class);
		typeBuilder.add("nlanes", String.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for(Link link : this.network.getLinks().values()){
			
			SimpleFeature feature = builder.buildFeature(null, new Object[]{
					new GeometryFactory().createLineString(new Coordinate[]{
								new Coordinate(link.getFromNode().getCoord().getX(),link.getFromNode().getCoord().getY()),
								new Coordinate(link.getToNode().getCoord().getX(),link.getToNode().getCoord().getY())
					}),
					link.getId(),
					link.getLength(),
					link.getFreespeed(),
					link.getCapacity(),
					link.getNumberOfLanes()
					
				});
			
			features.add(feature);
			
		}
		
		this.linksFileName = "C:/Users/Daniel/Desktop/MATSimQGisIntegration/links.shp";
		
		ShapeFileWriter.writeGeometries(features, this.linksFileName);
		
		typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("link",Point.class);
		typeBuilder.add("ID",String.class);
		builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		features = new ArrayList<SimpleFeature>();
		
		for(Node node : this.network.getNodes().values()){
			
			SimpleFeature feature = builder.buildFeature(null, new Object[]{
					new GeometryFactory().createPoint(MGC.coord2Coordinate(node.getCoord())),
					node.getId(),
					
				});
			
			features.add(feature);
			
		}
		
		this.nodesFileName = "C:/Users/Daniel/Desktop/MATSimQGisIntegration/nodes.shp";
		
		ShapeFileWriter.writeGeometries(features, this.nodesFileName);
		
	}
	
	public void addLayer(String name, QGisLayer layer, QGisConstants.renderingType renderingType){
		
		this.layers.put(name,layer);
		
	}
	
	/**
	 * 
	 * @return the map of the map layers inside this *.qgs file. for default access on nodes / links use .get("nodes")/.get("link")
	 */
	public Map<String,QGisLayer> getLayers(){
		return this.layers;
	}
	
	public double[] getExtent(){
		return this.extent;
	}
	
}
