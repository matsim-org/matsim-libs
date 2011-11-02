package playground.gregor.multidestpeds.helper;

import java.util.Iterator;

import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

public class OffenRemover {

	public static void main(String [] args) {
		String in = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/boundaries_closed_transformed.shp";
		String out = "/Users/laemmel/devel/dfg/input/boundaries_closed.shp";
		ShapeFileReader r = new ShapeFileReader();
		r.readFileAndInitialize(in);
		Iterator<Feature> it = r.getFeatureSet().iterator();
		while (it.hasNext()) {
			Feature ft = it.next();
			if (ft.getAttribute("lines").toString().equals("offen")) {
				it.remove();
			}
		}
		ShapeFileWriter.writeGeometries(r.getFeatureSet(), out);
	}
}
