package playground.sebhoerl.plcpc;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.matsim.core.router.util.LeastCostPathCalculator;

class ParallelLeastCostPathCalculatorWorker extends Thread implements LeastCostPathCalculatorWorker {
    final private BlockingQueue<ParallelLeastCostPathCalculatorTask> pending = new LinkedBlockingQueue<>();
    final private LeastCostPathCalculator router;

    final private Object waitLock = new Object();

    public ParallelLeastCostPathCalculatorWorker(LeastCostPathCalculator router) {
        this.router = router;
    }

    @Override
    public void addTask(ParallelLeastCostPathCalculatorTask task) {
        try {
            pending.put(task);
        } catch (InterruptedException e) {}
    }

    @Override
    public void run() {
        try {
            while (true) {
                ParallelLeastCostPathCalculatorTask task = pending.take(); // blocking call; waits until element is available 
                task.result = router.calcLeastCostPath(task.fromNode, task.toNode, task.time, task.person, task.vehicle);

                synchronized (waitLock) {
                    waitLock.notifyAll();
                }
            }
        } catch (InterruptedException e) {}
    }

    public void terminate() {
        interrupt();
    }

    public void waitForTasksToFinish() {
        try {
            synchronized (waitLock) {
                while(pending.peek() != null) {
                    waitLock.wait();
                }
            }
        } catch (InterruptedException e) { }
    }
}
