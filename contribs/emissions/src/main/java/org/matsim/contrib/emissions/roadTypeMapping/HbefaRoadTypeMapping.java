package org.matsim.contrib.emissions.roadTypeMapping;

import com.google.inject.ProvidedBy;

/**
 * Created by molloyj on 01.12.2017.
 */
@ProvidedBy(RoadTypeMappingProvider.class)
public interface HbefaRoadTypeMapping {
    String get(String roadType, double freeVelocity);

}
