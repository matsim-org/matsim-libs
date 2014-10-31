package lcrociani.TestMatsimConnector;

import matsimConnector.scenario.CAEnvironment;
import matsimConnector.scenario.CAScenario;

import org.junit.Test;

import pedCA.context.Context;

public class ConectorTests {
	@Test
	public void testCAEnvironment(){
		String path = "c:/tmp/pedCATest/corridor";
		Context context = new Context(path);
		System.out.println(context.getNetwork().toString());
		
		CAEnvironment environment = new CAEnvironment(""+0,context);
		CAScenario scenario = new CAScenario();
		scenario.addCAEnvironment(environment);
		
	}
}
