package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.dsim.simulation.SimStepMessaging;

import static org.mockito.Mockito.*;

class ActiveLinksTest {

	@Test
	public void activateLink() {

		var link = mock(SimLink.class);
		when(link.getId()).thenReturn(Id.createLinkId("to"));
		when(link.doSimStep(any(), anyDouble())).thenReturn(false);

		var activeLinks = new ActiveLinks(mock(SimStepMessaging.class));

		// this should not iterate anything
		activeLinks.doSimStep(0);
		verify(link, times(0)).doSimStep(any(), anyDouble());

		// activate
		activeLinks.activate(link);
		// this should call the link, which we have just activated
		activeLinks.doSimStep(0);
		verify(link, times(1)).doSimStep(any(), anyDouble());

		// our link should not be part of the active links anymore, as TestLink
		// signals false whether the link is still active after moveLink
		activeLinks.doSimStep(0);
		verify(link, times(1)).doSimStep(any(), anyDouble());
	}
}
