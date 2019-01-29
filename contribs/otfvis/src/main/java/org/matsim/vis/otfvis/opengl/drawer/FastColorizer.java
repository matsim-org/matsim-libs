package org.matsim.vis.otfvis.opengl.drawer;

import org.matsim.vis.otfvis.gui.ValueColorizer;

import java.awt.*;

public class FastColorizer {

	private final int grain;
	private Color[] fastValues;
	private double minVal, maxVal, valRange;

	public FastColorizer(double[] ds, Color[] colors) {
		this(ds, colors, 1000);
	}

	private FastColorizer(double[] ds, Color[] colors, int grain) {
		ValueColorizer helper = new ValueColorizer(ds,colors);
		this.grain = grain;
		this.fastValues = new Color[grain];
		this.minVal = ds[0];
		this.maxVal = ds[ds.length-1];
		this.valRange = this.maxVal - this.minVal;
		// calc prerendered Values
		double step = this.valRange/grain;
		for(int i = 0; i< grain; i++) {
			double value = i*step + this.minVal;
			this.fastValues[i] = helper.getColor(value);
		}
	}

	public Color getColor(double value) {
		if (value >= this.maxVal) return this.fastValues[this.grain-1];
		if (value < this.minVal) return this.fastValues[0];
		return this.fastValues[(int)((value-this.minVal)*this.grain/this.valRange)];
	}

	public Color getColorZeroOne( double value ) {
		if ( value >= 1. ) return this.fastValues[this.grain-1] ;
		if ( value <= 0. ) return this.fastValues[0] ;
		return this.fastValues[(int)(value*this.grain)] ;
	}

}
