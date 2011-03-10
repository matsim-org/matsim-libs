package playground.jbischoff.commuterDemand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class MunicipalityShapeReader {
private List<String> filters;
private Map<String,Geometry> shapeMap;
	
	public MunicipalityShapeReader(){
		filters = new ArrayList<String>();
		this.shapeMap = new HashMap<String,Geometry>();
	
	}
	
	public void addFilter(String filter){
		this.filters.add(filter);
	}
	
	public void addFilterRange(int startFilter){
		for (int i = 0;i<1000;i++){
			Integer community = startFilter + i; 
			this.filters.add(community.toString());
		}
		
	}

	@SuppressWarnings("unchecked")
	public void readShapeFile(String filename){
		
		FeatureSource fts;
		try {
			fts = ShapeFileReader.readDataFile(filename);
			Iterator<Feature> it = fts.getFeatures().iterator();
			
			while (it.hasNext()) {
				Feature ft = it.next(); 
				if (this.filters.contains(ft.getAttribute("NR"))){
					GeometryFactory geometryFactory= new GeometryFactory();
					WKTReader wktReader = new WKTReader(geometryFactory);
					Geometry geometry;
					
					try {
						geometry = wktReader.read((ft.getAttribute("the_geom")).toString());
						this.shapeMap.put(ft.getAttribute("NR").toString(),geometry);
						
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				} 
				
		
					}
		
		} catch (IOException e) {
				e.printStackTrace();
		} 
	}

	public Map<String, Geometry> getShapeMap() {
		return shapeMap;
	}
	
}
	
	

