package org.matsim.contrib.ev.reservation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.matsim.api.core.v01.IdMap;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChargerReservability {
    static public String ATTRIBUTE = "wevc:reservable";

    private final IdMap<Charger, List<ReservableSlot>> slots = new IdMap<>(Charger.class);

    private List<ReservableSlot> getSlots(ChargerSpecification charger) {
        List<ReservableSlot> entry = slots.get(charger.getId());

        if (entry == null) {
            entry = new ArrayList<>(getReservableSlots(charger));
            mergeSlots(entry);
            slots.put(charger.getId(), entry);
        }

        return entry;
    }

    static void mergeSlots(List<ReservableSlot> slots) {
        Collections.sort(slots, Comparator.comparing(ReservableSlot::startTime).thenComparing(ReservableSlot::endTime));

        int k = 1;
        while (k < slots.size()) {
            int previousIndex = k - 1;
            int currentIndex = k;

            ReservableSlot previousSlot = slots.get(previousIndex);
            ReservableSlot currentSlot = slots.get(currentIndex);

            if (currentSlot.startTime <= previousSlot.endTime) {
                slots.set(previousIndex, new ReservableSlot(previousSlot.startTime, currentSlot.endTime));
                slots.remove(currentIndex);
                continue;
            }

            k++;
        }
    }

    public boolean isReservable(ChargerSpecification charger, double startTime, double endTime) {
        for (ReservableSlot slot : getSlots(charger)) {
            if (slot.startTime <= startTime && slot.endTime >= endTime) {
                return true;
            }
        }

        return false;
    }

    static public record ReservableSlot(double startTime, double endTime) {
    }

    private final static ObjectMapper objectMapper = new ObjectMapper();

    static public void setReservableSlots(ChargerSpecification charger, Collection<ReservableSlot> slots)
            throws JsonProcessingException {
        String representation = objectMapper.writeValueAsString(slots);
        charger.getAttributes().putAttribute(ATTRIBUTE, representation);
    }

    static public List<ReservableSlot> getReservableSlots(ChargerSpecification charger) {
        try {
            String representation = (String) charger.getAttributes().getAttribute(ATTRIBUTE);

            if (representation == null) {
                return Collections.emptyList();
            }

            return objectMapper.readValue(representation, new TypeReference<List<ReservableSlot>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    static public void setReservableSlot(ChargerSpecification charger, ReservableSlot slot) {
        try {
            setReservableSlots(charger, Collections.singleton(slot));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    static public void setReservable(ChargerSpecification charger, boolean reservable) {
        if (reservable) {
            setReservableSlot(charger, new ReservableSlot(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        } else {
            charger.getAttributes().removeAttribute(ATTRIBUTE);
        }
    }
}
