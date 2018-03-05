package org.onap.champ.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ChampProperties {

  private static Properties properties;

  static {
    properties = new Properties();
    File file = new File(ChampServiceConstants.CHAMP_CONFIG_FILE);
    try {
      properties.load(new FileInputStream(file));
    } catch (IOException e) {
      e.printStackTrace();
      Runtime.getRuntime().halt(1);
    }
  }

  public static String get(String key) {
    return properties.getProperty(key);
  }

  public static String get(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }

  public static void put(String key, String value) {
    properties.setProperty(key, value);
    FileOutputStream fileOut = null;
    try {
      fileOut = new FileOutputStream(new File(ChampServiceConstants.CHAMP_CONFIG_FILE));
      properties.store(fileOut, "Added property: " + key);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {

      try {
        fileOut.close();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }

  }


}
