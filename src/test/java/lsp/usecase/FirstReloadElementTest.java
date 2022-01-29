package lsp.usecase;

import static org.junit.Assert.*;

import lsp.LSPUtils;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import lsp.LogisticsSolutionElement;
import lsp.resources.LSPResource;

public class FirstReloadElementTest {

	private ReloadingPoint point;
	private LogisticsSolutionElement reloadingElement;

	@Before
	public void initialize() {
			UsecaseUtils.ReloadingPointSchedulerBuilder schedulerBuilder =  UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
	        schedulerBuilder.setCapacityNeedFixed(10);
	        schedulerBuilder.setCapacityNeedLinear(1);


		Id<LSPResource> reloadingId = Id.create("ReloadingPoint1", LSPResource.class);
		Id<Link> reloadingLinkId = Id.createLinkId("(4 2) (4 3)");
	        
	        UsecaseUtils.ReloadingPointBuilder reloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance(reloadingId, reloadingLinkId);
	        reloadingPointBuilder.setReloadingScheduler(schedulerBuilder.build());
	        point = reloadingPointBuilder.build();

		Id<LogisticsSolutionElement> elementId = Id.create("FiretReloadElement", LogisticsSolutionElement.class);
			LSPUtils.LogisticsSolutionElementBuilder reloadingElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(elementId);
			reloadingElementBuilder.setResource(point);
			reloadingElement  = reloadingElementBuilder.build();
	}

	@Test
	public void testDistributionElement() {
		assertTrue(reloadingElement.getIncomingShipments()!= null);
		assertTrue(reloadingElement.getIncomingShipments().getShipments() != null);
		assertTrue(reloadingElement.getIncomingShipments().getSortedShipments().isEmpty());
		assertTrue(reloadingElement.getInfos() != null);
		assertTrue(reloadingElement.getInfos().isEmpty());
		assertTrue(reloadingElement.getLogisticsSolution() == null);
		assertTrue(reloadingElement.getNextElement() == null);
		assertTrue(reloadingElement.getOutgoingShipments()!= null);
		assertTrue(reloadingElement.getOutgoingShipments().getShipments() != null);
		assertTrue(reloadingElement.getOutgoingShipments().getSortedShipments().isEmpty());
		assertTrue(reloadingElement.getPreviousElement() == null);
		assertTrue(reloadingElement.getResource() == point);
		assertTrue(reloadingElement.getResource().getClientElements().iterator().next() == reloadingElement);
	}

}
