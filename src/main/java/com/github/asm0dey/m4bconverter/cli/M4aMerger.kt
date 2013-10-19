package com.github.asm0dey.m4bconverter.cli

import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecuteResultHandler as ResultHandler
import org.apache.commons.exec.DefaultExecutor as Executor
import org.apache.commons.exec.PumpStreamHandler as StreamHandler
import java.io.File
import java.text.DecimalFormat
import com.google.common.collect.Iterables.partition
import java.io.File.separator
import org.apache.commons.io.FilenameUtils.getName
import java.util.ArrayList
import org.apache.commons.exec.PumpStreamHandler

open class M4aMerger {
    class object {
        private val df = DecimalFormat("0000")
        private val partitionSize = 20
        public open fun mergeM4a(files: Collection<File>): File {
            var counter = 0

            fun mergePartition(fileList: List<File>): File {
                var cl = CommandLine.parse("/usr/bin/MP4Box")!!
                var parent = fileList[0].getParentFile()!!
                fileList.forEach { cl.addArgument("-cat")!!.addArgument(getName(it.getAbsolutePath())) }
                var absolutePath = "${parent.getAbsolutePath()}${separator}temp_${df.format(++counter)}.m4a"
                cl.addArgument(getName(absolutePath))
                println("\n${cl}")
                var executor = Executor(parent,StreamHandler());
                var handler = ResultHandler()
                executor.execute(cl, handler)
                handler.waitFor()
                fileList.forEach { it.delete() }
                File(absolutePath)
            }
            var result = ArrayList<File>(files)
            while (result.size > partitionSize){
                result = partition(result, partitionSize)?.mapTo(ArrayList<File>(), { mergePartition(it) })!!
            }
            var file = mergePartition(result)
            var dest = File("${file.getParent()}${separator}book.m4b")
            if (dest.exists())
                dest.delete()
            file.renameTo(dest)
            return dest
        }
        fun Executor(workDir:File,streamHandler:StreamHandler):Executor{
            var executor = Executor()
            executor.setWorkingDirectory(workDir)
            executor.setStreamHandler(streamHandler)
            return executor
        }
    }
}
