package playground.anhorni.locationchoice.cs.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;
import playground.anhorni.locationchoice.cs.helper.TravelTimeBudget;

public class BudgetWriter {
	
	public BudgetWriter() {
	}
	
	public void write(String outfile, List<TravelTimeBudget> budgets)  {
		
		try {		
			final String header="Person_id\tTrip_nr\tBudget";
						
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(header);
			out.newLine();
			
			Iterator<TravelTimeBudget> budget_it = budgets.iterator();
			while (budget_it.hasNext()) {
				TravelTimeBudget budget = budget_it.next();				
				out.write(budget.getPersonId() +"\t" + budget.getSubChainIndex() + "\t" + budget.getTravelTimeBudget());
				out.newLine();
				out.flush();
			}
			out.flush();
			out.close();
						
		} catch (final IOException e) {
				Gbl.errorMsg(e);
		}	
	}
}
