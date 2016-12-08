package playground.sebhoerl.plcpc;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Iterator;

public class ParallelLeastCostPathCalculator implements MobsimAfterSimStepListener, IterationEndsListener, ShutdownListener {
    private Logger log = Logger.getLogger(ParallelLeastCostPathCalculator.class);

    private final ArrayList<ParallelLeastCostPathCalculatorWorker> workers;
    private Iterator<ParallelLeastCostPathCalculatorWorker> workerIterator = null;
    private SequentialLeastCostPathCalculatorWorker sequentialWorker = null;

    private long count = 0;

    public ParallelLeastCostPathCalculator(int numberOfThreads, ParallelLeastCostPathCalculatorFactory factory) {
        workers = new ArrayList<>(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            workers.add(new ParallelLeastCostPathCalculatorWorker(factory.createRouter()));
        }

        if (numberOfThreads == 0) {
            sequentialWorker = new SequentialLeastCostPathCalculatorWorker(factory.createRouter());
        }

        for (ParallelLeastCostPathCalculatorWorker worker : workers) {
            worker.start();
        }
    }

    private LeastCostPathCalculatorWorker getNextWorker() {
        if (sequentialWorker != null) {
            return sequentialWorker;
        }

        if (workerIterator == null || !workerIterator.hasNext()) {
            workerIterator = workers.iterator();
        }

        return workerIterator.next();
    }

    public LeastCostPathFuture calcLeastCostPath(Node fromNode, Node toNode, double starttime, final Person person, final Vehicle vehicle) {
        ParallelLeastCostPathCalculatorTask task = new ParallelLeastCostPathCalculatorTask(fromNode, toNode, starttime, person, vehicle);

        LeastCostPathCalculatorWorker worker = getNextWorker();
        worker.addTask(task);

        count++;

        return new LeastCostPathFuture(task);
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        for (ParallelLeastCostPathCalculatorWorker worker : workers) {
            worker.terminate();
        }
    }

    @Override
    public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
        for (ParallelLeastCostPathCalculatorWorker worker : workers) {
            worker.waitForTasksToFinish();
        }

        if (sequentialWorker != null) {
            sequentialWorker.run();
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        String message = String.valueOf(count) + " tasks processed ";

        if (sequentialWorker == null) {
            message += " (parallel, " + workers.size() + " workers)";
        } else {
            message += " (serial)";
        }

        log.info(message);
        count = 0;
    }
}
