/**
 * 
 */
package kid.filter;

import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import utils.RegionSchema;


/**
 * @author stefan
 *
 */
public class StuttgartRegionFilter implements SimpleFeatureFilter{

	private List<String> regionNames = new ArrayList<String>();
	
	public StuttgartRegionFilter() {
		init();
	}

	public boolean judge(SimpleFeature feature) {
		String regName = feature.getProperty(RegionSchema.REGION_NAME).getValue().toString();
		if(isInRegionList(regName)){
			return true;
		}
		else{
			return false;
		}
	}
	
	private boolean isInRegionList(String regName) {
		if(regionNames.contains((String)regName)){
			return true;
		}
		else{
			return false;
		}
		
	}

	private void init(){
		regionNames.add("DE111");
		regionNames.add("DE112");
		regionNames.add("DE113");
		regionNames.add("DE115");
		regionNames.add("DE116");
	}

}
