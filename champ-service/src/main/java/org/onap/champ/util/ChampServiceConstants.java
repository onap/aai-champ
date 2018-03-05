package org.onap.champ.util;

public class ChampServiceConstants {
  public static final String CHAMP_FILESEP = (System.getProperty("file.separator") == null) ? "/"
      : System.getProperty("file.separator");

  public static final String CHAMP_SPECIFIC_CONFIG = System.getProperty("CONFIG_HOME") + CHAMP_FILESEP;
  public static final String CHAMP_CONFIG_FILE = CHAMP_SPECIFIC_CONFIG + "champ-api.properties";
  public static final String CHAMP_KEY_NAME = "keyName";
  public static final String CHAMP_SOT_NAME = "sourceOfTruthName";
  public static final String CHAMP_CREATED_TS_NAME = "createdTsName";
  public static final String CHAMP_LAST_MOD_TS_NAME = "lastModTsName";
  public static final String CHAMP_COLLECTION_PROPERTIES_KEY = "collectionPropertiesKey";
}