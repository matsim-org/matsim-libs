package gunnar.ihop2.regent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import gunnar.ihop2.regent.demandreading.RegentPopulationReader;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RegentDictionary {

	public static final Map<String, String> regent2matsim;
	public static final Map<String, String> matsim2regent;

	static {
		final Map<String, String> regent2matsimTmp = new LinkedHashMap<>();
		final Map<String, String> matsim2regentTmp = new LinkedHashMap<>();

		regent2matsimTmp.put(RegentPopulationReader.CAR_ATTRIBUTEVALUE, "car");
		matsim2regentTmp.put("car", RegentPopulationReader.CAR_ATTRIBUTEVALUE);

		regent2matsimTmp.put(RegentPopulationReader.PT_ATTRIBUTEVALUE, "pt");
		matsim2regentTmp.put("pt", RegentPopulationReader.PT_ATTRIBUTEVALUE);

		regent2matsim = Collections.unmodifiableMap(regent2matsimTmp);
		matsim2regent = Collections.unmodifiableMap(matsim2regentTmp);
	}

	private RegentDictionary() {
	}

}
