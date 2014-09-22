package edu.buffalo.cse.cse486586.simpledynamo.constants;

import android.net.Uri;

import static edu.buffalo.cse.cse486586.simpledynamo.utils.HashingUtils.buildUri;


public class Constants {
    public static String TAG = "SimpleDynamo";

    public static final String DELIMITER = "---";

    public static final String KEY_FIELD = "key";
    public static final String VALUE_FIELD = "value";
    public static final String VER_FIELD = "version";

    public static final String TABLE_NAME = "content";
    public static final String DB_NAME = "db";

    public static final String SQL_CREATE_CONTENT = "CREATE TABLE content (key TEXT UNIQUE, value TEXT)";

    public static final int PORTS_LIST[] = {11108, 11112, 11116, 11120, 11124};
    public static final int SERVER_PORT = 10000;

    public static final String HOME_AUTHORITY = "edu.buffalo.cse.cse486586.simpledynamo.provider";
//    public static final String EXTERNAL_AUTHORITY = "edu.buffalo.cse.cse486586.simpledynamo.provider/external";

    public static final Uri TESTER_URI = buildUri(TABLE_NAME, HOME_AUTHORITY, null);
    public static final Uri INTERNAL_URI = buildUri(TABLE_NAME, HOME_AUTHORITY, "internal");
    public static final Uri EXTERNAL_URI = buildUri(TABLE_NAME, HOME_AUTHORITY, "external");
    public static final Uri RECOVERY_URI = buildUri(TABLE_NAME, HOME_AUTHORITY, "recovery");

    public static final String KEYS_IN_RESPONDER_STRICT_SCOPE = "&";
    public static final String KEYS_IN_REQUESTER_STRICT_SCOPE = "#";
    public static final String KEYS_IN_SCOPE_OF_PORT = "$";
    public static final String ALL_LOCAL = "@";
    public static final String ALL_IN_CHORD = "*";

    public static Integer myIndex;

    public static final String DELIVERED = "delivered";
    public static final String AWAITED = "awaited";
    public static final String NULL = "null";
}
