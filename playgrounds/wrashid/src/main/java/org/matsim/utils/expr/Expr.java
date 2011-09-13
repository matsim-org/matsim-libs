package org.matsim.utils.expr;

// Mathematical expressions.
// Copyright 1996 by Darius Bacon; see the file COPYING.


/**
 * A mathematical expression, built out of literal numbers, variables,
 * arithmetic and relational operators, and elementary functions.  It
 * can be evaluated to get its value given its variables' current
 * values.  The operator names are from java.lang.Math where possible.
 */
public abstract class Expr {

    /** Calculate the expression's value.
     * @return the value given the current variable values */
    public abstract double value();

    /** Binary operator: addition        */  public static final int ADD =  0;  
    /** Binary operator: subtraction     */  public static final int SUB =  1;
    /** Binary operator: multiplication  */  public static final int MUL =  2;
    /** Binary operator: division        */  public static final int DIV =  3;
    /** Binary operator: exponentiation  */  public static final int POW =  4;
    /** Binary operator: arctangent      */  public static final int ATAN2 = 5;
    /** Binary operator: maximum         */  public static final int MAX =  6;
    /** Binary operator: minimum         */  public static final int MIN =  7;
    /** Binary operator: less than       */  public static final int LT  =  8;
    /** Binary operator: less or equal   */  public static final int LE  =  9;
    /** Binary operator: equality        */  public static final int EQ  = 10;
    /** Binary operator: inequality      */  public static final int NE  = 11;
    /** Binary operator: greater or equal*/  public static final int GE  = 12;
    /** Binary operator: greater than    */  public static final int GT  = 13;
    /** Binary operator: logical and     */  public static final int AND = 14;
    /** Binary operator: logical or      */  public static final int OR  = 15;
  
    /** Unary operator: absolute value*/   public static final int ABS   = 100;
    /** Unary operator: arccosine */       public static final int ACOS  = 101;
    /** Unary operator: arcsine   */       public static final int ASIN  = 102;
    /** Unary operator: arctangent*/       public static final int ATAN  = 103;
    /** Unary operator: ceiling   */       public static final int CEIL  = 104;
    /** Unary operator: cosine    */       public static final int COS   = 105;
    /** Unary operator: e to the x*/       public static final int EXP   = 106;
    /** Unary operator: floor     */       public static final int FLOOR = 107;
    /** Unary operator: natural log*/      public static final int LOG   = 108;
    /** Unary operator: negation        */ public static final int NEG   = 109;
    /** Unary operator: rounding  */       public static final int ROUND = 110;
    /** Unary operator: sine      */       public static final int SIN   = 111;
    /** Unary operator: square root */     public static final int SQRT  = 112;
    /** Unary operator: tangent */         public static final int TAN   = 113;

    /** Make a literal expression.
     * @param v the constant value of the expression
     * @return an expression whose value is always v */
    public static Expr makeLiteral(double v) { 
	return new LiteralExpr(v); 
    }
    /** Make an expression that applies a unary operator to an operand.
     * @param rator a code for a unary operator
     * @param rand operand
     * @return an expression meaning rator(rand)
     */
    public static Expr makeApp1(int rator, Expr rand) {
	Expr app = new UnaryExpr(rator, rand);
	return rand instanceof LiteralExpr
	    ? new LiteralExpr(app.value()) 
	    : app;
    }
    /** Make an expression that applies a binary operator to two operands.
     * @param rator a code for a binary operator
     * @param rand0 left operand
     * @param rand1 right operand
     * @return an expression meaning rator(rand0, rand1)
     */
    public static Expr makeApp2(int rator, Expr rand0, Expr rand1) {
	Expr app = new BinaryExpr(rator, rand0, rand1);
	return rand0 instanceof LiteralExpr && rand1 instanceof LiteralExpr
	    ? new LiteralExpr(app.value()) 
	    : app;
    }
    /** Make a conditional expression.
     * @param test `if' part
     * @param consequent `then' part
     * @param alternative `else' part
     * @return an expression meaning `if test, then consequent, else
     *         alternative' 
     */
    public static Expr makeIfThenElse(Expr test,
				      Expr consequent,
				      Expr alternative) {
	Expr cond = new ConditionalExpr(test, consequent, alternative);
	if (test instanceof LiteralExpr)
            return test.value() != 0 ? consequent : alternative;
	else
	    return cond;
    }
}

// These classes are all private to this module because we could
// plausibly want to do it in a completely different way, such as a
// stack machine.

class LiteralExpr extends Expr {
    double v;
    LiteralExpr(double v) { this.v = v; }
    public double value() { return v; }
}

class UnaryExpr extends Expr {
    int rator;
    Expr rand;

    UnaryExpr(int rator, Expr rand) { 
	this.rator = rator;
	this.rand = rand;
    }

    public double value() {
	double arg = rand.value();
	switch (rator) {
	case ABS:   return Math.abs(arg);
	case ACOS:  return Math.acos(arg);
	case ASIN:  return Math.asin(arg);
	case ATAN:  return Math.atan(arg);
	case CEIL:  return Math.ceil(arg);
	case COS:   return Math.cos(arg);
	case EXP:   return Math.exp(arg);
	case FLOOR: return Math.floor(arg);
	case LOG:   return Math.log(arg);
	case NEG:   return -arg;
	case ROUND: return Math.rint(arg);
	case SIN:   return Math.sin(arg);
	case SQRT:  return Math.sqrt(arg);
	case TAN:   return Math.tan(arg);
	default: throw new RuntimeException("BUG: bad rator");
	}
    }
}

class BinaryExpr extends Expr {
    int rator;
    Expr rand0, rand1;

    BinaryExpr(int rator, Expr rand0, Expr rand1) {
	this.rator = rator;
	this.rand0 = rand0;
	this.rand1 = rand1;
    }
    public double value() {
	double arg0 = rand0.value();
	double arg1 = rand1.value();
	switch (rator) {
	case ADD:   return arg0 + arg1;
	case SUB:   return arg0 - arg1;
	case MUL:   return arg0 * arg1;
	case DIV:   return arg0 / arg1; // division by 0 has IEEE 754 behavior
	case POW:   return Math.pow(arg0, arg1);
	case ATAN2: return Math.atan2(arg0, arg1);
	case MAX:   return arg0 < arg1 ? arg1 : arg0;
	case MIN:   return arg0 < arg1 ? arg0 : arg1;
        case LT:    return arg0 <  arg1 ? 1.0 : 0.0;
        case LE:    return arg0 <= arg1 ? 1.0 : 0.0;
        case EQ:    return arg0 == arg1 ? 1.0 : 0.0;
        case NE:    return arg0 != arg1 ? 1.0 : 0.0;
        case GE:    return arg0 >= arg1 ? 1.0 : 0.0;
        case GT:    return arg0  > arg1 ? 1.0 : 0.0;
        case AND:   return arg0 != 0 && arg1 != 0 ? 1.0 : 0.0;
        case OR:    return arg0 != 0 || arg1 != 0 ? 1.0 : 0.0; 
	default: throw new RuntimeException("BUG: bad rator");
	}
    }
}

class ConditionalExpr extends Expr {
    Expr test, consequent, alternative;

    ConditionalExpr(Expr test, Expr consequent, Expr alternative) {
	this.test = test;
	this.consequent = consequent;
	this.alternative = alternative;
    }

    public double value() {
	return test.value() != 0 ? consequent.value() : alternative.value();
    }
}
