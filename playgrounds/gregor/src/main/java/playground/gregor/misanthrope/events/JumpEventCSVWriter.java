package playground.gregor.misanthrope.events;/* *********************************************************************** *
 * project: org.matsim.*
 *
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

import java.io.BufferedWriter;
import java.io.IOException;

public class JumpEventCSVWriter implements JumpEventHandler {

    private final BufferedWriter bw;

    public JumpEventCSVWriter(BufferedWriter bw) {
        this.bw = bw;
        try {
            this.bw.append("#time,person_id,desired_direction,from_cell,to_cell\n");
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
    }

    @Override
    public void handleEvent(JumpEvent e) {
        try {
            this.bw.append(e.getRealtime() + "," + e.getPersonId() + "," + e.getDir() + "," + e.getFrom() + "," + e.getTo() + "\n");
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }

    }

    @Override
    public void reset(int iteration) {

    }
}
