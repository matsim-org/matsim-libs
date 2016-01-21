/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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
package playground.johannes.synpop.sim;

import org.matsim.api.core.v01.Coord;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.sim.data.CachedElement;
import playground.johannes.synpop.sim.data.CachedSegment;
import playground.johannes.synpop.sim.data.Converters;
import playground.johannes.synpop.sim.data.DoubleConverter;

/**
 * @author jillenberger
 */
public class GeoDistanceUpdater implements AttributeChangeListener {

    private Object facDataKey;

    private final Object geoDistDataKey = Converters.register(CommonKeys.LEG_GEO_DISTANCE, DoubleConverter.getInstance());

    private AttributeChangeListener listener;

    private Predicate<CachedSegment> predicate;

    public GeoDistanceUpdater() {
        this.listener = null;
    }

    public GeoDistanceUpdater(AttributeChangeListener listener) {
        setListener(listener);
    }

    public void setPredicate(Predicate<CachedSegment> predicate) {
        this.predicate = predicate;
    }

    public void setListener(AttributeChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onChange(Object dataKey, Object oldValue, Object newValue, CachedElement element) {
        if (facDataKey == null) facDataKey = Converters.getObjectKey(CommonKeys.ACTIVITY_FACILITY);

        if (facDataKey.equals(dataKey)) {
            CachedSegment act = (CachedSegment) element;
            CachedSegment toLeg = (CachedSegment) act.previous();
            CachedSegment fromLeg = (CachedSegment) act.next();

            if (toLeg != null) {
                CachedSegment prevAct = (CachedSegment) toLeg.previous();
                double d = distance(prevAct, act);
                Object old = toLeg.getData(geoDistDataKey);
                toLeg.setData(geoDistDataKey, d);

                if (listener != null) {
                    if (predicate == null || predicate.test(toLeg))
                        listener.onChange(geoDistDataKey, old, d, toLeg);
                }
            }

            if (fromLeg != null) {
                CachedSegment nextAct = (CachedSegment) fromLeg.next();
                double d = distance(act, nextAct);
                Object old = fromLeg.getData(geoDistDataKey);
                fromLeg.setData(geoDistDataKey, d);

                if (listener != null) {
                    if (predicate == null || predicate.test(fromLeg))
                        listener.onChange(geoDistDataKey, old, d, fromLeg);
                }
            }
        }
    }

    private double distance(CachedSegment from, CachedSegment to) {
        ActivityFacility fac1 = (ActivityFacility) from.getData(facDataKey);
        ActivityFacility fac2 = (ActivityFacility) to.getData(facDataKey);

        Coord c1 = fac1.getCoord();
        Coord c2 = fac2.getCoord();

        double dx = c1.getX() - c2.getX();
        double dy = c1.getY() - c2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
