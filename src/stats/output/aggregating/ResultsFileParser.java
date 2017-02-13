package stats.output.aggregating;

import exceptions.ResultFileMalformedException;
import exceptions.StatisticException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import stats.Statistics;
import static stats.Statistics.DEFAULT_ROUND_DECIMAL;
import stats.StatsToValuesMapping;
import static stats.Constants.AGGR_RESULTS_FILE__DEFAULT_TIME;
import static stats.Constants.IGNORE;
import static stats.Constants.IGNORE_REPEAT;
import static stats.Constants.MODE__AGGREGATES_STATS;
import static stats.Constants.MODE__TRANSIENT_AGGREGATES_STATS;
import static stats.Constants.MODE__TRANSIENT_STATS;
import static stats.Constants.REPEAT_DETAILS_BEGIN;
import static stats.Constants.REPEAT_DETAILS_END;
import static stats.Constants.SCENARIO_SETUP_BEGIN;
import static stats.Constants.SCENARIO_SETUP_END;
import static stats.Constants.TIME;
import utilities.CommonFunctions;

/**
 *
 * @author xvas
 */
public class ResultsFileParser {

    /**
     * Invoke after reading scenario setup parameters and scenario repeat
     * details.
     *
     * @param bfin
     * @return
     */
    private static List<String> loadStatisticsTitles(
            Set<String> selectedTitles, String csvTitles, String filePath) {

        Set<String> tmp = new HashSet<>(selectedTitles);

        List<String> titlesFilter = new ArrayList();
        StringTokenizer tokTitles = new StringTokenizer(csvTitles, ",");
        while (tokTitles.hasMoreTokens()) {
            String nxtStatTile = tokTitles.nextToken().trim();
            if (tmp.contains(nxtStatTile)) {
                tmp.remove(nxtStatTile);
                titlesFilter.add(nxtStatTile);
            } else if (!nxtStatTile.equalsIgnoreCase(TIME)) {
                // i.e. from the rest only time is not marked with ignored 
                titlesFilter.add(IGNORE);
            }
        }// inner while

        if (!tmp.isEmpty()) {
            LOG.log(Level.WARNING, "Results in file "
                    + "\"{0}"
                    + "\""
                    + " lack the following statistics: {1}", new Object[]{filePath, CommonFunctions.toString(tmp)});
        }

        return titlesFilter;
    }

