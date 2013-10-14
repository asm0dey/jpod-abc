package com.github.asm0dey.m4bconverter.cli

import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecuteResultHandler
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.PumpStreamHandler
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.text.MessageFormat.format
import java.util.ArrayList
import com.google.common.collect.Iterables.partition
import com.google.common.collect.Lists.newArrayList
import java.io.File.separator
import org.apache.commons.io.FilenameUtils.getName
import java.util.SortedSet
import com.google.common.collect.FluentIterable

open class M4aMerger() {
    private var counter  = 0
    public open fun merge(files: Collection<File>): File {
        var arrayList = internReduce(files)
        while (arrayList.size()>partitionSize){
            arrayList = internReduce(arrayList)
        }
        val file = arrayList.reduce { f1, f2 -> mergePartition(listOf(f1, f2)) }
        var dest = File(file.getParent() + separator + "book.m4a")
        if (dest.exists())
            dest.delete()
        file.renameTo(dest)
        return dest
    }
    private fun internReduce(iter:Iterable<File>):List<File>{
        return partition(iter, partitionSize)!!.reduce { list1,list2-> listOf(mergePartition(list1),mergePartition(list2))}
    }
    private open fun mergePartition(fileList: List<File>): File {
        var cl = CommandLine.parse("/usr/bin/MP4Box")!!
        var parent = fileList[0].getParentFile()
        fileList.forEach { cl.addArgument("-cat")!!.addArgument(getName(it.getAbsolutePath())) }
        var absolutePath = "${parent?.getAbsolutePath()}${separator}temp_${df.format(++counter)}.m4a"
        cl.addArgument(getName(absolutePath))
        System.out.println("\n" + cl)
        var executor = DefaultExecutor()
        executor.setWorkingDirectory(parent)
        var handler = DefaultExecuteResultHandler()
        executor.setStreamHandler(PumpStreamHandler())
        executor.execute(cl, handler)
        handler.waitFor()
        fileList.forEach { it.delete() }
        return File(absolutePath)
    }

    class object {
        private val df = DecimalFormat("0000")
        private val partitionSize = 20;
    }
}
