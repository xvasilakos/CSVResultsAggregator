package stats;

import exceptions.InconsistencyException;
import exceptions.InvalidOrUnsupportedException;
import exceptions.StatisticException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import static stats.Constants.AGGR_RESULTS_FILE__DEFAULT_TIME;

import stats.output.aggregating.ResultsFile;
import utilities.CommonFunctions;
import utilities.Couple;

/**
 * Instances of this class keep track of statistics over simTime.
 *
 * The values of each statistic recorded at a point of simulation simTime t are
 * kept in a instance of class StatsToValuesMapping, which in turn keeps the
 * multiple values mapped to the same statistics title. The StatsToValuesMapping
 * instance is mapped to the point of simTime t. Mapped
 * simTime-to-StatsToValuesMapping are kept in ascending order after simTime t.
 *
 * Note that there can be multiple values recorded in an StatsToValuesMapping
 * instance, which implies that many values for the same simTime t, that refer
 * to the same statistic, can be separately aggregated per within t.
 *
 * Last, this class offers the ability to aggregate all StatsToValuesMapping
 * separate aggregate values per statistic per simTime. This means that you can
 * compute the mean/stddev/variance/confidence interval values (through the
 * different times t recorded) of the different mean/stddev/variance/confidence
 * interval aggregates per each simTime t.
 *
 * @author xvas
 */
public class Statistics {

    public static final int DEFAULT_ROUND_DECIMAL = 4;

    private final SortedMap<Integer, StatsToValuesMapping> _stats2TimeMapping;
    private final Set<String> _statisticsTitles;
    private final int _roundDecimal;

    private boolean _transientTitlesWerePrint;
    private boolean _transientAggrTitlesWerePrint;
    private final Logger _logger;

    public enum ConfidenceInterval {

        NONE("NONE", -1.0),
        Percentile_50("50%", 0.674),
        Percentile_60("60%", 0.842),
        Percentile_70("70%", 1.036),
        Percentile_80("80%", 1.282),
        Percentile_90("90%", 1.645),
        PERCENTILE_95("95%", 1.96),
        Default("default", 1.96),
        Percentile_98("98%", 2.326),
        Percentile_99("99%", 2.576),
        Percentile_99_5("99.9%", 3.291);
        private final String confidencePercentile;
        private final double z;
        public static String defaultConfinceTooltip = Default.name() + " implies " + PERCENTILE_95.confidencePercentile + " confidence";

        private ConfidenceInterval(String value, double z) {
            this.confidencePercentile = value;
            this.z = z;
        }

        public double z() {
            return z;
        }

        @Override
        public String toString() {
            return name() + ": <confidence level=" + confidencePercentile + ", z=" + z + ">";
        }

        /**
         * @param confidencePercentile e.g. "50%", "60%", .. "default", or by
         * name, e.g. "Percentile_50" or "Default"
         * @return
         */
        public static ConfidenceInterval find(String confidencePercentile) {
            for (ConfidenceInterval conf : ConfidenceInterval.values()) {
                if (conf.getConfidencePercentile().equals(confidencePercentile)) {
                    return conf;
                }
            }
            ConfidenceInterval conf = valueOf(confidencePercentile.trim());
            return null;
        }

        public static double getZFor(String name) {
            for (ConfidenceInterval conf : ConfidenceInterval.values()) {
                if (conf.getConfidencePercentile().equals(name)) {
                    return conf.z();
                }
            }
            return -1;
        }

        public String getConfidencePercentile() {
            return this.confidencePercentile;
        }
    };

    public static String toTitlesCSV(String[] statTitles, boolean mean, boolean stddev, Statistics.ConfidenceInterval confInterval) {
        if (statTitles.length == 0) {
            return "";
        }
        StringBuilder _toString = new StringBuilder();
        for (String nxt_title : statTitles) {
            if (mean) {
                _toString.append(nxt_title);
                _toString.append("").append(',');
            }
            if (stddev) {
                _toString.append(nxt_title);
                _toString.append("(stddev)").append(',');
            }
            if (confInterval != Statistics.ConfidenceInterval.NONE) {
                _toString.append(nxt_title);
                _toString.append('(').append(confInterval.getConfidencePercentile()).append("conf.)").append(',');
            }

        }
        return _toString.toString();
    }

