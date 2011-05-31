package kid.filter;

import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

public class RegionFilter implements SimpleFeatureFilter{

	private List<SimpleFeature> regions;
	
	public RegionFilter(List<SimpleFeature> regions) {
		super();
		this.regions = regions;
	}

	public boolean judge(SimpleFeature feature) {
		Geometry featureGeo = (Geometry)feature.getDefaultGeometry();
		boolean featureIsInRegions = false;
		for(SimpleFeature f : regions){
			Geometry regionGeo = (Geometry)f.getDefaultGeometry();
			if(featureGeo.within(regionGeo)){
				featureIsInRegions = true;
			}
		}
		return featureIsInRegions;
	}
}
