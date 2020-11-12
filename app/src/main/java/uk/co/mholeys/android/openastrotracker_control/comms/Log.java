package uk.co.mholeys.android.openastrotracker_control.comms;

import android.os.Environment;
import android.provider.ContactsContract;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Log {

    private static Calendar appStartTime = Calendar.getInstance();
    private static Lock oLock = new ReentrantLock();
    private static String sFolder;
    private static String sPath;

    private static List<String> lstBuffer = new ArrayList<String>();
    private static Calendar dtLastUpdate = now();
    private static int maxBuffered = 0;

    public static String filename() {
        return Log.sPath;
    }

    public static void init(String sTitle) {
        // Create our logfile folder in AppData/Roaming
        // TODO get a working lcoation
//        sFolder = String.format("{0}\\OpenAstroTracker", Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData));
        sFolder = "";
        File f = new File(sFolder);
        f.mkdirs();

        // Create this session logfile
        sPath = String.format("%s\\OATControl_%s.log", sFolder, nowString());

        // Find old logfiles and keep the latest 5 around.
//        var oldLogFiles = Directory.GetFiles(sFolder, "OATControl*.log").OrderByDescending(s => s).Skip(5).ToList();

        // Probably should run this by the user.... for now they can jhust manually delete them
        // oldLogFiles.AddRange(Directory.GetFiles(Environment.GetFolderPath(Environment.SpecialFolder.Personal), "oat_*.log"));

//        foreach (var logFile in oldLogFiles)
//        {
//            try
//            {
//                File.Delete(logFile);
//            }
//            catch
//            {
//                // Oh well....
//            }
//        }

        Log.writeLine("*********************************");
        Log.writeLine(String.format("*  %s *", sTitle));
        Log.writeLine("*********************************");
        Log.writeLine("* Started : " + nowString() + " *");
        Log.writeLine("*********************************");
    }

    private static String formatMessage(String message, Object[] args)
    {
        StringBuilder sb = new StringBuilder(message.length() + 64);

        sb.append(String.format("[%s] [%02s}]: ", nowString(), Thread.currentThread().getId()));

        if (args != null && args.length > 0)
        {
            sb.append(String.format(message, args));
        }
        else
        {
            sb.append(message);
        }

        return sb.toString();
    }

    public static void writeLine(String message, Object... args) {
        long mills = now().getTimeInMillis() - Log.dtLastUpdate.getTimeInMillis();
        if (mills > 1000) {
            Log.oLock.lock();
            try {
                StringBuilder sb = new StringBuilder();
                for (int i = 0 ; i < Log.lstBuffer.size(); i++) {
                    sb.append(Log.lstBuffer.get(i));
                    sb.append("\r\n");
                }
                sb.append("\r\n");

                File f = new File(Log.sPath);
                FileWriter fw = new FileWriter(f);
                fw.append(sb.toString());
                Log.lstBuffer.clear();

            } catch (IOException e){
                e.printStackTrace();
            } finally {
                Log.oLock.unlock();
            }
            Log.dtLastUpdate = now();
        }

        String sLine = formatMessage(message, args);

        Log.oLock.lock();
        try {
            Log.lstBuffer.add(sLine);
//            Debug.WriteLine(sLine);
            if (Log.lstBuffer.size() > Log.maxBuffered)  {
                Log.maxBuffered = Log.lstBuffer.size();
            }
        } finally {
            Log.oLock.unlock();
        }
    }

    public static void Quit()
    {
        if (Log.lstBuffer.size() > 0)
        {
            Log.oLock.lock();
            try {
                Log.lstBuffer.add(Log.formatMessage("Shutdown logging. Maximum of %s lines buffered.", new Object[] { Log.maxBuffered }));

                StringBuilder sb = new StringBuilder();
                for (int i = 0 ; i < Log.lstBuffer.size(); i++) {
                    sb.append(Log.lstBuffer.get(i));
                    sb.append("\r\n");
                }
                sb.append("\r\n");

                File f = new File(Log.sPath);
                FileWriter fw = new FileWriter(f);
                fw.append(sb.toString());

                Log.lstBuffer.clear();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Log.oLock.unlock();
            }
        }
    }

    private static Calendar now() {
        return Calendar.getInstance();
    }

    private static String nowString() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss.SSSZ", Locale.getDefault());
        return f.format(now());
    }

}