    public static String toTitlesCSV(Collection<String> titles, boolean mean, boolean stddev, Statistics.ConfidenceInterval confInterval) {
        return toTitlesCSV(titles.toArray(new String[titles.size()]), mean, stddev, confInterval);
    }

    public static String toTitlesCSV(Collection<String> titles) {
        return toTitlesCSV(
                titles.toArray(new String[titles.size()]), false, false, Statistics.ConfidenceInterval.NONE
        );
    }

    public Statistics() {
        _roundDecimal = DEFAULT_ROUND_DECIMAL;
        _logger = Logger.getLogger(Statistics.class.getCanonicalName());

        _transientTitlesWerePrint = false;
        _transientAggrTitlesWerePrint = false;
        _statisticsTitles = new TreeSet<>();
        _stats2TimeMapping = new TreeMap();
    }

    /**
     * Checks if this statistic is recorded; if not, then it throws an
     * StatisticException
     *
     * @param statTitle
     * @throws StatisticException
     */
    private void checkName(String statTitle) throws StatisticException {
        if (!_statisticsTitles.contains(statTitle)) {
            throw new StatisticException(statTitle);
        }
    }

    /**
     * @return the titles of the recorded statistics
     */
    public synchronized Set<String> getStatisticsTitles() {
        return Collections.unmodifiableSet(_statisticsTitles);
    }

    public synchronized SortedSet<Integer> times() {
        return Collections.unmodifiableSortedSet((SortedSet<Integer>) _stats2TimeMapping.keySet());
    }

    public synchronized void addTitle(String statTitle) {
        _statisticsTitles.add(statTitle);
    }

    public synchronized String resultsTransient(boolean prinAll,
            boolean mean, boolean stddev, ConfidenceInterval confInterval) throws StatisticException {
        StringBuilder sb = new StringBuilder(1024);

        if (!_transientTitlesWerePrint) {// in order to print titles only once at the beggining
            sb.append("Time,").append(Statistics.toTitlesCSV(
                    _statisticsTitles, mean, stddev, confInterval)).append('\n');
            _transientTitlesWerePrint = true;
        }

        if (!prinAll) {
            return sb.append(
                    resultsTransientMostRecent(mean, stddev, confInterval)
            ).toString();
        }

        Set<Integer> timesSet = _stats2TimeMapping.keySet();
        Iterator<Integer> timesIter = timesSet.iterator();
        while (timesIter.hasNext()) {
            Integer nxtTime = timesIter.next();

            StatsToValuesMapping recordedAtNxtTime = _stats2TimeMapping.get(nxtTime);
            String csvLine = recordedAtNxtTime.toCSVString(mean, false, stddev, confInterval, _statisticsTitles, false);
            // add simTime followed by csv values
            sb.append(nxtTime).append(',').append(csvLine).append("\n");
        }
        return sb.toString();
    }

    private String resultsTransientMostRecent(
            boolean mean, boolean stddev, ConfidenceInterval confInterval) throws StatisticException {
        StringBuilder sb = new StringBuilder(1024);

        Integer mostRecentTime = _stats2TimeMapping.lastKey();

        StatsToValuesMapping recordedAt_nxt_Time = _stats2TimeMapping.get(mostRecentTime);
        String csvLine = recordedAt_nxt_Time.toCSVString(mean, false, stddev, confInterval, _statisticsTitles, false);
        // add simTime followed by csv values
        sb.append(mostRecentTime).append(',').append(csvLine).append("\n");

        return sb.toString();
    }

