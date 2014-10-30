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
package uk.ac.leeds.ccg.modeling.parallelrepast.complex;

import mpi.*;  
import uchicago.src.sim.engine.SimpleModel; 
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.Controller;
import uchicago.src.sim.gui.*;
import uchicago.src.sim.space.Object2DTorus;
import uchicago.src.reflector.ListPropertyDescriptor;
import java.io.*;
import java.util.*;
import java.awt.*;



/**
 * This is a simple example of how to run a Repast-based model overMPI.<P>
 * It essentially runs the Repast interface on node zero, while the rest 
 * of the model runs independent of Repast, and is synchronized by the  
 * the node zero code. 
 * This version includes a fixed number of agents that don't move.  At each time
 * step the agent lifestage increases and their colour changes accordingly.  
 * The agents are distributed from node 0 to the other processors where their 
 * lifestage is then increased.  The result is then returned to node 0.  </P>
 * <P>To do:</P>
 * <UL>
 * <LI>Not totally convinced the ints need wrapping in arrays for the MPI sends...</LI>
 * </UL>
 * @author <A href="http://www.geog.leeds.ac.uk/people/a.evans/">Andy Evans</A> and <A href="http://www.geog.leeds.ac.uk/people/h.parry/">Hazel Parry</A> 
 * @version 1.2
 */ 
public class Model extends SimpleModel { 
    
	/**
	 *
 	 * Changes:
	 * v.1.1: HP: 17 Aug 2004: Extracted sendComand method so isn't repeated in step() etc. Fixed modelIteration on node zero.
	 * v.1.1: AJE : 17 Aug 2004: Extracted sendComand matching code in atEnd().
     * v 1.2: AJE: 23 Aug 2004: Added example code to setup example agents and get some stuff back from them.
	 * v.1.2: HP : 30 Aug 2004: Altered getAgents() and setAgents() methods and minor 
	 *		            alterations to steps to enable correct agent passing 
	 *		   	    (as MPI.OBJECT). 
	 **/ 

	private int nodeRank = 0;
    private int numberOfNodes = 0;
    private int modelIteration = 0;
    private int totalNumberOfAgents = 10;
	private DisplaySurface dsurf; 
    private Object2DTorus world;
    private Object2DDisplay agentDisplay;
	private Agent[] localAgentList = null; 
    private int nodeChunkSize = 0;
	private int finalNodeChunkSize = 0;
	private int width = 300;
	private int height = 300; 

        
	/**
     * Constructor sets up model on node zero and sets up the MPI variables.<P>
	 * On node zero, this does all the setting up that Repast demands and 
 	 * sets the model so it knows the number of nodes and its own node number.  
	 * On other nodes, it just does the latter, but it also sets up a localAgentList 
     * which will contain an ~even split of Agents between the processors. 
   	**/
	public Model(int nodeRank, int numberOfNodes) {
		
		
		this.nodeRank = nodeRank;
		this.numberOfNodes = numberOfNodes;

		// Set size of agent arrays on various nodes. Note that if the 
		// numberOfNodes is not a factor of totalNumberOfAgents there
		// will be a remainder that is dealt with on the final node.

		nodeChunkSize = totalNumberOfAgents / (numberOfNodes - 1);			
		finalNodeChunkSize = nodeChunkSize + (totalNumberOfAgents % (numberOfNodes - 1));
                
		// For node zero, set up the model name and user interface.
		// for other nodes, set up the Agent list of an even size, 
		// but remember that if numberOfNodes is not a factor of totalNumberOfAgents
		// there is a remainder that needs to be taken up by the last node.
 
		if (nodeRank == 0) {

			name = "Model";
			localAgentList = new Agent[totalNumberOfAgents]; 
                        
				// AJE XXXX It's a bit clunky to have this as well as the
				// arrayList - ideally the arrayList should be used, 
				// but need to work out the equivalent to arraycopy.
			

		} else if (nodeRank < (numberOfNodes - 1)) {

			localAgentList = new Agent[nodeChunkSize];
		

		} else { // final node

			localAgentList = new Agent[finalNodeChunkSize];

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

				case (1) :
					buildModel();
					break;
				
				case (2) :
					preStep();
					break;

				case (3) :
					step();
					break;

				case (4) :
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
	 * This is needed by Repast - it sets up the initial display.
     * This is only done on node zero, as all other nodes wait 
     * at the constructor code.
	**/      
    	public void setup() {

	  	super.setup();   
		dsurf = new DisplaySurface(this, "Model Display" );
	        registerDisplaySurface("Model Display", dsurf);            

    	}       


        
        
        
    /** 
	 * This is needed by Repast - it sets up the initial model.
     * This is only done on node zero, as all other nodes wait 
     * at the constructor code.
	**/      
	public void buildModel() {
		

		// If node zero, build the world and send out agents.

		if (nodeRank == 0) { 	
                    
			sendCommand(1);	// Tell other nodes to buildModel.
			world = new Object2DTorus(width, height);

                        // Build the Agents and store them on node zero for the mo.
                        
			for ( int i = 0; i < totalNumberOfAgents; i++ ) {
                		int x = (int) (Math.random() * (double) width);
                		int y = (int) (Math.random() * (double) height);
                		Agent agent = new Agent(x, y);
                		agentList.add(agent);
				localAgentList[i] = agent;	// This double set of arrays is clunky.
				world.putObjectAt(x, y, agent);
            		}
			
			buildDisplay();
			setAgents(localAgentList); // Sends agents out to nodes.
			
			
			
		} else {
                    
			// If other nodes, get in sent agents.
                    
			int size = localAgentList.length;					
			try {
 				MPI.COMM_WORLD.Recv(localAgentList, 0, size, MPI.OBJECT, 0, 50);
			} catch (MPIException mpiE) {
				mpiE.printStackTrace();
			}
			
			for (int i = 0; i < localAgentList.length; i++) {			
				System.out.println("node = " + nodeRank + ": Agent " + localAgentList[i].getId() + " Value = " + localAgentList[i].getValue() + " RECEIVED ");
				
			}
		}


     	} // End of buildModel.
 
 


   	
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
			sendCommand(2);
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
                    
			sendCommand(3); // Run step.
                        
		} else {

			// Do something on each other node (in this case, increment the
                        // agents internal value).

			for (int i = 0; i < localAgentList.length; i++) {
				localAgentList[i].incrementValue();
				System.out.println("Agent " + localAgentList[i].getId() + " value = " + localAgentList[i].getValue());
			}

		}

		// This is where you'd do the step work on all the nodes 
		// or all the nodes except zero, depending on whether you want 
  	 	// to use zero for processing.

		System.out.println("Step done on processor " + nodeRank + " for model iteration " + modelIteration);

     } // End of step.





