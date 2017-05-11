package playground.clruch.traveltimetracker;

import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import com.google.inject.Singleton;

@Singleton
public class AVTravelTimeRecorder implements MobsimBeforeSimStepListener {
    


    
    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
        double now = e.getSimulationTime();
        if(now % 10 == 0){
            System.out.println("juhui");
            System.out.println("================================================");
        }
    }
}
