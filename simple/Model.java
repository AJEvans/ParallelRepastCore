/**
 * Model
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
package uk.ac.leeds.ccg.modelling.parallelrepast.simple;

import mpi.*;
import uchicago.src.sim.engine.SimpleModel;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.Controller;
import java.io.*;
import java.util.*;
import java.awt.Color;


/**
 * This is a simple example of how to run a Repast-based model overMPI.<P>
 * It essentially runs the Repast interface on node zero, while the rest 
 * of the model runs independent of Repast, and is synchronized by the  
 * the node zero code.</P>
 * <P>To do:</P>
 * <UL>
 * <LI>Hazel is still working this architecture to fruition, but it seems likely to work.</LI>
 * <LI>Not totally convinced the ints need wrapping in arrays for the MPI sends...</LI>
 * </UL>
 * @author <A href="http://www.geog.leeds.ac.uk/people/a.evans/">Andy Evans</A> and <A href="http://www.geog.leeds.ac.uk/people/h.parry/">Hazel Parry</A> 
 * @version 1.1
 */ 
public class Model extends SimpleModel { 
    
	/**
	 *
 	 * Changes:
	 * v.1.1: HP: 17 Aug 2004: Extracted sendComand method so isn't repeated in step() etc. Fixed modelIteration on node zero.
	 * v.1.1: AJE : 17 Aug 2004: Extracted sendComand matching code in atEnd().
	 *
	**/ 

	private int nodeRank = 0;
    private int numberOfNodes = 0;
	private int modelIteration = 0;
    	

	/**
	 * Constructor sets up model on node zero and sets up the MPI variables.<P>
	 * On node zero, this does all the setting up that Repast demands and 
 	 * sets the model so it knows the number of nodes and its own node number.  
	 * On other nodes, it just does the latter. 
   	**/
	public Model(int nodeRank, int numberOfNodes) {

		this.nodeRank = nodeRank;
		this.numberOfNodes = numberOfNodes;

		// For node zero, set up the model name and user interface.

		if (nodeRank == 0) {

			name = "Model";
			params = new String[] {"RepastParams"};

		} 
    	}





	/** 
	 * This causes the code to go into a loop in which it awaits commands.
     * This should only be called on node numbers greater than 0. It sets 
     * the model waiting for messages from node zero. The messages contain 
     * an integer which sets methods running. For example, "1" might set the 
     * <CODE>preStep()</CODE> method running. As Repast can't call the <CODE>preStep()</CODE>
	 * method itself directly, and we can't(?) do method calls across processors, 
     * this method allows node zero to use variable setting to initiate method 
	 * calls on other processors.<P>
	 * <P>To do</P>
	 * <UL>
	 * <LI>Not totally convinced the ints need wrapping in arrays...</LI>
	 * </UL>
    **/ 
	public void waitForCommands () {

		int whatToDo = 0;
		int[] whatToDoArray = new int[1];
		whatToDoArray[0] = 0;

		// Loop until whatToDo equal to a message to end - in this case "9".

		while (whatToDo != 9) {

			// Wait for a message from node zero.

			try {

			MPI.COMM_WORLD.Recv(whatToDoArray, 0, 1,  MPI.INT, 0, 50);

			} catch (MPIException mpiE) {
				mpiE.printStackTrace();
			} 

			// Convert message into int.

			try {

				whatToDo = whatToDoArray[0];

			} catch (Exception e) {
				e.printStackTrace();
				whatToDoArray[0] = 0;
				whatToDo = 0;
			}

			// Call methods as you feel fit...

			switch (whatToDo) {

				// Keep running.

				case (0) :
					break;

				// preStep.
				
				case (1) :
					preStep();
					break;

				case (2) :
					step();
					break;

				case (3) :
					postStep();
					break;

				case (9) :
					atEnd();
					break;

			} // End of switch.

		} // End of while.
	      
	} // End of waitForCommands.





	/**
	 * From node 0, this method sends out messages to the other nodes telling them to 
	 * preStep(), step() or postStep() etc, depending on int whatToDo passed in. Remember that  
	 * these nodes are waiting in the waitForCommands method, and this is where this message 
   	 * should be picked up.<BR> 
	 * The waitForCommands method will then call this method on nodes greater than zero. 
	**/  
	public void sendCommand(int whatToDo) {

		int[] whatToDoArray = new int[whatToDo];
		whatToDoArray[0] = whatToDo;

		for (int i = 1; i < numberOfNodes; i++) {

	    		try {

     		    		MPI.COMM_WORLD.Send(whatToDoArray, 0, 1,  MPI.INT, i, 50);	

			} catch (MPIException mpiE) {
					
				mpiE.printStackTrace();

			} catch (Exception e) {
					
				e.printStackTrace();

			}
		   	    
		}  // end for

	} // end what to do





