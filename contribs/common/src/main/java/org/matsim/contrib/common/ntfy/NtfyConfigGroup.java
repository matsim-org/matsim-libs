package org.matsim.contrib.common.ntfy;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * @author sebhoerl
 */
public class NtfyConfigGroup extends ReflectiveConfigGroup {
    static public final String GROUP_NAME = "ntfy";

    @Parameter
    @Comment("Whether to notify the start of the simulation")
    boolean notifyStartup = true;

    @Parameter
    @Comment("Whether to notify the end of the simulation")
    boolean notifyShutdown = true;

    @Parameter
    @Comment("Whether to notify the start of iterations")
    boolean notifyIterationStart = false;

    @Parameter
    @Comment("Whether to notify the end of iterations")
    boolean notifyIterationEnd = true;

    @Parameter
    @Comment("Defines the interval at which iterations are notified (start or end)")
    @PositiveOrZero
    int notifyIterationInterval = 1;

    @Parameter
    @Comment("An optional name of your simulation for identification")
    @Nullable
    String simulationName = null;

    @Parameter
    @Comment("The ntfy.sh topic to which to send the messages")
    @NotBlank
    String topic;

    public NtfyConfigGroup() {
        super(GROUP_NAME);
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String value) {
        this.topic = value;
    }

    public String getSimulationName() {
        return simulationName;
    }

    public void setSimulationName(String value) {
        this.simulationName = value;
    }

    public boolean getNotifyStartup() {
        return notifyStartup;
    }

    public void setNotifyStartup(boolean value) {
        this.notifyStartup = value;
    }

    public boolean getNotifyShutdown() {
        return notifyShutdown;
    }

    public void setNotifyShutdown(boolean value) {
        this.notifyShutdown = value;
    }

    public boolean getNotifyIterationStart() {
        return notifyIterationStart;
    }

    public void setNotifyIterationStart(boolean value) {
        this.notifyIterationStart = value;
    }

    public boolean getNotifyIterationEnd() {
        return notifyIterationEnd;
    }

    public void setNotifyIterationEnd(boolean value) {
        this.notifyIterationEnd = value;
    }

    public int getNotifyIterationInterval() {
        return notifyIterationInterval;
    }

    public void setNotifyIterationInterval(int value) {
        this.notifyIterationInterval = value;
    }

    static public NtfyConfigGroup get(Config config, boolean create) {
        NtfyConfigGroup group = (NtfyConfigGroup) config.getModules().get(GROUP_NAME);

        if (group == null) {
            group = new NtfyConfigGroup();
            config.addModule(group);
        }

        return group;
    }

    static public NtfyConfigGroup get(Config config) {
        return get(config, false);
    }
}
