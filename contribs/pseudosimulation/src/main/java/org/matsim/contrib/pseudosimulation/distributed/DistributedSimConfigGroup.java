package org.matsim.contrib.pseudosimulation.distributed;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Created by fouriep on 4/4/17.
 */
public class DistributedSimConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "distributed";

    // parameters and their defaults
    final static String MASTER_PORT_NUMBER = "masterPortNumber";
    private int masterPortNumber = 12345;
    static final String SLAVE_ITERS_PER_MASTER_ITER = "slaveIterationsPerMasterIteration";
    private int slaveIterationsPerMasterIteration = 10;
    static final String DEFAULT_NUM_THREADS_ON_SLAVE = "defaultNumThreadsOnSlave";
    private int defaultNumThreadsOnSlave = 1;
    /**
     * The number of slaves the master will distribute to from the start.
     * Defaults to 2, each with one thread for replanning.
     * You can still add more afterwards and load balancing should take care of it.
     *
     * Override the default number of threads on the slave with a
     */
    static final String INIT_NUM_SLAVES = "initialNumberOfSlaves";
    private int initialNumberOfSlaves = 2;
    /**
     * Overall innovation rate on master.
     */
    static final String MASTER_MUTATION_RATE = "masterMutationRate";
    private double masterMutationRate = 0.1;
    /**
     * Overall rate of innovation on slaves per slave iteration. Total number of plans produced
     * per master iteration across all slaves is slaveMutationRate * slaveIterationsPerMasterIteration.
     */
    static final String SLAVE_MUTATION_RATE = "slaveMutationRate";
    private double slaveMutationRate = 0.3;
    /**
     * The probability that the master will accept a plan from a slave.
     */
    static final String MASTER_BORROWING_RATE = "masterBorrowingRate";
    private double masterBorrowingRate = 0.6667;
    /**
     * if false, randomised routing takes place. Should be modified to
     * allow injection of custom routers...
     */
    static final String INTELLIGENT_ROUTERS = "intelligentRouters";
    private boolean intelligentRouters = true;
    /**
     * If true, a data structure recording each vehicle's arrivals and departures at each stop
     * in the master simulation is transmitted to the slaves, instead of an average StopStopTime
     * that gives stop-to-stop travel time for time bins between each pair of stops.
     */
    static final String FULL_TRANSIT_PERFORMANCE = "fullTransitPerformanceTransmission";
    private boolean fullTransitPerformanceTransmission = true;
    /**
     * If in parallel, the slaves run while the Master is sunning QSim. That means they operate
     * on information that is 2 iterations older than the master's current iteration.
     * If not in parallel, slaves wait or the latest travel times from the master.
     * The master, in turn, waits for all slaves to complete the requisite number of
     * slaveIterationsPerMasterIteration. Slaves then operate on information one iteration older than
     * the master's current iteration, i.e. similar to default MATSim.
     */
    static final String PARALLEL_SIMULATION = "slavesRunInParallelToMaster";
    private boolean slavesRunInParallelToMaster = true;

    public DistributedSimConfigGroup() {
        super(GROUP_NAME);
    }
    @StringGetter(MASTER_PORT_NUMBER)
    public int getMasterPortNumber() {
        return masterPortNumber;
    }
    @StringSetter(MASTER_PORT_NUMBER)
    public void setMasterPortNumber(int masterPortNumber) {
        this.masterPortNumber = masterPortNumber;
    }
    @StringGetter(SLAVE_ITERS_PER_MASTER_ITER)
    public int getSlaveIterationsPerMasterIteration() {
        return slaveIterationsPerMasterIteration;
    }
    @StringSetter(SLAVE_ITERS_PER_MASTER_ITER)
    public void setSlaveIterationsPerMasterIteration(int slaveIterationsPerMasterIteration) {
        this.slaveIterationsPerMasterIteration = slaveIterationsPerMasterIteration;
    }
    @StringGetter(INIT_NUM_SLAVES)
    public int getInitialNumberOfSlaves() {
        return initialNumberOfSlaves;
    }
    @StringSetter(INIT_NUM_SLAVES)
    public void setInitialNumberOfSlaves(int initialNumberOfSlaves) {
        this.initialNumberOfSlaves = initialNumberOfSlaves;
    }
    @StringGetter(MASTER_MUTATION_RATE)
    public double getMasterMutationRate() {
        return masterMutationRate;
    }
    @StringSetter(MASTER_MUTATION_RATE)
    public void setMasterMutationRate(double masterMutationRate) {
        this.masterMutationRate = masterMutationRate;
    }
    @StringGetter(DEFAULT_NUM_THREADS_ON_SLAVE)
    public int getDefaultNumThreadsOnSlave() {
        return defaultNumThreadsOnSlave;
    }
    @StringSetter(DEFAULT_NUM_THREADS_ON_SLAVE)
    public void setDefaultNumThreadsOnSlave(int defaultNumThreadsOnSlave) {
        this.defaultNumThreadsOnSlave = defaultNumThreadsOnSlave;
    }
    @StringGetter(SLAVE_MUTATION_RATE)
    public double getSlaveMutationRate() {
        return slaveMutationRate;
    }
    @StringSetter(SLAVE_MUTATION_RATE)
    public void setSlaveMutationRate(double slaveMutationRate) {
        this.slaveMutationRate = slaveMutationRate;
    }
    @StringGetter(MASTER_BORROWING_RATE)
    public double getMasterBorrowingRate() {
        return masterBorrowingRate;
    }
    @StringSetter(MASTER_BORROWING_RATE)
    public void setMasterBorrowingRate(double masterBorrowingRate) {
        this.masterBorrowingRate = masterBorrowingRate;
    }
    @StringGetter(INTELLIGENT_ROUTERS)
    public boolean isIntelligentRouters() {
        return intelligentRouters;
    }
    @StringSetter(INTELLIGENT_ROUTERS)
    public void setIntelligentRouters(boolean intelligentRouters) {
        this.intelligentRouters = intelligentRouters;
    }
    @StringGetter(FULL_TRANSIT_PERFORMANCE)
    public boolean isFullTransitPerformanceTransmission() {
        return fullTransitPerformanceTransmission;
    }
    @StringSetter(FULL_TRANSIT_PERFORMANCE)
    public void setFullTransitPerformanceTransmission(boolean fullTransitPerformanceTransmission) {
        this.fullTransitPerformanceTransmission = fullTransitPerformanceTransmission;
    }
    @StringGetter(PARALLEL_SIMULATION)
    public boolean isSlavesRunInParallelToMaster() {
        return slavesRunInParallelToMaster;
    }
    @StringSetter(PARALLEL_SIMULATION)
    public void setSlavesRunInParallelToMaster(boolean slavesRunInParallelToMaster) {
        this.slavesRunInParallelToMaster = slavesRunInParallelToMaster;
    }
}
