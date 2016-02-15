package gunnar.ihop2.scaper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScaperToMatsimDictionary {
	public static Map<String, String> scaper2matsim;

	static {
		final Map<String, String> scaper2matsimTmp = new LinkedHashMap<>();

		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_BICYCLE, "bike");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_BUS, "bus");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_TRAM, "tram");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_TRAIN, "train");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_PT, "pt");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_RAIL, "rail");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_WALK, "walk");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_CAR, "car");
		scaper2matsimTmp.put(ScaperPopulationCreator.MODE_SUBWAY, "subway");

		scaper2matsimTmp.put(ScaperPopulationCreator.HOME_ACTIVITY, "home");
		scaper2matsimTmp
				.put(ScaperPopulationCreator.SHOPPING_ACTIVITY, "other"); // "shopping");
		scaper2matsimTmp.put(ScaperPopulationCreator.LEISURE_ACTIVITY, "other"); // "leisure");
		scaper2matsimTmp.put(ScaperPopulationCreator.OTHER_ACTIVITY, "other");
		scaper2matsimTmp.put(ScaperPopulationCreator.WORK_ACTIVITY, "work");
		scaper2matsim = Collections.unmodifiableMap(scaper2matsimTmp);
	}

	private ScaperToMatsimDictionary() {
	}
}
