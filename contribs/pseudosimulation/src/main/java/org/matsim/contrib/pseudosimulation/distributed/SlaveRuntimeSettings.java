package org.matsim.contrib.pseudosimulation.distributed;

record SlaveRuntimeSettings(int number, int iterationsPerCycle, int numberOfPlans, double mutationRate,
        int lastIteration, boolean initialRouting, boolean quickReplanning, boolean fullTransitPerformance,
        boolean trackGenome, boolean intelligentRouters, boolean diversityGeneratingPlanSelection) {

    static SlaveRuntimeSettings from(SlaveMasterSession.Initialization initialization) {
        return new SlaveRuntimeSettings(initialization.number(), initialization.iterationsPerCycle(),
                initialization.numberOfPlans(), initialization.mutationRate(), initialization.lastIteration(),
                initialization.initialRouting(), initialization.quickReplanning(),
                initialization.fullTransitPerformance(), initialization.trackGenome(),
                initialization.intelligentRouters(), initialization.diversityGeneratingPlanSelection());
    }
}
