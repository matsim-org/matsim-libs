package kid.filter;

import org.opengis.feature.simple.SimpleFeature;

public class DefaultFeatureFilter implements SimpleFeatureFilter {

	public boolean judge(SimpleFeature feature) {
		return true;
	}

}
