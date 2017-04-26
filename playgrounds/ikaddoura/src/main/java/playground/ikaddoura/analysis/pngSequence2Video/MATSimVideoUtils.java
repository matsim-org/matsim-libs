/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.pngSequence2Video;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.jcodec.api.awt.SequenceEncoder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

/**
* @author ikaddoura
*/

public class MATSimVideoUtils {
	private static final Logger log = Logger.getLogger(MATSimVideoUtils.class);

	public static void createLegHistogramVideo(String runDirectory) throws IOException {
		createVideo(runDirectory, 1, "legHistogram_all");
	}

	public static void createVideo(String outputDirectory, int interval, String pngFileName) throws IOException {

		log.info("Generating a video using a png sequence...");
		
		if (!outputDirectory.endsWith("/")) {
			outputDirectory = outputDirectory + "/";
		}
		String outputFile = outputDirectory + pngFileName + ".mp4";
		SequenceEncoder enc = new SequenceEncoder(new File(outputFile));
						
		final Config config = ConfigUtils.loadConfig(outputDirectory + "output_config.xml.gz");

		int counter = 0;
		for (int i = config.controler().getFirstIteration(); i<= config.controler().getLastIteration(); i++) {
			
			if (counter % interval == 0) {
				String pngFile = null;
				BufferedImage image = null;
				
				try {
					pngFile = outputDirectory + "ITERS/it." + i + "/" + i + "." + pngFileName + ".png";
//					log.info("Trying to read " + pngFile + "...");
					image = ImageIO.read(new File(pngFile));
				
				} catch (IOException e) {
//					log.info("File does not exist.");
					
					try {
						pngFile = outputDirectory + "ITERS/it." + i + "/" + pngFileName + ".png";
//						log.info("Trying to read " + pngFile + "...");
						image = ImageIO.read(new File(pngFile));
					} catch (IOException e2){
						log.warn("Skipping...");
					}
				}
								
				if (image != null) {
//					log.info("... File successfully read.");
					enc.encodeImage(image);
				} else {
					log.warn("Skipping...");
				}
			}
			counter++;
		}
		
		enc.finish();
		
		log.info("Generating a video using a png sequence... Done. Video written to " + outputFile);
	}

}

