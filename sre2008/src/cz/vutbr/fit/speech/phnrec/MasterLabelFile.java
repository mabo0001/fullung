package cz.vutbr.fit.speech.phnrec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.lunglet.util.AssertUtils;

public final class MasterLabelFile {
    private static final Set<String> BAD_LABELS;

    static {
        BAD_LABELS = new HashSet<String>();
        BAD_LABELS.add("int");
        BAD_LABELS.add("oth");
        BAD_LABELS.add("pau");
        BAD_LABELS.add("spk");
    }

    private static List<MasterLabel> readLabels(final Reader reader) throws IOException {
        List<MasterLabel> labels = new ArrayList<MasterLabel>();
        BufferedReader bufReader = new BufferedReader(reader);
        String line = bufReader.readLine();
        while (line != null) {
            String[] parts = line.split("\\s+");
            String label = parts[2];
            long startTime = Long.valueOf(parts[0]);
            long endTime = Long.valueOf(parts[1]);
            float score = Float.valueOf(parts[3]);
            labels.add(new MasterLabel(label, startTime, endTime, score));
            line = bufReader.readLine();
        }
        return labels;
    }

    private final List<MasterLabel> labels;

    public MasterLabelFile(final File file) throws IOException {
        this(new FileReader(file));
    }

    public MasterLabelFile(final Reader reader) throws IOException {
        this.labels = readLabels(reader);
        if (labels.size() == 0) {
            throw new IOException("Invalid label file");
        }
    }

    /**
     * @param start start of frame in seconds
     * @param end end of frame in seconds
     */
    public boolean isOnlySpeech(final double start, final double end) {
        if (start < labels.get(0).getStartTimeSeconds()) {
            throw new IllegalArgumentException();
        }
        if (labels.get(labels.size() - 1).getEndTimeSeconds() < end) {
            throw new IllegalArgumentException();
        }
        // set startIndex to last block to handle the case where start and end
        // are both the last timestamp in the file
        int startIndex = labels.size() - 1;
        for (int i = 0; i < labels.size(); i++) {
            MasterLabel label = labels.get(i);
            // the less than comparison means that startIndex is never set
            // inside this loop if start and end are equal to the last timestamp
            // in the file
            if (start >= label.getStartTimeSeconds() && start < label.getEndTimeSeconds()) {
                startIndex = i;
                break;
            }
        }
        int endIndex = -1;
        for (int i = startIndex; i < labels.size(); i++) {
            MasterLabel label = labels.get(i);
            if (end >= label.getStartTimeSeconds() && end <= label.getEndTimeSeconds()) {
                endIndex = i;
                break;
            }
        }
        AssertUtils.assertTrue(endIndex >= 0 && endIndex >= startIndex);
        for (int i = startIndex; i <= endIndex; i++) {
            String label = labels.get(i).getLabel();
            if (BAD_LABELS.contains(label)) {
                return false;
            }
        }
        return true;
    }
}
