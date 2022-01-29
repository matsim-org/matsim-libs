package solutionElementTests;

import static org.junit.Assert.*;

import lsp.LSPUtils;
import lsp.usecase.UsecaseUtils;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import lsp.LogisticsSolutionElement;
import lsp.resources.LSPResource;

public class SecondReloadElementTest {

	private LSPResource point;
	private LogisticsSolutionElement reloadElement;
	
	@Before
	public void initialize() {
		UsecaseUtils.ReloadingPointSchedulerBuilder schedulerBuilder =  UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
        schedulerBuilder.setCapacityNeedFixed(10);
        schedulerBuilder.setCapacityNeedLinear(1);


		Id<LSPResource> reloadingId = Id.create("ReloadingPoint2", LSPResource.class);
		Id<Link> reloadingLinkId = Id.createLinkId("(14 2) (14 3)");
        
        UsecaseUtils.ReloadingPointBuilder reloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance(reloadingId, reloadingLinkId);
        reloadingPointBuilder.setReloadingScheduler(schedulerBuilder.build());
        point = reloadingPointBuilder.build();
        
        Id<LogisticsSolutionElement> elementId = Id.create("SecondReloadElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder reloadingElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(elementId );
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
