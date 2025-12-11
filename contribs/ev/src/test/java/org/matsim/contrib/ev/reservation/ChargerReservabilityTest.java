package org.matsim.contrib.ev.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.ev.reservation.ChargerReservability.ReservableSlot;

public class ChargerReservabilityTest {
    @Test
    public void testSlotNoMerging() {
        List<ReservableSlot> slots = new ArrayList<>(List.of( //
                new ReservableSlot(1000.0, 2000.0), //
                new ReservableSlot(3000.0, 4000.0) //
        ));

        ChargerReservability.mergeSlots(slots);

        assertEquals(2, slots.size());
    }

    @Test
    public void testSlotMerging() {
        List<ReservableSlot> slots = new ArrayList<>(List.of( //
                new ReservableSlot(1000.0, 2000.0), //
                new ReservableSlot(1500.0, 4000.0) //
        ));

        ChargerReservability.mergeSlots(slots);

        assertEquals(1, slots.size());
        assertEquals(1000.0, slots.get(0).startTime());
        assertEquals(4000.0, slots.get(0).endTime());
    }

    @Test
    public void testSlotMergingLater() {
        List<ReservableSlot> slots = new ArrayList<>(List.of( //
                new ReservableSlot(1000.0, 2000.0), //
                new ReservableSlot(1500.0, 4000.0), //
                new ReservableSlot(100.0, 200.0) //
        ));

        ChargerReservability.mergeSlots(slots);

        assertEquals(2, slots.size());
        assertEquals(100.0, slots.get(0).startTime());
        assertEquals(200.0, slots.get(0).endTime());
        assertEquals(1000.0, slots.get(1).startTime());
        assertEquals(4000.0, slots.get(1).endTime());
    }

    @Test
    public void testSlotMergingEmpty() {
        List<ReservableSlot> slots = Collections.emptyList();

        ChargerReservability.mergeSlots(slots);

        assertEquals(0, slots.size());
    }
}
