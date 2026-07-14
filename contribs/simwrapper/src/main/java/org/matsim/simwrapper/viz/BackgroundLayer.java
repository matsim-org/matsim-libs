package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a background layer configuration for map-based visualizations in SimWrapper.
 * Background layers allow displaying geographic data (shapefiles, GeoJSON, etc.) behind or on top of the main visualization.
 */
public class BackgroundLayer {

	/**
	 * Path to the geometry file (.shp, .gpkg, .geojson) or HTTP(S) URL.
	 */
	@JsonProperty(required = true)
	public String shapes;

	/**
	 * Fill color as Hex (#ff8800), CSS name (salmon), D3 color scheme (Viridis), or "none".
	 * Optional.
	 */
	@JsonProperty(required = false)
	public String fill;

	/**
	 * Transparency from 0.0 (invisible) to 1.0 (fully opaque).
	 * Default: 1.0
	 */
	@JsonProperty(required = false)
	public Double opacity;

	/**
	 * Border width in pixels.
	 * Default: 0
	 */
	@JsonProperty(required = false)
	public Integer borderWidth;

	/**
	 * Border color as Hex or CSS name.
	 * Default: white
	 */
	@JsonProperty(required = false)
	public String borderColor;

	/**
	 * Visibility on start.
	 * Default: true
	 */
	@JsonProperty(required = false)
	public Boolean visible;

	/**
	 * Layer rendering order: true = on top of main data, false = beneath main data.
	 * Default: false
	 */
	@JsonProperty(required = false)
	public Boolean onTop;

	/**
	 * Field name from geometry attributes to use for labels.
	 * Optional.
	 */
	@JsonProperty(required = false)
	public String label;

	/**
	 * Creates a new background layer with the specified shapes file.
	 *
	 * @param shapes Path to the geometry file or HTTP(S) URL
	 */
	public BackgroundLayer(String shapes) {
		this.shapes = shapes;
	}

	/**
	 * Default constructor for Jackson deserialization.
	 */
	public BackgroundLayer() {
	}

	/**
	 * Sets the fill color.
	 *
	 * @param fill Fill color as Hex, CSS name, D3 color scheme, or "none"
	 * @return this layer for method chaining
	 */
	public BackgroundLayer setFill(String fill) {
		this.fill = fill;
		return this;
	}

	/**
	 * Sets the opacity.
	 *
	 * @param opacity Transparency from 0.0 to 1.0
	 * @return this layer for method chaining
	 */
	public BackgroundLayer setOpacity(double opacity) {
		this.opacity = opacity;
		return this;
	}

	/**
	 * Sets the border width.
	 *
	 * @param borderWidth Width in pixels
	 * @return this layer for method chaining
	 */
	public BackgroundLayer setBorderWidth(int borderWidth) {
		this.borderWidth = borderWidth;
		return this;
	}

	/**
	 * Sets the border color.
	 *
	 * @param borderColor Color as Hex or CSS name
	 * @return this layer for method chaining
	 */
	public BackgroundLayer setBorderColor(String borderColor) {
		this.borderColor = borderColor;
		return this;
	}

	/**
	 * Sets the visibility.
	 *
	 * @param visible true if visible on start
	 * @return this layer for method chaining
	 */
	public BackgroundLayer setVisible(boolean visible) {
		this.visible = visible;
		return this;
	}

	/**
	 * Sets whether the layer is rendered on top of the main data.
	 *
	 * @param onTop true to render on top, false to render beneath
	 * @return this layer for method chaining
	 */
	public BackgroundLayer setOnTop(boolean onTop) {
		this.onTop = onTop;
		return this;
	}

	/**
	 * Sets the label field name.
	 *
	 * @param label Field name from geometry attributes
	 * @return this layer for method chaining
	 */
	public BackgroundLayer setLabel(String label) {
		this.label = label;
		return this;
	}
}
