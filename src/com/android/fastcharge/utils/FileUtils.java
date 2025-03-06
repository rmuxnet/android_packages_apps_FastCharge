/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2023-2024 cyberknight777
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.fastcharge.utils;

import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility class for file operations used by the Fast Charge app
 * Provides methods to read, write, and check system files
 */
public final class FileUtils {
  private static final String TAG = "FileUtils";

  // Private constructor prevents instantiation
  private FileUtils() {
    // This class is not supposed to be instantiated
  }

  /**
   * Reads the first line of text from the given file.
   * Uses try-with-resources to automatically close the reader
   *
   * @param fileName Path to the file to read
   * @return the read line contents, or null on failure
   */
  public static String readOneLine(String fileName) {
    String line = null;

    try (BufferedReader reader =
        new BufferedReader(new FileReader(fileName), 512)) {
      line = reader.readLine();
    } catch (FileNotFoundException e) {
      // File doesn't exist - log and return null
      Log.w(TAG, "No such file " + fileName + " for reading", e);
    } catch (IOException e) {
      // Error reading the file - log and return null
      Log.e(TAG, "Could not read from file " + fileName, e);
    }

    return line;
  }

  /**
   * Writes the given value into the given file
   * Uses try-with-resources to automatically close the writer
   *
   * @param fileName Path to the file to write
   * @param value String content to write to the file
   * @return true on success, false on failure
   */
  public static boolean writeLine(String fileName, String value) {
    try (BufferedWriter writer =
        new BufferedWriter(new FileWriter(fileName))) {
      writer.write(value);
    } catch (FileNotFoundException e) {
      // File doesn't exist - log and return false
      Log.w(TAG, "No such file " + fileName + " for writing", e);
      return false;
    } catch (IOException e) {
      // Error writing to the file - log and return false
      Log.e(TAG, "Could not write to file " + fileName, e);
      return false;
    }

    return true;
  }

  /**
   * Checks whether the given file exists
   *
   * @param fileName Path to the file to check
   * @return true if exists, false if not
   */
  public static boolean fileExists(String fileName) {
    final File file = new File(fileName);
    return file.exists();
  }

  /**
   * Checks whether the given file is readable
   * File must exist and have read permissions
   *
   * @param fileName Path to the file to check
   * @return true if readable, false if not
   */
  public static boolean isFileReadable(String fileName) {
    final File file = new File(fileName);
    return file.exists() && file.canRead();
  }

  /**
   * Checks whether the given file is writable
   * File must exist and have write permissions
   *
   * @param fileName Path to the file to check
   * @return true if writable, false if not
   */
  public static boolean isFileWritable(String fileName) {
    final File file = new File(fileName);
    return file.exists() && file.canWrite();
  }

  /**
   * Deletes an existing file
   * Handles security exceptions that may occur during deletion
   *
   * @param fileName Path to the file to delete
   * @return true if the delete was successful, false if not
   */
  public static boolean delete(String fileName) {
    final File file = new File(fileName);
    boolean ok = false;
    try {
      ok = file.delete();
    } catch (SecurityException e) {
      // Permission denied - log and return false
      Log.w(TAG, "SecurityException trying to delete " + fileName, e);
    }
    return ok;
  }

  /**
   * Renames an existing file to a new path
   * Handles security and null pointer exceptions
   *
   * @param srcPath Original file path
   * @param dstPath Destination file path
   * @return true if the rename was successful, false if not
   */
  public static boolean rename(String srcPath, String dstPath) {
    final File srcFile = new File(srcPath);
    final File dstFile = new File(dstPath);
    boolean ok = false;
    try {
      ok = srcFile.renameTo(dstFile);
    } catch (SecurityException e) {
      // Permission denied - log and return false
      Log.w(TAG,
            "SecurityException trying to rename " + srcPath + " to " + dstPath,
            e);
    } catch (NullPointerException e) {
      // Null path provided - log and return false
      Log.e(TAG,
            "NullPointerException trying to rename " + srcPath + " to " +
                dstPath,
            e);
    }
    return ok;
  }

  /**
   * Gets value of a node (file) as a boolean
   * Typically used for reading system control files that contain 0 or 1
   * A value of "0" is considered false, any other value is true
   *
   * @param filename Path to the file to read
   * @param defValue Default value to return if file can't be read
   * @return The boolean value from the file or default if file can't be read
   */
  public static boolean getNodeValueAsBoolean(String filename, boolean defValue) {
    String fileValue = readOneLine(filename);
    if (fileValue != null) {
      // Any value that's not "0" is considered true
      return (!fileValue.equals("0"));
    }
    return defValue;
  }
}
