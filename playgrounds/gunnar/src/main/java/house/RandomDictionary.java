package house;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RandomDictionary {

	private final List<String> labels;

	private Map<String, String> real2fake = new LinkedHashMap<>();

	public RandomDictionary(final Random rnd) {
		this.labels = new LinkedList<>(asList("Kermit the Frog", "Miss Piggy",
				"Fozzie Bear", "The Great Gonzo", "Scooter", "Animal",
				"Bunsen Honeydew", "Beaker", "The Swedish Chef",
				"Statler and Waldorf", "Lew Zealand"));
		Collections.shuffle(this.labels, rnd);
	}

	public String getFakeName(final String realName) {
		String result = real2fake.get(realName);
		if (result == null) {
			result = labels.remove(0);
			real2fake.put(realName, result);
		}
		return result;
	}

}
