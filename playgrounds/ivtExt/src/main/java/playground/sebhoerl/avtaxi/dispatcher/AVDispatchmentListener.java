package playground.sebhoerl.avtaxi.dispatcher;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import playground.sebhoerl.avtaxi.data.AVOperator;

@Singleton
public class AVDispatchmentListener implements MobsimBeforeSimStepListener {
    @Inject
    Map<Id<AVOperator>, AVDispatcher> dispatchers;

    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
        for (AVDispatcher dispatcher : dispatchers.values()) {
            dispatcher.onNextTimestep(e.getSimulationTime());
        }
    }
}
