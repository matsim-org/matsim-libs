package org.matsim.core.mobsim.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.qnetsimengine.AcceptedVehiclesDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.EventDto;
import org.matsim.core.mobsim.qsim.qnetsimengine.MoveVehicleDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface WorkerDelegate {

    void waitForUpdates();

    void startIteration();

    void terminateSystem();

    List<AcceptedVehiclesDto> update(Integer workerId, List<MoveVehicleDto> v, double timeOfDay);

    void accepted(Integer workerId, Map<Id<Node>, Collection<List<AcceptedVehiclesDto>>> accepted);

    void initialize();

    void sendFinished();

    void movingNodesFinished();

    void readyForNextStep(boolean finished);

    void initializeForNextStep();

    void sendReadyForNextStep();

    void waitUntilReadyForNextStep();

    boolean shouldFinish();

    void sendEvents(List<EventDto> eventDtos);

    void sendFinishEventsProcessing();

    void sendAfterMobsim();
}
