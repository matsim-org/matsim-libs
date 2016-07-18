package playground.gregor;
/****************************************************************************/
// casim, cellular automaton simulation for multi-destination pedestrian
// crowds; see https://github.com/CACrowd/casim
// Copyright (C) 2016 CACrowd and contributors
/****************************************************************************/
//
//   This file is part of casim.
//   casim is free software: you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, either version 2 of the License, or
//   (at your option) any later version.
//
/****************************************************************************/

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.DoubleAccumulator;

/**
 * Created by laemmel on 05/07/16.
 */
public class BlockingQTest {


    public static void main(String[] args) throws InterruptedException {

        BlockingQueue<Double> q = new LinkedBlockingQueue<>();

        List<Double> buffer = new ArrayList<>();


        long startTime = System.currentTimeMillis();
        long timeOut = 10000;

        while (true) {

            Double element = q.poll(timeOut, TimeUnit.MILLISECONDS);
            if (element != null) {
                //

            } else {
                timeOut -= System.currentTimeMillis() - startTime;

                buffer.add(element);
            }

//            Thread.sleep(1);


            if (buffer.size() >= 10) {

                //


                buffer.clear();
                startTime = System.currentTimeMillis();
                timeOut = 10000;
            }


        }

    }
}
