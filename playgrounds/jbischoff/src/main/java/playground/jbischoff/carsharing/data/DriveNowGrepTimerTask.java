/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jbischoff.carsharing.data;

import java.util.Timer;
import java.util.TimerTask;

public class DriveNowGrepTimerTask
    extends TimerTask
{
    DriveNowParser dnp = new DriveNowParser();
    Car2GoParser cp = new Car2GoParser(); 
    
    public static void main(String[] args)
    {
        DriveNowGrepTimerTask dngt = new DriveNowGrepTimerTask();
        Timer t = new Timer();
        t.scheduleAtFixedRate(dngt, 0, 60*1000);
    }

    @Override
    public void run()
    {
    	String folder = "./";
        dnp.grepAndDumpOnlineDatabase(folder +"drivenow/");
        cp.grepAndDumpOnlineDatabase(folder + "car2go/");
    }

}
