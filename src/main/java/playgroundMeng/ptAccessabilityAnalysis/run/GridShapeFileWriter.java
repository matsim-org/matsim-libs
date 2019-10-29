package playgroundMeng.ptAccessabilityAnalysis.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Polygon;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;




public class GridShapeFileWriter {
	Map<String, Polygon> string2PolygonMap = new HashedMap();
	String outputString;
	
	public GridShapeFileWriter(Map<String, Polygon> string2PolygonMap, String outputString) {
		this.outputString = outputString;
		this.string2PolygonMap = string2PolygonMap;
	}
	public void write() {
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("polygon");
		typeBuilder.setCRS(MGC.getCRS("EPSG:25832"));
		typeBuilder.add("the_geom", Polygon.class);
		typeBuilder.add("name",String.class);
		SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		List<SimpleFeature> features = new ArrayList<>();
		
		 for(String string: this.string2PolygonMap.keySet()) {
	        	Object [] attribs = new Object[2];
	        	attribs[0]=this.string2PolygonMap.get(string);
	        	attribs[1]=string;
	        
	            SimpleFeature feature = simpleFeatureBuilder.buildFeature(null, attribs); 
	            features.add(feature);  
	            }
		 String fileString = outputString+"grid"+this.string2PolygonMap.size()+".shp";
         ShapeFileWriter.writeGeometries(features, fileString);
	}
}
