package com.github.asm0dey.m4bconverter.gui

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.gui.WritableTableFormat
import ca.odell.glazedlists.swing.DefaultEventSelectionModel
import chrriis.dj.nativeswing.swtimpl.NativeInterface
import chrriis.dj.nativeswing.swtimpl.components.JFileDialog
import com.github.asm0dey.m4bconverter.model.Track
import java.io.File
import java.text.DecimalFormat
import java.util.ArrayList
import java.util.Collections
import java.text.MessageFormat.format
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.awt.EventQueue
import kotlin.swing.*
import javax.swing.JLabel
import javax.swing.JFrame
import net.miginfocom.swing.MigLayout
import net.miginfocom.layout.LC
import net.miginfocom.layout.AC
import javax.swing.UIManager
import net.miginfocom.layout.CC
import com.github.asm0dey.m4bconverter.cli.ConverterMain
import ca.odell.glazedlists.swing.EventJXTableModel
import ca.odell.glazedlists.SortedList
import org.jdesktop.swingx.JXTable
import javax.swing.ListSelectionModel
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.SwingConstants
import org.apache.commons.io.FilenameUtils
import org.jaudiotagger.audio.mp3.MP3File
import org.jaudiotagger.audio.AudioFileIO

public open class GuiMain() {
    private val eventList = BasicEventList<Track>()
    private val overallLength = JLabel()
    private var mainFrame = JFrame()
    private var length = 0.toLong()
    private var selectionModel = DefaultEventSelectionModel<Track>(eventList);
    private open fun initialize() {
        mainFrame = frame("JPod: MP3 to M4B audiobook converter") {
            exitOnClose()
            var addFilesActions = action("Add files") { selectFiles() }
            var exitAction = action("Exit") { System.exit(0) }
            jmenuBar = menuBar {
                menu("File") {
                    add(addFilesActions)
                    add(JSeparator(SwingConstants.HORIZONTAL))
                    add(exitAction)
                }
            }
            size = Pair(800, 600)
            setLayout(MigLayout(LC().wrapAfter(4), AC().grow((100).toFloat(), 0)?.align("c")))
            add(JLabel("JPod: MP3 to M4B audiobook converter"), CC().spanX(2))
            add(overallLength)
            add(button("Run", { ConverterMain.generateBook(Runtime.getRuntime().availableProcessors() / 2, eventList) }))
            var format = TrackTableFormat()
            var trackSortedList = SortedList<Track>(eventList)
            val model = EventJXTableModel<Track>(trackSortedList, format)
            val table = JXTable(model)
            selectionModel = DefaultEventSelectionModel<Track>(eventList)
            table.setSelectionModel(selectionModel : ListSelectionModel)
            selectionModel.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
            var scrollPane = JScrollPane(table)
            add(scrollPane, CC().spanX(3)?.spanY(3)?.grow()?.width("100%"))
            add(button("Up", { moveUp() }), CC().cell(3, 1))
            add(button("Down", { moveDown() }), CC().cell(3, 2))
            add(button("Delete", { delete() }), CC().cell(3, 3)?.wrap())

        }
    }
    private open fun delete(): Unit {
        if (!selectionModel.isSelectionEmpty()){
            var selected = selectionModel.getSelected()
            var tracks = ArrayList<Track>(selected!!)
            for (track in tracks){
                length -= track.length
                updateLengthLabel()
                eventList.remove(track)
            }
        }

    }
    private open fun moveDown(): Unit {
        if (!selectionModel.isSelectionEmpty()){
            var maxSelectionIndex: Int = selectionModel.getMaxSelectionIndex()
            var minSelectionIndex: Int = selectionModel.getMinSelectionIndex()
            var i = maxSelectionIndex
            while (i >= minSelectionIndex){
                Collections.swap(eventList, i, i + 1)
                i--
            }
            selectionModel.setAnchorSelectionIndex(maxSelectionIndex + 1)
            selectionModel.setLeadSelectionIndex(minSelectionIndex + 1)
        }

    }
    private open fun moveUp(): Unit {
        if (!selectionModel.isSelectionEmpty()){
            var minSelectionIndex = selectionModel.getMinSelectionIndex()
            var maxSelectionIndex = selectionModel.getMaxSelectionIndex()
            for (next in (minSelectionIndex..maxSelectionIndex))
                Collections.swap(eventList, next, next - 1)
            selectionModel.setAnchorSelectionIndex(maxSelectionIndex - 1)
            selectionModel.setLeadSelectionIndex(minSelectionIndex - 1)
        }

    }
    private open fun selectFiles(): Unit {
        var dialog = JFileDialog()
        dialog.setDialogType(JFileDialog.DialogType.OPEN_DIALOG_TYPE)
        dialog.setSelectionMode(JFileDialog.SelectionMode.MULTIPLE_SELECTION)
        dialog.setExtensionFilters(array("*.mp3"), array("mp3 files"), 0)
        dialog.show(mainFrame)
        var selectedFileNames = dialog.getSelectedFileNames()!!
        var parentDirectory = dialog.getParentDirectory()
        if (selectedFileNames.isNotEmpty()){
            length = 0
            eventList.clear()
            generateList(selectedFileNames, parentDirectory!!)
        }

    }
    private open fun generateList(selectedFileNames: Array<String>, parentDirectory: String): Unit {
        var parent = File(parentDirectory)
        var files = selectedFileNames.map { File(parent, it) }.sortBy { it.getAbsolutePath() }
        for (file in files){
            val mp3File = AudioFileIO.read(file) as MP3File
            val trackLength = mp3File.getAudioHeader()?.getTrackLength()?.toLong()?.times(1000)
            length += trackLength!!
            updateLengthLabel()
            eventList.add(Track(file, mp3File.getID3v1Tag()?.getFirstTitle()?:FilenameUtils.getBaseName(file.getAbsolutePath())!!, trackLength))
        }

    }
    class object {
        private val twoNums = DecimalFormat("00")
        private val threeNums = DecimalFormat("000")
        public open fun main(args: Array<String>) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            NativeInterface.open()
            EventQueue.invokeLater({
                val window = GuiMain()
                window.initialize()
                window.mainFrame.setVisible(true)
            })
            NativeInterface.runEventPump()
        }
        public open fun getBeautifulTime(l: Long): String? {
            val hours = MILLISECONDS.toHours(l)
            val minutes = MILLISECONDS.toMinutes(l) - (MILLISECONDS.toHours(l) * 60)
            val seconds = MILLISECONDS.toSeconds(l) - (MILLISECONDS.toMinutes(l) * 60)
            val tail = MILLISECONDS.toMillis(l) - (MILLISECONDS.toSeconds(l) * 1000)
            return format("{0}:{1}:{2}.{3}", twoNums.format(hours), twoNums.format(minutes), twoNums.format(seconds), threeNums.format(tail))
        }

    }
    private fun updateLengthLabel(): Unit {
        val format = getBeautifulTime(length)
        overallLength.setText(format)
    }
    private open class TrackTableFormat() : WritableTableFormat<Track?> {
        public override fun getColumnCount() = Track.getColumnsAndEditable().size()
        public override fun getColumnName(column: Int) = Track.getColumnsAndEditable().get(column).first
        public override fun getColumnValue(baseObject: Track?, column: Int) = baseObject?.getValue(column)
        public override fun isEditable(baseObject: Track?, column: Int) = Track.getColumnsAndEditable().get(column).second
        public override fun setColumnValue(baseObject: Track?, editedValue: Any?, column: Int) = baseObject?.setValue(editedValue, column)
    }
}
fun main(args: Array<String>) = GuiMain.main(args)
/*
public fun button(title:String,action:()->Unit):JButton{
    val button = JButton(title)
    button.addActionListener { action.invoke() }
    return button
}
*/
