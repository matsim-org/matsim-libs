package playground.michalm.vrp.data.network;

import org.matsim.api.core.v01.*;


public interface MATSimVertexBuilder
{
    MATSimVertexBuilder setLinkId(Id linkId);


    MATSimVertex build();
}
