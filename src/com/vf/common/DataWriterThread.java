package com.vf.common;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DataWriterThread implements Runnable, DataWriter {

	private static DataWriterThread dataWriterThread;

	private String fileName;
	private BlockingQueue<Data> blockingQueue;
	private Thread thread;
	private volatile boolean stopThread;

	private DataWriterThread(String fileName, int capacity) {
		this.fileName = fileName;
		stopThread = false;
		blockingQueue = new LinkedBlockingQueue<Data>(capacity);
	}

	public static synchronized DataWriterThread startDataWriterThread(String fileName, int capacity) {
		if (dataWriterThread == null) {
			dataWriterThread = new DataWriterThread(fileName, capacity);
			dataWriterThread.thread = new Thread(dataWriterThread);
			dataWriterThread.thread.start();
		}
		return dataWriterThread;
	}

	public void stopThread() {
		stopThread = true;
		dataWriterThread.thread.interrupt();
	}

	@Override
	public void writeData(Data data) throws InterruptedException {
		blockingQueue.put(data);
	}

	@Override
	public void run() {
		RandomAccessFile output = null;
		try {
			output = new RandomAccessFile(this.fileName, "rw");

			while (!(stopThread && blockingQueue.isEmpty())) {
				try {
					Data data = blockingQueue.take();
					output.seek(data.getOffset());
					output.write(data.getOutputData());
				} catch (InterruptedException e) {
					Thread.interrupted();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
