package com.vf;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.vf.common.Data;
import com.vf.common.DataReaderContext;
import com.vf.common.DataReaderContext2;
import com.vf.common.DataWriter;
import com.vf.common.DataWriterThread;
import com.vf.common.FileHelper;
import com.vf.common.Progress;
import com.vf.common.ProgressThread;

public class Main {

	public static final Integer DEFAULT_NUMBER_OF_ROWS = Integer.valueOf(2000000);
	public static final int QUEUE_CAPACITY = 200;
	public static final String INPUT_FILE_NAME = "a.txt";
	public static final String OUTPUT_FILE_NAME = "b.txt";
	public static final String ENCODING = "UTF-8";
	public static final long THREAD_POOL_RESTART_PERIOD = 1000;
	public static final int THREAD_NUMBER = Runtime.getRuntime().availableProcessors() * 2;

	public static void main(String[] args) {
		System.setProperty("file.encoding", ENCODING);
		try {
			Integer numberOfRows = DEFAULT_NUMBER_OF_ROWS;
			BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));

			// Read number of rows from console
			System.out.format("Please select number of rows: [default(%d)] ->%n", DEFAULT_NUMBER_OF_ROWS);
			String rowNumberStr = bufferRead.readLine();
			try {
				numberOfRows = Integer.parseInt(rowNumberStr);
			} catch (Exception e) {
				// Ignore it
			}
			System.out.println("Choose task: [main/fork(default)] ->");
			String choice = bufferRead.readLine();

			System.out.println("Generate input file...");
			FileHelper.generateFile(INPUT_FILE_NAME, numberOfRows, ENCODING);

			System.out.println("Prepare result file...");
			FileHelper.copyResultFile(INPUT_FILE_NAME, OUTPUT_FILE_NAME);

			System.out.println("Starting progress thread");
			Progress progress = ProgressThread.startProgressThread(numberOfRows);

			System.out.println("Starting result writer thread");
			DataWriterThread dataWriter = DataWriterThread.startDataWriterThread(OUTPUT_FILE_NAME, QUEUE_CAPACITY);

			System.out.println("Staring calculation...");
			long start = System.currentTimeMillis();

			// test in main thread
			try {
				// testWorkflowInMainThread(progress, dataWriter);
				// testWorkflowInMainThread2(progress, dataWriter);
				if ("main".equals(choice)) {
					testWorkflowInMainThread3(progress, dataWriter);
				} else {
					testWorkflowInMainThread4(progress, dataWriter);
				}
				// Thread.sleep(2000);
			} catch (Exception e) {
				e.printStackTrace();
			}
			dataWriter.stopThread();

			long duration = System.currentTimeMillis() - start;
			System.out.println("Done in " + ((double) duration / 1000) + " seconds");
			System.out.println("Processed lines " + progress.getProgress() + "/" + progress.getTotalCount());

			// TODO implement it later
			// 2. Main task
			// System.out.println("Choose task: [main/fork] ->");
			// String choice = bufferRead.readLine();

			// Start calculator thread pool
			// Fork join???
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void testWorkflowInMainThread(Progress progress, DataWriter dataWriter) throws Exception {
		String NL = System.getProperty("line.separator");
		int NLL = NL.getBytes("UTF-8").length;

		long offset = 0;

		BufferedReader a = new BufferedReader(new InputStreamReader(new FileInputStream(INPUT_FILE_NAME), ENCODING));

		String str = null;
		while ((str = a.readLine()) != null) {
			Data data = new Data();
			data.setOutputData(new StringBuilder(str).reverse().toString().getBytes(ENCODING));
			data.setOffset(offset);
			dataWriter.writeData(data);
			progress.incrementProgress(1);
			int len = str.getBytes(ENCODING).length + NLL;
			offset = offset + len;
		}
		a.close();

	}

	private static void testWorkflowInMainThread2(final Progress progress, final DataWriter dataWriter) throws Exception {

		ReentrantLock lock = new ReentrantLock();
		Condition threadpoolRestartCondition = lock.newCondition();
		final DataReaderContext dataReaderContext = new DataReaderContext(INPUT_FILE_NAME, ENCODING, lock, threadpoolRestartCondition,
				THREAD_POOL_RESTART_PERIOD);

		new Thread() {
			public void run() {
				Data data = null;
				while ((data = dataReaderContext.readData()) != null) {
					try {
						data.setOutputData(new StringBuilder(data.getInputData()).reverse().toString().getBytes(data.getEncoding()));
						dataWriter.writeData(data);
					} catch (Exception e) {
						e.printStackTrace();
					}
					progress.incrementProgress(1);
				}
			}
		}.start();

		while (dataReaderContext.hasDataToRead()) {
			lock.lock();
			try {
				threadpoolRestartCondition.await();
			} finally {
				lock.unlock();
			}
		}

	}

