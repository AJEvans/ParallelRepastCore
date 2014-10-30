/**
 * Agent
 *
 * Basic Parallel Model Code : Copyright (c) University of Leeds.  All rights reserved.
 * 
 * This code may be distributed in source and binary code, with or without
 * modification, provided that the copyright/licence statement here and above is 
 * reproduced, and versions developed outside the University of Leeds have attached 
 * one or more of the Open Source Licenses available at:
 * http://www.opensource.org/licenses/ 
 * This work is offered "as is" and without warranty.
 * 
**/
package uk.ac.leeds.ccg.modeling.parallelrepast.complex;

import uchicago.src.sim.gui.*;
import java.awt.Color;
import uchicago.src.sim.engine.CustomProbeable;
import mpi.*; 
import java.io.*;


/**
 * This is a simple Agent example used to run in a Repast-based model overMPI.<P>
 * @author <A href="http://www.geog.leeds.ac.uk/people/a.evans/">Andy Evans</A> and <A href="http://www.geog.leeds.ac.uk/people/h.parry/">Hazel Parry</A> 
 * @version 1.2
 */ 
public class Agent implements Serializable, Drawable, CustomProbeable {


	private int value = 1;
	private int x = 0;
	private int y = 0;
	private int id = 0; // Identification number for the individual agent. 
	public static int n = 0;  // Number that changes each time a new agent is created.


	/** 
	 * Sets the Agent's x and y coordinates in some space and creates and ID number.
	 * Agents also start with their <CODE>value</CODE> set to one.
	 **/
	public Agent(int x, int y) {
		this.x = x;
		this.y = y;
		this.id = n++;	
	}


        
        
        
	/**
	 * Increase the Agent's value by one.
	 **/
	public void incrementValue () {
		value++;
	}


        
        
        
	/**
	 * Get the Agent's current value.
	 * Agents start with a value of one.
	 **/
	public int getValue() {
		return value;
	}


        
        
        
	/**
	 * Get the Agent's current x coordinate.
	 **/
	public int getX() {
		return x;
	}


        
        
        
	/**
	 * Get the Agent's current y coordinate.
	 **/
	public int getY() {
		return y;
	}


        
        
        
	/**
	 * Sets the Agent's ID number.
	 **/
	public void setId( int id ) {
        	this.id = id;
	}
    

        
        
        
	/**
	 * Gets the Agent's ID number.
	 **/
   	 public int getId() {
      	  	return id;
   	 }


        
        
        
	/**
	 * Required by RePast - draws a rectangle on a given Graphics object.
	 **/
	public void draw(SimGraphics g) {
		int color = (value * 250) % 255;
        	g.drawFastRect(new Color(color,0,0));    		
	}   
  

        
        
        
	/**
	 * Required by RePast - returns the Strings "x" and "y".<P>
	 * To do:
	 * <UL>
	 * <LI>Should this return "value" and "ID" as well?</LI>
	 * </UL>
	 **/   
	public String[] getProbedProperties() {
		return new String[] {"x", "y"};
	}    

        
// End of Agent class.
}
