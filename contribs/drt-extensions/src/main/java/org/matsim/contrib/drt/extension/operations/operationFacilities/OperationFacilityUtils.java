package org.matsim.contrib.drt.extension.operations.operationFacilities;

import com.google.common.base.Verify;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.Facility;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author nkuehnel / MOIA
 */
public final class OperationFacilityUtils {

    private OperationFacilityUtils() {}

    public static Map<Id<Link>, List<OperationFacility>> getFacilitiesByLink(OperationFacilities operationFacilities) {
        return operationFacilities.getFacilities().values().stream().collect(Collectors.groupingBy(Facility::getLinkId));
    }

    public static Optional<OperationFacility> getFacilityForLink(OperationFacilities operationFacilities, Id<Link> linkId) {
        List<OperationFacility> facilities = getFacilitiesByLink(operationFacilities).get(linkId);
        if(facilities == null || facilities.isEmpty()) {
            return Optional.empty();
        }
        Verify.verify(facilities.size() == 1, "Ambiguous call: More than one facility found for link.");
        return Optional.of(facilities.getFirst());
    }
}
