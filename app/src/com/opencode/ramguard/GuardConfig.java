package com.opencode.ramguard;
import java.io.*;
import java.util.*;

public final class GuardConfig {
    // App-private storage: readable/writable without storage permission on Android 13.
    // The root daemon reads the same path.
    public static final File DIR = new File("/data/data/com.opencode.ramguard/files");
    public static final File FILE = new File(DIR, "config.txt");
    public String target = "droidhang.twgame.restaurant";
    public int interval = 3;
    public final List<String> kills = new ArrayList<>();

    public void load() {
        kills.clear();
        if (!FILE.exists()) { save(); return; }
        try (BufferedReader r = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("target=")) target = line.substring(7).trim();
                else if (line.startsWith("interval=")) {
                    try { interval = Integer.parseInt(line.substring(9).trim()); } catch (Exception ignore) {}
                } else if (line.startsWith("kill=")) {
                    String p = line.substring(5).trim();
                    if (!p.isEmpty()) kills.add(p);
                }
            }
        } catch (Exception ignore) {}
        if (interval < 1) interval = 3;
    }

    public void save() {
        try {
            DIR.mkdirs();
            StringBuilder sb = new StringBuilder();
            sb.append("# RamGuard config\n");
            sb.append("target=").append(target).append("\n");
            sb.append("interval=").append(interval).append("\n");
            for (String k : kills) sb.append("kill=").append(k).append("\n");
            try (FileOutputStream out = new FileOutputStream(FILE)) {
                out.write(sb.toString().getBytes("UTF-8"));
            }
        } catch (Exception ignore) {}
    }
}