	/** 
	 * This is needed by Repast.
	**/      
    	public void setup() {

	  	super.setup();           

    	}       


    /** 
	 * This is needed by Repast.
	**/      
	public void buildModel() {

     	} 
 
 


   	
	/**
	 * This is done before the step.
	 * On node zero, the method sends out messages to 
   	 * the other nodes telling them to preStep(). Remember that these nodes are waiting 
 	 * in the waitForCommands method, and this is where this message should be picked up. 
	 * That method will then call this method on nodes greater than zero. 
	**/  
	public void preStep() {

		// Increase model iteration counter in the first method called, 
		// either this or step. 

        	modelIteration++;


		if (nodeRank == 0) {
			sendCommand(1);
		}

		// This is where you'd do the preStep work on all the nodes 
		// or all the nodes except zero, depending on whether you want 
  	 	// to use zero for processing.

		System.out.println("Prestep done on processor " + nodeRank + " for model iteration " + modelIteration);
	
    
	} // End of preStep.
    




	/**
	 * This is done after pre-step and before the step.
	 * On node zero, the method sends out messages to 
   	 * the other nodes telling them to step(). Remember that these nodes are waiting 
 	 * in the waitForCommands method, and this is where this message should be picked up. 
	 * That method will then call this method on nodes greater than zero. 
	**/  
    	public void step() {

		if (nodeRank == 0) {
			sendCommand(2);
		}

		// This is where you'd do the step work on all the nodes 
		// or all the nodes except zero, depending on whether you want 
  	 	// to use zero for processing.

		System.out.println("Step done on processor " + nodeRank + " for model iteration " + modelIteration);

     	}





	/**
	 * This is done after step.
	 * On node zero, the method sends out messages to 
   	 * the other nodes telling them to postStep(). Remember that these nodes are waiting 
 	 * in the waitForCommands method, and this is where this message should be picked up. 
	 * That method will then call this method on nodes greater than zero. 
	**/  
    	public void postStep() {

		if (nodeRank == 0) {
			sendCommand(3);
		}

		// This is where you'd do the step work on all the nodes 
		// or all the nodes except zero, depending on whether you want 
  	 	// to use zero for processing.

		System.out.println("poststep done on processor " + nodeRank + " for model iteration " + modelIteration);

     	}





	/**
 	 * This is called by Repast at the end of the model.
	 * On node zero, this signals to the other nodes to exit processing and 
	 * it shuts down MPI. On other nodes, where this is called by waitForCommands, 
	 * this exits the process.
	**/ 
	public void atEnd() {

		// Send a "shutdown" message to other nodes.

		if (nodeRank == 0) {
			sendCommand(9);
		} 
		
		// On all nodes, shut down MPI.

		try {

         		MPI.Finalize();
        	
		} catch (MPIException mpiE){

          		mpiE.printStackTrace();

     		}
		
		// On nodes greater than zero, exit process.

		if (nodeRank != 0) {
			System.exit(0);
		} 

	} // End of atEnd().





    /** 
	 * On node zero, this code creates an instance of the SimInit class (the same class with which
     * you can load your models on the command line), and an instance of the model itself.
     * It then uses SimInit to load the model via the loadModel method. The second and 
     * third parameters of the loadModel method specify a parameter file and whether or 
     * not the model is a batch model. For other nodes, it just makes an object of this class and 
	 * puts it into waitForCommands mode. Note the local variables to get around the problem of 
	 * static variables and MPI being needed before and after the construction process. 
	 * @param args String[] Number of nodes to run on? Set by prunjava.
    **/     
    	public static void main(String [] args) throws MPIException {    
 
		MPI.Init(args);
        	int nodeRanklocal = MPI.COMM_WORLD.Rank();
        	int numberOfNodes = MPI.COMM_WORLD.Size(); 
  
		if (nodeRanklocal == 0) {

 	       	SimInit init = new SimInit();
	        	Model model = new Model(nodeRanklocal, numberOfNodes);
        		init.loadModel(model, null, false );
            
		} else {

			Model model = new Model(nodeRanklocal, numberOfNodes);
			model.waitForCommands();
			
		}

    	}

// End of Model.
}
