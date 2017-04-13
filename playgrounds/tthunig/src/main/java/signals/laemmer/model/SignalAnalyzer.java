package signals.laemmer.model;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import signals.Analyzable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nkuehnel on 05.04.2017.
 */
public class SignalAnalyzer implements MobsimInitializedListener, MobsimAfterSimStepListener {

    private  Config config;
    private Collection<SignalSystem> systems;
    private Map<Id<SignalSystem>, BufferedWriter> writers;

    @Inject
    public SignalAnalyzer(SignalSystemsManager manager, Config config) {
        this.systems = manager.getSignalSystems().values();
        this.config = config;
        this.writers = new HashMap<>();
    }

    @Override
    public void notifyMobsimInitialized(MobsimInitializedEvent e) {
        for (SignalSystem system : this.systems) {
            SignalController controller = system.getSignalController();
            if (controller instanceof Analyzable && ((Analyzable) controller).analysisEnabled()) {
                try {
                    BufferedWriter writer = Files.newBufferedWriter(Paths.get(this.config.controler().getOutputDirectory() + "/analyzer" + system.getId() + ".csv"));
                    writer.write("s;" + ((Analyzable) controller).getStatFields());
                    this.writers.put(system.getId(), writer);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @Override
    public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {

        for (SignalSystem system : this.systems) {
            SignalController controller = system.getSignalController();
            if (controller instanceof Analyzable && ((Analyzable) controller).analysisEnabled()) {
                try {
                    BufferedWriter writer = writers.get(system.getId());
                    writer.newLine();
                    String out = e.getSimulationTime() + ";" + ((Analyzable) controller).getStepStats(e.getSimulationTime());
                    writer.write(out.replace(".", ","));
                    writer.flush();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
