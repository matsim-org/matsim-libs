/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.vsp.parametricRuns;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

/**
 * Created by amit on 04.10.17.
 */

public class JobScriptWriter {

    private static final Logger LOGGER = Logger.getLogger(JobScriptWriter.class);
    private static final String newLine = System.getProperty("line.separator");

    private final StringBuilder buffer = new StringBuilder();
    private String jobScript ;

    /**
     * The defaults are:
     * <ul>
     * <li>max run time is set to 9 days</li>
     * <li>error messages and output into the output file</li>
     * <li>send an email at beginning and end of the job</li>
     * <li>email address is agarwal@vsp.tu-berlin.de</li>
     * <li>a 16GB node with 4 processors</li>
     * <li>required free memory is 7.5 GB</li>
     * </ul>
     * In order to override, pass the arguments with 'qsub'.
     * @param userName 
     */
    public JobScriptWriter(String userName) {
        createDefaults(userName);
    }

    //main example
    public static void main(String[] args) {
        String fileName = "../../runs-svn/opdyts/equil/car,bicycle/testCalib/test.sh";
        String locationOfClusterLogFile = "../../runs-svn/opdyts/equil/car,bicycle/testCalib/";
        String jobName = "run00";
        String [] additionalLines = {
                "echo \"========================\"",
                "echo \"r_6fcba9f631fedc82ecc01a48bbc43abfefac78c1_opdyts\"",
                "echo \"========================\"",
                newLine,

                "cd /net/ils4/agarwal/matsim/r_6fcba9f631fedc82ecc01a48bbc43abfefac78c1_opdyts/",
                newLine,
                "java -Djava.awt.headless=true -Xmx29G -cp agarwalamit-0.10.0-SNAPSHOT.jar " +
                        "playground/agarwalamit/opdyts/equil/MatsimOpdytsEquilMixedTrafficIntegration " +
                        "/net/ils4/agarwal/equilOpdyts/carBicycle/inputs/ " +
                        "/net/ils4/agarwal/equilOpdyts/carBicycle/output/"+jobName+"/ " +
                        "/net/ils4/agarwal/equilOpdyts/carBicycle/relaxedPlans/output_plans.xml.gz "+
                        "diagonal_random"
        };

        JobScriptWriter jobScriptWriter = new JobScriptWriter("agarwal");
        jobScriptWriter.appendCommands(jobName, locationOfClusterLogFile, additionalLines);
        jobScriptWriter.writeLocally(fileName); // only local address
    }

    public void appendCommands(final String jobName, final String locationOfClusterLogFile, final String [] additionalLinesInJobScript) {
        buffer.append("#$ -N "+ jobName);
        buffer.append(newLine);

        buffer.append("#$ -o "+locationOfClusterLogFile+"/clusterLog_"+jobName+".txt");
        buffer.append(newLine);
        buffer.append(newLine);

        buffer.append("date");
        buffer.append(newLine);

        buffer.append("hostname");
        buffer.append(newLine);

        buffer.append("java -version");
        buffer.append(newLine);

        buffer.append("echo \"using alternative java\" ");
        buffer.append(newLine);

        buffer.append("module add java-1.8");
        buffer.append(newLine);

        buffer.append("java -version");
        buffer.append(newLine);
        buffer.append(newLine);

        buffer.append("echo \"=========================== Job start ===========================\"");
        buffer.append(newLine);
        buffer.append(newLine);

        for (String str : additionalLinesInJobScript) {
            buffer.append(str);
            buffer.append(newLine);
        }

        buffer.append(newLine);
        buffer.append(newLine);
        buffer.append("date");
        buffer.append(newLine);

        buffer.append("echo \"=========================== Job end ===========================\"");
        buffer.append(newLine);
    }

    public void writeLocally(final String localFileName){
        BufferedWriter writer;
        if (localFileName.endsWith(".sh")) {
            jobScript = localFileName;
        } else{
            LOGGER.warn(localFileName + "does not ends with \".sh\". Assuming it is a location rather than a script name, adding \"test.sh\".");
            jobScript = localFileName+"/test.sh";
        }

        writer = IOUtils.getBufferedWriter(jobScript);

        try {
            writer.write(buffer.toString());
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    public void writeRemoteLocation(final ChannelSftp sftp, final String scriptFileName){
        if (scriptFileName.endsWith(".sh")) {
            jobScript = scriptFileName;
        } else{
            LOGGER.warn(scriptFileName + "does not ends with \".sh\". Assuming it is a location rather than a script name, adding \"test.sh\".");
            jobScript = scriptFileName+"/test.sh";
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream w = new DataOutputStream(baos);
            w.writeBytes(buffer.toString());
            w.flush();

            sftp.put(new ByteArrayInputStream(baos.toByteArray()), scriptFileName);

            w.close();
            baos.close();
        } catch (SftpException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    private void createDefaults(String userName){
        buffer.append("#!/bin/bash --login");
        buffer.append(newLine);

        buffer.append("#$ -l h_rt=777600"); //run time limit (in seconds).
        buffer.append(newLine);

        buffer.append("#$ -j y"); // 'Join'=yes, i.e. write error messages and output into the output file
        buffer.append(newLine);

        buffer.append("#$ -cwd"); //Changes into the directory from where the job was submitted.
        buffer.append(newLine);

        buffer.append("#$ -m be"); //write a mail at beginning and end of the job
        buffer.append(newLine);

        buffer.append("#$ -M "+userName+"@vsp.tu-berlin.de"); // email address
        buffer.append(newLine);

        buffer.append("#$ -pe mp 4"); // job runs on a 16 GB node with 4 processors
        buffer.append(newLine);

        buffer.append("#$ -l mem_free=15G"); //memory requirements of the job
        buffer.append(newLine);
    }

    @Override
    public String toString() {
        return buffer.toString();
    }

    public String getJobScript(){
        return jobScript;
    }
}
