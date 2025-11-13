package org.matsim.dsim.utils;

import org.apache.logging.log4j.LogManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CountedWarning {

	private static final ConcurrentMap<String, AtomicInteger> registeredWarnings = new ConcurrentHashMap<>();

	public static void warn(String name, int maxCount, String message, Object arg) {
		var count = registeredWarnings.computeIfAbsent(name, _ -> new AtomicInteger()).incrementAndGet();
		if (count < maxCount) {
			LogManager.getLogger(name).warn(message, arg);
		} if (count == maxCount) {
			LogManager.getLogger(name).warn(message + " \t Further messages are suppressed.", arg);
		}
	}
}
