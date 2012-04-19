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

import com.google.common.io.*;


public class MServer
{
    public static final int SOCKET_PORT = 4440;

    private ServerSocket serverSocket = null;
    private Socket clientSocket = null;

    private LittleEndianDataOutputStream out;
    private LittleEndianDataInputStream in;


    public void initServer()
    {
        try {
            serverSocket = new ServerSocket(SOCKET_PORT);

            //WalClient.launchClient();

            clientSocket = serverSocket.accept();

            out = new LittleEndianDataOutputStream(clientSocket.getOutputStream());
            in = new LittleEndianDataInputStream(clientSocket.getInputStream());
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


    public void closeServer()
    {
        try {
            out.close();
            in.close();

            clientSocket.close();
            serverSocket.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
