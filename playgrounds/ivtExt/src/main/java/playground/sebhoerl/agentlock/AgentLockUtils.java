package playground.sebhoerl.agentlock;

import java.util.Collection;
import java.util.Iterator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.utils.misc.Time;

public class AgentLockUtils {
    static public double getMaximumLockTimeFromConfig(Config config) {
        double maximumLockTime = config.qsim().getEndTime();
        
        if (maximumLockTime == Time.UNDEFINED_TIME || maximumLockTime == 0) {
            maximumLockTime = Double.MAX_VALUE;
        }
        
        return maximumLockTime;
    }
    
    static public QSim createQSim(Scenario scenario, EventsManager events, Collection<AbstractQSimPlugin> plugins, LockEngine lockEngine) {
        // Remove default ActivityHandler from the engine
        for (Iterator<AbstractQSimPlugin> iterator = plugins.iterator(); iterator.hasNext();) {
            if (iterator.next() instanceof ActivityEnginePlugin) {
                iterator.remove();
            }
        }
        
        // Instantiate without ActivityHandler
        QSim qsim = QSimUtils.createQSim(scenario, events, plugins);

        // Add LockEngine as a replacement for the ActivityHandler
        qsim.addActivityHandler(lockEngine);
        qsim.addMobsimEngine(lockEngine);
        
        return qsim;
    }
}
