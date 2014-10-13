/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package playground.pieter.events;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class Perceptron extends JPanel implements ActionListener {
  private final NeuronIn Input;
  private final NeuronOut Output;
  private final NeuronSoma Soma;
  private final ControlPanel Controls;
  private final double[] Weight ;
  private final int[] X ;
  private double Threshold;
  private final int[] X1 ;
  private final int[] X2 ;
  private int Step;
  private int Epoch;
  private int Error;
  private final double[] Delta ;
  private final double[] Old ;
  private final double Alpha;
  private int Out;
  private double Potential;
  private int Target;
  private int Errors;
  
  public Perceptron () {
    int i;
    int Count = 2;
    Errors = 0;
    X = new int [2];
    X1 = new int [4];
    X1[0] = 0;
    X1[1] = 1;
    X1[2] = 0;
    X1[3] = 1;
    X2 = new int [4];
    X2[0] = 0;
    X2[1] = 0;
    X2[2] = 1;
    X2[3] = 1;
    Weight = new double [2];
    Old = new double [2];
    Delta = new double [2];
    Input = new NeuronIn ();
    Soma = new NeuronSoma ();
    Output = new NeuronOut ();
    Controls = new ControlPanel ();
    Controls.addActionListener (this);
    Step = -1;
    Epoch = 0;
    Alpha = 0.2;
    add (Controls);
    add (Input);
    add (Soma);
    //add (Output);
    i = 0;
    Init ();
    Display ();
  }
  
  void Display () {
    Output.setThreshold (Threshold);
    Input.setThreshold (Threshold);
    Input.setWeight (0, Weight[0]);
    Input.setWeight (1, Weight[1]);
    Input.setOldWeight (0, Old[0]);
    Input.setOldWeight (1, Old[1]);
    Input.setX (0, X[0]);
    Input.setX (1, X[1]);
    Input.setDelta (0, Delta[0]);
    Input.setDelta (1, Delta[1]);
    Input.setEpoch (Epoch);
    Input.setStep (Step);
    Input.setTarget (Target);
    Input.setActual (Out);
    Input.setError (Error);
    Input.setErrors (Errors);
    Input.setPotential (Potential);
    Input.repaint();
    Soma.setWeight(0, Weight[0]);
    Soma.setWeight (1, Weight[1]);
    Soma.setThreshold (Threshold);
  }
  
  void Init () {
    Threshold = Math.random() - 0.5;
    Weight[0] = Math.random() - 0.5;
    Weight[1] = Math.random() - 0.5;
    Threshold = Math.random() / 2;
    Weight[0] = Math.random() / 2;
    Weight[1] = Math.random() / 2;
    X[0] = (Math.random() < .5)?0:1;
    X[1] = (Math.random() < .5)?0:1;
    Epoch = 0;
    Step = -1;
  }
  
  void LearnIt () {
    Epoch ();
    while (Errors != 0) {
      Epoch ();
    }
  }
  
  void Epoch () {
    Step ();
    while (Step != 3) {
      Step ();
    }
  }
  
  void Step () {
    ++Step;
    if (Step > 3) {
      Step = 0;
      ++Epoch;
      Errors = 0;
    }
    Learn (X1[Step], X2[Step], X1[Step] & X2[Step]);
    Display ();
  }
  
  void Learn (int X1, int X2, int Correct) {
    X[0] = X1;
    X[1] = X2;
    Target = Correct;
    Potential = X1 * Weight [0] + X2 * Weight [1];
    Out = (Potential > Threshold)?1:0;
    System.out.println ("X1        = " + X1);
    System.out.println ("Weight1   = " + Weight[0]);
    System.out.println ("X2        = " + X2);
    System.out.println ("Weight2   = " + Weight[1]);
    System.out.println ("Potential = " + Potential);
    System.out.println ("Threshold = " + Threshold);
    System.out.println ("Output = " + Out);
    Error = Correct - Out;
    //Error = Out - Correct;
    Errors += (Error == 0)?0:1;
    Old[0] = Weight [0];
    Old[1] = Weight [1];
    Delta[0] = Alpha * Weight [0] * Error;
    Delta[1] = Alpha * Weight [1] * Error;
    Weight [0] += Delta [0];
    Weight [1] += Delta [1];
  }

  public void actionPerformed (ActionEvent e) {
    String Action;
    Action = e.getActionCommand ();
    if (Action == "Initialize") {
      Init ();
    } else if (Action == "Step") {
      Step ();
    } else if (Action == "Epoch") {
      Epoch ();
    } else if (Action == "Learn") {
      LearnIt ();
    }
    Display ();
  }

}

