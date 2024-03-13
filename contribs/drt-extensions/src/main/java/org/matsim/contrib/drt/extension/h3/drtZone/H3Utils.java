package org.matsim.contrib.drt.extension.h3.drtZone;

import com.uber.h3core.H3Core;

import java.io.IOException;

/**
 * @author nkuehnel / MOIA
 */
public final class H3Utils {

	private static H3Core h3;

	public final static int MAX_RES = 15;


	public static H3Core getInstance() {
		if(h3 == null) {
			try {
				h3 = H3Core.newInstance();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return h3;
	}
}
