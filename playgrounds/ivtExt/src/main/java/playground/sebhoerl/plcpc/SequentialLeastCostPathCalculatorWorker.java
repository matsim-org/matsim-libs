package playground.sebhoerl.plcpc;

import org.matsim.core.router.util.LeastCostPathCalculator;

import java.util.LinkedList;
import java.util.Queue;

public class SequentialLeastCostPathCalculatorWorker implements LeastCostPathCalculatorWorker {
    final private Queue<ParallelLeastCostPathCalculatorTask> pending = new LinkedList<>();
    final private LeastCostPathCalculator router;

    public SequentialLeastCostPathCalculatorWorker(LeastCostPathCalculator router) {
        this.router = router;
    }

    @Override
    public void addTask(ParallelLeastCostPathCalculatorTask task) {
        pending.add(task);
    }

    @Override
    public void run() {
        while (!pending.isEmpty()) {
            ParallelLeastCostPathCalculatorTask task = pending.poll();
            task.result = router.calcLeastCostPath(task.fromNode, task.toNode, task.time, task.person, task.vehicle);
        }
    }
}
