/* *********************************************************************** *
 * project: org.matsim.*
 * TemporalConfig.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.netvis.config;

import org.matsim.utils.misc.Time;

public class TemporalConfig extends ConfigModule {

    // -------------------- CLASS VARIABLES --------------------

    private static final String CLASS_NAME = "temporal";

    private static final String START_TIME_S = "starttime";

    private static final String END_TIME_S = "endtime";

    private static final String BUFFER_SIZE = "buffersize";

    private static final String TIMESTEP_LENGTH_S = "timesteplength";

    // -------------------- CONSTRUCTION --------------------

    public TemporalConfig(String fileName) {
        super(CLASS_NAME, fileName);
    }

    public TemporalConfig(int startTime_s, int endTime_s, int bufferSize,
            int timeStepLength_s) {
        super(CLASS_NAME);

        set(START_TIME_S, Time.writeTime(startTime_s, Time.TIMEFORMAT_HHMMSS, '-'));
        set(END_TIME_S, Time.writeTime(endTime_s, Time.TIMEFORMAT_HHMMSS, '-'));
        set(BUFFER_SIZE, Integer.toString(bufferSize));
        set(TIMESTEP_LENGTH_S, Time.writeTime(timeStepLength_s, Time.TIMEFORMAT_HHMMSS, '-'));
    }
//
//    @Override
//    public boolean isComplete() {
//        return containsKey(START_TIME_S) && containsKey(END_TIME_S)
//                && containsKey(BUFFER_SIZE) && containsKey(TIMESTEP_LENGTH_S);
//    }

    // -------------------- CACHING --------------------

    private int startTime_s;

    private int endTime_s;

    private int bufferSize;

    private int timeStepLength_s;

    @Override
    protected void cache(String name, String value) {
        if (START_TIME_S.equals(name))
            startTime_s = (int)Time.parseTime(value, '-');
        else if (END_TIME_S.equals(name))
            endTime_s = (int)Time.parseTime(value, '-');
        else if (BUFFER_SIZE.equals(name))
            bufferSize = Integer.parseInt(value);
        else if (TIMESTEP_LENGTH_S.equals(name))
            timeStepLength_s = (int)Time.parseTime(value, '-');
    }

    public int getStartTime_s() {
        return startTime_s;
    }

    public int getEndTime_s() {
        return endTime_s;
    }

    public int getTimeStepLength_s() {
        return timeStepLength_s;
    }

    public int getBufferSize() {
        return bufferSize;
    }

}
