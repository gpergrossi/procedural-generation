package com.gpergrossi.util.task;

import java.util.function.LongSupplier;

public interface TimedIncrementalTask extends IncrementalTask {

	/**
	 * Work on the task for at least the suggested max work time.
	 * At a minimum, do the smallest non-zero amount of work towards completing the task.
	 */
	public void doTimedWork(LongSupplier timeSupplierMS, int suggestedMaxWorkTimeMS);
	
}
