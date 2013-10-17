package com.github.asm0dey.m4bconverter.model

import com.github.asm0dey.m4bconverter.gui.GuiMain

/**
 * User: finkel
 * Date: 14.10.13
 * Time: 0:55
 */
public data class Track(var fileName: String, var track: String, var length: Long) : Comparable<Track>, IKnowMyValues, ICanSetMyValues<Track>{
    override fun compareTo(other: Track) = fileName.compareTo(other.fileName)
    override fun setValue(value: Any?, column: Int): Track {
        if (getColumnsAndEditable().map { it.second }[column] == true )
            when (column) {
                1 -> track =  value as String
            }
        return this
    }
    override fun getValue(column: Int): Any? {
        return when (column) {
            0 -> fileName
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

