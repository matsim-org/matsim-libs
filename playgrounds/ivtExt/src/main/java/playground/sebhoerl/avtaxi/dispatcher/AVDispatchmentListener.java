package playground.sebhoerl.avtaxi.dispatcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import playground.sebhoerl.avtaxi.data.AVOperator;

import java.util.Map;

@Singleton
public class AVDispatchmentListener implements MobsimBeforeSimStepListener, MobsimInitializedListener {
    @Inject
    Map<Id<AVOperator>, AVOperator> operators;

    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
        for (AVOperator operator : operators.values()) {
            operator.getDispatcher().onNextTimestep(e.getSimulationTime());
        }
    }

    @Override
    public void notifyMobsimInitialized(MobsimInitializedEvent e) {
        for (AVOperator operator : operators.values()) {
            operator.getDispatcher().reset();
        }
    }
}
