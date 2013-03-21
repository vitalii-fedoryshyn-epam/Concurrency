package com.vf.common;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class DataReaderContext2 {

	private volatile boolean hasMoreData = true;
	private BufferedReader dataReader;
	private final String ENCODING;
	private final String NL;
	private int NLL;
	private long offset = 0;
	private final int DATA_PACKET_SIZE = 1000;

	public DataReaderContext2(String fileName, String encoding) {
		this.ENCODING = encoding;
		this.NL = System.getProperty("line.separator");
		try {
			this.NLL = NL.getBytes(ENCODING).length;
			dataReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), encoding));
		} catch (Exception e) {
			close();
			e.printStackTrace();
		}
	}

	public synchronized Data[] readData() {
		Data[] dataPachet = null;
		if (!hasDataToRead()) {
			return dataPachet;
		}
		dataPachet = new Data[DATA_PACKET_SIZE];

		for (int i = 0; i < DATA_PACKET_SIZE; i++) {
			String str;
			try {
				str = dataReader.readLine();
				if (str == null) {
					close();
					break;
				}
				long currentOffet = offset;
				int len = str.getBytes(ENCODING).length + NLL;
				offset = offset + len;
				Data data = new Data();
				data.setEncoding(ENCODING);
				data.setInputData(str);
				data.setOffset(currentOffet);
				dataPachet[i] = data;
			} catch (IOException e) {
				close();
				e.printStackTrace();
				break;
			}
		}
		return dataPachet;
	}

	public void close() {
		hasMoreData = false;
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
