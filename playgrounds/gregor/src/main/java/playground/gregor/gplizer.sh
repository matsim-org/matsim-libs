#!/bin/bash
function header() {
echo "/* *********************************************************************** *
 * project: org.matsim.*
 * $1
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
 * *********************************************************************** */";
}


for i in `find`; do
echo $i | grep java$ > /dev/null
if [[ $? != 0 ]]; then 
	continue;
fi 

cat $i | grep "GNU General Public License" > /dev/null
if [[ $? == 1 ]]; then
	header `basename $i` > gplizer.temp
	cat $i >> gplizer.temp
	mv gplizer.temp $i
fi


done
