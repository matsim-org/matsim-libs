// code by jph
package playground.clruch.net;

import java.util.HashMap;
import java.util.Map;

public class IdIntegerDatabase {
    private final Map<String, Integer> map = new HashMap<>();

    public int getId(String string) {
        if (!map.containsKey(string))
            map.put(string, map.size());
        return map.get(string);
    }

	public int size() {
		return map.size();
	}
}
