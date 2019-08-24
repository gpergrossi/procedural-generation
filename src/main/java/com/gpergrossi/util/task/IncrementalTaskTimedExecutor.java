package com.gpergrossi.util.task;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

public class IncrementalTaskTimedExecutor implements TimedIncrementalTask {

	private final BooleanSupplier isFinishedFunction;
	private final Runnable workFunction;
	
	public IncrementalTaskTimedExecutor(IncrementalTask incrementalTask) {
		this.workFunction = incrementalTask::doWork;
		this.isFinishedFunction = incrementalTask::isFinished;
	}
	
	public IncrementalTaskTimedExecutor(Runnable workFunction, BooleanSupplier isFinishedFunction) {
		this.workFunction = workFunction;
		this.isFinishedFunction = isFinishedFunction;
	}
	
	@Override
	public void doWork() {
		workFunction.run();
	}

	@Override
	public boolean isFinished() {
		return isFinishedFunction.getAsBoolean();
	}
	
	@Override
	public void doTimedWork(LongSupplier timeSupplierMS, int suggestedMaxWorkTimeMS) {
		if (this.isFinished()) return;
		
		if (suggestedMaxWorkTimeMS <= 0) {
			this.doWork();
			return;
		}
		
		long currentTime = timeSupplierMS.getAsLong();
		long stopTime = timeSupplierMS.getAsLong() + suggestedMaxWorkTimeMS;
		
		// Edge case: long value wrap-around
		if (stopTime < currentTime) {
			do {
				this.doWork();
				if (this.isFinished()) return;
				currentTime = timeSupplierMS.getAsLong();
			} while (currentTime > 0);
			
			if (currentTime >= stopTime) return;
		}
		
		// Regular operation
		do {
			this.doWork();
			if (this.isFinished()) return;
		} while (timeSupplierMS.getAsLong() < stopTime);
	}
	
}
