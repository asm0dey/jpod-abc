package com.github.asm0dey.m4bconverter;

import com.google.common.collect.FluentIterable;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Iterables.partition;
import static com.google.common.collect.Lists.newArrayList;
import static java.io.File.separator;
import static org.apache.commons.io.FilenameUtils.getName;

/**
 * User: finkel Date: 12.10.13 Time: 14:07
 */
class M4aMerger {
	private static final DecimalFormat df = new DecimalFormat("0000");
	private int counter = 0;

	public File merge(FluentIterable<File> files) {
		ArrayList<File> concatenated = interConcat(files);
		while (concatenated.size() != 1) {
			concatenated = interConcat(concatenated);
		}
		File file = concatenated.get(0);
		File dest = new File(file.getParent() + separator + "book.m4a");
		if (dest.exists())
			dest.delete();
		file.renameTo(dest);
		return file;
	}

	private ArrayList<File> interConcat(Iterable<File> files) {
		Iterable<List<File>> partition = partition(files, 10);
		ArrayList<File> concatenated = newArrayList();
		for (List<File> fileList : partition) {
			concatenated.add(mergePartition(fileList));
		}
		return concatenated;
	}

	private File mergePartition(List<File> fileList) {
		CommandLine cl = CommandLine.parse("/usr/bin/MP4Box");
		File parent = fileList.get(0).getParentFile();
		for (File file : fileList) {
			cl.addArgument("-cat").addArgument(getName(file.getAbsolutePath()));
		}
		try {
			String absolutePath = MessageFormat.format("{0}{1}temp_{2}.m4a", parent.getAbsolutePath(), separator, df.format(++counter));
			cl.addArgument(getName(absolutePath));
			System.out.println("\n" + cl);
			DefaultExecutor executor = new DefaultExecutor();
			executor.setWorkingDirectory(fileList.get(0).getParentFile());
			DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
			executor.setStreamHandler(new PumpStreamHandler());
			executor.execute(cl, handler);
			handler.waitFor();
			for (File file : fileList) {
				file.delete();
			}
			return new File(absolutePath);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
}
