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

public class CreateVideo {
	private static final Logger log = Logger.getLogger(CreateVideo.class);

	public static void run(String runDirectory) throws IOException {
		
		log.info("Creating a video for the departure times...");
	
		SequenceEncoder enc = new SequenceEncoder(new File(runDirectory + "departureTimes.mp4"));
				
		final Config config = ConfigUtils.loadConfig(runDirectory + "output_config.xml.gz");

		for(int i = config.controler().getFirstIteration(); i<= config.controler().getLastIteration(); i++) {
			
			String pngFile = runDirectory + "ITERS/it." + i + "/" + i + ".legHistogram_car.png";
			BufferedImage image = ImageIO.read(new File(pngFile));
			enc.encodeImage(image);
		}
		enc.finish();
		
		log.info("Creating a video for the departure times... Done.");
	}

}

