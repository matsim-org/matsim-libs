package playground.michalm.vrp.data.network;

import org.matsim.api.core.v01.Id;


public interface MATSimVertexBuilder
{
    MATSimVertexBuilder setLinkId(Id linkId);


    MATSimVertex build();
}
