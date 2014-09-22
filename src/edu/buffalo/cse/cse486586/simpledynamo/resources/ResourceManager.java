package edu.buffalo.cse.cse486586.simpledynamo.resources;

import android.content.ContentResolver;
import edu.buffalo.cse.cse486586.simpledynamo.communicator.ChordManager;
import edu.buffalo.cse.cse486586.simpledynamo.communicator.Communicator;
import edu.buffalo.cse.cse486586.simpledynamo.communicator.RequestFactory;
import edu.buffalo.cse.cse486586.simpledynamo.messaging.MailboxService;

import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.TAG;
import static edu.buffalo.cse.cse486586.simpledynamo.constants.Constants.myIndex;

public class ResourceManager {
    public static ContentResolver contentResolver;
    public static Communicator communicator;
    public static ChordManager chordManager;
    public static volatile MailboxService mailboxService;
    public static RequestFactory requestFactory;

    public ResourceManager(ContentResolver cr, int myPort) {
        chordManager = new ChordManager(myPort);
        communicator = new Communicator();
        contentResolver = cr;
        mailboxService = new MailboxService();
        requestFactory = new RequestFactory();
        TAG += "-" + myIndex;
    }
}
