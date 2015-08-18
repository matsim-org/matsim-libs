/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.synpop.sim.data;

import playground.johannes.synpop.data.Attributable;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author johannes
 */
public abstract class CachedElement implements Attributable {

    private Map<Object, Object> cache;

    private final Attributable delegate;

    public CachedElement(Attributable delegate) {
        this.delegate = delegate;
    }

    protected Attributable getDelegate() {
        return  delegate;
    }

    @Override
    public String getAttribute(String key) {
        synchronize(key);
        return delegate.getAttribute(key);
    }

    @Override
    public String setAttribute(String key, String value) {
        invalidateCache(key);
        return delegate.setAttribute(key, value);
    }

    @Override
    public String removeAttribute(String key) {
        invalidateCache(key);
        return delegate.removeAttribute(key);
    }

    @Override
    public Collection<String> keys() {
        return delegate.keys();
    }

    public Object getData(Object key) {
        initCache();
        Object value = cache.get(key);
        if (value == null) value = initObjectValue(key);
        return value;
    }

    public Object setData(Object key, Object value) {
        initCache();
        return cache.put(key, value);
    }

    public Object removeData(Object key) {
        initCache();
        return cache.remove(key);
    }

    private void initCache() {
        if (cache == null) cache = new IdentityHashMap<>(5);
    }

    private Object initObjectValue(Object key) {
        /*
        Check if there is a plain-object-key-pair. If not, this key is "standalone" and is not to be synchronized with
         the plain attribute.
         */
        String plainKey = Converters.getPlainKey(key);
        if (plainKey == null) return null;
        else {
            /*
            Check if the delegate stores a value for this key. If yes, convert it to an object value.
             */
            String plainValue = delegate.getAttribute(plainKey);
            if (plainValue == null) return null;
            else {
                Object value = Converters.toObject(plainKey, plainValue);
                setData(key, value);
                return value;
            }
        }
    }

    private void invalidateCache(String key) {
        /*
        Invalidate the cache if there is a plain-object-key-pair.
         */
        Object objKey = Converters.getObjectKey(key);
        if (objKey != null) removeData(objKey);
    }

    private void synchronize(String key) {
        if(cache != null) {
        /*
        Synchronize the cached data with the plain data, if there is a plain-object-key-pair. Do nothing if there is no
        data for a key, i.e. setting a data value to null does not affect the plain value.
         */
            Object objKey = Converters.getObjectKey(key);
            if (objKey != null) {
                Object value = cache.get(objKey);
                if (value != null) {
                    String plainValue = Converters.toString(objKey, value);
                    delegate.setAttribute(key, plainValue);
                }
            }
        }
    }
}
