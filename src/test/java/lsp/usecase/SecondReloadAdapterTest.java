package lsp.usecase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import lsp.usecase.UsecaseUtils;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import lsp.resources.CarrierResource;
import lsp.resources.Resource;
import lsp.usecase.ReloadingPoint;


public class SecondReloadAdapterTest {

	private ReloadingPoint reloadingPoint;
	private Id<Link> reloadingLinkId;
	private Id<Resource> reloadingId;
	
	@Before
	public void initialize(){
		UsecaseUtils.ReloadingPointSchedulerBuilder schedulerBuilder =  UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
        schedulerBuilder.setCapacityNeedFixed(10);
        schedulerBuilder.setCapacityNeedLinear(1);

		reloadingId = Id.create("ReloadingPoint2", Resource.class);
        reloadingLinkId = Id.createLinkId("(14 2) (14 3)");
        
        UsecaseUtils.ReloadingPointBuilder reloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance(reloadingId, reloadingLinkId);
        reloadingPointBuilder.setReloadingScheduler(schedulerBuilder.build());
        reloadingPoint = reloadingPointBuilder.build();
        
	}

	@Test
	public void reloadingPointTest() {
		assertTrue(reloadingPoint.getCapacityNeedFixed() == 10);
		assertTrue(reloadingPoint.getCapacityNeedLinear() == 1);
		assertFalse(CarrierResource.class.isAssignableFrom(reloadingPoint.getClass()));
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
