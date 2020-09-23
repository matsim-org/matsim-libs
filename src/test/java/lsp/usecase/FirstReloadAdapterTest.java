package lsp.usecase;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import lsp.resources.LSPCarrierResource;
import lsp.resources.LSPResource;


public class FirstReloadAdapterTest {

	private  Id<LSPResource> reloadingId;
	private Id<Link> reloadingLinkId;
	private ReloadingPoint reloadingPoint;
	
	@Before
	public void initialize(){
		
        
        UsecaseUtils.ReloadingPointSchedulerBuilder schedulerBuilder =  UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
        schedulerBuilder.setCapacityNeedFixed(10);
        schedulerBuilder.setCapacityNeedLinear(1);

		reloadingId = Id.create("ReloadingPoint1", LSPResource.class);
        reloadingLinkId = Id.createLinkId("(4 2) (4 3)");
        
        UsecaseUtils.ReloadingPointBuilder reloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance(reloadingId, reloadingLinkId);
        reloadingPointBuilder.setReloadingScheduler(schedulerBuilder.build());
        reloadingPoint = reloadingPointBuilder.build();
	}
	
	@Test
	public void reloadingPointTest() {
		assertTrue(reloadingPoint.getCapacityNeedFixed() == 10);
		assertTrue(reloadingPoint.getCapacityNeedLinear() == 1);
		assertFalse(LSPCarrierResource.class.isAssignableFrom(reloadingPoint.getClass()));
		assertTrue(reloadingPoint.getClassOfResource() == ReloadingPoint.class);
		assertTrue(reloadingPoint.getClientElements() != null);
		assertTrue(reloadingPoint.getClientElements().isEmpty());
		assertTrue(reloadingPoint.getEndLinkId() == reloadingLinkId);
		assertTrue(reloadingPoint.getStartLinkId() == reloadingLinkId);
		assertTrue(reloadingPoint.getEventHandlers() != null);
		assertFalse(reloadingPoint.getEventHandlers().isEmpty());
		assertTrue(reloadingPoint.getEventHandlers().size() == 1);
		assertTrue(reloadingPoint.getInfos() != null);
		assertTrue(reloadingPoint.getInfos().isEmpty());
	}
}
