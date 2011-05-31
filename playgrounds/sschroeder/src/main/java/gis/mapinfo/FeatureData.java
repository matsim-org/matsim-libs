package gis.mapinfo;

import java.util.HashMap;
import java.util.Map;

public class FeatureData{
	private Map<String,String> attributes = new HashMap<String, String>();
	
	public FeatureData(){
		
	}
	
	public Map<String, String> getAttributes() {
		return attributes;
	}
}