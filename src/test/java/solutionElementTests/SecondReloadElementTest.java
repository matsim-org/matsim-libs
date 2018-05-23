package solutionElementTests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import lsp.usecase.ReloadingPoint;
import lsp.usecase.ReloadingPointScheduler;
import lsp.LogisticsSolutionElement;
import lsp.LogisticsSolutionElementImpl;
import lsp.resources.Resource;

public class SecondReloadElementTest {

	private  Id<Resource> reloadingId;
	private  Id<Link> reloadingLinkId;
	private Resource point;
	private LogisticsSolutionElement reloadElement;
	
	@Before
	public void initialize() {
		ReloadingPointScheduler.Builder schedulerBuilder =  ReloadingPointScheduler.Builder.newInstance();
        schedulerBuilder.setCapacityNeedFixed(10);
        schedulerBuilder.setCapacityNeedLinear(1);
       
        
        reloadingId = Id.create("ReloadingPoint2", Resource.class);
        reloadingLinkId = Id.createLinkId("(14 2) (14 3)");
        
        ReloadingPoint.Builder reloadingPointBuilder = ReloadingPoint.Builder.newInstance(reloadingId, reloadingLinkId);
        reloadingPointBuilder.setReloadingScheduler(schedulerBuilder.build());
        point = reloadingPointBuilder.build();
        
        Id<LogisticsSolutionElement> elementId = Id.create("SecondReloadElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder reloadingElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(elementId);
		reloadingElementBuilder.setResource(point);
		reloadElement = reloadingElementBuilder.build();
	
	}
	
	@Test
	public void testDistributionElement() {
		assertTrue(reloadElement.getIncomingShipments()!= null);
		assertTrue(reloadElement.getIncomingShipments().getShipments() != null);
		assertTrue(reloadElement.getIncomingShipments().getSortedShipments().isEmpty());
		assertTrue(reloadElement.getInfos() != null);
		assertTrue(reloadElement.getInfos().isEmpty());
		assertTrue(reloadElement.getLogisticsSolution() == null);
		assertTrue(reloadElement.getNextElement() == null);
		assertTrue(reloadElement.getOutgoingShipments()!= null);
		assertTrue(reloadElement.getOutgoingShipments().getShipments() != null);
		assertTrue(reloadElement.getOutgoingShipments().getSortedShipments().isEmpty());
		assertTrue(reloadElement.getPreviousElement() == null);
		assertTrue(reloadElement.getResource() == point);
		assertTrue(reloadElement.getResource().getClientElements().iterator().next() == reloadElement);
	}
}
