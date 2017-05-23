package org.matsim.contrib.analysis.vsp.qgis.layerTemplates;

import org.apache.log4j.Logger;
import org.matsim.contrib.analysis.vsp.qgis.*;
import org.matsim.contrib.analysis.vsp.qgis.utils.AccessibilityRuleWithFilterCreator;
import org.matsim.contrib.analysis.vsp.qgis.utils.ColorRangeUtils;

import java.awt.*;

/**
 * @author gthunig on 17.05.2017.
 */
public class AccessibilityRuleBasedRenderer extends RuleRenderer{
    public static final Logger log = Logger.getLogger(AccessibilityRuleBasedRenderer.class);

    private String nameOfAccessibilityLayer;
    private double firstBound;
    private double binWidth;

    private String nameOfDensityLayer;
    private double minimumDensity;

    private int ruleSize;
    private int symbolSize;
    private ColorRangeUtils.ColorRange colorRange;

    private Rule[] rules;

    public AccessibilityRuleBasedRenderer(VectorLayer layer, int ruleSize, int symbolSize, ColorRangeUtils.ColorRange colorRange) {
        super(layer.getHeader(),layer);
        this.ruleSize = ruleSize;
        this.symbolSize = symbolSize;
        this.colorRange = colorRange;
        init();
    }

    @Override
    public Rule[] getRules() {
        return rules;
    }

    @Override
    public void init() {
        this.rules = new Rule[ruleSize];

        double sizeMapUnitScale[] = {0,0};
        double colorRangeStep = 1.0 / 8.0;

        QGisPointSymbolLayer level0 = new QGisPointSymbolLayer();
        level0.setId(0);
        level0.setColor(ColorRangeUtils.getColor(colorRange, colorRangeStep*0));
        level0.setColorBorder(new Color(0,0,0,255));
        level0.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
        level0.setSize(symbolSize);
        level0.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
        level0.setSizeMapUnitScale(sizeMapUnitScale);
        level0.setPenStyle(QGisConstants.penstyle.no);
        level0.setLayerTransparency(1);
        this.addSymbolLayer(level0);

        QGisPointSymbolLayer level1 = new QGisPointSymbolLayer();
        level1.setId(1);
        level1.setColor(ColorRangeUtils.getColor(colorRange, colorRangeStep*1));
        level1.setColorBorder(new Color(0,0,0,255));
        level1.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
        level1.setSize(symbolSize);
        level1.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
        level1.setSizeMapUnitScale(sizeMapUnitScale);
        level1.setPenStyle(QGisConstants.penstyle.no);
        level1.setLayerTransparency(1);
        this.addSymbolLayer(level1);

        QGisPointSymbolLayer level2 = new QGisPointSymbolLayer();
        level2.setId(2);
        level2.setColor(ColorRangeUtils.getColor(colorRange, colorRangeStep*2));
        level2.setColorBorder(new Color(0,0,0,255));
        level2.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
        level2.setSize(symbolSize);
        level2.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
        level2.setSizeMapUnitScale(sizeMapUnitScale);
        level2.setPenStyle(QGisConstants.penstyle.no);
        level2.setLayerTransparency(1);
        this.addSymbolLayer(level2);

        QGisPointSymbolLayer level3 = new QGisPointSymbolLayer();
        level3.setId(3);
        level3.setColor(ColorRangeUtils.getColor(colorRange, colorRangeStep*3));
        level3.setColorBorder(new Color(0,0,0,255));
        level3.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
        level3.setSize(symbolSize);
        level3.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
        level3.setSizeMapUnitScale(sizeMapUnitScale);
        level3.setPenStyle(QGisConstants.penstyle.no);
        level3.setLayerTransparency(1);
        this.addSymbolLayer(level3);

        QGisPointSymbolLayer level4 = new QGisPointSymbolLayer();
        level4.setId(4);
        level4.setColor(ColorRangeUtils.getColor(colorRange, colorRangeStep*4));
        level4.setColorBorder(new Color(0,0,0,255));
        level4.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
        level4.setSize(symbolSize);
        level4.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
        level4.setSizeMapUnitScale(sizeMapUnitScale);
        level4.setPenStyle(QGisConstants.penstyle.no);
        level4.setLayerTransparency(1);
        this.addSymbolLayer(level4);

        QGisPointSymbolLayer level5 = new QGisPointSymbolLayer();
        level5.setId(5);
        level5.setColor(ColorRangeUtils.getColor(colorRange, colorRangeStep*5));
        level5.setColorBorder(new Color(0,0,0,255));
        level5.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
        level5.setSize(symbolSize);
        level5.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
        level5.setSizeMapUnitScale(sizeMapUnitScale);
        level5.setPenStyle(QGisConstants.penstyle.no);
        level5.setLayerTransparency(1);
        this.addSymbolLayer(level5);

        QGisPointSymbolLayer level6 = new QGisPointSymbolLayer();
        level6.setId(6);
        level6.setColor(ColorRangeUtils.getColor(colorRange, colorRangeStep*6));
        level6.setColorBorder(new Color(0,0,0,255));
        level6.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
        level6.setSize(symbolSize);
        level6.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
        level6.setSizeMapUnitScale(sizeMapUnitScale);
        level6.setPenStyle(QGisConstants.penstyle.no);
        level6.setLayerTransparency(1);
        this.addSymbolLayer(level6);

        QGisPointSymbolLayer level7 = new QGisPointSymbolLayer();
        level7.setId(7);
        level7.setColor(ColorRangeUtils.getColor(colorRange, colorRangeStep*7));
        level7.setColorBorder(new Color(0,0,0,255));
        level7.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
        level7.setSize(symbolSize);
        level7.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
        level7.setSizeMapUnitScale(sizeMapUnitScale);
        level7.setPenStyle(QGisConstants.penstyle.no);
        level7.setLayerTransparency(1);
        this.addSymbolLayer(level7);

        QGisPointSymbolLayer level8 = new QGisPointSymbolLayer();
        level8.setId(8);
        level8.setColor(ColorRangeUtils.getColor(colorRange, colorRangeStep*8));
        level8.setColorBorder(new Color(0,0,0,255));
        level8.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
        level8.setSize(symbolSize);
        level8.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
        level8.setSizeMapUnitScale(sizeMapUnitScale);
        level8.setPenStyle(QGisConstants.penstyle.no);
        level8.setLayerTransparency(1);
        this.addSymbolLayer(level8);

    }

