package org.matsim.utils;

import org.apache.logging.log4j.LogManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CountedWarning {

	private static final ConcurrentMap<String, AtomicInteger> registeredWarnings = new ConcurrentHashMap<>();

	public static void warn(String name, int maxCount, String message) {
		var count = registeredWarnings.computeIfAbsent(name, _ -> new AtomicInteger()).incrementAndGet();
		if (count <= maxCount) {
			LogManager.getLogger(name).warn(message);
		} else {
			LogManager.getLogger(name).warn("{} \t Further messages are suppressed.", message);
		}
	}
}
