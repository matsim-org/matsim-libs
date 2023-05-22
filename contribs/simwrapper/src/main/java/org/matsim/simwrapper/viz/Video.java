package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Video extends Viz{

	/**
	 * Defines if the control buttons are available
	 */
	public boolean controls;

	/**
	 * Defines if fullscreen is allowed
	 */
	public boolean allowfullscreen;

	/**
	 * Defines if the video should be played in a loop
	 */
	public boolean loop;

	/**
	 * Defines if the video should start automatically
	 */
	public boolean autoplay;

	/**
	 * Sets the mute status
	 */
	public boolean muted;

	/**
	 * Sets the Video MIME Type and the associated filepath
	 */
	@JsonProperty(required = true)
	public Map<VideoMIMETypes, String> sources;

	public Video() {
		super("video");
	}

}
