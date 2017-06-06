// code by jph
package playground.clruch.net;

import java.util.Collections;
import java.util.Comparator;

/**
 * while {@link SimulationObject}
 * does not have any helper/member functions,
 * 
 * {@link SimulationObjects} has utility functions
 */
public enum SimulationObjects {
    ;
    // ---
    public static boolean hasVehicles(SimulationObject simulationObject) {
        return !simulationObject.vehicles.isEmpty();
    }

    private static final Comparator<VehicleContainer> VEHICLEINDEX_COMPARATOR = new Comparator<VehicleContainer>() {
        @Override
        public int compare(VehicleContainer vc1, VehicleContainer vc2) {
            return Integer.compare(vc1.vehicleIndex, vc2.vehicleIndex);
        }
    };

    /**
     * sorts the vehicles in the given simulationObject
     * 
     * @param simulationObject
     */
    public static void sortVehiclesAccordingToIndex(SimulationObject simulationObject) {
        Collections.sort(simulationObject.vehicles, VEHICLEINDEX_COMPARATOR);
    }

}
