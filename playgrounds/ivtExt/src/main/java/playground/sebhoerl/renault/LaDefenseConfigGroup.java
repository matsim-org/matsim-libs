package playground.sebhoerl.renault;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

public class LaDefenseConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "la_defense";

    public static final String NODE_FILTER_INPUT_PATH = "nodeFilterInputPath";

    private String nodeFilterInputPath = null;

    public LaDefenseConfigGroup() {
        super(GROUP_NAME);
    }

    @StringGetter(NODE_FILTER_INPUT_PATH)
    public String getNodeFilterInputPath() {
        return nodeFilterInputPath;
    }

    @StringSetter(NODE_FILTER_INPUT_PATH)
    public void setNodeFilterInputPath(String nodeFilterInputPath) {
        this.nodeFilterInputPath = nodeFilterInputPath;
    }
}
