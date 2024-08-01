package org.matsim.utils.objectattributes.attributable;

import com.google.common.base.Equivalence;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.matsim.vehicles.PersonVehicles;

import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public final class AttributesComparison {

    private AttributesComparison(){}

    public static boolean equals(Attributes a1, Attributes a2) {
        if(a1.size() != a2.size()) {
            return false;
        }

        return Maps.difference(a1.getAsMap(), a2.getAsMap(), new CustomEquivalence()).areEqual();
    }

    private static class CustomEquivalence extends Equivalence<Object> {
        @Override
        protected boolean doEquivalent(Object a, Object b) {
            if (a instanceof Map<?, ?> mapA && b instanceof Map<?, ?> mapB) {
                return Maps.difference(mapA, mapB, new CustomEquivalence()).areEqual();
            } else if (a instanceof PersonVehicles vehiclesA && b instanceof PersonVehicles vehiclesB) {
                return Maps.difference(vehiclesA.getModeVehicles(), vehiclesB.getModeVehicles(), new CustomEquivalence()).areEqual();
            }
            return a.equals(b);
        }

        @Override
        protected int doHash(Object o) {
            return o.hashCode();
        }
    }
}
