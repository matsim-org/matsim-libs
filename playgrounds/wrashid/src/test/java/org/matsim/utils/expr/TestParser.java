package org.matsim.utils.expr;

import junit.framework.TestCase;

public class TestParser extends TestCase {

	public void testNoVarialbe() {
		expect(9, "3^2");
		expect(256, "2^2^3");
		expect(6, "3*2");
		expect(1.5, "3/2");
		expect(5, "3+2");
		expect(1, "3-2");
		expect(-3, "-3");
		expect(1, "2<3");
		expect(0, "2<2");
		expect(0, "3<2");
		expect(1, "2<=3");
		expect(1, "2<=2");
		expect(0, "3<=2");
		expect(0, "2=3");
		expect(1, "2=2");
		expect(1, "2<>3");
		expect(0, "2<>2");
		expect(0, "2>=3");
		expect(1, "2>=2");
		expect(1, "3>=2");
		expect(0, "2>3");
		expect(0, "2>2");
		expect(1, "3>2");
		expect(1, "(1 and 1)");
		expect(0, "(1 and 0)");
		expect(0, "(0 and 1)");
		expect(0, "(0 and 0)");
		expect(1, "(1 or 1)");
		expect(1, "(1 or 0)");
		expect(1, "(0 or 1)");
		expect(0, "(0 or 0)");
		expect(2, "abs(-2)");
		expect(2, "abs(2)");
		expect(0, "acos(1)");
		expect(Math.PI / 2, "asin(1)");
		expect(Math.PI / 4, "atan(1)");
		expect(-3 * Math.PI / 4, "atan2(-1, -1)");
		expect(4, "ceil(3.5)");
		expect(-3, "ceil(-3.5)");
		expect(1, "cos(0)");
		expect(Math.exp(1), "exp(1)");
		expect(3, "floor(3.5)");
		expect(-4, "floor(-3.5)");
		expect(1, "log(2.7182818284590451)");
		expect(4, "round(3.5)");
		expect(-4, "round(-3.5)");
		expect(3, "sqrt(9)");
		
		expect(3, "max(2, 3)");
		expect(2, "min(2, 3)");
		expect(137, "if(0, 42, 137)");
		expect(42, "if(1, 42, 137)");
		expect(137, "137");

		expect(-3.0 * Math.pow(1.01, 100.1), "  -3 * 1.01^100.1  ");
	}
	
	public void testsPI(){
		Parser parser = new Parser("sin(pi/2)");
		parser.setVariable("pi", Math.PI);
		expect(1, parser);
		
		parser = new Parser("tan(pi/4)");
		parser.setVariable("pi", Math.PI);
		expect(0.99999999999999989, parser);
	}
	
	private Parser prepareParserSingleVariable(String expression,double xValue){
		Parser parser = new Parser(expression);
		parser.setVariable("x", xValue);
		return parser;
	}
	
	public void testSingleVariable(){
		
		expect(1.1, prepareParserSingleVariable("x",1.1));
		
		expect(-171.375208, prepareParserSingleVariable("-0.00504238 * x^2 + 2.34528 * x - 69.4962",-40.0));
		
		expect(3.8013239000000003, prepareParserSingleVariable("3.14159 * x^2",1.1));
		expect(-1.457526100326025, prepareParserSingleVariable("sin(10*x) + sin(9*x)",1.1));
		expect(0.8907649332805846, prepareParserSingleVariable("sin(x) + sin(100*x)/100",1.1));
		expect(-0.16000473871962462, prepareParserSingleVariable("sin(0.1*x) * (sin(9*x) + sin(10*x))",1.1));
		expect(0.29819727942988733, prepareParserSingleVariable("exp(-x^2)",1.1));
		expect(0.43226861565393254, prepareParserSingleVariable("2^(-x^2)",1.1));
		expect(0.7075295010833899, prepareParserSingleVariable("(x^3)^(-x^2)",1.1));
		expect(0.8678400091286832, prepareParserSingleVariable("x*sin(1/x)",1.1));
		expect(-5.89, prepareParserSingleVariable("x^2-x-6",1.1));
		expect(3.1953090617340916, prepareParserSingleVariable("sqrt(3^2 + x^2)",1.1));
		expect(1.3542460218188073, prepareParserSingleVariable("atan(5/x)",1.1));
		expect(1.5761904761904764, prepareParserSingleVariable("(x^2 + x + 1)/(x + 1)",1.1));
		expect(2.6451713395638627, prepareParserSingleVariable("(x^3 - (4*x^2) + 12)/(x^2 + 2)",1.1));
		expect(-2.2199999999999998, prepareParserSingleVariable("-2*(x-3)^2+5",1.1));
		expect(1.2000000000000002, prepareParserSingleVariable("2*abs(x+1)-3",1.1));
		expect(2.7910571473905725, prepareParserSingleVariable("sqrt(9-x^2)",1.1));
	}
	
	public void testForStaticDependencyOfParsers(){
		Parser parser1=new Parser("x");
		Parser parser2=new Parser("x");
		
		parser1.setVariable("x", 1);
		parser2.setVariable("x", 2);
		
		expect(1.0, parser1);
		expect(2.0, parser2);
	}
	
	public void testMultipleVariables(){
		Parser parser1=new Parser("x+y");
		
		parser1.setVariable("x", 1);
		parser1.setVariable("y", 2);
		
		expect(3.0, parser1);
	}

	private static void expect(double expected, Parser parser) {
		Expr expr;
		try {
			expr = parser.parse();
		} catch (SyntaxException e) {
			throw new Error(e.explain());
		}

		double result = expr.value();
		assertEquals(expected, result);
	}
	
	private static void expect(double expected, String input) {
		Expr expr;
		try {
			expr = new Parser(input).parse();
		} catch (SyntaxException e) {
			throw new Error(e.explain());
		}

		double result = expr.value();
		assertEquals(expected, result);
	}

}
