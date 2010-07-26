package org.matsim.core.config.groups;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.utils.misc.Time;

public class CharyparNagelScoringConfigGroupTest {

	@Test
	public void testAddActivityParams() {
		CharyparNagelScoringConfigGroup c = new CharyparNagelScoringConfigGroup();
		Assert.assertNull(c.getActivityParams("type1"));
		Assert.assertEquals(0, c.getActivityParams().size());
		ActivityParams ap = new ActivityParams("type1");
		c.addActivityParams(ap);
		Assert.assertEquals(ap, c.getActivityParams("type1"));
		Assert.assertEquals(1, c.getActivityParams().size());
	}

	@Test
	public void testAddActivityParams_getParams() {
		CharyparNagelScoringConfigGroup c = new CharyparNagelScoringConfigGroup();
		Map<String, String> params = c.getParams();
		Assert.assertNull(params.get("activityTypicalDuration_0"));
		ActivityParams ap = new ActivityParams("type1");
		c.addActivityParams(ap);
		Map<String, String> params2 = c.getParams();
		Assert.assertTrue(params2.size() > params.size());
		Assert.assertEquals(Time.writeTime(Time.UNDEFINED_TIME), params2.get("activityTypicalDuration_0"));
	}
}
