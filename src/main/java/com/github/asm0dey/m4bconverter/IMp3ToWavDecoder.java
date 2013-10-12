package com.github.asm0dey.m4bconverter;

import java.io.File;

/**
 * User: finkel
 * Date: 11.10.13
 * Time: 19:24
 */
public interface IMp3ToWavDecoder {
    public void decode(File input, File output);
}
