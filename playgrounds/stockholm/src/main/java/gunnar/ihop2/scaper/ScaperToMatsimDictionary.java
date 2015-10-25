package gunnar.ihop2.scaper;

import gunnar.ihop2.regent.demandreading.RegentPopulationReader;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScaperToMatsimDictionary {
	public static final Map<String, String> scaper2matsim;

	static {
		final Map<String, String> scaper2matsimTmp  = new LinkedHashMap<>();

		scaper2matsimTmp.put(ScaperPopulationReader.MODE_BICYCLE, "bike");
		scaper2matsimTmp.put(ScaperPopulationReader.WORK_ACTIVITY, "work");

		scaper2matsimTmp.put(ScaperPopulationReader.HOME_ACTIVITY, "home");
		scaper2matsimTmp.put(ScaperPopulationReader.SHOPPING_ACTIVITY, "shopping");
		scaper2matsimTmp.put(ScaperPopulationReader.LEISURE_ACTIVITY, "leisure");
		scaper2matsimTmp.put(ScaperPopulationReader.OTHER_ACTIVITY, "other");

		scaper2matsim = Collections.unmodifiableMap(scaper2matsimTmp);
	}
}
