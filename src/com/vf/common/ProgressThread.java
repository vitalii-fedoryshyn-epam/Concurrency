package com.vf.common;

import java.util.concurrent.atomic.AtomicInteger;

public class ProgressThread implements Runnable, Progress {

	private static final long DEFAULT_SLEEP_TIME = 2000;

	private static ProgressThread progressThread;

	// private Thread thread;
	private int totalCount;
	private AtomicInteger progress = new AtomicInteger();
	private long sleepTime;

	private ProgressThread(int totalCount) {
		this.totalCount = totalCount;
		this.sleepTime = DEFAULT_SLEEP_TIME;
	}

	public void incrementProgress(int delta) {
		progress.addAndGet(delta);
	}

	public static synchronized ProgressThread startProgressThread(int totalCount) {
		if (progressThread == null) {
			progressThread = new ProgressThread(totalCount);
			Thread thread = new Thread(progressThread);
			// progressThread.thread = thread;
			thread.setDaemon(true);
			thread.start();
		}
		return progressThread;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(this.sleepTime);
			} catch (InterruptedException e) {
				// Ignore it. Thread is demon
			}
			System.out.println("Current progress " + progress.get() + "/" + totalCount);
		}
	}

	@Override
	public int getProgress() {
		return progress.get();
	}

	@Override
	public int getTotalCount() {
		return totalCount;
	}

}
