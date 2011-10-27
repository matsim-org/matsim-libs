/**
 * 
 */
package kid.filter;

import org.opengis.feature.simple.SimpleFeature;

import java.util.ArrayList;
import java.util.List;

/**
 * @author stefan
 *
 */
public abstract class LogicFeatureFilter implements SimpleFeatureFilter{
	
	protected List<SimpleFeatureFilter> filters = new ArrayList<SimpleFeatureFilter>();
	
	public void addFilter(SimpleFeatureFilter filter){
		filters.add(filter);
	}
	
	public abstract boolean judge(SimpleFeature feature);
}
