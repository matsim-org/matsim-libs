package playground.sergioo.NetworksMatcher.kernel;

import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;

import playground.sergioo.NetworksMatcher.kernel.core.ComposedNetwork;
import playground.sergioo.NetworksMatcher.kernel.core.NetworksStep;
import playground.sergioo.NetworksMatcher.kernel.core.NodesMatching;
import playground.sergioo.NetworksMatcher.kernel.core.Region;

public class EdgeDeletionStep extends NetworksStep {

	
	//Attributes

	private Set<NodesMatching> incidentLinksNodesMatchings;


	//Methods
	
	public EdgeDeletionStep(Region region, Set<NodesMatching> incidentLinksNodesMatchings) {
		super(region);
		this.incidentLinksNodesMatchings = incidentLinksNodesMatchings;
	}

	@Override
	protected ComposedNetwork[] execute() {
		ComposedNetwork[] networks = new ComposedNetwork[] {networkA.clone(), networkB.clone()};
		for(Link link:networks[0].getLinks().values()) {
			byte numFree = 0;
			for(NodesMatching incidentLinksNodesMatching:incidentLinksNodesMatchings) {
				if(!((IncidentLinksNodesMatching) incidentLinksNodesMatching).isSmallABigB()) {
					List<Link> incidentLinks = incidentLinksNodesMatching.getComposedNodeA().getIncidentLinks();
					boolean isIncident = false;
					for(Link incidentLink:incidentLinks)
						if(incidentLink.getId().equals(link.getId()))
							isIncident = true;
					if(isIncident)
						for(Integer index:((IncidentLinksNodesMatching) incidentLinksNodesMatching).getLinksMatchingIndices())
							if(incidentLinks.get(index).getId().equals(link.getId()))
								numFree++;
				}
			}
			if(numFree==2)
				networks[0].removeLink(link.getId());
		}
		for(Link link:networks[1].getLinks().values()) {
			byte numFree = 0;
			for(NodesMatching incidentLinksNodesMatching:incidentLinksNodesMatchings) {
				if(((IncidentLinksNodesMatching) incidentLinksNodesMatching).isSmallABigB()) {
					List<Link> incidentLinks = incidentLinksNodesMatching.getComposedNodeB().getIncidentLinks();
					boolean isIncident = false;
					for(Link incidentLink:incidentLinks)
						if(incidentLink.getId().equals(link.getId()))
							isIncident = true;
					if(isIncident)
						for(Integer index:((IncidentLinksNodesMatching) incidentLinksNodesMatching).getLinksMatchingIndices())
							if(incidentLinks.get(index).getId().equals(link.getId()))
								numFree++;
				}
			}
			if(numFree==2)
				networks[0].removeLink(link.getId());
		}
		return networks;
	}


}