	private static void testWorkflowInMainThread3(final Progress progress, final DataWriter dataWriter) {

		ReentrantLock lock = new ReentrantLock();
		Condition threadpoolRestartCondition = lock.newCondition();
		final DataReaderContext dataReaderContext = new DataReaderContext(INPUT_FILE_NAME, ENCODING, lock, threadpoolRestartCondition,
				THREAD_POOL_RESTART_PERIOD);

		ExecutorService processDataExecutor = Executors.newFixedThreadPool(THREAD_NUMBER);

		for (int i = 0; i < THREAD_NUMBER; i++) {
			processDataExecutor.execute(new DataProcessor(dataReaderContext, progress, dataWriter));
		}

		while (dataReaderContext.hasDataToRead()) {
			lock.lock();
			try {
				threadpoolRestartCondition.await();
				processDataExecutor.shutdownNow();
				if (!dataReaderContext.hasDataToRead()) {
					break;
				}
				// System.out.println("restarting...");
				processDataExecutor = Executors.newFixedThreadPool(THREAD_NUMBER);

				for (int i = 0; i < THREAD_NUMBER; i++) {
					processDataExecutor.execute(new DataProcessor(dataReaderContext, progress, dataWriter));
				}
			} catch (InterruptedException e) {
				processDataExecutor.shutdownNow();
				e.printStackTrace();
			} finally {
				lock.unlock();
			}
		}
		try {
			processDataExecutor.shutdownNow();
			processDataExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void testWorkflowInMainThread4(final Progress progress, final DataWriter dataWriter) {

		final DataReaderContext2 dataReaderContext = new DataReaderContext2(INPUT_FILE_NAME, ENCODING);

		ForkJoinPool pool = new ForkJoinPool(THREAD_NUMBER);

		while (dataReaderContext.hasDataToRead()) {
			pool.invoke(new DataProcessorSolver(dataReaderContext.readData(), progress, dataWriter));
		}

		// pool.shutdown();
		// try {
		// pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
	}

}

class DataProcessor implements Runnable {
	DataReaderContext dataReaderContext;
	Progress progress;
	DataWriter dataWriter;

	public DataProcessor(final DataReaderContext dataReaderContext, final Progress progress, final DataWriter dataWriter) {
		this.dataReaderContext = dataReaderContext;
		this.progress = progress;
		this.dataWriter = dataWriter;
	}

	@Override
	public void run() {
		boolean interruptFlag = false;
		Data data = null;
		while ((data = dataReaderContext.readData()) != null) {
			try {
				data.setOutputData(new StringBuilder(data.getInputData()).reverse().toString().getBytes(data.getEncoding()));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			while (true) {
				try {
					dataWriter.writeData(data);
					break;
				} catch (InterruptedException e) {
					Thread.interrupted();
					interruptFlag = true;
				}
			}
			progress.incrementProgress(1);
			if (interruptFlag) {
				break;
			}
		}
	}
}

class DataProcessorSolver extends RecursiveAction {

	private static int s = 0;

	private Data[] list;
	private Progress progress;
	private DataWriter dataWriter;

	public DataProcessorSolver(Data[] array, final Progress progress, final DataWriter dataWriter) {
		this.list = array;
		this.progress = progress;
		this.dataWriter = dataWriter;
	}

	@Override
	protected void compute() {
		if (list.length < 3) {
			// solve problem with two data items
			for (int i = 0; i < list.length; i++) {
				Data data = list[i];
				if (data != null) {
					try {
						data.setOutputData(new StringBuilder(data.getInputData()).reverse().toString().getBytes(data.getEncoding()));
					} catch (UnsupportedEncodingException e1) {
						e1.printStackTrace();
					}
					while (true) {
						try {
							dataWriter.writeData(data);
							break;
						} catch (InterruptedException e) {
						}
					}
					progress.incrementProgress(1);
				}

			}
		} else {
			int midpoint = list.length / 2;
			Data[] l1 = Arrays.copyOfRange(list, 0, midpoint);
			Data[] l2 = Arrays.copyOfRange(list, midpoint, list.length);
			DataProcessorSolver s1 = new DataProcessorSolver(l1, progress, dataWriter);
			DataProcessorSolver s2 = new DataProcessorSolver(l2, progress, dataWriter);
			invokeAll(s1, s2);
		}
	}
}
