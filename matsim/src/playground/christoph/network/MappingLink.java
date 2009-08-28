package playground.christoph.network;

import java.util.List;

import org.matsim.api.basic.v01.network.BasicLink;
import org.matsim.api.core.v01.network.Link;

public abstract class MappingLink implements Link, BasicLink {

	public abstract List<Link> getMappedLinks();
	
	public abstract void setMappedLinks(List<Link> links);
}