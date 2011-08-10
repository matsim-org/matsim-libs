package playground.sergioo.NetworksMatcher;
import java.util.Collection;

import org.matsim.api.core.v01.network.Network;


public interface Function {
	public Collection<Relation> execute(Network network1, Network network2);
	public Collection<Relation> execute(Network network1, Network network2, Region region);
}
