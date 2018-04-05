package vwExamples.peoplemoverVWExample.CustomRebalancing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;
import java.util.Iterator;



public class RelocationWriter implements IterationEndsListener {
	private List<String> relocationsLog = new ArrayList<String>();
	
	@Inject 
	MatsimServices matsimServices;
	private final DecimalFormat format = new DecimalFormat();

	@Inject
	public RelocationWriter() {
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);
	}

	
@Override
public void notifyIterationEnds(IterationEndsEvent event) {
	writeRelocations(event.getIteration());
}

private void writeRelocations(int iteration) {
	collection2Text(relocationsLog,matsimServices.getControlerIO().getIterationFilename(iteration, "drt_relocations.csv"),"vehicleID;time;beeline");
	relocationsLog.clear();
}

public void setRelocation(String relocationEntry)
{
//	System.out.println("Add Entry!");
	relocationsLog.add(relocationEntry);
}


public <T> void collection2Text(Collection<T> c, String filename, String header) {
	BufferedWriter bw = IOUtils.getBufferedWriter(filename);
	try {
		if (header != null) {
			bw.write(header);
			bw.newLine();
		}
		for (Iterator<T> iterator = c.iterator(); iterator.hasNext();) {

			bw.write(iterator.next().toString());
			bw.newLine();
		}
		bw.flush();
		bw.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

	


}
