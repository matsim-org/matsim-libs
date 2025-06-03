package org.matsim.core.events;

import java.util.Arrays;

import org.matsim.api.core.v01.events.Event;

public class EventArray {
	private Event[] array;
	private int size;

	public EventArray(int capacity) {
		this.array = new Event[capacity];
	}

	public EventArray() {
		this(32);
	}

	public void add(Event element) {
		if (size == array.length) {
			array = Arrays.copyOf(array, array.length + array.length/2);
		}
		array[size] = element;
		size++;
	}

	public void removeLast() {
		array[size - 1] = null;
		size--;
	}

	public int size() {
		return size;
	}
	public Event get(int index) {
		assert index < size : "Index " + index + " out of bounds, for array size " + size + ".";
		assert array[index] != null : "Index " + index + " is null.";
		return array[index];
	}

	public void clear() {
		for (int i = 0; i < size; i++) {
			array[i] = null;
		}
		size = 0;
	}

	public Event[] array() {
		return array;
	}
}
