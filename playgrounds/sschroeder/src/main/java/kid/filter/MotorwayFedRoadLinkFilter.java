package kid.filter;

import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

public class MotorwayFedRoadLinkFilter implements SimpleFeatureFilter{

	private static String TYPE = "DISPLAYT~3"; 
	
	private List<Integer> roadTypes;
	
	public MotorwayFedRoadLinkFilter() {
		init();
	}

	private void init() {
		roadTypes = new ArrayList<Integer>();
		roadTypes.add(0);
		roadTypes.add(1);
		roadTypes.add(2);
		roadTypes.add(3);
	}

	public boolean judge(SimpleFeature feature) {
		Integer roadType = (Integer)feature.getProperty(TYPE).getValue();
		if(roadTypes.contains(roadType)){
			return true;
		}
		else{
			return false;
		}
	}
}