	/**
	 * This is done after step.
	 * On node zero, the method sends out messages to 
   	 * the other nodes telling them to postStep(). Remember that these nodes are waiting 
 	 * in the waitForCommands method, and this is where this message should be picked up. 
	 * That method will then call this method on nodes greater than zero. 
	**/  
	public void postStep() {
		
		// In this example, we get back a value from each Agent 
		// on the other nodes and display it.
		
		if (nodeRank == 0) {
					
			sendCommand(4);  // Run post-step.   
						
		} else {								
			try {
				MPI.COMM_WORLD.Send(localAgentList, 0, localAgentList.length, MPI.OBJECT, 0, 50);
				System.out.println("Processor " + nodeRank + " sending agents to node 0");				
			} catch (MPIException mpiE) {
				mpiE.printStackTrace();
			} 

		}
		if (nodeRank == 0) {			
			getAgents(localAgentList);		
		
				dsurf.updateDisplay();
		}
	
			

		// This is where you'd do the poststep work on all the nodes 
		// or all the nodes except zero, depending on whether you want 
  	 	// to use zero for processing.

		System.out.println("poststep done on processor " + nodeRank + " for model iteration " + modelIteration);

    } // End of postStep.




        
    /**
     * Builds the basic model-display Objects.
    **/
	private void buildDisplay() {
        	agentDisplay = new Object2DDisplay( world );
		//agentDisplay.reSize(300,300);	
        	agentDisplay.setObjectList( agentList );             
        	dsurf.addDisplayableProbeable ( agentDisplay, "Agents" );   
        	addSimEventListener(dsurf);
		dsurf.display();                
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
	 * Sends out Agents to nodes from node zero.<P>
	 * To do:
	 * <UL>
	 * <LI> Should be generalized to take in and get back object arrays.</LI>
	 * </UL>
	 **/
	public void setAgents(Agent[] list) {

		int size = nodeChunkSize;

		for (int i = 1; i < numberOfNodes; i++) {

			if (i == numberOfNodes - 1) {
				size = finalNodeChunkSize;
			}

			Agent[] listToSend = new Agent[size];
			System.arraycopy(list, (nodeChunkSize * (i - 1)),
				 listToSend, 0, size);
			try {
				MPI.COMM_WORLD.Send(listToSend, 0, size, MPI.OBJECT, i, 50);
				for (int j = 0; j <listToSend.length; j++) {
					System.out.print("Processor " + i + ":");
					System.out.println(" agent " + listToSend[j].getId() + " value " + listToSend[j].getValue()); 
				}
				System.out.println("sending agent array to processor " + i);
			} catch (MPIException mpiE) {
				mpiE.printStackTrace();
			} 


		}

	} // End setAgents.


        
        
        
	/**
	 * Gets Agents to node zero from other nodes.<P>
	 * To do:
	 * <UL>
	 * <LI> Should be generalized to take in and get back object arrays.</LI>
	 * </UL>
	 **/
	public void getAgents(Agent[] list) {	
		
		int size = nodeChunkSize;

		for (int i = 1; i < numberOfNodes; i++) {
			
			if (i == (numberOfNodes - 1)) {
				size = finalNodeChunkSize;
				}

			Agent[] listToGet = new Agent[size];

			try {	
 				MPI.COMM_WORLD.Recv(listToGet, 0, size, MPI.OBJECT, i, 50);
				
			} catch (MPIException mpiE) {
				mpiE.printStackTrace();
			}

			System.arraycopy(listToGet, 0, list, 
				(nodeChunkSize * (i - 1)), size);

		}
		agentList.clear();

			for ( int i = 0; i < totalNumberOfAgents; i++ ) {
                		Agent agent = list[i];
				//System.out.println("adding to agentList agent " + agent.getId());
                		agentList.add(agent);
            		}
			
				
			agentList.trimToSize();
                        
	} // End getAgents.

        
        
        
        
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
