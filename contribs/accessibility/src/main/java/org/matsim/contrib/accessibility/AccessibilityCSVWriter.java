package org.matsim.contrib.accessibility;

import org.matsim.api.core.v01.network.Node;
import org.matsim.facilities.ActivityFacility;

import java.util.Map;

/**
 * @author thibautd
 */
public interface AccessibilityCSVWriter {
    void writeRecord( ActivityFacility measurePoint, Node fromNode, Map<Modes4Accessibility,Double> accessibilities );
}