    public void createRules() {
        createFirstRule();
        createMidRules();
        createLastRule();
    }

    private void createFirstRule() {
        rules[0] = AccessibilityRuleWithFilterCreator.createRuleWithoutLowerBound(nameOfAccessibilityLayer,
                firstBound, nameOfDensityLayer, minimumDensity, 0, String.valueOf(firstBound));
    }

    private void createMidRules() {
        for (int i = 1; i < ruleSize -1; i++) {
            createMidRule(i);
        }
    }

    private void createMidRule(int i) {
        double lowerBound = calcLowerBound(i);
        double upperBound = calcUpperBound(i);
        rules[i] = AccessibilityRuleWithFilterCreator.createRule(nameOfAccessibilityLayer, lowerBound, upperBound,
                nameOfDensityLayer, minimumDensity, i,
                String.valueOf(lowerBound) + " - " + String.valueOf(upperBound));
    }

    private void createLastRule() {
        double lowerBound = calcLowerBound(ruleSize - 1);
        rules[ruleSize -1] = AccessibilityRuleWithFilterCreator.createRuleWithoutUpperBound(nameOfAccessibilityLayer,
                lowerBound, nameOfDensityLayer, minimumDensity,
                ruleSize-1, "> " + String.valueOf(lowerBound));
    }

    private double calcLowerBound(int i) {
        return firstBound + binWidth * (i - 1);
    }

    private double calcUpperBound(int i) {
        return calcLowerBound(i + 1);
    }

    public void setNameOfAccessibilityLayer(String nameOfAccessibilityLayer) {
        this.nameOfAccessibilityLayer = nameOfAccessibilityLayer;
    }

    public void setFirstBound(double firstBound) {
        this.firstBound = firstBound;
    }

    public void setBinWidth(double binWidth) {
        this.binWidth = binWidth;
    }

    public void setNameOfDensityLayer(String nameOfDensityLayer) {
        this.nameOfDensityLayer = nameOfDensityLayer;
    }

    public void setMinimumDensity(double minimumDensity) {
        this.minimumDensity = minimumDensity;
    }
}
