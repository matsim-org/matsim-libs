package playground.clruch.traveltimetracker;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AVTravelTimeRecorder implements MobsimBeforeSimStepListener {
    
    @Inject
    AVTravelTimeTracker travelTimeTracker;
    
    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
        double now = e.getSimulationTime();
        if(now % 60 == 0){
            String theLinkToTrack = "9908557_0_rL2";
            Id<Link> linkToTrackID = Id.create(theLinkToTrack,Link.class);
                    

            double travelTime = travelTimeTracker.getLinkTravelTime(linkToTrackID).travelTime;
            
            // System.out.println("juhui");
            // System.out.println(travelTime);
            // System.out.println("================================================");
        }
    }
}
