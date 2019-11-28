package org.matsim.osmNetworkReader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OsmNodeToKeep {

    private final boolean isEndNode;
    private final int hierachyLevel;
}
