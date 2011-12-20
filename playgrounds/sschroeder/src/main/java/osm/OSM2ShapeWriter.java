package osm;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.osmosis.core.domain.v0_5.EntityType;
import org.openstreetmap.osmosis.core.filter.v0_6.TagFilter;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.XmlReader;

import com.vividsolutions.jts.geom.Polygon;

public class OSM2ShapeWriter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("start");
		
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
//		tagKeyValues.put("name", new HashSet<String>(Arrays.asList("Karlsruhe, Stadt")));
//		tagKeyValues.put("admin_level", new HashSet<String>(Arrays.asList("6")));//,"8","9","10"
//		tagKeyValues.put("boundary", new HashSet<String>(Arrays.asList("administrative")));//,"8","9","10"
		String filename = "/Users/stefan/Documents/Spielwiese/data/osm/ka_stadtteileV2.osm";
		Map<String, Set<String>> emptyKVs = Collections.emptyMap();
		Set<String> emptyKeys = Collections.emptySet();
		
		TagFilter tagFilterWays = new TagFilter("accept-ways", emptyKeys, emptyKVs);
		TagFilter tagFilterRelations = new TagFilter("accept-relations", emptyKeys, tagKeyValues);
		TagFilter tagFilterNodes = new TagFilter("accept-node", emptyKeys, emptyKVs);
		
		XmlReader reader = new XmlReader(new File(filename), true, CompressionMethod.None);
		reader.setSink(tagFilterRelations);
		tagFilterWays.setSink(tagFilterWays);
		tagFilterRelations.setSink(tagFilterNodes);
		
		OSM2Shape osm2Shape = new OSM2Shape();
		osm2Shape.addFeatureColumn("name");
//		osm2Shape.addFeatureColumn("shop");
		osm2Shape.addFeatureColumn("admin_level");
		osm2Shape.addFeatureColumn("boundary");
		osm2Shape.addFeatureColumn("id");
		osm2Shape.setType2Geometry(EntityType.Way, Polygon.class);
		tagFilterNodes.setSink(osm2Shape);
		reader.run();
		osm2Shape.write("/Volumes/parkplatz/Stefan/ka_stadtteileV3.shp");

	}
}