    private static String readUntilEmptyOrEnd(BufferedReader bfin) throws IOException {
        String nxtLine;
        while ((nxtLine = bfin.readLine()) != null && nxtLine.trim().isEmpty()) ;
        return nxtLine;
    }

//    /**
//     * To be invoked for reading reading every csv line.
//     *
//     * @param csvLine
//     * @param statTitles
//     * @throws NumberFormatException
//     */
//    private static Couple<Set<String>, StatsToValuesMapping> loadCSVLine4Transient(
//            String csvLine, List<String> statTitles, File file)
//            throws NumberFormatException {
//
//        csvLine = csvLine.substring(csvLine.indexOf(',') + 1);// in order to ignore time values when parsing based on titles of statistics
//
//        return loadCSVValues(csvLine, statTitles, file);
//
////        StatsToValuesMapping titls2multVals = new StatsToValuesMapping();
////        String[] tokValues = csvLine.split(",");
////        int statTitlesIdx = 0;
////        for (String nxtVal : tokValues) {
////
////            String nxtTitle = statTitles.get(statTitlesIdx++);
////
////            if (nxtTitle.equals(IGNORE)) {
////                continue;//ignore this statitsitc 
////            }
////            if (nxtVal.isEmpty()) {
////                nxtVal = "NaN";//so ignore this particular value, yet include the statistic title
////            }
////
////            titls2multVals.update(
////                    nxtTitle,
////                    DEFAULT_ROUND_DECIMAL,
////                    Double.valueOf(nxtVal));
////
////        }
////        return titls2multVals;
//    }
    private static StatsToValuesMapping parseCSVValues(
            Map<String, List<Integer>> emptyValueTitles,
            String csvLine, List<String> statTitlesFilter,
            File file, int time) throws ResultFileMalformedException {

        StatsToValuesMapping titls2multVals = new StatsToValuesMapping();
        String[] tokValues = csvLine.split(",");
        int statTitlesIdx = 0;

        for (String nxtVal : tokValues) {

            String nxtTitle = statTitlesFilter.get(statTitlesIdx++);

            if (nxtTitle.equals(IGNORE)) {
                continue;//inogre this value
            }

            if (nxtVal.isEmpty()) {
                List<Integer> times = emptyValueTitles.get(nxtTitle);
                if (times == null) {
                    emptyValueTitles.put(nxtTitle, (times = new ArrayList<>()));
                }
                times.add(time);
//                nxtVal = "NaN"; // so to include the title, yet not consider the value inside the next calls
                continue;
            }

            double v;
            try {
                if (nxtVal.endsWith("%")) {
                    nxtVal = nxtVal.trim().replace("%", "");
                    v = Double.valueOf(nxtVal) / 100.0;
                } else {
                    v = Double.valueOf(nxtVal);
                }
            } catch (NumberFormatException e) {
                throw new ResultFileMalformedException(
                        "Unexpected or wrong value "
                        + "\"" + nxtVal + "\""
                        + "parsed for statistic "
                        + "\"" + nxtTitle + "\""
                        + " in path "
                        + "\"" + file.getAbsolutePath() + "\""
                        + ".");
            }

            // this ignores NaN values
            titls2multVals.update(
                    nxtTitle,
                    DEFAULT_ROUND_DECIMAL,
                    v);

        }
        return titls2multVals;
    }
    private static final Logger LOG = Logger.getLogger(ResultsFileParser.class.getName());

    private static Statistics parseLines(boolean isTransient,
            int skipUntilTime, List<String> statTitlesFilter,
            BufferedReader bfin, File file)
            throws IOException, ResultFileMalformedException, StatisticException {

        // maps the titles with some empty values to the times for which there are no values recorded.
        Map<String, List<Integer>> emptyValueTitles = new HashMap<>();

        Statistics statsOfFile = new Statistics();

        String csvNxtLine;
        while (null != (csvNxtLine = bfin.readLine())) {
            StatsToValuesMapping titles2values;

            int time = -1;
            if (isTransient) {
                //then read the time first
                int firstCommaPos = csvNxtLine.indexOf(',');
                time = Integer.valueOf(csvNxtLine.substring(0, firstCommaPos));

                if (time < skipUntilTime) {
                    continue; // ignore the recorded values for this time 
                }
                // other wiseparse after the time value
                csvNxtLine = csvNxtLine.substring(firstCommaPos + 1);// in order to ignore time values when parsing based on titles of statistics
            }

            titles2values = parseCSVValues(emptyValueTitles, csvNxtLine, statTitlesFilter, file, time);

            titles2values.finalizeState();
            if (isTransient) {
                statsOfFile.addValuesForTime(time, titles2values, file);
            } else {// if aggregates of the whole simulation run
                statsOfFile.addValuesForAggr(titles2values);
            }
        }

        if (!emptyValueTitles.isEmpty()) {
            if (LOG.isLoggable(Level.FINER)) {
                // warning again, only with more details.
                LOG.log(Level.WARNING, "File in path "
                        + "\"{0}"
                        + "\"."
                        + " contains some lines with no values for statistics titles:\n{1}",
                        new Object[]{file.getAbsolutePath(),
                            CommonFunctions.toString(emptyValueTitles)});
            } else {
                LOG.log(Level.WARNING, "File in path "
                        + "\"{0}"
                        + "\"."
                        + " contains some lines with no values.",
                        new Object[]{file.getAbsolutePath()});
            }
        }
        return statsOfFile;
    }

