package com.github.asm0dey.m4bconverter.cli;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static java.text.MessageFormat.format;
import static org.apache.commons.io.FilenameUtils.getBaseName;

/**
 * 
 * @author finkel
 */
public class ConverterMain {

	private static final DecimalFormat df = new DecimalFormat("00000");
	private static final AtomicInteger toConvert = new AtomicInteger(0);

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) throws IOException, InterruptedException {

		File a = new File(args[0]);
		final File parentFile = a.getParentFile();
		final File[] listFiles = parentFile.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return FilenameUtils.getExtension(pathname.getAbsolutePath()).equalsIgnoreCase("mp3");
			}
		});
		Arrays.sort(listFiles);
		ExecutorService mp3ToWavThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2,
				Executors.privilegedThreadFactory());
		final String sign = RandomStringUtils.randomAlphanumeric(10);
		int i = 0;
		printProgBar(0, "");
		final TreeSet<File> toMerge = new TreeSet<>();
		final int allBatchTasksCount = listFiles.length;
		for (final File file : listFiles)
			mp3ToWavThreadPool.submit(new ConversionQueue(file, ++i, sign, allBatchTasksCount, toMerge));
		mp3ToWavThreadPool.shutdown();
		// while (!mp3ToWavThreadPool.isTerminated()){}
		mp3ToWavThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		File merge = new M4aMerger().merge(from(toMerge));
		System.out.println("merge = " + merge);
	}

	private static void convertMP3ToWav(File input, File output) {
		new LameMp3ToWavDecoder().decode(input, output);
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

	private static File convertWavToM4a(final File wav) {
		return new ProcessWavToM4a().decode(wav);
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
										resultingFileName))).directory(parentFile).inheritIO().redirectError(buffer)
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
		/*
		 * @Override public void run() { final String tempWavName = source.getParent() + File.separator + df.format(trackNumber) + "-" + sign +
		 * ".wav"; final File wav = new File(tempWavName); printProgBar(Double.valueOf((double) toConvert.get() / tasksTotal * 100).intValue(),
		 * format("\t[Converting {0} to wav]", getName(source.getAbsolutePath()))); convertMP3ToWav(source, wav); printProgBar(Double.valueOf((double)
		 * toConvert.incrementAndGet() / tasksTotal * 100).intValue(), format("\t[Converting {0} to m4a]", getName(wav.getAbsolutePath()))); File m4a
		 * = convertWavToM4a(wav); printProgBar(Double.valueOf((double) toConvert.incrementAndGet() / tasksTotal * 100).intValue(),
		 * format("\t[Converted {0} to m4a]", getName(wav.getAbsolutePath()))); resultContainer.add(m4a);
		 * 
		 * }
		 */

	}
}
