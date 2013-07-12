package gov.nrel.bacnet.consumer.beans;

public class Counter {

	private int counter = 0;
	
	public synchronized int increment() {
		counter++;
		return counter;
	}
	
}
