package org.matsim.contrib.analysis.vsp.qgis;

/**
 * @author gthunig on 17.05.2017.
 */
public class Rule {

    private String filter;
    private int symbol;
    private String label;

    public Rule(String filter, int symbol, String label){

        this.filter = filter;
        this.symbol = symbol;
        this.label = label;

    }

    public String getFilter() {
        return filter;
    }

    public int getSymbol() {
        return symbol;
    }

    public String getLabel() {
        return label;
    }
}
