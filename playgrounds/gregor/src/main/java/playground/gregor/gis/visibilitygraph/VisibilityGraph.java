package playground.gregor.gis.visibilitygraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.gregor.sim2d_v3.helper.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class VisibilityGraph {

	private final Set<Feature> fts;

	public VisibilityGraph(Set<Feature> featureSet) {
		this.fts = featureSet;
	}

	public void run() {
		List<Coordinate> coords = new ArrayList<Coordinate>();
		for (Feature ft : this.fts) {
			Geometry geo = ft.getDefaultGeometry();
			for (int i = 0; i < geo.getCoordinates().length; i++) {
				coords.add(geo.getCoordinates()[i]);
			}

		}

		GeometryFactory geofac = new GeometryFactory();
		for (int i = 0; i < coords.size()-1; i++) {
			for (int j = i+1; j < coords.size(); j++) {
				Coordinate [] cc = {coords.get(i),coords.get(j)};
				LineString ls = geofac.createLineString(cc);
				if (!crosses(ls)) {
					GisDebugger.addGeometry(ls);
				}
			}
		}
		GisDebugger.dump("/Users/laemmel/devel/sim2dDemoII/raw_input/visibilityGraph.shp");
	}

	private boolean crosses(LineString ls) {
		for (Feature ft : this.fts) {
			Geometry geo = ft.getDefaultGeometry();
			if (ls.crosses(geo)) {
				return true;
			}
		}
		return false;
	}

	public static void main(String [] args) {
		String file = "/Users/laemmel/devel/sim2dDemoII/raw_input/floorplan.shp";
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(file);
		new VisibilityGraph(reader.getFeatureSet()).run();
	}

}
