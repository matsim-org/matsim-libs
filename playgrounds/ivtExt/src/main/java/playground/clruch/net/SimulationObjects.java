package playground.clruch.net;

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
}
