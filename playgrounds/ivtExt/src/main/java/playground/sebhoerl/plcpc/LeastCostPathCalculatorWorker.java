package playground.sebhoerl.plcpc;

/**
 * interface implemented by {@link ParallelLeastCostPathCalculatorWorker}
 */
public interface LeastCostPathCalculatorWorker extends Runnable {
    void addTask(ParallelLeastCostPathCalculatorTask task);
}
