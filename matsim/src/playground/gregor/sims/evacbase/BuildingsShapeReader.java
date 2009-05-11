package playground.gregor.sims.evacbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.gis.ShapeFileReader;


import com.vividsolutions.jts.geom.Geometry;

public class BuildingsShapeReader {
	
	public static List<Building> readDataFile(String inFile){
		List<Building> ret = new ArrayList<Building>();
		FeatureSource fts;
		try {
			fts = ShapeFileReader.readDataFile(inFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Iterator it;
		try {
			it = fts.getFeatures().iterator();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Geometry geo = ft.getDefaultGeometry();
			Id id = new IdImpl((Integer)ft.getAttribute("ID"));
			int popNight = (Integer) ft.getAttribute("popNight");
			int popDay = (Integer) ft.getAttribute("popDay");
			int floor = (Integer) ft.getAttribute("floor");
			double space = (Double) ft.getAttribute("space");
			int quakeProof = (Integer) ft.getAttribute("quakeProof");
			ret.add(new Building(id,popNight,popDay,floor,space,quakeProof,geo));
		}
		
		return ret;
	}
	
}
