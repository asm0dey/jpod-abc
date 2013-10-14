package com.github.asm0dey.m4bconverter.model

/**
 * User: finkel
 * Date: 14.10.13
 * Time: 0:55
 */
public data class Track(var fileName:String,var track:String,var length:Long?=null):Comparable<Track>{
    override fun compareTo(other: Track): Int {
        return fileName.compareTo(other.fileName)
    }

}
