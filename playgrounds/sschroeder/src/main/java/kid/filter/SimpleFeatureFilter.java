package kid.filter;

import org.opengis.feature.simple.SimpleFeature;

public interface SimpleFeatureFilter {
	public boolean judge(SimpleFeature feature);
}
