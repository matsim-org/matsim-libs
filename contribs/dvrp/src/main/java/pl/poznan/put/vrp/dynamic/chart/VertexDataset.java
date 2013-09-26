package pl.poznan.put.vrp.dynamic.chart;

import java.util.*;

import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.xy.*;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;


/**
 * @author michalm
 */
@SuppressWarnings("serial")
public class VertexDataset
    extends AbstractXYDataset
    implements XYDataset
{
    private List<Comparable<String>> seriesKeys;
    private List<VertexSource> seriesList;


    public VertexDataset()
    {
        seriesKeys = new ArrayList<Comparable<String>>();
        seriesList = new ArrayList<VertexSource>();
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
        return (getItem(series, item).getId() + 1) + "";
    }


    public Vertex getItem(int series, int item)
    {
        return seriesList.get(series).getVertex(item);
    }


    public void addSeries(String seriesKey, VertexSource data)
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
