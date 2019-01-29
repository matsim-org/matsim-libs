package org.matsim.contrib.analysis.vsp.qgis;

import org.matsim.contrib.analysis.vsp.qgis.utils.ColorRangeUtils;

/**
 * @author gthunig on 22.05.2017.
 */
public class RendererFactory {

    public static GraduatedSymbolRenderer createNoiseRenderer(VectorLayer layer, double receiverPointGap) {
        return new GraduatedSymbolRenderer(layer, 45d, 75d, 8, (int) receiverPointGap);
    }

    public static GraduatedSymbolRenderer createDensitiesRenderer(VectorLayer layer, int populationThreshold, int symbolSize) {
        return new GraduatedSymbolRenderer(layer, (double)populationThreshold, (double)populationThreshold, 1,
                symbolSize, ColorRangeUtils.ColorRange.DENSITY);
    }
}
