package com.vf.common;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.UUID;

public class FileHelper {

	public static void generateFile(String filePath, Integer numberOfRows, String encoding) {
		File file = new File(filePath);
		if (file.exists() && file.isDirectory()) {
			throw new IllegalArgumentException("File expected");
		}
		if (file.exists()) {
			file.delete();
		}
		try {
			BufferedWriter buffWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));

			for (int i = 0; i < numberOfRows; i++) {
				buffWriter.write(UUID.randomUUID().toString());
				buffWriter.newLine();
			}

			buffWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void copyResultFile(String inputFilePath, String resultFilePath) {
		File sourceFile = new File(inputFilePath);
		File destFile = new File(resultFilePath);
		try {
			if (!destFile.exists()) {
				destFile.createNewFile();
			}

			FileChannel source = null;
			FileChannel destination = null;
			try {
				source = new FileInputStream(sourceFile).getChannel();
				destination = new FileOutputStream(destFile).getChannel();
				destination.transferFrom(source, 0, source.size());
			} finally {
				if (source != null) {
					source.close();
				}
				if (destination != null) {
					destination.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