class ControlPanel extends JPanel {
  private final JButton Initialize;
  private final JButton Step;
  private final JButton Epoch;
  private final JButton Learn;
  private final LayoutManager Layout;
  
  ControlPanel () {
    Layout = new BoxLayout (this, BoxLayout.Y_AXIS);
    setLayout (Layout);
    Initialize = new JButton ("Initialize");
    Step = new JButton ("Step");
    Epoch = new JButton ("Epoch");
    Learn = new JButton ("Learn");
    add (Initialize);
    add (Step);
    add (Epoch);
    add (Learn);
  }
  
  void addActionListener (ActionListener Handler) {
    Initialize.addActionListener (Handler);
    Step.addActionListener (Handler);
    Epoch.addActionListener (Handler);
    Learn.addActionListener (Handler);
  }
}

class NeuronIn extends JPanel {
  private final int[] X;
  double Weight[];
  private double Threshold;
  private final JLabel StepTag;
  private final JLabel StepValue;
  private final JLabel EpochTag;
  private final JLabel EpochValue;
  private final JLabel ThresholdTag;
  private final JLabel ThresholdValue;
  private final JLabel TargetTag;
  private final JLabel TargetValue;
  private final JLabel ActualTag;
  private final JLabel ActualValue;
  private final JLabel ErrorTag;
  private final JLabel ErrorValue;
  private final JLabel ErrorsTag;
  private final JLabel ErrorsValue;
  private final JLabel[] InputTag;
  private final JLabel[] InputValue;
  private final JLabel PotentialTag;
  private final JLabel PotentialValue;
  private final JLabel[] WeightTag;
  private final JLabel[] WeightValue;
  private final JLabel[] OldWeightTag;
  private final JLabel[] OldWeightValue;
  private final JLabel[] DeltaTag;
  private final JLabel[] DeltaValue;
  private final LayoutManager Layout;
  private int Step;
  private int Epoch;
  private int Target;
  private int Actual;
  private int Error;
  private int Errors;
  private double Potential;
  
