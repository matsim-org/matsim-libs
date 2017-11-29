package playground.sebhoerl.plcpc;

import java.util.ArrayList;
import java.util.Iterator;

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

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.routing.AVParallelRouterFactory;


public class ParallelLeastCostPathCalculator implements MobsimAfterSimStepListener, IterationEndsListener, ShutdownListener {
    private Logger log = Logger.getLogger(ParallelLeastCostPathCalculator.class);

    private final ArrayList<ParallelLeastCostPathCalculatorWorker> workers;
    private Iterator<ParallelLeastCostPathCalculatorWorker> workerIterator = null;
    private SequentialLeastCostPathCalculatorWorker sequentialWorker = null;

    private long count = 0;
    
    @Provides @Singleton @Named(AVModule.AV_MODE)
    private ParallelLeastCostPathCalculator provideParallelLeastCostPathCalculator(AVConfigGroup config, AVParallelRouterFactory factory) {
           return new ParallelLeastCostPathCalculator((int) config.getParallelRouters(), factory);
       }

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
        // throw new RuntimeException("numberOfThreads " + numberOfThreads);
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
