package kid.filter;

import org.opengis.feature.simple.SimpleFeature;

import java.util.ArrayList;
import java.util.List;

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
