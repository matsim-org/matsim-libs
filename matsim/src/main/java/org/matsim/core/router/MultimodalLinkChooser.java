package org.matsim.core.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.facilities.Facility;

public interface MultimodalLinkChooser {

    public Link decideOnLink( final Facility facility, final Network network );
}
