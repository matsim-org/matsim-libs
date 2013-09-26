package pl.poznan.put.vrp.dynamic.chart;

import org.jfree.chart.JFreeChart;

import pl.poznan.put.vrp.dynamic.data.VrpData;


public interface ChartCreator
{
    JFreeChart createChart(VrpData data);
}
