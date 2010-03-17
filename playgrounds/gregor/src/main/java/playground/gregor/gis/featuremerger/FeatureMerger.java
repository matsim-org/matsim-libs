package playground.gregor.gis.featuremerger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import playground.gregor.MY_STATIC_STUFF;

public class FeatureMerger {
	
	
	private static AttributeType [] attr = null;
	
	public static void main (String [] args) {
		
		String inOld = MY_STATIC_STUFF.SVN_ROOT + "/shared-svn/studies/countries/id/padang/gis/buildings_v20090728/evac_zone_buildings_v20090728.shp";
		String inNew = MY_STATIC_STUFF.SVN_ROOT + "/shared-svn/studies/countries/id/padang/gis/buildings_v20100315/raw_buildings_v20100315.shp";
		String out = MY_STATIC_STUFF.SVN_ROOT + "/shared-svn/studies/countries/id/padang/gis/buildings_v20100315/evac_zone_buildings_v20100315.shp";
		
		Map<Integer,Feature> ftsOld = getOldFeatureMap(inOld);
		
		Iterator it = null;
		try {
			FeatureSource fs = ShapeFileReader.readDataFile(inNew);
			it = fs.getFeatures().iterator();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		AttributeType af = AttributeTypeFactory.newAttributeType("popAf", Integer.class);
		AttributeType [] newAttr = new AttributeType [attr.length+1];
		for (int i = 0; i < attr.length; i++) {
			newAttr[i] = attr[i];
		}
		newAttr[newAttr.length-1] = af;
		FeatureType featureType = null;
		try {
			featureType = FeatureTypeFactory.newFeatureType(newAttr, "building");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		
		Collection<Feature> outColl = new ArrayList<Feature>();
		
		while(it.hasNext()) {
			Feature ft = (Feature)it.next();
			Integer id = (Integer) ft.getAttribute("OBJECTID");
			Integer morning = (Integer) ft.getAttribute("POPMO");
			Integer afternoon = (Integer) ft.getAttribute("POPAF");
			Integer night = (Integer) ft.getAttribute("POPNT");
			Feature oldFt = ftsOld.get(id);
			
			Integer floor = 1;
			Integer space = 0;
			Integer quakeProof = 0;
			Double minWidth = 0.;
			if (oldFt != null) {
				floor = (Integer) oldFt.getAttribute("floor");
				space = (Integer) oldFt.getAttribute("capacity");
				quakeProof = (Integer) oldFt.getAttribute("quakeProof");
				minWidth = (Double) oldFt.getAttribute("minWidth");
			}
			try {
				Feature f = featureType.create(new Object [] {ft.getDefaultGeometry(),id,night,morning,floor,space,quakeProof, minWidth,afternoon});
				outColl.add(f);
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
			
			
		}
		try {
			ShapeFileWriter.writeGeometries(outColl, out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Map<Integer,Feature> getOldFeatureMap(String inOld) {
		Map<Integer,Feature> ret = new HashMap<Integer,Feature>();
		Iterator it = null;
		try {
			FeatureSource fs = ShapeFileReader.readDataFile(inOld);
			it = fs.getFeatures().iterator();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Feature ft = (Feature)it.next();
		Integer i = (Integer) ft.getAttribute("ID");
		attr = ft.getFeatureType().getAttributeTypes();
		ret.put(i, ft);
		
		while(it.hasNext()) {
			ft = (Feature)it.next();
			i = (Integer) ft.getAttribute("ID");
			ret.put(i, ft);
		}
		
		return ret;
	}

}
