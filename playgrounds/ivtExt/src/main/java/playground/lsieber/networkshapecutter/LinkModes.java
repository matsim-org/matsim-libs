package playground.lsieber.networkshapecutter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// TODO String Tokenizer
public class LinkModes {

    public final String ALLMODES = "all";
    private final Set<String> modes;
    public final boolean allModesAllowed;

    public LinkModes(String spaceSeperatedString) {
        modes = new HashSet<String>(Arrays.asList(spaceSeperatedString.split(" ")));
        allModesAllowed = modes.contains(ALLMODES);
    }
    
    public LinkModes(Set<String> modes) {
        this.modes = modes;
        allModesAllowed = modes.contains(ALLMODES);
    }

    public Set<String> getModesSet() {
        return modes;
    }

}
