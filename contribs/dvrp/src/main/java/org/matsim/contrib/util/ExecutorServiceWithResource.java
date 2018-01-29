/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.util;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.util.concurrent.Futures;

/**
 * @author michalm
 */
public class ExecutorServiceWithResource<R> {
	public interface CallableWithResource<V, R> {
		V call(R resource) throws Exception;
	}

	public interface RunnableWithResource<R> {
		void run(R resource);
	}

	private final BlockingQueue<R> resources = new LinkedBlockingQueue<>();
	private final ExecutorService executorService;

	public ExecutorServiceWithResource(Iterable<R> resources) {
		resources.forEach(this.resources::add);
		executorService = Executors.newFixedThreadPool(this.resources.size());
	}

	public <V> Future<V> submitCallable(CallableWithResource<V, R> task) {
		return executorService.submit(() -> {
			R resource = resources.remove();
			V value = task.call(resource);
			resources.add(resource);
			return value;
		});
	}

	public <V> List<Future<V>> submitCallables(Stream<CallableWithResource<V, R>> tasks) {
		return tasks.map(t -> this.submitCallable(t)).collect(Collectors.toList());
	}

	public <V> List<V> submitCallablesAndGetResults(Stream<CallableWithResource<V, R>> tasks) {
		return submitCallables(tasks).stream().map(Futures::getUnchecked).collect(Collectors.toList());
	}

	public Future<?> submitRunnable(RunnableWithResource<R> task) {
		return executorService.submit(() -> {
			R resource = resources.remove();
			task.run(resource);
			resources.add(resource);
		});
	}

	public List<Future<?>> submitRunnables(Stream<RunnableWithResource<R>> tasks) {
		return tasks.map(t -> this.submitRunnable(t)).collect(Collectors.toList());
	}

	public void submitRunnablesAndWait(Stream<RunnableWithResource<R>> tasks) {
		submitRunnables(tasks).forEach(Futures::getUnchecked);
	}

	public void shutdown() {
		executorService.shutdown();
	}
}
