package org.processmining.tests.bpmnmeasures;

import org.junit.Test;
import org.processmining.contexts.cli.CLI;
import org.processmining.contexts.test.PromTest;


public class BPMNMeasuresTest  extends PromTest {

	  @Test
	  public void testBPMN1() throws Throwable {
	    String args[] = new String[] {"-l"};
	    CLI.main(args);
	  }

	  @Test
	  public void testBPMN2() throws Throwable {
	    String testFileRoot = System.getProperty("test.testFileRoot", PromTest.defaultTestDir);
	    String args[] = new String[] {"-f", testFileRoot+"/BPMNMeasures_Example.txt"};
	    
	    CLI.main(args);
	  }
	  
	  public static void main(String[] args) {
	    junit.textui.TestRunner.run(BPMNMeasuresTest.class);
	  }
	  
	}
