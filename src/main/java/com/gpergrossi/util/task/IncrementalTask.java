package com.gpergrossi.util.task;

public interface IncrementalTask {

	/**
	 * Do the smallest non-zero amount of work towards completing the task
	 */
	public void doWork();
	
	/**
	 * Query to see if the work to be done is complete
	 * @return true of finished, else false
	 */
	public boolean isFinished();
	
}
