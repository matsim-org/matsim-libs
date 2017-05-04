package playground.dziemke.analysis.general.srv;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import playground.dziemke.cemdapMatsimCadyts.Zone;

/**
 * @author gthunig on 13.04.2017.
 */
public class ActivityUtils {

    private static final String CA_ZONE_ID = "zoneID";

    public static Id<Zone> getZoneId(Activity activity) {
        return Id.create((String) activity.getAttributes().getAttribute(CA_ZONE_ID), Zone.class);
    }

    static void setZoneId(Activity activity, Id<Zone> zoneId) {
        activity.getAttributes().putAttribute(CA_ZONE_ID, zoneId.toString());
    }
}
