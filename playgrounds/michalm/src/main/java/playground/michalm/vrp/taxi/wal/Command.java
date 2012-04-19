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

import com.google.common.io.*;


public class Command
{
    public static enum CommandType
    {
        INIT(3), // $startTime$ $endTime$ $realVehicleCount$
        VEHICLE(2), // $vehId$ $linkId$
        STARTED(0), //

        VERTEX(1), // $vertexId$
        CUSTOMER(1), // $customerId$
        REQUEST(1), // $requestId$

        TASK_ENDED(1), // $taskId$ [$time$ -- dla symulacji w real-time - pole niepotrzebne]
        SCHEDULE_UPDATED(1), // $scheduleId$
        SCHEDULES_REOPTIMIZED(0), //
        NEXT_TASK(1), // $taskId$

        NEXT_LINK(1);// $scheduleId$

        int paramCount;


        private CommandType(int paramCount)
        {
            this.paramCount = paramCount;
        }


        public int getParamCount()
        {
            return paramCount;
        }
    }


    private final CommandType type;
    private final int[] params;


    public Command(CommandType type, int... params)
    {
        this.type = type;
        this.params = params;
    }


    public CommandType getType()
    {
        return type;
    }


    public int getParam(int idx)
    {
        return params[idx];
    }


    public int getParamCount()
    {
        return params.length;
    }


    @Override
    public String toString()
    {
        String str = type.name();

        for (int i = 0; i < params.length; i++) {
            str += " " + params[i];
        }

        return str;
    }


    public static Command readCommand(LittleEndianDataInputStream in)
    {
        try {
            String name = readString(in);
            CommandType type = CommandType.valueOf(name);

            int[] params = new int[type.getParamCount()];

            for (int i = 0; i < params.length; i++) {
                params[i] = in.readInt();
            }

            Command command = new Command(type, params);

            System.err.println("READ command: " + command);

            return command;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void writeCommand(LittleEndianDataOutputStream out, Command command)
    {
        try {
            System.err.println("WRITE command: " + command);

            CommandType type = command.type;

            writeString(out, type.name());

            int[] params = command.params;

            for (int i = 0; i < params.length; i++) {
                out.writeInt(params[i]);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static void writeString(LittleEndianDataOutputStream out, String str)
        throws IOException
    {
        out.write(str.length());
        out.write(str.getBytes());
    }


    private static String readString(LittleEndianDataInputStream in)
        throws IOException
    {
        int length = in.read();
        byte[] bytes = new byte[length];

        in.read(bytes);

        return new String(bytes);
    }
}
