package com.github.asm0dey.m4bconverter.cli

import com.github.asm0dey.m4bconverter.gui.GuiMain
import com.github.asm0dey.m4bconverter.model.Track
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecuteResultHandler
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.PumpStreamHandler
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.text.DecimalFormat
import java.util.TreeSet
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.text.MessageFormat.format
import org.apache.commons.io.FilenameUtils.*
import java.util.ArrayList

public open class ConverterMain() {
    class object {
        val df = DecimalFormat("00000")
        val toConvert = AtomicInteger(0)
        fun printProgBar(percent: Int, customMessage: String? = null) {
            var bar = StringBuilder("[")
            for (i in 0..50 - 1)
                if (i < (percent / 2))
                    bar.append("=")
                else bar.append(if(i == (percent / 2)) '>' else ' ')
            bar.append("]   ${percent}%     ")
            bar.append((if (customMessage == null) "" else customMessage))
            print("\r${bar.toString()}")
            if (percent == 100)println("\n")
        }
        inner data class ConversionQueue(val source: File, val trackNumber: Int, val sign: String, val tasksTotal: Int, val resultContainer: MutableCollection<File>) : Runnable {
            public override fun run() {
                var parentFile = source.getParentFile()
                var resultingFileName = format("{0}_{1}.m4a", df.format((trackNumber).toLong()), sign)
                var buffer = File.createTempFile("buffer", "")
                ProcessBuilder(
                        listOf(
                                (if (SystemUtils.IS_OS_WINDOWS) "cmd" else "bash")
                                , (if (SystemUtils.IS_OS_WINDOWS) "/c" else "-c")
                                , "/usr/bin/lame --decode ${getBaseName(source.getAbsolutePath())}.mp3 -|neroAacEnc -if - -of ${resultingFileName}"))
                        .directory(parentFile)
                        .inheritIO()
                        .redirectError(buffer)
                        .redirectInput(ProcessBuilder.Redirect.PIPE)
                        .redirectOutput(ProcessBuilder.Redirect.PIPE)
                        .start()
                        .waitFor()
                printProgBar(((toConvert.incrementAndGet().toDouble()) / tasksTotal * 100).toInt(), "\t[${getBaseName(source.getAbsolutePath())}.mp3 converted to ${resultingFileName}]")
                buffer.delete()
                resultContainer.add(File(parentFile, resultingFileName))
            }
        }

        public open fun generateBook(threadsNumber: Int, listFiles: List<Track>): Unit {
            var mp3ToWavThreadPool = Executors.newFixedThreadPool(threadsNumber, Executors.privilegedThreadFactory())
            val sign = RandomStringUtils.randomAlphanumeric(10)!!
            var i = 0
            printProgBar(0)
            val toMerge = TreeSet<File>()
            val allBatchTasksCount = listFiles.size()
            listFiles.forEach { mp3ToWavThreadPool.submit(ConversionQueue(it.file, ++i, sign, allBatchTasksCount, toMerge)) }
            mp3ToWavThreadPool.shutdown()
            mp3ToWavThreadPool.awaitTermination(java.lang.Long.MAX_VALUE, TimeUnit.DAYS)
            var m4a = M4aMerger.mergeM4a(toMerge)
            var strings = ArrayList<String>()
            var currentTime = 0
            for (listFile  in listFiles)
            {
                strings += ( "${GuiMain.getBeautifulTime(currentTime.toLong())} ${listFile.track}")
                currentTime += listFile.length
            }
            var chapter = File(m4a.getParent(), "${FilenameUtils.getBaseName(m4a.getAbsolutePath())}.chapters.txt")
            chapter.writeBytes(strings.reduce { s1, s2 -> "${s1}\n${s2}" }.getBytes())
            fun chapterFile(m4a: File) {
                var cl = CommandLine.parse("/usr/bin/mp4chaps -i -Q")?.addArgument(getName(m4a.getAbsolutePath()))
                var executor = DefaultExecutor()
                var handler = DefaultExecuteResultHandler()
                executor.setStreamHandler(PumpStreamHandler())
                executor.setWorkingDirectory(m4a.getParentFile())
                executor.execute(cl, handler)
                handler.waitFor()
            }
            chapterFile(m4a)
            chapter.delete()
        }
    }


}
