package playground.dhosse.bachelorarbeit;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class MinimumEnvelope {
	
	private Logger log = Logger.getLogger(this.getClass());
	
	private Network net;
	
	private SimpleFeatureBuilder builder;

	public MinimumEnvelope(Network net) {
		
		this.net = net;
		
	}
	
	public static Geometry run(Network net){
		
		MinimumEnvelope me = new MinimumEnvelope(net);
		return(me.createMinimumEnvelope());
		
	}
	
	private Geometry createMinimumEnvelope(){
		
		log.info("creating minimum envelope for given network...");
		
		BoundingBox bbox = BoundingBox.createBoundingBox(this.net);
		
		List<Coord> coordinates = new ArrayList<Coord>();
		List<Id> outerNodes = new ArrayList<Id>();
		
		for(double x = bbox.getXMin(); x<=bbox.getXMax();x+=50){
			//unterer rand
			coordinates.add(new Coord(x, bbox.getYMin()));
			
		}
		
		for(double y = bbox.getYMin(); y<=bbox.getYMax();y+=50){
			coordinates.add(new Coord(bbox.getXMax(), y));
		}
		
		for(double xx = bbox.getXMax();xx>=bbox.getXMin();xx-=50){
			coordinates.add(new Coord(xx, bbox.getYMax()));
		}
		
		for(double yy = bbox.getYMax();yy>=bbox.getYMin();yy-=50){
			coordinates.add(new Coord(bbox.getXMin(), yy));
		}
		
		for(Coord coord : coordinates){
			Id nodeId = ((NetworkImpl) this.net).getNearestNode(coord).getId();
			if(!outerNodes.contains(nodeId)){
				outerNodes.add(nodeId);
			}
		}
		
		log.info("writing minimum envelope into ESRI shapefile...");
		
		Coordinate[] coords = new Coordinate[outerNodes.size()+1];
		int i = 0;
		
		for(Id nodeId : outerNodes){
			coords[i] = new Coordinate(this.net.getNodes().get(nodeId).getCoord().getX(),
					this.net.getNodes().get(nodeId).getCoord().getY());
			i++;
		}
		
		coords[outerNodes.size()] = coords[0];
		
		LinearRing shell = new GeometryFactory().createLinearRing(coords);
		Polygon poly = new GeometryFactory().createPolygon(shell, null);
		
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("envelope",LinearRing.class);
		typeBuilder.add("area",Double.class);
		this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		SimpleFeature feature = this.builder.buildFeature(null, new Object[]{
				poly,
				poly.getArea()
		});
		
		features.add(feature);
		
		ShapeFileWriter.writeGeometries(features, "NetworkInspector.output/envelope.shp");
		
		return (Geometry) poly;
		
	}
	
	

}
