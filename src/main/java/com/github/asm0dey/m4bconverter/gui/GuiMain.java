package com.github.asm0dey.m4bconverter.gui;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.AdvancedTableModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import chrriis.dj.nativeswing.swtimpl.NativeInterface;
import chrriis.dj.nativeswing.swtimpl.components.JFileDialog;
import com.github.asm0dey.m4bconverter.cli.ConverterMain;
import com.github.asm0dey.m4bconverter.model.Track;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;

import static java.text.MessageFormat.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FilenameUtils.getName;

public class GuiMain {

	private JFrame frame;
	private EventList<Track> eventList = new BasicEventList<>();
	private JLabel overallLength = new JLabel();
	private long length = 0;

	/**
	 * Create the application.
	 */
	public GuiMain() {
		initialize();
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException,
			IllegalAccessException {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		NativeInterface.open();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GuiMain window = new GuiMain();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		NativeInterface.runEventPump();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		SortedList<Track> tracks = new SortedList<Track>(eventList, new Comparator<Track>() {
			@Override
			public int compare(Track o1, Track o2) {
				return o1.getFileName().compareTo(o2.getFileName());
			}
		});
		frame.setBounds(100, 100, 800, 600);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		Container contentPane = frame.getContentPane();
		contentPane.setLayout(new MigLayout(new LC().wrapAfter(3), new AC().grow(100, 0).align("c")));
		JLabel title = new JLabel("JPod Audiobook converter");
		contentPane.add(title);
		contentPane.add(overallLength);
		contentPane.add(new JButton("Run"));
		AdvancedTableModel<Track> model = GlazedListsSwing.eventTableModelWithThreadProxyList(tracks, new TrackTableFormat());
		JTable table = new JTable(model);
		TableComparatorChooser<Track> sorter = TableComparatorChooser.install(table, tracks, TableComparatorChooser.MULTIPLE_COLUMN_MOUSE);
		JScrollPane scrollPane = new JScrollPane(table);
		contentPane.add(scrollPane, new CC().span(3).grow().width("100%"));
		createMenu();
	}

	private void createMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenuItem exit = new JMenuItem("Exit");
		JMenuItem addFiles = new JMenuItem("Add files", KeyEvent.CTRL_MASK | KeyEvent.VK_O);
		addFiles.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectFiles();
			}
		});
		file.add(addFiles);
		file.addSeparator();
		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		file.add(exit);
		menuBar.add(file);
		frame.setJMenuBar(menuBar);
	}

	private void selectFiles() {

		JFileDialog dialog = new JFileDialog();
		dialog.setDialogType(JFileDialog.DialogType.OPEN_DIALOG_TYPE);
		dialog.setSelectionMode(JFileDialog.SelectionMode.MULTIPLE_SELECTION);
		dialog.setExtensionFilters(new String[] { "*.mp3" }, new String[] { "mp3 file" }, 0);
		dialog.show(frame);
		String[] selectedFileNames = dialog.getSelectedFileNames();
		String parentDirectory = dialog.getParentDirectory();
		if (selectedFileNames.length != 0) {
			eventList.clear();
			generateList(selectedFileNames, parentDirectory);
			length = 0;
		}
	}

	private void generateList(String[] selectedFileNames, String parentDirectory) {
		File parent = new File(parentDirectory);
		File[] files = new File[selectedFileNames.length];
		for (int i = 0; i < selectedFileNames.length; i++) {
			String selectedFileName = selectedFileNames[i];
			files[i] = new File(parent, selectedFileName);
		}
		for (File file : files) {
			try {
				Mp3File mp3File = new Mp3File(file.getAbsolutePath());
				String songTitle = mp3File.getId3v2Tag().getTitle();
				// CharsetMatch detect = new CharsetDetector().setText(songTitle.getBytes()).detect();
				long lengthInSeconds = mp3File.getLengthInSeconds();
				setLength(lengthInSeconds);
				eventList.add(new Track(getName(file.getAbsolutePath()), songTitle, lengthInSeconds));
			} catch (IOException | UnsupportedTagException | InvalidDataException e) {
				e.printStackTrace();
			}
		}
		try {
			ConverterMain.generateBook(Runtime.getRuntime().availableProcessors() / 2, files);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void setLength(long lengthInSeconds) {
		length += lengthInSeconds;
		long hours = SECONDS.toHours(length);
		long minutes = SECONDS.toMinutes(length) - (SECONDS.toHours(length) * 60);
		long seconds = SECONDS.toSeconds(length) - (SECONDS.toMinutes(length) * 60);
		overallLength.setText(format("{0}:{1}:{2}", hours, minutes, seconds));
	}

	public class TrackTableFormat implements TableFormat<Track> {

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int column) {
			switch (column) {
			case 0:
				return "File name";
			case 1:
				return "Chapter name";
			default:
				return "";
			}
		}

		@Override
		public Object getColumnValue(Track baseObject, int column) {
			switch (column) {
			case 0:
				return baseObject.getFileName();
			case 1:
				return baseObject.getTrack();
			default:
				return "";
			}
		}
	}
}
