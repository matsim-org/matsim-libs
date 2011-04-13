//package herbie.freight;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//
//import org.apache.log4j.Logger;
//import org.geotools.feature.AttributeType;
//import org.geotools.feature.AttributeTypeFactory;
//import org.geotools.geometry.DirectPosition2D;
//import org.geotools.geometry.jts.FactoryFinder;
//import org.geotools.feature.Feature;
//import org.geotools.feature.FeatureType;
//import org.geotools.feature.FeatureTypeBuilder;
//import org.geotools.feature.IllegalAttributeException;
//import org.geotools.feature.SchemaException;
//import org.matsim.api.core.v01.Coord;
//import org.matsim.api.core.v01.Id;
//import org.matsim.core.utils.gis.ShapeFileWriter;
//import org.opengis.spatialschema.geometry.geometry.GeometryFactory;
//
//import com.vividsolutions.jts.geom.LineSegment;
//
//public class CSShapeFileWriter {
//	private final static Logger log = Logger.getLogger(CSShapeFileWriter.class);	
//	private FeatureType featureType;
//					
//	public void writeODRelations(String outdir, List<ODRelation> relations) {
//		log.info("Writing freight OD relations");	
//		this.initGeometries();
//		ArrayList<Feature> features = new ArrayList<Feature>();	
//		
//		for (ODRelation relation : relations) {
//			Feature feature = this.createFeature(relation.getOrigin(), relation.getDestination(), relation.getId());
//			features.add(feature);	
//		}
//		try {
//			if (!features.isEmpty()) {
//				ShapeFileWriter.writeGeometries((Collection<Feature>)features, outdir + "heaviestODRelations.shp");
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//			
//	private void initGeometries() {
//		AttributeType [] attr = new AttributeType[2];
//		attr[0] = AttributeTypeFactory.newAttributeType("REL", LineSegment.class);
//		attr[1] = AttributeTypeFactory.newAttributeType("ID", String.class);
//		
//		try {
//			this.featureType = FeatureTypeBuilder.newFeatureType(attr, "rel");
//		} catch (SchemaException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	private Feature createFeature(Coord coordStart, Coord coordEnd, Id id) {
//		GeometryFactory geometryFactory = (GeometryFactory) FactoryFinder.getGeometryFactory(null);
//
//		Feature feature = null;
//		try {
//			feature = this.featureType.create(
//					new Object [] {geometryFactory.createLineSegment(
//							new DirectPosition2D(coordStart.getX(), coordStart.getY()) , 
//							new DirectPosition2D(coordEnd.getX(), coordEnd.getY())), 
//							id.toString()});
//		} catch (IllegalAttributeException e) {
//			e.printStackTrace();
//		}
//		return feature;
//	}
//}
