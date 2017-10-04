// code by jph
package playground.clruch.net;

import ch.ethz.idsc.queuey.view.util.net.ObjectHandler;

public enum SimulationDistribution {
    ;
    // ---

    public static void of(SimulationObject simulationObject) {
        SimulationObjects.sortVehiclesAccordingToIndex(simulationObject);

        new StorageSubscriber().handle(simulationObject);

        if (SimulationServer.INSTANCE.getWaitForClients()) { // <- server is running && wait for clients is set
            if (SimulationClientSet.INSTANCE.isEmpty())
                System.out.println("waiting for connections...");
            // block for connections
            while (SimulationClientSet.INSTANCE.isEmpty())
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }

        for (ObjectHandler objectHandler : SimulationClientSet.INSTANCE)
            objectHandler.handle(simulationObject);
    }

}
