/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.api.core.v01;

import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import com.google.common.base.Preconditions;

/**
 * @author Michal Maciejewski (michalm)
 */
public class IdCollectors {
	public static <T, K, V> Collector<T, ?, IdMap<K, V>> toIdMap(Class<K> idClass, Function<? super T, ? extends Id<K>> keyMapper,
			Function<? super T, ? extends V> valueMapper) {
		return Collector.of(
				// supplier
				() -> new IdMap<>(idClass),
				// accumulator
				(map, element) -> {
					Id<K> k = keyMapper.apply(element);
					V v = Objects.requireNonNull(valueMapper.apply(element));
					V u = map.putIfAbsent(k, v);
					Preconditions.checkState(u == null, "Duplicate key %s (attempted merging values %s and %s)", k, u, v);
				},
				// combiner
				(m1, m2) -> {
					for (Map.Entry<Id<K>, V> e : m2.entrySet()) {
						Id<K> k = e.getKey();
						V v = Objects.requireNonNull(e.getValue());
						V u = m1.putIfAbsent(k, v);
						Preconditions.checkState(u == null, "Duplicate key %s (attempted merging values %s and %s)", k, u, v);
					}
					return m1;
				},
				// characteristics
				Characteristics.IDENTITY_FINISH);
	}

	public static <T, K, V> Collector<T, ?, IdMap<K, V>> toIdMap(Class<K> idClass, Function<? super T, ? extends Id<K>> keyMapper,
			Function<? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
		return Collector.of(
				// supplier
				() -> new IdMap<>(idClass),
				// accumulator
				(map, element) -> map.merge(keyMapper.apply(element), valueMapper.apply(element), mergeFunction),
				// combiner
				(m1, m2) -> {
					for (Map.Entry<Id<K>, V> e : m2.entrySet())
						m1.merge(e.getKey(), e.getValue(), mergeFunction);
					return m1;
				},
				// characteristics
				Characteristics.IDENTITY_FINISH);
	}

	public static <T> Collector<Id<T>, ?, IdSet<T>> toIdSet(Class<T> idClass) {
		return Collector.of(
				// supplier
				() -> new IdSet<>(idClass),
				// accumulator
				IdSet::add,
				// combiner
				(left, right) -> {
					if (left.size() < right.size()) {
						right.addAll(left);
						return right;
					} else {
						left.addAll(right);
						return left;
					}
				},
				// characteristics
				Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH);
	}
}
