package com.github.asm0dey.m4bconverter.model

import com.github.asm0dey.m4bconverter.gui.GuiMain
import java.io.File
import org.apache.commons.io.FilenameUtils

/**
 * User: finkel
 * Date: 14.10.13
 * Time: 0:55
 */
public data class Track(var file: File, var track: String, var length: Long) : Comparable<Track>, IKnowMyValues, ICanSetMyValues<Track>{
    override fun compareTo(other: Track) = file.compareTo(other.file)
    override fun setValue(value: Any?, column: Int): Track {
        if (getColumnsAndEditable().map { it.second }[column] == true )
            when (column) {
                1 -> track =  value as String
            }
        return this
    }
    override fun getValue(column: Int): Any? {
        return when (column) {
            0 -> FilenameUtils.getName(file.getAbsolutePath())
            1 -> track
            2 -> GuiMain.getBeautifulTime(length)
            else -> null
        }
    }
    class object {
        public fun getColumnsAndEditable(): List<Pair<String, Boolean>> = listOf("File name" to false, "Track name" to true, "Length" to false)
    }
}
public trait IKnowMyValues{
    fun getValue(column: Int): Any?
}
public trait ICanSetMyValues<T>{
    fun setValue(value: Any?, column: Int): T
}

