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

package playground.michalm.vrp.taxi.wal;

import java.io.IOException;
import java.net.*;

import playground.michalm.vrp.taxi.wal.Command.CommandType;

import com.google.common.io.*;


public class WalClient
{
    private Socket socket;
    private LittleEndianDataOutputStream out;
    private LittleEndianDataInputStream in;


    public void initClient()
    {
        try {
            socket = new Socket((String)null, MServer.SOCKET_PORT);

            out = new LittleEndianDataOutputStream(socket.getOutputStream());
            in = new LittleEndianDataInputStream(socket.getInputStream());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public Command readCommand()
    {
        return Command.readCommand(in);
    }


    public void writeCommand(Command command)
    {
        Command.writeCommand(out, command);
    }


    public void closeClient()
    {
        try {
            out.close();
            in.close();

            socket.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void launchClient()
    {
        Runnable runnable = new Runnable() {
            @Override
            public void run()
            {
                WalClient wClient = new WalClient();
                wClient.initClient();

                wClient.writeCommand(new Command(CommandType.INIT, 600, 620, 1));
                wClient.writeCommand(new Command(CommandType.VEHICLE, 111, 3402));

                for (;;) {
                    wClient.readCommand();
                }
            }
        };

        new Thread(runnable).start();
    }
}
