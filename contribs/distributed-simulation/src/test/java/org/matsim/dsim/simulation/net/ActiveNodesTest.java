package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.dsim.DSimConfigGroup;
import org.matsim.dsim.TestUtils;
import org.matsim.dsim.simulation.SimpleAgent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActiveNodesTest {

	@Test
	public void doSimStepOfferingLinksOnly() {

		var driver = mock(SimpleAgent.class);
		when(driver.getCurrentLinkId()).thenReturn(Id.createLinkId("next-link"));
		when(driver.chooseNextLinkId()).thenReturn(Id.createLinkId("next-link"));

		var vehicle = TestUtils.createVehicle("vehicle", driver, 1, 100);
		var link = TestUtils.createSingleLink(0, 0);
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		config.setStuckTime(1000);
		var offeringLink = TestUtils.createLink(link, config, 0);
		offeringLink.pushVehicle(vehicle, SimLink.LinkPosition.QStart, 0);
		offeringLink.doSimStep(null, 100.);
		offeringLink = spy(offeringLink);
		SimLink emptyInLink = mock(SimLink.class);
		when(emptyInLink.isOffering()).thenReturn(false);
		SimLink nextLink = mock(SimLink.class);
		when(nextLink.isAccepting(any(), anyDouble())).thenReturn(true);
		var nextLinkId = Id.createLinkId("next-link");
		when(nextLink.getId()).thenReturn(nextLinkId);
		var node = new SimNode(Id.createNodeId("test"));
		node.addInLink(offeringLink);
		node.addInLink(emptyInLink);
		node.addOutLink(nextLink);

		//var node = new SimNode(Id.createNodeId("test"), List.of(offeringLink, emptyInLink), Map.of(nextLinkId, nextLink));
		var activeNodes = new ActiveNodes(mock(EventsManager.class));
		//activeNodes.setActivateLink(a -> assertEquals(nextLinkId, a));
		activeNodes.activate(node);

		activeNodes.doSimStep(0);

		verify(nextLink, times(1)).pushVehicle(any(), any(), anyDouble());

		// verify if peek method of link was called. This relies on the internal functioning of the
		// method, but I think it is quite simple to test it this way
		verify(offeringLink, times(1)).peekFirstVehicle();
		verify(emptyInLink, times(3)).isOffering();

	}

	@Test
	public void doSimStepStorageCapacity() {

		var driver = mock(SimpleAgent.class);
		when(driver.chooseNextLinkId()).thenReturn(Id.createLinkId("next-link"));

		var vehicle = TestUtils.createVehicle("vehicle", driver, 1, 100);
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		config.setStuckTime(1000);
		var inLink = TestUtils.createLink(TestUtils.createSingleLink(0, 0), config, 0);
		inLink.pushVehicle(vehicle, SimLink.LinkPosition.QStart, 0);
		var nextLink = mock(SimLink.class);
		when(nextLink.isAccepting(any(), anyDouble())).thenReturn(false);
		when(nextLink.getId()).thenReturn(Id.createLinkId("next-link"));

		var node = new SimNode(Id.createNodeId("test"));
		node.addInLink(inLink);
		node.addOutLink(nextLink);
		var activeNodes = new ActiveNodes(mock(EventsManager.class));
		activeNodes.activate(node);

		// move the vehicle into the buffer
		inLink.doSimStep(null, 100);
		// attempt to move the vehicle which should not happen, as next link has no space
		activeNodes.doSimStep(100);
		// link should still be active as the vehicle has not left.
		assertTrue(inLink.isOffering());

		// do it again, but this time the next link has space
		when(nextLink.isAccepting(any(), anyDouble())).thenReturn(true);
		activeNodes.doSimStep(100);
		// in link should be inactive as no vehicle is on the link anymore
		assertFalse(inLink.isOffering());
	}

	@Test
	public void doSimStepFlowCapacity() {
		SimpleAgent driver = mock(SimpleAgent.class);
		when(driver.chooseNextLinkId()).thenReturn(Id.createLinkId("next-link"));

		var link = TestUtils.createSingleLink(0, 0);
		link.setCapacity(1800); // the link can release one vehicle every two seconds
		link.setLength(100);
		link.setFreespeed(200);
		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		config.setStuckTime(1000);
		var inLink = TestUtils.createLink(link, config, 0);
		for (var i = 0; i < 100; i++) {
			inLink.pushVehicle(TestUtils.createVehicle(String.valueOf(i), driver, 1, 200), SimLink.LinkPosition.QStart, 0);
		}
		// call dostimstep here, so that the first vehicle is moved to the buffer
		inLink.doSimStep(null, 99);
		var nextLink = mock(SimLink.class);
		when(nextLink.isAccepting(any(), anyDouble())).thenReturn(true);
		when(nextLink.getId()).thenReturn(Id.createLinkId("next-link"));
		var node = new SimNode(Id.createNodeId("test"));
		node.addInLink(inLink);
		node.addOutLink(nextLink);
		var activeNodes = new ActiveNodes(mock(EventsManager.class));
		activeNodes.activate(node);

		// make sure that one vehicle is released every other second.
		for (var i = 0; i < 200; i++) {
			var now = i + 100;
			activeNodes.doSimStep(now);
			inLink.doSimStep(null, now);
			verify(nextLink, times((i / 2) + 1)).pushVehicle(any(), any(), anyDouble());
			activeNodes.activate(node);
		}
	}

	@Test
	public void doSimStepStuckTime() {
		final var stuckThreshold = 42;

		var driver = mock(SimpleAgent.class);
		when(driver.chooseNextLinkId()).thenReturn(Id.createLinkId("next-link"));

		var vehicle = TestUtils.createVehicle("vehicle", driver, 1, 100);

		var config = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), DSimConfigGroup.class);
		config.setStuckTime(stuckThreshold);
		var inLink = TestUtils.createLink(TestUtils.createSingleLink(0, 0), config, 0);
		inLink.pushVehicle(vehicle, SimLink.LinkPosition.QStart, 0);
		inLink.doSimStep(null, 100); // move vehicle into the buffer, which starts the stuck timer

		var nextLink = mock(SimLink.class);
		when(nextLink.isAccepting(any(), anyDouble())).thenReturn(false);
		when(nextLink.getId()).thenReturn(Id.createLinkId("next-link"));

		var node = new SimNode(Id.createNodeId("test"));
		node.addInLink(inLink);
		node.addOutLink(nextLink);

		var activeNodes = new ActiveNodes(mock(EventsManager.class));
		activeNodes.activate(node);

		for (var now = 100; now < 100 + stuckThreshold; now++) {
			activeNodes.doSimStep(now);
			verify(nextLink, never()).pushVehicle(any(), any(), anyDouble());
		}

		activeNodes.doSimStep(100 + stuckThreshold);
		verify(nextLink, times(1)).pushVehicle(any(), any(), anyDouble());
	}

	@Test
	public void doSimStepSignalActive() {

		var inLink = mock(SimLink.class);
		when(inLink.isOffering()).thenReturn(false);

		var node = new SimNode(Id.createNodeId("test"));
		node.addInLink(inLink);
		var activeNodes = new ActiveNodes(mock(EventsManager.class));

		activeNodes.activate(node);
		assertEquals(1, activeNodes.size());

		activeNodes.doSimStep(0);
		assertEquals(0, activeNodes.size());
	}
}
