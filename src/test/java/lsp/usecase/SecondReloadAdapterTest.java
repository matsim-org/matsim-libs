package lsp.usecase;

import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import lsp.LSPCarrierResource;
import lsp.LSPResource;

import static org.junit.Assert.*;


public class SecondReloadAdapterTest {

	private ReloadingPoint reloadingPoint;
	private Id<Link> reloadingLinkId;

	@Before
	public void initialize(){
		UsecaseUtils.ReloadingPointSchedulerBuilder schedulerBuilder =  UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
        schedulerBuilder.setCapacityNeedFixed(10);
        schedulerBuilder.setCapacityNeedLinear(1);

		Id<LSPResource> reloadingId = Id.create("ReloadingPoint2", LSPResource.class);
        reloadingLinkId = Id.createLinkId("(14 2) (14 3)");
        
        UsecaseUtils.ReloadingPointBuilder reloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance(reloadingId, reloadingLinkId);
        reloadingPointBuilder.setReloadingScheduler(schedulerBuilder.build());
        reloadingPoint = reloadingPointBuilder.build();
        
	}

	@Test
	public void reloadingPointTest() {
		assertEquals(10, reloadingPoint.getCapacityNeedFixed(), 0.0);
		assertEquals(1, reloadingPoint.getCapacityNeedLinear(), 0.0);
		assertFalse(LSPCarrierResource.class.isAssignableFrom(reloadingPoint.getClass()));
		assertSame(reloadingPoint.getClassOfResource(), ReloadingPoint.class);
		assertNotNull(reloadingPoint.getClientElements());
		assertTrue(reloadingPoint.getClientElements().isEmpty());
		assertSame(reloadingPoint.getEndLinkId(), reloadingLinkId);
		assertSame(reloadingPoint.getStartLinkId(), reloadingLinkId);
		assertNotNull(reloadingPoint.getEventHandlers());
		assertFalse(reloadingPoint.getEventHandlers().isEmpty());
		assertEquals(1, reloadingPoint.getEventHandlers().size());
		assertNotNull(reloadingPoint.getInfos());
		assertTrue(reloadingPoint.getInfos().isEmpty());
	}

}
