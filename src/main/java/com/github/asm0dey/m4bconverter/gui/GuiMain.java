package com.github.asm0dey.m4bconverter.gui;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.WritableTableFormat;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import ca.odell.glazedlists.swing.EventJXTableModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import chrriis.dj.nativeswing.swtimpl.NativeInterface;
import chrriis.dj.nativeswing.swtimpl.components.JFileDialog;
import com.github.asm0dey.m4bconverter.model.Track;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import jet.IntRange;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static java.text.MessageFormat.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.io.FilenameUtils.getName;

public class GuiMain {

	private static final DecimalFormat twoNums = new DecimalFormat("00");
	private static final DecimalFormat threeNums = new DecimalFormat("000");
	private JFrame frame;
	private EventList<Track> eventList = new BasicEventList<>();
	private JLabel overallLength = new JLabel();
	private long length = 0;
	private DefaultEventSelectionModel<Track> selectionModel;

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

	public static String getBeautifulTime(Long l) {
		long hours = MILLISECONDS.toHours(l);
		long minutes = MILLISECONDS.toMinutes(l) - (MILLISECONDS.toHours(l) * 60);
		long seconds = MILLISECONDS.toSeconds(l) - (MILLISECONDS.toMinutes(l) * 60);
		long tail = MILLISECONDS.toMillis(l) - (MILLISECONDS.toSeconds(l) * 1000);
		return format("{0}:{1}:{2}.{3}", twoNums.format(hours), twoNums.format(minutes), twoNums.format(seconds), threeNums.format(tail));
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		SortedList<Track> trackSortedList = new SortedList<>(eventList);
		frame = new JFrame();
		frame.setBounds(100, 100, 800, 600);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		Container contentPane = frame.getContentPane();
		contentPane.setLayout(new MigLayout(new LC().wrapAfter(4), new AC().grow(100, 0).align("c")));
		JLabel title = new JLabel("JPod Audiobook converter");
		contentPane.add(title, new CC().spanX(2));
		contentPane.add(overallLength);
		contentPane.add(new JButton("Run"));
		TrackTableFormat format = new TrackTableFormat();
		final DefaultEventTableModel<Track> model = new EventJXTableModel<>(trackSortedList, format);
		final JXTable table = new JXTable(model);
		TableComparatorChooser<Track> chooser = TableComparatorChooser.install(table, trackSortedList,
				TableComparatorChooser.MULTIPLE_COLUMN_MOUSE_WITH_UNDO, format);

		selectionModel = new DefaultEventSelectionModel<>(eventList);
		table.setSelectionModel(selectionModel);
		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		JScrollPane scrollPane = new JScrollPane(table);
		contentPane.add(scrollPane, new CC().spanX(3).spanY(4).grow().width("100%"));
		JButton up = new JButton("Up");
		up.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!selectionModel.isSelectionEmpty()) {
					int minSelectionIndex = selectionModel.getMinSelectionIndex();
					int maxSelectionIndex = selectionModel.getMaxSelectionIndex();
					for (Integer next : new IntRange(minSelectionIndex, maxSelectionIndex))
						try {
							Collections.swap(eventList, next, next - 1);
						} catch (Exception ignored) {
						}
					selectionModel.setAnchorSelectionIndex(maxSelectionIndex - 1);
					selectionModel.setLeadSelectionIndex(minSelectionIndex - 1);

				}
			}
		});
		JButton down = new JButton("Down");
		down.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!selectionModel.isSelectionEmpty()) {
					int maxSelectionIndex = selectionModel.getMaxSelectionIndex();
					int minSelectionIndex = selectionModel.getMinSelectionIndex();
					for (int i = maxSelectionIndex; i >= minSelectionIndex; i--)
						try {
							Collections.swap(eventList, i, i + 1);
						} catch (Exception ignored) {
						}
					selectionModel.setAnchorSelectionIndex(maxSelectionIndex + 1);
					selectionModel.setLeadSelectionIndex(minSelectionIndex + 1);
				}
			}
		});
		JButton delete = new JButton("Delete");
		delete.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!selectionModel.isSelectionEmpty()) {
					EventList<Track> selected = selectionModel.getSelected();
					ArrayList<Track> c = new ArrayList<>(selected);
					for (Track t : c) {
						length -= t.getLength();
                        updateLengthLabel();
						eventList.remove(t);
					}
				}
			}
		});
		contentPane.add(up, new CC().cell(3, 1));
		contentPane.add(down, new CC().cell(3, 2));
		contentPane.add(delete, new CC().cell(3, 3));
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
			length = 0;
			eventList.clear();
			generateList(selectedFileNames, parentDirectory);
		}
	}

	private void generateList(String[] selectedFileNames, String parentDirectory) {
		File parent = new File(parentDirectory);
		File[] files = new File[selectedFileNames.length];
		for (int i = 0; i < selectedFileNames.length; i++) {
			String selectedFileName = selectedFileNames[i];
			files[i] = new File(parent, selectedFileName);
		}
		Arrays.sort(files);
		for (File file : files) {
			try {
				Mp3File mp3File = new Mp3File(file.getAbsolutePath());
				String songTitle = mp3File.getId3v2Tag().getTitle();
				final long lengthInMillis = mp3File.getLengthInMilliseconds();
				length += lengthInMillis;
				updateLengthLabel();
				eventList.add(new Track(getName(file.getAbsolutePath()), songTitle, lengthInMillis));
			} catch (IOException | UnsupportedTagException | InvalidDataException e) {
				e.printStackTrace();
			}
		}
		// try {
		// ConverterMain.generateBook(Runtime.getRuntime().availableProcessors() / 2, files);
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
	}

	private void updateLengthLabel() {
		String format = getBeautifulTime(length);
		overallLength.setText(format);
	}

	public class TrackTableFormat implements WritableTableFormat<Track> {

		@Override
		public int getColumnCount() {
			return Track.object$.getColumnsAndEditable().size();
		}

		@Override
		public String getColumnName(int column) {
			return Track.object$.getColumnsAndEditable().get(column).getFirst();
		}

		@Override
		public Object getColumnValue(Track baseObject, int column) {
			return baseObject.getValue(column);
		}

		@Override
		public boolean isEditable(Track baseObject, int column) {
			return Track.object$.getColumnsAndEditable().get(column).getSecond();
		}

		@Override
		public Track setColumnValue(Track baseObject, Object editedValue, int column) {
			return baseObject.setValue(editedValue, column);
		}

	}
}
