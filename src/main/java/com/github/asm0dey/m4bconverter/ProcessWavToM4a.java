package com.github.asm0dey.m4bconverter;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: finkel Date: 11.10.13
 * Time: 19:22
 */
public class ProcessWavToM4a implements IWavToM4a {
	@Override
	public File decode(File wav) {
		CommandLine cl = CommandLine.parse("/usr/bin/neroAacEnc");
		final HashMap<String,String> hashMap = new HashMap<>();
		hashMap.put("if", FilenameUtils.getName(wav.getAbsolutePath()));
		final File m4aFile = new File(FilenameUtils.removeExtension(wav.getAbsolutePath()) + ".m4a");
		hashMap.put("of", FilenameUtils.getName(m4aFile.getAbsolutePath()));
		cl.setSubstitutionMap(hashMap);
		// cl.addArgument("-ignorelength");
		// cl.addArgument("-cbr ").addArgument(String.valueOf(128 * 1024));
		cl.addArgument("-if").addArgument("${if}");
		cl.addArgument("-of").addArgument("${of}");
		DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler() {
		};
		DefaultExecutor executor = new DefaultExecutor();
		executor.setStreamHandler(new PumpStreamHandler(null));
		executor.setWorkingDirectory(wav.getParentFile());
		try {
			executor.execute(cl, handler);
			handler.waitFor();
			wav.delete();
			return m4aFile;
		} catch (IOException | InterruptedException ex) {
			Logger.getLogger(ConverterMain.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}
}