    public synchronized void addValuesForTime(int time, String statTitle, Double... value) throws StatisticException {
        // ensure record exists in map
        StatsToValuesMapping titles2Values;
        if ((titles2Values = _stats2TimeMapping.get(time)) == null) {
            titles2Values = new StatsToValuesMapping();
            _stats2TimeMapping.put(time, titles2Values);
        }

        // in any case addValuesForTime for statTitle, and if first simTime added, ensure recorded in knwon names ..
        titles2Values.update(statTitle, _roundDecimal, value);

        _statisticsTitles.add(statTitle);
    }

    public synchronized void addValuesForTime(int time, StatsToValuesMapping titles2Values, File file)
            throws StatisticException {
        StatsToValuesMapping prevExists = _stats2TimeMapping.put(time, titles2Values);
        if (prevExists != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("A record allready exists for time ")
                    .append(time).append(" in statistics of file ")
                    .append('\"').append(file.getAbsolutePath()).append('\"')
                    .append(".\n")
                    .append("The old value(s) will be ignored.");
            _logger.log(Level.SEVERE, sb.toString(),
                    new StatisticException(sb.toString()));
            System.err.println("Fatality..");
            System.exit(-100);
//            throw new StatisticException(sb.toString());
        }
        _statisticsTitles.addAll(titles2Values.getTitles());
    }

    public synchronized void addValuesForAggr(StatsToValuesMapping titles2Values) {
        StatsToValuesMapping prev = _stats2TimeMapping.get(AGGR_RESULTS_FILE__DEFAULT_TIME);
        if (prev != null) {
            prev.update(_roundDecimal, titles2Values);
            
            System.err.println("**" 
                    + prev.toCSVString(true, true, true, ConfidenceInterval.PERCENTILE_95, true)
            );//xxx
            
        } else {
            _stats2TimeMapping.put(AGGR_RESULTS_FILE__DEFAULT_TIME, titles2Values);
        }
        // in case, make sure you added the titles
        _statisticsTitles.addAll(titles2Values.getTitles());
    }

    /**
     * Invoke this method to finalize and compress state in memory for the given
     * simulation simTime. State is compressed in the sense that discrete values
     * become garbage collectable.
     *
     * State finalization implies that after invoking this method you cannot
     * record more values for this simTime; otherwise the aggregated results
     * returned are inaccurate and the overall behavior undefined.
     *
     * @param time
     * @throws StatisticException
     *
     */
    public synchronized void finalizeState(int time) throws StatisticException {
        // ensure record exists in map
        StatsToValuesMapping recs;
        if ((recs = _stats2TimeMapping.get(time)) == null) {
            throw new InconsistencyException("Illegal action:"
                    + " Trying to finalize statistics record for time " + time + " that is not recorded."
            );
        }
        recs.ensureTitles(_statisticsTitles);
        recs.finalizeState();

    }

    public synchronized Values aggregates(
            String statTitle, int atTime, ResultsFile resFile)
            throws StatisticException {
        StatsToValuesMapping m = _stats2TimeMapping.get(atTime);
        if (m == null) {
            return null;
        }

        if (!m.containsStatTitle(statTitle)) {
//            if (atTime == AGGR_RESULTS_FILE__DEFAULT_TIME) {
//                _logger.log(Level.WARNING,
//                        "There are no values found for statistic \"{0}"
//                        + "\""
//                        + "\n"
//                        + "Loaded statistic titles are: {1}",
//                        new Object[]{
//                            statTitle,
//                            CommonFunctions.toString(_statisticsTitles)
//                        }
//                );
//            } else {
//                _logger.log(Level.WARNING,
//                        "There are no values found for statistic \"{0}"
//                        + "\""
//                        + " at time={1}"
//                        + "\n"
//                        + "Loaded statistic titles are: {2}",
//                        new Object[]{
//                            statTitle,
//                            atTime,
//                            CommonFunctions.toString(_statisticsTitles)
//                        }
//                );
//            }
            return null;
        }

        return m.aggregatesFor(statTitle);
    }

