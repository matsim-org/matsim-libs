package org.matsim.contrib.noise;

import org.matsim.api.core.v01.Id;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class NoiseReceiverPoints implements Map<Id<ReceiverPoint>, NoiseReceiverPoint> {

    public static final String NOISE_RECEIVER_POINTS = "noiseReceiverPoints";

    private final Map<Id<ReceiverPoint>, NoiseReceiverPoint> delegate = new HashMap<>();

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public NoiseReceiverPoint get(Object key) {
        return delegate.get(key);
    }

    @Override
    public NoiseReceiverPoint put(Id<ReceiverPoint> key, NoiseReceiverPoint value) {
        return delegate.put(key, value);
    }

    @Override
    public NoiseReceiverPoint remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends Id<ReceiverPoint>, ? extends NoiseReceiverPoint> m) {
        delegate.putAll(m);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<Id<ReceiverPoint>> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<NoiseReceiverPoint> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<Id<ReceiverPoint>, NoiseReceiverPoint>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public NoiseReceiverPoint getOrDefault(Object key, NoiseReceiverPoint defaultValue) {
        return delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super Id<ReceiverPoint>, ? super NoiseReceiverPoint> action) {
        delegate.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super Id<ReceiverPoint>, ? super NoiseReceiverPoint, ? extends NoiseReceiverPoint> function) {
        delegate.replaceAll(function);
    }

    @Override
    public NoiseReceiverPoint putIfAbsent(Id<ReceiverPoint> key, NoiseReceiverPoint value) {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return delegate.remove(key, value);
    }

    @Override
    public boolean replace(Id<ReceiverPoint> key, NoiseReceiverPoint oldValue, NoiseReceiverPoint newValue) {
        return delegate.replace(key, oldValue, newValue);
    }

    @Override
    public NoiseReceiverPoint replace(Id<ReceiverPoint> key, NoiseReceiverPoint value) {
        return delegate.replace(key, value);
    }

    @Override
    public NoiseReceiverPoint computeIfAbsent(Id<ReceiverPoint> key, Function<? super Id<ReceiverPoint>, ? extends NoiseReceiverPoint> mappingFunction) {
        return delegate.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public NoiseReceiverPoint computeIfPresent(Id<ReceiverPoint> key, BiFunction<? super Id<ReceiverPoint>, ? super NoiseReceiverPoint, ? extends NoiseReceiverPoint> remappingFunction) {
        return delegate.computeIfPresent(key, remappingFunction);
    }

    @Override
    public NoiseReceiverPoint compute(Id<ReceiverPoint> key, BiFunction<? super Id<ReceiverPoint>, ? super NoiseReceiverPoint, ? extends NoiseReceiverPoint> remappingFunction) {
        return delegate.compute(key, remappingFunction);
    }

    @Override
    public NoiseReceiverPoint merge(Id<ReceiverPoint> key, NoiseReceiverPoint value, BiFunction<? super NoiseReceiverPoint, ? super NoiseReceiverPoint, ? extends NoiseReceiverPoint> remappingFunction) {
        return delegate.merge(key, value, remappingFunction);
    }
}
