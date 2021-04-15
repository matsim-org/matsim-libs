package org.mjanowski.worker;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.WorkerDelegate;
import org.matsim.core.mobsim.qsim.qnetsimengine.AcceptedVehiclesDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.EventDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.MoveVehicleDto;
import org.mjanowski.MySimConfig;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerDelegateImpl implements WorkerDelegate {

    private WorkerMain workerMain;
    private WorkerSim workerSim;
    private Scenario sc;
    private CountDownLatch initialized = new CountDownLatch(1);
    private CountDownLatch movingNodesFinishedLatch;
    private boolean finished = false;
    private AtomicBoolean allFinished = new AtomicBoolean(true);
    private CountDownLatch canStartNextStep;

    @Inject
    public WorkerDelegateImpl(Scenario scenario, Mobsim mobsim) {
        workerSim = (WorkerSim) mobsim;
        MySimConfig mySimConfig = (MySimConfig) scenario.getConfig().getModules().get("mySimConfig");
        this.workerMain = new WorkerMain(mySimConfig, workerSim);
        workerSim.setWorkerDelegate(this);
    }

    @Override
    public void startIteration() {
        workerMain.startIteration();
    }

    @Override
    public void terminateSystem() {
        workerMain.terminateSystem();
    }

    @Override
    public List<AcceptedVehiclesDto> update(Integer workerId, List<MoveVehicleDto> moveVehicleDtos, double timeOfDay) {
        return workerMain.update(workerId, moveVehicleDtos, timeOfDay);
    }

    @Override
    public void accepted(Integer workerId, Map<Id<Node>, Collection<List<AcceptedVehiclesDto>>> accepted) {
        workerMain.accepted(workerId, accepted);
    }

    @Override
    public void initialize() {
        movingNodesFinishedLatch = new CountDownLatch(workerSim.getWorkerNodesIds().size() - 1);
        initialized.countDown();

    }

    @Override
    public void sendFinished() {
        //todo potrzebne tutaj usprawnienie w przypadku gdy długo czekamy i nie ma poruszających się pojazdów?
        finished =  !workerSim.getAgentCounter().isLiving() || workerSim.getStopTime() <= workerSim.getSimTimer().getTimeOfDay();
//        Logger.getRootLogger().info("agents: " + workerSim.getAgentCounter().getLiving());
//        Logger.getRootLogger().info("stop time: " + workerSim.getStopTime());
//        Logger.getRootLogger().info("now: " + workerSim.getSimTimer().getTimeOfDay());
        allFinished = new AtomicBoolean(finished);
        canStartNextStep = new CountDownLatch(workerSim.getWorkerNodesIds().size() - 1);
        workerMain.sendFinished();
    }

    @Override
    public void movingNodesFinished() {
        if (initialized.getCount() == 0)
            movingNodesFinishedLatch.countDown();
        else {
            new Thread(() -> {
                try {
                    initialized.await();
                    movingNodesFinishedLatch.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @Override
    public void readyForNextStep(boolean finished) {
        allFinished.compareAndSet(true, finished);
        canStartNextStep.countDown();
    }

    @Override
    public void waitForUpdates() {
        try {
//            Logger.getRootLogger().info("start waiting for updates");
            movingNodesFinishedLatch.await();
//            Logger.getRootLogger().info("finished waiting for updates");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initializeForNextStep() {
        movingNodesFinishedLatch = new CountDownLatch(workerSim.getWorkerNodesIds().size() - 1);
    }

    @Override
    public void sendReadyForNextStep() {
        workerMain.sendReadyForNextMoving(finished);
    }

    @Override
    public void waitUntilReadyForNextStep() {
        try {
            canStartNextStep.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean shouldFinish() {
        return allFinished.get();
    }

    @Override
    public void sendEvents(List<EventDto> eventDtos) {
        workerMain.sendEvents(eventDtos);
    }

    @Override
    public void sendFinishEventsProcessing() {
        workerMain.sendFinishEventsProcessing();
    }

    @Override
    public void sendAfterMobsim() {
        workerMain.sendAfterMobsim();
    }

}
