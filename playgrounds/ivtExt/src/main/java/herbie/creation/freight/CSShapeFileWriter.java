package herbie.creation.freight;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter; 
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class CSShapeFileWriter {
	private final static Logger log = Logger.getLogger(CSShapeFileWriter.class);	
					
	public void writeODRelations(String outdir, List<ODRelation> relations) {
		log.info("Writing freight OD relations");			
		ArrayList<Feature> features =  (ArrayList<Feature>) this.generateRelations(relations);	
		log.info("Created " + features.size() + " features");
		try {
			if (!features.isEmpty()) {
				ShapeFileWriter.writeGeometries((Collection<Feature>)features, outdir + "/heaviestODRelations.shp");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Collection<Feature> generateRelations(List<ODRelation> relations) {
		GeometryFactory geoFac = new GeometryFactory();
		Collection<Feature> features = new ArrayList<Feature>();
		CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
		
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("LineString", Geometry.class, true, null, null, crs);
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Double.class);
		FeatureType ftRel;
		try {
			ftRel = FeatureTypeBuilder.newFeatureType(new AttributeType[] {geom, id}, "rel");
			for (ODRelation relation : relations) {
				LineString ls = new LineString(new CoordinateArraySequence(
						new Coordinate [] {MGC.coord2Coordinate(relation.getOrigin()), MGC.coord2Coordinate(relation.getDestination())}), geoFac);
				try {
					Feature ft;
					ft = ftRel.create(new Object [] {ls , relation.getWeight()}, "links");
					features.add(ft);
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
			}
		} catch (SchemaException e) {
			e.printStackTrace();
		}		
		return features;
	}
}
