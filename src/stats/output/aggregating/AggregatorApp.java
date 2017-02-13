package stats.output.aggregating;

import exceptions.ResultFileMalformedException;
import exceptions.StatisticException;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.filechooser.FileFilter;
import stats.Statistics;
import stats.Values;
import stats.Constants;
import static stats.Constants.MODE__TRANSIENT_STATS;
import static stats.Constants.TIME;
import utilities.CommonFunctions;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class AggregatorApp {

    private static final Logger LOG = Logger.getLogger(AggregatorApp.class.getName());

    private static boolean printMean = true;
    private static boolean printStdev = true;
    private static int roundDecimal = 5;
    //////////////// aggrAndPrint varibles ////////////
    /**
     * which statistic titles to keep
     */
    private static Set<String> statTitles = new HashSet<>(Arrays.asList(
            new String[]{ //<editor-fold defaultstate="collapsed" desc="stats.handlers.iterative.mu.handover.**.TITLE">
            //         stats.handlers.iterative.mu.handover.EPC_Gain.TITLE 
            //</editor-fold>
            }));
    /**
     * Different param values for the params in this set are ignored when
     * merging different scenarios setups, i.e. if the rest params have same
     * values, then the two setups are merged same group, neglecting difference
     * with respect to members of this set. The stats printed underneeth each
     * resulting group thow are differenciated per scenario setup by using the
     * ignored param value with each stat title.
     */
    private static Set<String> grping__ignoredParams = new TreeSet<>(Arrays.asList(
            new String[]{
                "TU",
                "beta"
            //         "#SCs",
            //      "mu_delay"
            //            "probs_policy",
            //            "#MUs_tot"
            }));

    private static void userAbortedMsg() {
        JOptionPane.showMessageDialog(null, "You have chosen to Abort", "Aborting", JOptionPane.INFORMATION_MESSAGE);
    }

    private static Values[] reduceByMeanValue(
            Set<String> statTitles, ResultsFile[] resFiles, Integer nxtTime, int roundDecimal,
            Map totallyLacks, Map partiallyLacks) throws StatisticException {
        /*
       * Aggregate values from all files, for nxtTime, to a Values instance. 
       * Use an array of Values instances, one per each title to print.
         */
        Values[] aggrFilesValues = new Values[statTitles.size()];
        for (int i = 0; i < aggrFilesValues.length; i++) {
            aggrFilesValues[i] = new Values(roundDecimal);
            /*aggregates values from all files, for nxtTime*/
        }

        FOR_RESULT_FILE:
        for (ResultsFile nxtRF : resFiles) {
            Set<String> lackedValsOrTitls = new HashSet<>();
            Set<String> encounteredTitls = new HashSet<>();

//            System.err.println("\n\nstatsPerTime for file " + nxtRF.getFilePath() + ": " + nxtRF.getStatistics().toString());//xxx
            int i = -1; // index to aggrFilesValues for the i-th stat title
            for (String nxtTitle : statTitles) {
                ++i;
                Values aggrVal = nxtRF.getStatistics().aggregates(nxtTitle, nxtTime, nxtRF);
                /* Aggregates from one file, for all records for nxtTime*/

                if (aggrVal == null) {
                    lackedValsOrTitls.add(nxtTitle);
                } else {
                    encounteredTitls.add(nxtTitle);
                    double theMean = aggrVal.mean();
                    aggrFilesValues[i].updt(theMean);

//                    System.err.println("1) updt for title " + nxtTitle);
//                    System.err.println("\t" + aggrFilesValues[i].toString());
                }
            }//for i-th stat title

            if (!totallyLacks.isEmpty()) {
                HashSet tmp = new HashSet(lackedValsOrTitls);
                tmp.removeAll(encounteredTitls);
                totallyLacks.put(nxtRF, tmp);
            }

            if (!partiallyLacks.isEmpty()) {

                HashSet tmp = new HashSet(lackedValsOrTitls);
                lackedValsOrTitls.removeAll(encounteredTitls);// now it has the totally lacked titles
                tmp.removeAll(lackedValsOrTitls);

                partiallyLacks.put(nxtRF, tmp);

                Logger.getGlobal().log(Level.FINE,
                        "File "
                        + "\"{0}"
                        + "\" "
                        + "lacks some values "
                        + " for these titles:\n{1}",
                        new Object[]{
                            nxtRF.getFilePath(),
                            CommonFunctions.toString(partiallyLacks)
                        }
                );
            }

        }

        return aggrFilesValues;
    }

    /**
     * Aggregates the results from different repeats of the same scenario setup,
     * i.e. setups with different seed, and prints them into a file.
     *
     * The statistics printed are the ones included in the grping__statTitles,
     * and aggregated together under each group of results. Groups of results
     * stem from the same scenario parameter setup (all parameters have the same
     * value), but from from different repeats (different seed parameter).
     *
     * @param statTitles
     * @param resultsFiles
     * @param aggrResultsPath
     * @param printMean
     * @param printStdev
     * @param operation
     * @param confidenceLevel
     * @throws IOException
     * @throws StatisticException
     */
    public static void aggrAndPrint(
            Set<String> statTitles, ResultsFile[] resultsFiles,
            String aggrResultsPath,
            String operation, boolean printMean, boolean printStdev,
            Statistics.ConfidenceInterval confidenceLevel)
            throws IOException, StatisticException {

       

        try (PrintStream printer = new PrintStream(aggrResultsPath)) {

            boolean isTransient
                    = operation.equalsIgnoreCase(MODE__TRANSIENT_STATS)
                    || operation.equalsIgnoreCase(Constants.MODE__TRANSIENT_AGGREGATES_STATS);

            SortedSet<Integer> times = null;
            if (isTransient) {
                times = new TreeSet<>();
                /*
                 * the maximum set of times, from minimum time
                 * to maximum time recorded in any result file. 
                 * Note that result can files contain different sets
                 * of record-times.
                 */
                for (ResultsFile rf : resultsFiles) {
                    times.addAll(rf.times());
                }
            } else {
                /*
                 * This is because the results are from aggregated (not
                 * transient) results per simulation run, thus with no times
                 * printed
                 */
                times = new TreeSet<>();//create a moke time
                times.add(Constants.AGGR_RESULTS_FILE__DEFAULT_TIME);
            }

            printTitles(isTransient, statTitles, null, printer, printMean,
                    printStdev, confidenceLevel);

            printResAggregated(isTransient, times, resultsFiles, statTitles, printer,
                    printMean, printStdev, confidenceLevel);
        }

        //<editor-fold defaultstate="collapsed" desc="logging output file path to user">
        String logMsg = "==== INFO ===="
                + "\n\t- Results files processed: {0}"
                + "\n\t- Output in path: {1}";

        Object[] msgArr = new Object[]{
            resultsFiles.length,
            aggrResultsPath
        };
        LOG.log(Level.INFO, logMsg, msgArr);
        //</editor-fold>
    }

    public static void printResAggregated(
            boolean printTimes, SortedSet<Integer> times, ResultsFile[] resFiles,
            Set<String> statTitles, PrintStream printer,
            boolean mean, boolean stdev, Statistics.ConfidenceInterval confPercentile)
            throws StatisticException {

        for (Integer nxtTime : times) {
            if (printTimes) { // print the time first if transient results
                printer.print(nxtTime);
                printer.print(',');
            }

            Map<File, Set<String>> totallyLacks = new HashMap();
            Map<File, Set<String>> partiallyLacks = new HashMap();
            Values[] reduceByMeanValue = reduceByMeanValue(statTitles, resFiles, nxtTime, roundDecimal,
                    totallyLacks, partiallyLacks);

            warnUser(totallyLacks, partiallyLacks);

            if (mean) {
                for (Values nxt : reduceByMeanValue) {
                    if (nxt == null) {
                        printer.print("NULL");
                    } else {
                        printer.print(nxt.mean());
                    }
                    printer.print(',');

//xxx                    System.err.println("<mean printing> ");
//                    System.err.println("\t" + nxt.mean());
//                    System.err.println("\t" + nxt.toString());
                }//for

            }
//////////////////////////////////////////////////////////////////            
//////////////////////////////////////////////////////////////////            
//////////////////////////////////////////////////////////////////            
            if (confPercentile != Statistics.ConfidenceInterval.NONE) {
                for (Values nxt : reduceByMeanValue) {
                    if (nxt == null) {
                        printer.print("NULL");
                    } else {
                        printer.print(nxt.confIntervalABS(confPercentile.z()));
                    }

//xxx                    System.err.println("<conf printing> ");
//                    System.err.println("\t" + nxt.confIntervalABS(Statistics.ConfidenceInterval.PERCENTILE_95.z()));
//                    System.err.println("\t" + nxt.toString());
                    printer.print(',');
                }//for
            }
//////////////////////////////////////////////////////////////////            
//////////////////////////////////////////////////////////////////            
//////////////////////////////////////////////////////////////////
            if (stdev) {
                for (Values nxt : reduceByMeanValue) {
                    if (nxt == null) {
                        printer.print("NULL");
                    } else {
                        printer.print(nxt.stddev());
                    }
                    printer.print(',');

//xxx                    System.err.println("<std printing> ");
//                    System.err.println("\t" + nxt.stddev());
//                    System.err.println("\t" + nxt.toString());
                }//for
            }

            printer.println();
        }
    }

    private static void warnUser(Map<File, Set<String>> totallyLacks, Map<File, Set<String>> partiallyLacks) {
        for (Map.Entry<File, Set<String>> entry : totallyLacks.entrySet()) {
            File key = entry.getKey();
            Set<String> value = entry.getValue();

            Logger.getGlobal().log(Level.WARNING,
                    "File "
                    + "\"{0}"
                    + "\" "
                    + "lacks statistics "
                    + " for these titles:\n{1}",
                    new Object[]{
                        key.getAbsolutePath(),
                        CommonFunctions.toString(value)
                    }
            );
        }

        for (Map.Entry<File, Set<String>> entry : partiallyLacks.entrySet()) {
            File key = entry.getKey();
            Set<String> value = entry.getValue();

            Logger.getGlobal().log(Level.FINE,
                    "File "
                    + "\"{0}"
                    + "\" "
                    + "lacks some values "
                    + " for these titles:\n{1}",
                    new Object[]{
                        key.getAbsolutePath(),
                        CommonFunctions.toString(value)
                    }
            );
        }
    }

    /**
     * Prints the titles of statistics for each ignored setup parameter. Results
     * from different setups are grouped together, thus the title of each
     * statistics is repeated for each different setup parameter value.
     *
     * @param printTimes
     * @param resultsGrouped
     * @param statTitles
     * @param ignoredSetupParams
     * @param printer
     * @throws StatisticException
     */
    public static void printTitles__perResFilePerGrp(
            boolean printTimes, Set<SetupsGrp> resultsGrouped,
            Set<String> statTitles,
            Set<String> ignoredSetupParams,
            PrintStream printer) throws StatisticException {

        // print ,the time first
        if (printTimes) {
            printer.print(TIME);
            printer.print(',');
        }

        Iterator<SetupsGrp> resultsGrouped_iter = resultsGrouped.iterator();
        while (resultsGrouped_iter.hasNext()) {
            SetupsGrp nxt_grp = resultsGrouped_iter.next();

            /* 
          * For each results file in the next group:
          * - print the title about the statistic title + grouping ignored parameter 
             */
            Set<ResultsFile> groupMembers = nxt_grp.members();
            for (ResultsFile nxt_resFile : groupMembers) {
                /* 
             * for every ignored parameter, find their value in the results file
                 */
                StringBuilder paramVals = new StringBuilder();
                for (String ignoredParam : ignoredSetupParams) {
                    paramVals.append(ignoredParam).append('=');
                    paramVals.append(nxt_resFile.getSetupParam(ignoredParam).getValue());
                }
                String paramVals_str = paramVals.toString();
                for (String nxtTitle : statTitles) {
                    printer.print(nxtTitle);
                    if (!paramVals_str.isEmpty()) {
                        printer.print("-" + paramVals_str);
                    }
                    printer.print(',');
                }
            }// For each results file in the next group

            printer.print(',');// print an extra comma gap to separate between groups.

        }
        printer.println();
    }

    /**
     * Prints the titles once per group. Titles are printed for mean, stdev, and
     * conf. interval is the latter are to be printed.
     *
     * @param printTimes
     * @param resultsGrouped
     * @param statTitles
     * @param ignoredSetupParams
     * @param printer
     * @throws StatisticException
     */
    @Deprecated
    public static void printTitlesPerGrp(
            boolean printTimes, Set<SetupsGrp> resultsGrouped,
            Set<String> statTitles,
            Set<String> ignoredSetupParams,
            PrintStream printer, boolean mean, boolean stdev, Statistics.ConfidenceInterval confLvl) throws StatisticException {

        // print ,the time first
        if (printTimes) {
            printer.print(TIME);
            printer.print(',');
        }

        Iterator<SetupsGrp> resultsGrouped_iter = resultsGrouped.iterator();
        while (resultsGrouped_iter.hasNext()) {
            resultsGrouped_iter.next();
            //<editor-fold defaultstate="collapsed" desc=" if (mean)">
            if (mean) {
                for (String nxtStatTitle : statTitles) {
                    printer.print(nxtStatTitle);
//                    printer.print("(mean)");
                    printer.print(',');

                }
            }
            //</editor-fold>
            //<editor-fold defaultstate="collapsed" desc=" if (stdev)">
            if (stdev) {
                for (String nxtStatTitle : statTitles) {
                    printer.print(nxtStatTitle);
                    printer.print("(stdev)");
                    printer.print(',');
                }
            }
            //</editor-fold>
            //<editor-fold defaultstate="collapsed" desc=" if(confLvl != Statistics.ConfidenceInterval.NONE)">
            if (confLvl != Statistics.ConfidenceInterval.NONE) {
                for (String nxtStatTitle : statTitles) {
                    printer.print(nxtStatTitle);
                    printer.print("(" + confLvl.getConfidencePercentile() + ")");
                    printer.print(',');
                }
            }
            //</editor-fold>
            if (resultsGrouped_iter.hasNext()) { // if there is a next group, print a gap (empty) column
                printer.print(',');
            }
        }
        printer.println();
    }

    public static void printTitles(
            boolean printTimes,
            Set<String> statTitles,
            Set<String> ignoredSetupParams,
            PrintStream printer, boolean mean, boolean stdev, Statistics.ConfidenceInterval confLvl) throws StatisticException {

        // print ,the time first
        if (printTimes) {
            printer.print(TIME);
            printer.print(',');
        }

        //<editor-fold defaultstate="collapsed" desc=" if (mean)">
        if (mean) {
            for (String nxtStatTitle : statTitles) {
                printer.print(nxtStatTitle);
//                    printer.print("(mean)");
                printer.print(',');

            }
        }
        //</editor-fold>
        //<editor-fold defaultstate="collapsed" desc=" if (stdev)">
        if (stdev) {
            for (String nxtStatTitle : statTitles) {
                printer.print(nxtStatTitle);
                printer.print("(stdev)");
                printer.print(',');
            }
        }
        //</editor-fold>
        //<editor-fold defaultstate="collapsed" desc=" if(confLvl != Statistics.ConfidenceInterval.NONE)">
        if (confLvl != Statistics.ConfidenceInterval.NONE) {
            for (String nxtStatTitle : statTitles) {
                printer.print(nxtStatTitle);
                printer.print("(" + confLvl.getConfidencePercentile() + ")");
                printer.print(',');
            }
        }
        //</editor-fold>

        printer.println();
    }

    public static ResultsFile[] parseResultsFiles(
            Set<String> selectedTitles, File[] selectedFiles, String operation, int skipUntilTime)
            throws IOException, FileNotFoundException, StatisticException {

        List<ResultsFile> rfList = new ArrayList<>();
        for (File nxtCSVFile : selectedFiles) {

            //hack
            if (nxtCSVFile == null) {
                continue;
            }

            try {
                ResultsFile rf = ResultsFileParser.parseFile(
                        selectedTitles, nxtCSVFile,
                        operation, skipUntilTime
                );
                rfList.add(rf);

                LOG.log(Level.INFO,
                        "File \""
                        + nxtCSVFile.getName()
                        + "\""
                        + " was sucessfully parsed."
                );
            } catch (IOException | NumberFormatException | ResultFileMalformedException | UnsupportedOperationException exp) {
                LOG.log(Level.WARNING,
                        "Ignoring file: "
                        + nxtCSVFile.getName(),
                        exp
                );
            }
        }

        return rfList.toArray(new ResultsFile[rfList.size()]);
    }

    public static Statistics.ConfidenceInterval chooseConfidenceInterval() throws HeadlessException {
        /* one radio button for each confidence interval enum category*/
        Statistics.ConfidenceInterval[] confidenceLevels = Statistics.ConfidenceInterval.values();

        //<editor-fold defaultstate="collapsed" desc="jpanels with message content and radios for the optionpane">
        JPanel msgPanel = new JPanel(new BorderLayout()); // panel with message on north and radios on south
        msgPanel.add(new JLabel("Choose a confidence level out of the following:"), BorderLayout.NORTH);

        JPanel confidenceRadiosPanel = new JPanel(new GridLayout(confidenceLevels.length / 3, 3)); // subpanel of radios
        msgPanel.add(confidenceRadiosPanel, BorderLayout.SOUTH);

        confidenceRadiosPanel.setToolTipText(Statistics.ConfidenceInterval.defaultConfinceTooltip
                + ". Also, you may chose " + Statistics.ConfidenceInterval.NONE.name()
                + " to disable the computation of confidence intervals.");
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="tryAdd radios, group them, preseection etc...">
        ButtonGroup radGrp = new ButtonGroup();

        for (Statistics.ConfidenceInterval nxt_confLev : confidenceLevels) {
            JRadioButton radio = new JRadioButton(nxt_confLev.getConfidencePercentile());
            radGrp.add(radio);

            radio.setToolTipText(nxt_confLev.toString());
            confidenceRadiosPanel.add(radio);
            if (nxt_confLev == Statistics.ConfidenceInterval.PERCENTILE_95) {
                radio.setSelected(true);
            } else {
                radio.setSelected(false);
            }
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="show the options dialog">
        int confDialog = JOptionPane.showOptionDialog(
                null, msgPanel, "Level of confidence",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);

        if (confDialog == JOptionPane.CANCEL_OPTION) {
            userAbortedMsg();
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="find  the selected radio and infer and return the enum chosen">
        Enumeration<AbstractButton> elements = radGrp.getElements();
        while (elements.hasMoreElements()) {
            JRadioButton jRadButton = (JRadioButton) elements.nextElement();
            if (jRadButton.isSelected()) {
                return Statistics.ConfidenceInterval.find(jRadButton.getText());
            }
        }//while
        //</editor-fold>

        throw new UnsupportedOperationException("No choice from user.. sth is wrong here..");
    }

    public static JFileChooser selectFiles(String dirPath, boolean exitOnAbort, String title, String tooltip) {
        int optionReturned = 0;

        JFileChooser jchosser = new JFileChooser(dirPath);

        jchosser.setMultiSelectionEnabled(true);
        jchosser.setFileSelectionMode(jchosser.FILES_AND_DIRECTORIES);

        jchosser.setToolTipText(tooltip);
        jchosser.setDialogTitle(title);
        jchosser.setApproveButtonText("Select");
        jchosser.setApproveButtonMnemonic('S');
        jchosser.setMultiSelectionEnabled(true);
        jchosser.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".csv");
            }

            @Override
            public String getDescription() {
                return "Accepts only csv (comma separated) files";
            }
        });

        do {
            optionReturned = jchosser.showDialog(null, "Select");
        } while (optionReturned != JFileChooser.CANCEL_OPTION && optionReturned != JFileChooser.APPROVE_OPTION);

        if (optionReturned == JFileChooser.CANCEL_OPTION) {
            userAbortedMsg();
            if (exitOnAbort) {
                System.exit(1);
            }
            return null;
        }

        return jchosser;
    }

    /**
     * Groups the ResultFile instances from the parameter array, to SetupsGrp
     * instances based on common scenario parameter setting (parameter name and
     * value), based on parameters excluding the ones to be ignored.
     *
     * @param resultsFiles
     * @param paramsIgnrd Ignores these parameter names while grouping.
     *
     * @return a set of grouped ResultsFile instance
     */
    @Deprecated
    public static Set<SetupsGrp> groupRFs(ResultsFile[] resultsFiles, Set<String> paramsIgnrd) {

        Set<SetupsGrp> groups = new HashSet<>();
        /*
       * - take the head of the list or stop looping if the list is empty
       * - loop over the list of all results files, 
       * - find the ones that are equal to the head of the list
       * - exclude them from beeing searched again
         */
        List<ResultsFile> resList = new ArrayList<>();
        resList.addAll(Arrays.asList(resultsFiles));

        ResultsFile head = null;

        while (!resList.isEmpty()) {
            if (null == (head = resList.remove(0))) {
                continue; // ignore problematic files passed as null references
            }

            SetupsGrp nxtGrp = new SetupsGrp(head, paramsIgnrd);
            groups.add(nxtGrp);

            for (Iterator<ResultsFile> it = resList.iterator(); it.hasNext();) {
                ResultsFile nxt_ResFile = it.next();
                if (nxtGrp.tryAdd(nxt_ResFile)) {
                    it.remove();
                    /* if found its group.. no need to recheck in next while loop
                * Otherwise it is left in the list and thus it will be rechecked in a next 
                * loop.*/

                }
            }//for
        }//while

        return groups;
    }

}
