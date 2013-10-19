package com.github.asm0dey.m4bconverter.cli;

import com.github.asm0dey.m4bconverter.gui.GuiMain;
import com.github.asm0dey.m4bconverter.model.Track;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.io.Files.write;
import static java.text.MessageFormat.format;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getName;

/**
 * 
 * @author finkel
 */
public class ConverterMain {

	private static final DecimalFormat df = new DecimalFormat("00000");
	private static final AtomicInteger toConvert = new AtomicInteger(0);

	public static void generateBook(int threadsNumber, Track... listFiles) throws InterruptedException {
		ExecutorService mp3ToWavThreadPool = Executors.newFixedThreadPool(threadsNumber, Executors.privilegedThreadFactory());
		final String sign = RandomStringUtils.randomAlphanumeric(10);
		int i = 0;
		printProgBar(0, "");
		final TreeSet<File> toMerge = new TreeSet<>();
		final int allBatchTasksCount = listFiles.length;
		for (final Track track : listFiles)
			mp3ToWavThreadPool.submit(new ConversionQueue(track.getFile(), ++i, sign, allBatchTasksCount, toMerge));
		mp3ToWavThreadPool.shutdown();
		// while (!mp3ToWavThreadPool.isTerminated()){}
		mp3ToWavThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		File m4a = M4aMerger.object$.mergeM4a(toMerge);
		List<String> strings = newArrayList();
		long currentTime = 0;
		for (Track listFile : listFiles) {
			strings.add(GuiMain.getBeautifulTime(currentTime) + " " + listFile.getTrack());
			currentTime += listFile.getLength();
		}
		File chapter = new File(m4a.getParent(), FilenameUtils.getBaseName(m4a.getAbsolutePath()) + ".chapters.txt");
		try {
			write(on('\n').join(strings).getBytes(), chapter);
		} catch (IOException e) {
			e.printStackTrace();
		}
		chapterFile(m4a);
	}

	private static void chapterFile(File m4a) {
		CommandLine cl = CommandLine.parse("/usr/bin/mp4chaps -i -Q ${m4a}");
		HashMap<String, String> substitutionMap = newHashMap();
		// substitutionMap.put("chapter",getBaseName(chapter.getAbsolutePath()));
		substitutionMap.put("m4a", getName(m4a.getAbsolutePath()));
		cl.setSubstitutionMap(substitutionMap);
		DefaultExecutor executor = new DefaultExecutor();
		DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
		executor.setStreamHandler(new PumpStreamHandler());
		executor.setWorkingDirectory(m4a.getParentFile());
		try {
			executor.execute(cl, handler);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			handler.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private static void printProgBar(int percent, String customMessage) {
		StringBuilder bar = new StringBuilder("[");

		for (int i = 0; i < 50; i++) {
			if (i < (percent / 2)) {
				bar.append("=");
			} else if (i == (percent / 2)) {
				bar.append(">");
			} else {
				bar.append(" ");
			}
		}

		bar.append("]   ").append(percent).append("%     ");
		bar.append(customMessage == null ? "" : customMessage);
		System.out.print("\r" + bar.toString());
	}

	private static class ConversionQueue implements Runnable {
		private final File source;
		private final int trackNumber;
		private final String sign;
		private final int tasksTotal;
		private final Collection<File> resultContainer;

		public ConversionQueue(File source, int trackNumber, String sign, int tasksTotal, Collection<File> resultContainer) {
			this.source = source;
			this.trackNumber = trackNumber;
			this.sign = sign;
			this.tasksTotal = tasksTotal;
			this.resultContainer = resultContainer;
		}

		@Override
		public void run() {
			File parentFile = source.getParentFile();
			String resultingFileName = format("{0}_{1}.m4a", df.format(trackNumber), sign);
			File buffer = null;
			try {
				buffer = File.createTempFile("buffer", "");
			} catch (IOException e) {
				e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
			}
			try {
				new ProcessBuilder(
						newArrayList(
								SystemUtils.IS_OS_WINDOWS ? "cmd" : "bash",
								SystemUtils.IS_OS_WINDOWS ? "/c" : "-c",
								format("/usr/bin/lame --decode {0}.mp3 -|neroAacEnc -if - -of {1}", getBaseName(source.getAbsolutePath()),
										resultingFileName))).directory(parentFile).inheritIO().redirectError(buffer == null ? null : buffer)
						.redirectInput(ProcessBuilder.Redirect.PIPE).redirectOutput(ProcessBuilder.Redirect.PIPE).start().waitFor();
				printProgBar(Double.valueOf((double) toConvert.incrementAndGet() / tasksTotal * 100).intValue(),
						format("\t[{0}.mp3 converted to {1}]", getBaseName(source.getAbsolutePath()), resultingFileName));
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
			if (buffer != null) {
				buffer.delete();
			}
			resultContainer.add(new File(parentFile, resultingFileName));
		}

	}
}
