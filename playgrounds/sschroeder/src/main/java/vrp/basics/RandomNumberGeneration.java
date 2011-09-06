/*******************************************************************************
 * Copyright (C) 2011 Stefan Schršder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package vrp.basics;

import java.util.Random;

public class RandomNumberGeneration {
	
	private static long DEFAULT_SEED = 4711;
	
	private static Random random = new Random(DEFAULT_SEED);
	
	public static Random getRandom(){
		return random;
	}
	
	public static void setSeed(long seed){
		random.setSeed(seed);
	}
	
	public static void reset(){
		random.setSeed(DEFAULT_SEED);
	}

}
