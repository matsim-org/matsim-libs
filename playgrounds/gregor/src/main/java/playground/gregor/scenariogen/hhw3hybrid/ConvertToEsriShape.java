package playground.gregor.scenariogen.hhw3hybrid;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Crossing;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Goal;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Polygon;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Room;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Subroom;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Transition;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Vertex;

import com.vividsolutions.jts.geom.Coordinate;

public class ConvertToEsriShape {

	private CoordinateReferenceSystem crs;
	private JuPedSimGeomtry geo;
	private String shapeFile;
	private Collection<SimpleFeature> features;

	public ConvertToEsriShape(CoordinateReferenceSystem crs, JuPedSimGeomtry geo, String shapeFile) {
		this.crs = crs;
		this.geo = geo;
		this.shapeFile = shapeFile;
	}
	
	public void run() {
		features = new ArrayList<SimpleFeature>();
		PolylineFeatureFactory ff = new PolylineFeatureFactory.Builder()
		.setName("EvacuationArea")
		.setCrs(this.crs)
		.addAttribute("name", String.class)
		.addAttribute("r_id", String.class)
		.create();
		int ftId = 0;
		for (Room r : this.geo.rooms) {
			for (Subroom sub : r.subrooms) {
				for (Polygon p : sub.polygons) {
					Coordinate[] coordinates = new Coordinate[p.vertices.size()];
					int idx = 0;
					for (Vertex vert : p.vertices) {
						coordinates[idx++] = new Coordinate(vert.px,vert.py);
					}
					SimpleFeature f = ff.createPolyline(coordinates, new Object[]{"room",r.id}, ftId+++"");
					features.add(f);
				}
			}
			for (Crossing c : r.crossings) {
				Coordinate[] coordinates = new Coordinate[]{new Coordinate(c.v1.px,c.v1.py),new Coordinate(c.v2.px,c.v2.py)};
				SimpleFeature f = ff.createPolyline(coordinates, new Object[]{"crossing",c.id}, ftId+++"");
				features.add(f);
			}
		}
		for (Transition c : this.geo.transitions) {
			Coordinate[] coordinates = new Coordinate[]{new Coordinate(c.v1.px,c.v1.py),new Coordinate(c.v2.px,c.v2.py)};
			SimpleFeature f = ff.createPolyline(coordinates, new Object[]{"transition",c.id}, ftId+++"");
			features.add(f);
		}
		for (Goal g : this.geo.goals) {
			Polygon p = g.p;
			Coordinate[] coordinates = new Coordinate[p.vertices.size()];
			int idx = 0;
			for (Vertex vert : p.vertices) {
				coordinates[idx++] = new Coordinate(vert.px,vert.py);
			}
			SimpleFeature f = ff.createPolyline(coordinates, new Object[]{"gool",g.id}, ftId+++"");
			features.add(f);
		}
		
		ShapeFileWriter.writeGeometries(features, shapeFile);

	}
	
}
