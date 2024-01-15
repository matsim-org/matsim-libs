package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Embed video into dashboard.
 */
public class Video extends Viz {

	/**
	 * Defines if the control buttons are available
	 */
	public Boolean controls;

	/**
	 * Defines if fullscreen is allowed
	 */
	public Boolean allowfullscreen;

	/**
	 * Defines if the video should be played in a loop
	 */
	public Boolean loop;

	/**
	 * Defines if the video should start automatically
	 */
	public Boolean autoplay;

	/**
	 * Sets the mute status
	 */
	public Boolean muted;

	/**
	 * Sets the Video MIME Type and the associated filepath
	 */
	@JsonProperty(required = true)
	public Map<MIMETypes, String> sources = new HashMap<>();

	public Video() {
		super("video");
	}

	/**
	 * Supported mime types for video source files.
	 */
	public enum MIMETypes {

		MP4("video/mp4"),
		MPEG("video/mpeg"),
		WEBM("video/webm");

		private final String t;

		MIMETypes(String type) {
			t = type;
		}

		@Override
		public String toString() {
			return t;
		}
	}
}
