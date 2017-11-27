package playground.lsieber.networkshapecutter;

import org.matsim.utils.objectattributes.ObjectAttributes;

public interface AttributeCutter {
    ObjectAttributes filter(ObjectAttributes objectAttributes);
}
