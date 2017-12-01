package roadTypeMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by molloyj on 01.12.2017.
 * class to mimic the old roadTypeMapping that berlin uses with VISUM
 */
public class VisumHbefaRoadTypeMapping implements HbefaRoadTypeMapping {

    Map<String, String> mapping = new HashMap<>();

    @Override
    public String get(String roadType, double freeVelocity) {
        return mapping.get(roadType);
    }

    public void put(String visumRtNr, String hbefaRtName) {
        mapping.put(visumRtNr, hbefaRtName);
    }

    public static VisumHbefaRoadTypeMapping emptyMapping() {
        return new VisumHbefaRoadTypeMapping();
    }
}
