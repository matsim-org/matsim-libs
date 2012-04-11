package playground.michalm.vrp.data.network;

import org.matsim.api.core.v01.Id;


public interface MatsimVertexBuilder
{
    MatsimVertexBuilder setLinkId(Id linkId);


    MatsimVertex build();
}
