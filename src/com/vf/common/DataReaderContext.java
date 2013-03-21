package com.vf.common;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class DataReaderContext {

	private volatile boolean hasMoreData = true;
	private BufferedReader dataReader;
	private final String ENCODING;
	private final String NL;
	private int NLL;
	private long offset = 0;
	private final long RESTART_EVERY;
	private AtomicLong count = new AtomicLong();
	private ReentrantLock lock;
	private Condition threadpoolRestartCondition;

	public DataReaderContext(String fileName, String encoding, ReentrantLock lock, Condition threadpoolRestartCondition, long restartEvery) {
		this.ENCODING = encoding;
		this.NL = System.getProperty("line.separator");
		this.RESTART_EVERY = restartEvery;
		this.lock = lock;
		this.threadpoolRestartCondition = threadpoolRestartCondition;
		try {
			this.NLL = NL.getBytes(ENCODING).length;
			dataReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), encoding));
		} catch (Exception e) {
			close();
			e.printStackTrace();
		}
	}

	public Data readData() {
		Data data = null;

		try {
			String str = null;
			long currentOffet = 0;
			synchronized (this) {
				if (!hasDataToRead()) {
					return data;
				}
				str = dataReader.readLine();
				currentOffet = offset;
				int len = str.getBytes(ENCODING).length + NLL;
				offset = offset + len;
			}
			data = new Data();
			data.setEncoding(ENCODING);
			data.setInputData(str);
			data.setOffset(currentOffet);
			if (count.incrementAndGet() % RESTART_EVERY == 0) {
				signalThreadPoolRestart();
			}

		} catch (IOException e) {
			close();
			e.printStackTrace();
		} catch (NullPointerException e) {
			close();
			// e.printStackTrace();
		}
		return data;
	}

	private void signalThreadPoolRestart() {
		lock.lock();
		try {
			threadpoolRestartCondition.signalAll();
//			System.out.println("notify " + count.get());
		} finally {
			lock.unlock();
		}
	}

	public void close() {
		hasMoreData = false;
		signalThreadPoolRestart();
		try {
			dataReader.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public boolean hasDataToRead() {
		return hasMoreData;
	}

}
