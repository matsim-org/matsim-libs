package playground.sebhoerl.plcpc;

public interface LeastCostPathCalculatorWorker extends Runnable {
    void addTask(ParallelLeastCostPathCalculatorTask task);
}
