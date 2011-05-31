package gis.mapinfo;

import java.util.ArrayList;
import java.util.List;


public abstract class FeatureGeo{
	private List<Node> nodes = new ArrayList<Node>();
	
	public abstract String getTYPE();
	
	public List<Node> getNodes(){
		return nodes;
	}
}