/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.utils;

import java.io.File;
import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.joint.JointCalibrationControler;

/**
 * I think, after introduction of URL and for uniformity, pass absolute path.
 * Because, relative paths are converted to new uri and then url using new File(" ").getAbsoluteFile() rather than
 * new File(" ").getCanonicalFile(); which eventually contains (../..) in the file path. see toURL() of {@link java.io.File}.
 *
 * Created by amit on 26/09/16.
 */


public final class FileUtils {

    public static final Logger LOGGER = Logger.getLogger(FileUtils.class);

    public static final String RUNS_SVN = "/Users/amit/Documents/repos/runs-svn/";

    public static final String SHARED_SVN = "/Users/amit/Documents/repos/shared-svn/";

    /*
    * To delete all intermediate iteration except first and last.
    */
    public static void deleteIntermediateIterations(final String outputDir, final int firstIteration, final int lastIteration) {
        for (int index =firstIteration+1; index <lastIteration; index ++){
            String dirToDel = outputDir+"/ITERS/it."+index;
            if (! new File(dirToDel).exists()) continue;
            Logger.getLogger(JointCalibrationControler.class).info("Deleting the directory "+dirToDel);
            IOUtils.deleteDirectoryRecursively(new File(dirToDel).toPath());
        }
    }
}
