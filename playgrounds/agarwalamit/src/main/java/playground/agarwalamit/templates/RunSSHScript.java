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

package playground.agarwalamit.templates;

import java.util.Properties;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Created by amit on 28/03/2017.
 */


public class RunSSHScript {

    private static final String myPassword = "xxx";

    public static void main(String[] args) {

        try {
            JSch jSch = new JSch();
            jSch.setKnownHosts("~/.ssh/known_hosts"); // location of the ssh fingerprint (unique host key)

            Properties config = new Properties();

            config.put("StrictHostKeyChecking", "no"); // so that no question asked, and script run without any problem

            Session session = jSch.getSession("agarwal", "cluster-i.math.tu-berlin.de", 22);
            session.setConfig(config);
            session.setPassword(myPassword);

            try {
                session.connect();

                System.out.println( session.getUserInfo() );

            } finally {
                session.disconnect();
            }


        } catch (JSchException e) {
            throw new RuntimeException("Aborting. Reason : " + e);
        }

    }

}
