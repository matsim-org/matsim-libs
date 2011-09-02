package playground.sergioo.NetworksMatcher.kernel;

import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;

import playground.sergioo.NetworksMatcher.kernel.core.MatchingComposedNetwork;
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
	protected MatchingComposedNetwork[] execute() {
		MatchingComposedNetwork[] networks = new MatchingComposedNetwork[] {networkA.clone(), networkB.clone()};
		for(Link link:networkA.getLinks().values()) {
			int numFree = 0;
			for(NodesMatching incidentLinksNodesMatching:incidentLinksNodesMatchings) {
				if(!((IncidentLinksNodesMatching) incidentLinksNodesMatching).isSmallABigB()) {
					List<Link> incidentLinks = incidentLinksNodesMatching.getComposedNodeA().getIncidentLinks();
					boolean isIncident = false;
					for(Link incidentLink:incidentLinks)
						if(incidentLink.getId().equals(link.getId()))
							isIncident = true;
					if(isIncident) {
						numFree++;
						for(Integer index:((IncidentLinksNodesMatching) incidentLinksNodesMatching).getLinksMatchingIndices())
							if(incidentLinks.get(index).getId().equals(link.getId()))
								numFree--;
					}
				}
			}
			if(numFree>=2)
				networks[0].removeLink(link.getId());
		}
		for(Link link:networkB.getLinks().values()) {
			byte numFree = 0;
			for(NodesMatching incidentLinksNodesMatching:incidentLinksNodesMatchings) {
				if(((IncidentLinksNodesMatching) incidentLinksNodesMatching).isSmallABigB()) {
					List<Link> incidentLinks = incidentLinksNodesMatching.getComposedNodeB().getIncidentLinks();
					boolean isIncident = false;
					for(Link incidentLink:incidentLinks)
						if(incidentLink.getId().equals(link.getId()))
							isIncident = true;
					if(isIncident) {
						numFree++;
						for(Integer index:((IncidentLinksNodesMatching) incidentLinksNodesMatching).getLinksMatchingIndices())
							if(incidentLinks.get(index).getId().equals(link.getId()))
								numFree--;
					}
				}
			}
			if(numFree==2)
				networks[0].removeLink(link.getId());
		}
		return networks;
	}


}
