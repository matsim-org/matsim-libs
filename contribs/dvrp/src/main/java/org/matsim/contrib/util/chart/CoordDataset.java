/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.util.chart;

import java.util.*;

import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.xy.*;
import org.matsim.api.core.v01.Coord;


/**
 * @author michalm
 */
@SuppressWarnings("serial")
public class CoordDataset
    extends AbstractXYDataset
    implements XYDataset
{
    public interface CoordSource
    {
        int getCount();


        Coord getCoord(int item);
    }


    private List<Comparable<String>> seriesKeys;
    private List<CoordSource> seriesList;


    public CoordDataset()
    {
        seriesKeys = new ArrayList<>();
        seriesList = new ArrayList<>();
    }


    @Override
    public int getSeriesCount()
    {
        return seriesList.size();
    }


    @Override
    public Comparable<String> getSeriesKey(int series)
    {
        return seriesKeys.get(series);
    }


    @Override
    public int getItemCount(int series)
    {
        return seriesList.get(series).getCount();
    }


    @Override
    public Number getX(int series, int item)
    {
        return new Double(getXValue(series, item));
    }


    @Override
    public double getXValue(int series, int item)
    {
        return getItem(series, item).getX();
    }


    @Override
    public Number getY(int series, int item)
    {
        return new Double(getYValue(series, item));
    }


    @Override
    public double getYValue(int series, int item)
    {
        return getItem(series, item).getY();
    }


    public String getText(int series, int item)
    {
        return getItem(series, item).toString();
    }


    public Coord getItem(int series, int item)
    {
        return seriesList.get(series).getCoord(item);
    }


    public void addSeries(String seriesKey, CoordSource data)
    {
        if (seriesKey == null) {
            throw new IllegalArgumentException("The 'seriesKey' cannot be null.");
        }

        if (data == null) {
            throw new IllegalArgumentException("The 'data' is null.");
        }

        int seriesIndex = indexOf(seriesKey);

        if (seriesIndex == -1) { // add a new series
            seriesKeys.add(seriesKey);
            seriesList.add(data);
        }
        else { // replace an existing series
            seriesList.set(seriesIndex, data);
        }

        notifyListeners(new DatasetChangeEvent(this, this));
    }


    public void removeSeries(String seriesKey)
    {
        int seriesIndex = indexOf(seriesKey);

        if (seriesIndex >= 0) {
            seriesKeys.remove(seriesIndex);
            seriesList.remove(seriesIndex);

            notifyListeners(new DatasetChangeEvent(this, this));
        }
    }
}