    /**
     * @param statTitles
     * @param startTime starting from this simTime up until the end
     * @return a list in which each next element contains an array of (name,
     * aggregates) couples, with next list element referencing the next simTime
     * period.
     */
    public synchronized List<Couple<String, Values>[]> aggregates(int startTime, Collection<String> statTitles) {
        List<Couple<String, Values>[]> _aggregates = new ArrayList<>();

        // for every simTime-record entry
        for (Map.Entry<Integer, StatsToValuesMapping> entry : _stats2TimeMapping.entrySet()) {
            Integer nxt_time = entry.getKey();
            if (nxt_time < startTime) {
                continue;
            }

            StatsToValuesMapping rec_atTime = entry.getValue();
            Iterator<String> statTitle_iter = statTitles.iterator();
            Couple<String, Values>[] coupleArray = new Couple[statTitles.size()];
            // for every aggregate per stat title
            int i = 0;
            while (statTitle_iter.hasNext()) {
                String statTitle = statTitle_iter.next();
                coupleArray[i++] = new Couple(statTitle, rec_atTime.aggregatesFor(statTitle));
            }
            _aggregates.add(coupleArray);
        }
        return _aggregates;
    }

    /**
     * @param statTitles
     * @param atTime
     * @return an array of (title, aggregates) couples for simTime atTime.
     * @throws exceptions.StatisticException
     */
    public synchronized Couple<String, Values>[] statTitleAggregates(Collection<String> statTitles, int atTime)
            throws StatisticException {
        Couple<String, Values>[] _statTitle_aggregates = new Couple[statTitles.size()];

        StatsToValuesMapping rec_atTime = _stats2TimeMapping.get(atTime);
        Iterator<String> statTitle_iter = statTitles.iterator();
        int i = 0;
        while (statTitle_iter.hasNext()) {
            String statTitle = statTitle_iter.next();
            Values aggr = rec_atTime.aggregatesFor(statTitle);
            if (aggr == null) {
                throw new StatisticException(
                        "There is no record kept for statistic \"" + statTitle + "\" at time " + atTime);
            }
            _statTitle_aggregates[i++] = new Couple(statTitle, aggr);
        }
        return _statTitle_aggregates;
    }

    /**
     * @param statTitle the title of a particular statistic title
     * @return mean value in aggregates per each simTime
     * @throws exceptions.StatisticException
     * @throws exceptions.InvalidOrUnsupportedException
     */
    public synchronized Values mean(String statTitle) throws StatisticException, InvalidOrUnsupportedException {
        checkName(statTitle);
        Values _aggregatesForMean = new Values(_roundDecimal);
        boolean nonFound = true;

        // for every simTime
        for (Map.Entry<Integer, StatsToValuesMapping> entry : _stats2TimeMapping.entrySet()) {
            // get aggregate for certain stat
            Values nxtTimeAggr = entry.getValue().aggregatesFor(statTitle);
            if (nxtTimeAggr != null) {
                _aggregatesForMean.updt(nxtTimeAggr.mean());
                nonFound = false;
            }
        }

        return nonFound ? null : _aggregatesForMean;
    }

    public synchronized Couple<String, Values>[] aggregatesForMean(Collection<String> statTitles)
            throws StatisticException, InvalidOrUnsupportedException {
        Couple<String, Values>[] _aggregatesForMean_arr = new Couple[statTitles.size()];
        int i = 0;
        for (String nxt_stat : statTitles) {
            Values nxt_statAggr = mean(nxt_stat);
            _aggregatesForMean_arr[i++] = new Couple<>(nxt_stat, nxt_statAggr);
        }
        return _aggregatesForMean_arr;
    }

