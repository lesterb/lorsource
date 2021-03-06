/*
 * Copyright 1998-2009 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.util;

import java.io.*;

public final class FileUtils {
  private FileUtils() {
  }

  public static String readfile(String filename) throws IOException {
    StringBuffer out = new StringBuffer();
    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "KOI8-R"));

    char[] buf = new char[8192];

    int i = 0;
    while ((i = in.read(buf, 0, buf.length)) > -1) {
      if (i > 0) {
        out.append(buf, 0, i);
      }
    }

    in.close();
    return out.toString();
  }
}
