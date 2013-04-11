package playground.wdoering.grips.v2.ptlines;

import org.matsim.api.core.v01.Id;

public class BusStop
{
		Id id;
		protected String hh = "--";
		protected String mm = "--";
		protected Object numDepSpinnerValue = new Integer(0);
		protected Object numVehSpinnerValue = new Integer(0);
		protected boolean circCheckSelected = false;
		protected Object capSpinnerValue = new Integer(0);


		@Override
		public String toString() {
			return this.id + " " + this.hh + " " + this.mm;
		}

}
