package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.dsim.simulation.SimStepMessaging;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

class ActiveLinksTest {

    @Test
    public void activateLink() {

        var link = mock(SimLink.class);
        when(link.getId()).thenReturn(Id.createLinkId("to"));
        when(link.doSimStep(any(), anyDouble())).thenReturn(false);

        var activeLinks = new ActiveLinks(mock(SimStepMessaging.class));
        activeLinks.setActivateNode(_ -> fail());

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

    @Test
    public void activateNode() {

        var link = mock(SimLink.class);
        when(link.getId()).thenReturn(Id.createLinkId("to"));
        when(link.doSimStep(any(), anyDouble()))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);
        when(link.isOffering())
                .thenReturn(false)
                .thenReturn(true);

        var activeLinks = new ActiveLinks(mock(SimStepMessaging.class));
        var activateNodeCalled = new AtomicInteger(0);
        activeLinks.setActivateNode(nodeId -> {
            assertEquals(link.getToNode(), nodeId);
            activateNodeCalled.incrementAndGet();
        });

        activeLinks.activate(link);

        // keep link active but don't activate node
        activeLinks.doSimStep(0);
        verify(link, times(1)).doSimStep(any(), eq(0.));
        assertEquals(0, activateNodeCalled.get());

        // keep link active and activate node
        activeLinks.doSimStep(1);
        verify(link, times(1)).doSimStep(any(), eq(1.));
        assertEquals(1, activateNodeCalled.get());

        // signal link inactive, but it is offering, so node is activated
        activeLinks.doSimStep(2);
        verify(link, times(1)).doSimStep(any(), eq(2.));
        verify(link, times(3)).isOffering();
        assertEquals(2, activateNodeCalled.get());
    }
}