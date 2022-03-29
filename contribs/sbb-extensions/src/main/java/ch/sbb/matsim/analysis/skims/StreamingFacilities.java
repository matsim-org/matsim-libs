package ch.sbb.matsim.analysis.skims;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.FailingObjectAttributes;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * A special facilities container that does not store facilities, but passes them on to a consumer to process otherwise.
 *
 * @author mrieser / SBB
 */
public class StreamingFacilities implements ActivityFacilities {

    private final Consumer<ActivityFacility> consumer;
    private final ActivityFacilitiesFactory factory = new ActivityFacilitiesFactoryImpl();

    public StreamingFacilities(Consumer<ActivityFacility> consumer) {
        this.consumer = consumer;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public ActivityFacilitiesFactory getFactory() {
        return this.factory;
    }

    @Override
    public Map<Id<ActivityFacility>, ? extends ActivityFacility> getFacilities() {
        return null;
    }

    @Override
    public void addActivityFacility(ActivityFacility facility) {
        this.consumer.accept(facility);
    }

    @Override
    @Deprecated
    public FailingObjectAttributes getFacilityAttributes() {
        return null;
    }

    @Override
    public TreeMap<Id<ActivityFacility>, ActivityFacility> getFacilitiesForActivityType(String actType) {
        return null;
    }

    @Override
    public Attributes getAttributes() {
        return null;
    }
}
