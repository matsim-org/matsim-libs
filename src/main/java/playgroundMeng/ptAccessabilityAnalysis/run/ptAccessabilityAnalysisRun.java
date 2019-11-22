package playgroundMeng.ptAccessabilityAnalysis.run;



import com.google.inject.Guice;
import com.google.inject.Injector;

import playgroundMeng.ptAccessabilityAnalysis.linksCategoryAnalysis.LinksPtInfoCollector;

public class ptAccessabilityAnalysisRun {
	public static void main(String[] args) throws Exception {
		 Injector injector = Guice.createInjector(new PtAccessabilityModule());
		 LinksPtInfoCollector linksPtInfoCollector = injector.getInstance(LinksPtInfoCollector.class);
		 linksPtInfoCollector.runAndFile();
		 
//		 NetworkChangeEventMerge networkChangeEventMerge = injector.getInstance(NetworkChangeEventMerge.class);
//		 networkChangeEventMerge.merge();
//		 
	}
}
