/**
 * Class TextAreaPrintStream
 * extends PrintStream.
 * A custom made PrintStream which overrides methods println(String)
 * and print(String).
 * Thus, when the out stream is set as this PrintStream (with System.setOut
 * method), all calls to System.out.println(String) or System.out.print(String)
 * will result in an output stream of characters in the JTextArea given as an
 * argument of the constructor of the class.
 **/

package com.github.asm0dey.m4bconverter.gui;

import javax.swing.*;
import java.io.OutputStream;
import java.io.PrintStream;

public class TextAreaPrintStream extends PrintStream {

	// The JTextArea to wich the output stream will be redirected.
	private JTextArea textArea;

	/**
	 * Method TextAreaPrintStream The constructor of the class.
	 * 
	 * @param the
	 *            JTextArea to wich the output stream will be redirected.
	 * @param a
	 *            standard output stream (needed by super method)
	 **/
	public TextAreaPrintStream(JTextArea area, OutputStream out) {
		super(out);
		textArea = area;
	}

	/**
	 * Method println
	 * 
	 * @param the
	 *            String to be output in the JTextArea textArea (private attribute of the class). After having printed such a String, prints a new
	 *            line.
	 **/
	public void println(String string) {
		textArea.append(string + "\n");
	}

	/**
	 * Method print
	 * 
	 * @param the
	 *            String to be output in the JTextArea textArea (private attribute of the class).
	 **/
	public void print(String string) {
		textArea.append(string);
	}
}