    /**
     * *
     * Parses a results file and returns it as a ResultsFile instance. If
     * anything went wrong, logs the reason and returns null.
     *
     * @param selectedTitles
     * @param file
     * @param operation true if it is a transient results file; otherwise false
     * if it is an aggregates results file.
     * @param skipUntilTime if at transient results aggregation mode, up until
     * which time to skip results.
     * @return
     * @throws java.io.FileNotFoundException
     * @throws exceptions.StatisticException
     * @throws exceptions.ResultFileMalformedException
     */
    public static ResultsFile parseFile(Set<String> selectedTitles, File file,
            String operation, int skipUntilTime)
            throws FileNotFoundException, IOException,
            ResultFileMalformedException, StatisticException {

        String filePath = null;
        SortedMap<String, ScenarioParamBean> scenarioSetup = null;
        Map<String, Double> repeatDetails = null;/*key: repeat variable short name
       * Value: the value in Double format. Only double assumed..*/

        Statistics statsPerTime = null;

        BufferedReader bfin = new BufferedReader(new FileReader(file));

        filePath = file.getCanonicalPath();

        String csvLine = readUntilEmptyOrEnd(bfin);
        if (csvLine == null) {
            throw new ResultFileMalformedException("File  in path \"" + file.getAbsolutePath() + "\""
                    + " appears to be empty.");
        }

        List<String> statTitlesFilter = null;

        statTitlesFilter = loadStatisticsTitles(selectedTitles, csvLine, filePath);

        switch (operation) {
            case MODE__TRANSIENT_STATS:
            case MODE__TRANSIENT_AGGREGATES_STATS:
                if (!csvLine.toUpperCase().startsWith(TIME.toUpperCase())) {
                    throw new ResultFileMalformedException("Unxpected file format in path "
                            + "\"" + file.getAbsolutePath() + "\""
                            + "\n"
                            + "First line expected to start with statistics titles in the form of \"TIME, ...\""
                            + ", yet first non-empty line contains: \"" + csvLine + "\"");
                }
                statsPerTime = parseLines(true, skipUntilTime, statTitlesFilter, bfin, file);
                break;
            case MODE__AGGREGATES_STATS:
                statsPerTime = parseLines(false, AGGR_RESULTS_FILE__DEFAULT_TIME, statTitlesFilter, bfin, file);
                break;

            default:
                throw new UnsupportedOperationException(operation);
        }

        bfin.close();
        return new ResultsFile(filePath, scenarioSetup, repeatDetails, statsPerTime);
    }

    public static Map<String, Double> loadRepeatDetails(BufferedReader bfin) throws IOException, ResultFileMalformedException {
        Map<String, Double> repeatDetails = new HashMap<>(3);
        boolean read__repeatBegin = false;
        String nxtLine;
        WHILE:
        while ((nxtLine = bfin.readLine()) != null) {
            System.err.println("xxx loadRepeatDetails1: " + nxtLine);//xxx

            if (nxtLine.isEmpty() || nxtLine.equals("\"")) {
                continue;
            }

            if (nxtLine.equals(REPEAT_DETAILS_BEGIN)
                    || nxtLine.startsWith("\"" + REPEAT_DETAILS_BEGIN)) {
                if (read__repeatBegin) {
                    throw new ResultFileMalformedException(REPEAT_DETAILS_BEGIN + " is encountered more than once ");
                }
                read__repeatBegin = true;
                continue;
            } else if (nxtLine.startsWith(REPEAT_DETAILS_END)
                    || nxtLine.startsWith(REPEAT_DETAILS_END + "\"")) {
                break WHILE;
            } else {
                StringTokenizer tok = new StringTokenizer(nxtLine, ",");
                tok.nextToken(); // ignore full name of repeat parameter e.g.  SCs_Per_Point_Ratio
                repeatDetails.put(tok.nextToken(), Double.parseDouble(tok.nextToken()));
            }
        }
        return repeatDetails;
    }
}
