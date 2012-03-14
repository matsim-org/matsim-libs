package playground.gregor.gis.featurefilter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.geotools.feature.Feature;
import org.geotools.feature.IllegalAttributeException;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

public class StringMatchBasedFilter {
	
	public static void main(String [] args) {
		String in = "/Users/laemmel/devel/pt_evac_demo/raw_input/hamburg_points.shp";
		String out = "/Users/laemmel/devel/pt_evac_demo/raw_input/points_filtered.shp";
		
		ShapeFileReader r = new ShapeFileReader();
		r.readFileAndInitialize(in);
		Set<Feature> fts = r.getFeatureSet();
		Iterator<Feature> it = fts.iterator();
		List<Feature> ff = new ArrayList<Feature>();
		while (it.hasNext()) {
			Feature ft = it.next();
			String tags = (String) ft.getAttribute("tags");
			if (tags.contains("shelter") && tags.contains("bus_stop") && !tags.contains("\"shelter\"=\"no\"")) {
				String t2 = tags.toLowerCase();
				try {
					ft.setAttribute("tags","BB");
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
				ff.add(ft);
				System.out.println(tags);
				
			}
		}
		ShapeFileWriter.writeGeometries(ff,out);
	}

}
