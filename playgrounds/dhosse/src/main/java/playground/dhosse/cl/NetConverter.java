package playground.dhosse.cl;

import java.util.ArrayList;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class NetConverter {
	
	public void convertCoordinates(Network net, String outputFile){
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:3857", "EPSG:32719");
		
		for(Node node : net.getNodes().values()){
			Coord newCoord = ct.transform(node.getCoord());
			((NodeImpl)node).setCoord(newCoord);
		}
		
		new NetworkWriter(net).write(outputFile);
		
	}
	
	public void convertNet2Shape(Network net){
		
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("type", LineString.class);
		typeBuilder.add("id", String.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:3857", "EPSG:32719");
		
		for(Link link : net.getLinks().values()){
		
			Coord from = ct.transform(link.getFromNode().getCoord());
			Coord to = ct.transform(link.getToNode().getCoord());
			
			double fromX = from.getX();
			double fromY = from.getY();
			double toX = to.getX();
			double toY = to.getY();
			
			SimpleFeature feature = builder.buildFeature(null, new Object[]{
				new GeometryFactory().createLineString(new Coordinate[]{
						new Coordinate(fromX, fromY), new Coordinate(toX, toY)
				}),
				link.getId().toString()
			});
			features.add(feature);
			
		}
		
		ShapeFileWriter.writeGeometries(features, "C:/Users/Daniel/Desktop/work/cl/Kai_und_Daniel/net.shp");
		
	}

}