  public NeuronIn () {
    int Count = 2;
    int i;
    Layout = new GridLayout (16, 2);
    setLayout (Layout);
    InputTag = new JLabel [2];
    InputValue = new JLabel [2];
    DeltaTag = new JLabel [2];
    DeltaValue = new JLabel [2];
    WeightTag = new JLabel [2];
    WeightValue = new JLabel [2];
    OldWeightTag = new JLabel [2];
    OldWeightValue = new JLabel [2];
    DeltaTag[0] = new JLabel ("Delta 1");
    DeltaTag[1] = new JLabel ("Delta 2");
    InputTag[0] = new JLabel ("Input 1");
    InputTag[1] = new JLabel ("Input 2");
    InputValue[0] = new JLabel ();
    InputValue[1] = new JLabel ();
    WeightTag[0] = new JLabel ("New Weight 1");
    WeightTag[1] = new JLabel ("New Weight 2");
    WeightValue[0] = new JLabel ();
    WeightValue[1] = new JLabel ();
    OldWeightTag[0] = new JLabel ("Old Weight 1");
    OldWeightTag[1] = new JLabel ("Old Weight 2");
    OldWeightValue[0] = new JLabel ();
    OldWeightValue[1] = new JLabel ();
    DeltaTag[0] = new JLabel ("Weight Delta 1");
    DeltaTag[1] = new JLabel ("Weight Delta 2");
    DeltaValue[0] = new JLabel ();
    DeltaValue[1] = new JLabel ();
    PotentialTag = new JLabel ("Potential");
    PotentialValue = new JLabel ();
    EpochTag = new JLabel ("Epoch");
    EpochValue = new JLabel ();
    StepTag = new JLabel ("Step");
    StepValue = new JLabel ();
    ThresholdTag = new JLabel ("Threshold");
    ThresholdValue = new JLabel ();
    TargetTag = new JLabel ("Desired Output");
    TargetValue = new JLabel ();
    ActualTag = new JLabel ("Actual Output");
    ActualValue = new JLabel ();
    ErrorTag = new JLabel ("Error");
    ErrorValue = new JLabel ();
    ErrorsTag = new JLabel ("Epoch Errors");
    ErrorsValue = new JLabel ();
    add (InputTag[0]);
    add (InputValue[0]);
    add (InputTag[1]);
    add (InputValue[1]);
    add (OldWeightTag[0]);
    add (OldWeightValue[0]);
    add (DeltaTag[0]);
    add (DeltaValue[0]);
    add (WeightTag[0]);
    add (WeightValue[0]);
    add (OldWeightTag[1]);
    add (OldWeightValue[1]);
    add (DeltaTag[1]);
    add (DeltaValue[1]);
    add (WeightTag[1]);
    add (WeightValue[1]);
    add (PotentialTag);
    add (PotentialValue);
    add (ThresholdTag);
    add (ThresholdValue);
    add (TargetTag);
    add (TargetValue);
    add (ActualTag);
    add (ActualValue);
    add (ErrorTag);
    add (ErrorValue);
    add (ErrorsTag);
    add (ErrorsValue);
    add (EpochTag);
    add (EpochValue);
    add (StepTag);
    add (StepValue);
    X = new int [Count];
//    Weight = new double [Count];
//    XLabel = new JLabel [Count];
//    WeightLabel = new JLabel [Count];
    i = 0;
/*    while (i < Count) {
      XLabel [i] = new JLabel();
      WeightLabel [i] = new JLabel();
      add (XLabel [i]);
      add (WeightLabel [i]);
      ++i;
    }*/
  }
  
  public void setX (int Selector, int Value) {
    X [Selector] = Value;
    InputValue [Selector].setText (Integer.toString(Value));
  }
  
  public void setTarget (int Value) {
    Target = Value;
    TargetValue.setText (Integer.toString(Value));
  }
  
  public void setActual (int Value) {
    Actual = Value;
    ActualValue.setText (Integer.toString(Value));
  }
  
  public void setError (int Value) {
    Error = Value;
    ErrorValue.setText (Integer.toString(Value));
  }
  
  public void setErrors (int Value) {
    Errors = Value;
    ErrorsValue.setText (Integer.toString(Value));
  }
  
  public void setPotential (double Value) {
    Potential = Value;
    PotentialValue.setText (Double.toString(Value));
  }
  
  public void setThreshold (double Value) {
    Threshold = Value;
    ThresholdValue.setText (Double.toString(Value));
  }
  
  public void setEpoch (int Value) {
    Epoch = Value;
    EpochValue.setText (Integer.toString(Value));
  }
  
  public void setStep (int Value) {
    Step = Value;
    StepValue.setText (Integer.toString(Value));
  }
  
  public void setWeight (int Selector, double Value) {
//    WeightValue [Selector] = Value;
    WeightValue [Selector].setText (Double.toString(Value));
  }

  public void setOldWeight (int Selector, double Value) {
//    WeightValue [Selector] = Value;
    OldWeightValue [Selector].setText (Double.toString(Value));
  }

  public void setDelta (int Selector, double Value) {
    DeltaValue [Selector].setText (Double.toString(Value));
  }
}

class NeuronOut extends JPanel {
  private final JLabel ThresholdTag;
  private final JLabel ThresholdValue;
  private double Threshold;
  
  public NeuronOut () {
    ThresholdTag = new JLabel("Threshold");
    ThresholdValue = new JLabel("");
    add (ThresholdTag);
    add (ThresholdValue);
    
  }
  
  public void setThreshold (double Value) {
    Threshold = Value;
    ThresholdValue.setText (Double.toString (Threshold));
  }
  
}