    /**
     *
     * @param statTitle
     * @param fromTime start aggregating from this simTime and on
     * @return mean value in aggregates per each simTime
     * @throws exceptions.StatisticException
     * @throws exceptions.InvalidOrUnsupportedException
     */
    public synchronized Values mean(String statTitle, int fromTime)
            throws StatisticException, InvalidOrUnsupportedException {
        checkName(statTitle);
        Values _aggregatesForMean = new Values(_roundDecimal);

        for (Map.Entry<Integer, StatsToValuesMapping> entry : _stats2TimeMapping.entrySet()) {
            if (entry.getKey() < fromTime) {
                continue;
            }

            Values nxtTimeAggr = entry.getValue().aggregatesFor(statTitle);
            if (nxtTimeAggr != null) {
                _aggregatesForMean.updt(nxtTimeAggr.mean());
            }
        }

        return _aggregatesForMean;
    }

    public synchronized Couple<String, Values>[] mean(Collection<String> statTitles, int minTime)
            throws StatisticException, InvalidOrUnsupportedException {
        Couple<String, Values>[] _aggregatesForMean_arr = new Couple[statTitles.size()];
        int i = 0;
        for (String nxt_stat : statTitles) {
            Values nxt_statAggr = mean(nxt_stat, minTime);
            _aggregatesForMean_arr[i++] = new Couple<>(nxt_stat, nxt_statAggr);
        }
        return _aggregatesForMean_arr;
    }

    /**
     *
     * @param statTitle
     * @param fromTime start aggregating from this simTime and on
     * @param maxTime stop aggregating after this max simTime threshold
     * @return aggregates with respect to the recorded mean value in aggregates
     * per each simTime
     * @throws exceptions.StatisticException
     */
    public synchronized Values mean(String statTitle, int fromTime, int maxTime)
            throws StatisticException {
        checkName(statTitle);
        Values theMean = new Values(_roundDecimal);

        for (Map.Entry<Integer, StatsToValuesMapping> entry : _stats2TimeMapping.entrySet()) {
            if (entry.getKey() < fromTime) {
                continue;
            }
            if (entry.getKey() > maxTime) {
                break;
            }

            Values nxtTime2Aggr = entry.getValue().aggregatesFor(statTitle);
            if (nxtTime2Aggr != null) {
                theMean.updt(nxtTime2Aggr.mean());
            }
        }

        return theMean;
    }

    public synchronized Couple<String, Values>[] mean(Collection<String> statTitles, int minTime, int maxTime)
            throws StatisticException, InvalidOrUnsupportedException {
        Couple<String, Values>[] aggrsForMeanArr = new Couple[statTitles.size()];
        int i = 0;
        for (String nxtTitle : statTitles) {
            Values nxtStat2Aggr = mean(nxtTitle, minTime, maxTime);
            aggrsForMeanArr[i++] = new Couple<>(nxtTitle, nxtStat2Aggr);
        }
        return aggrsForMeanArr;
    }

    @Override
    public String toString() {
        StringBuilder _toString = new StringBuilder();
        // print titles
        _toString.append("TIME,").append(CommonFunctions.toString("", "", "", ",", _statisticsTitles)).append('\n');

        for (Map.Entry<Integer, StatsToValuesMapping> entry : _stats2TimeMapping.entrySet()) {
            Integer time = entry.getKey();
            StatsToValuesMapping stats2Values = entry.getValue();

            _toString.append(time).append(',');// append simTime

            //<editor-fold defaultstate="collapsed" desc="append value for every title">
            Iterator<String> iterator = _statisticsTitles.iterator();
            while (iterator.hasNext()) {
                String statTitle = iterator.next();
                try {
                    _toString.append(statTitle).append("-mean").append(':');
                    _toString.append(stats2Values.mean(statTitle)).append(',');
                    _toString.append(statTitle).append("-sum").append(':');
                    _toString.append(stats2Values.sum(statTitle)).append(',');
                    _toString.append('\n');
                } catch (StatisticException ex) {
                    throw new RuntimeException(ex);
                }
            }
            //</editor-fold>

            _toString.append('\n');
        }

        return _toString.toString();

    }
}
