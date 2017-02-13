package stats;

import exceptions.StatisticException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import stats.Statistics.ConfidenceInterval;

/**
 * Keeps track of different statistic titles (categories) by mapping recorded
 * values to statistics titles. Values of a given statistic are recorded in an
 * Values instance, which is mapped to the title of the statistic. Note that
 * statistics and their corresponding values are kept in lexicographical order
 * according to statistics titles.
 *
 * @author xvas
 */
public class StatsToValuesMapping {

    /**
     * key: Statistic title
     *
     * value: Values aggregated
     */
    private final Map<String, Values> _statTitle2Values = new TreeMap<>();
    private ConfidenceInterval _confInterval;

    /**
     * Uses default confidence interval with 95% confidence.
     */
    public StatsToValuesMapping() {
        _confInterval = ConfidenceInterval.PERCENTILE_95;
    }

    /**
     * @param conf The confidence interval percentile to use.
     */
    public StatsToValuesMapping(ConfidenceInterval conf) {
        _confInterval = conf;
    }

    /**
     * @param confIntervalPercentile The confidence interval percentile to use.
     * @throws StatisticException
     */
    public StatsToValuesMapping(String confIntervalPercentile) throws StatisticException {
        this(ConfidenceInterval.find(confIntervalPercentile));
    }

    /**
     * NaN values are ignored.
     *
     * @param statName
     * @param roundDecismal
     * @param values
     * @return true if recorded this statName for the first time, otherwise
     * false
     */
    public boolean update(String statName, int roundDecismal, Double... values) {
        Values aggr;
        if ((aggr = _statTitle2Values.get(statName)) == null) {
            aggr = new Values(roundDecismal, values);
            _statTitle2Values.put(statName, aggr);
            return true;
        }
        aggr.updt(values);
        return false;
    }

    public boolean update(String statName, int rd, List<Double> values) {
        Values aggr;
        if ((aggr = _statTitle2Values.get(statName)) == null) {
            aggr = new Values(rd, values);
            _statTitle2Values.put(statName, aggr);
            return true;
        }
        aggr.updt(values);
        return false;
    }

    public boolean update(String title, int roundDecismal, double val) {
        Values aggr;
        boolean toReturn = false;
        if ((aggr = _statTitle2Values.get(title)) == null) {
            aggr = new Values(roundDecismal);
            _statTitle2Values.put(title, aggr);
            toReturn = true;
        }

        aggr.updt(val);

        return toReturn;
    }

    public void update(int rd, StatsToValuesMapping titles2Values) {
        for (String nxtT : titles2Values.getTitles()) {
            update(nxtT, rd, titles2Values.getValuesFor(nxtT));
        }
    }

    /**
     *
     * @param statName The title of the statistic
     *
     * @return An Values instance for the particular statistic or null if no
     * statistic such name is recorded.
     */
    public Values aggregatesFor(String statName) {
        return _statTitle2Values.get(statName);
    }

    /**
     * @param statName The title of the statistic.
     *
     * @return The mean value of the statistic
     *
     * @throws StatisticException in case there is no record for the statistic
     */
    public double mean(String statName) throws StatisticException {
        Values aggr;
        if ((aggr = _statTitle2Values.get(statName)) == null) {
            throw new StatisticException(statName + " is not recorded");
        }

        return aggr.mean();
    }

    /**
     * @param statName The title of the statistic.
     *
     * @return The summary of values recorder for the particular statistic
     *
     * @throws StatisticException in case there is no record for the statistic
     */
    public double sum(String statName) throws StatisticException {
        Values aggr;
        if ((aggr = _statTitle2Values.get(statName)) == null) {
            throw new StatisticException(statName + " is not recorded");
        }

        return aggr.sum();
    }

    /**
     * @param statName The title of the statistic.
     *
     * @return The variance of values recorder for the particular statistic
     *
     * @throws StatisticException in case there is no record for the statistic
     */
    public double variance(String statName) throws StatisticException {
        Values aggr;
        if ((aggr = _statTitle2Values.get(statName)) == null) {
            throw new StatisticException(statName + " is not recorded");
        }

        return aggr.variance();
    }

    public double stddev(String statName) throws StatisticException {
        Values aggr;
        if ((aggr = _statTitle2Values.get(statName)) == null) {
            throw new StatisticException(statName + " is not recorded");
        }

        return aggr.stddev();
    }

    /**
     * @return the mapping between statistics titles and mapped Values instances
     * as an unmodifiable map.
     */
    public Map<String, Values> names2aggregatesMapping() {
        return Collections.unmodifiableMap(_statTitle2Values);
    }

    public String toCSVString(
            boolean mean, boolean variance, boolean stddev, ConfidenceInterval confInterval,
            boolean includeTitles)  {

        return toCSVString(mean, variance, stddev, confInterval, _statTitle2Values.keySet(), includeTitles);
    }
    /**
     * Returns a string representation of the mapped aggregates in the form of a
     * table with optionally printed statistics titles combined to aggregates
     * titles as the headers of the table. A comma separated format is used to
     * form the table output.
     *
     * @param mean
     * @param variance
     * @param stddev
     * @param confInterval
     * @param statsTitles
     * @param includeTitles should the titles be printed?
     * @return A string representation of the mapped aggregates in the form of a
     * table.
     */
    

    public String toCSVString(
            boolean mean, boolean variance, boolean stddev, ConfidenceInterval confInterval,
            Iterable<String> statsTitles, boolean includeTitles)  {

        double confIntervalZ = confInterval.z();
        StringBuilder csvBuilder = new StringBuilder(180);

        //<editor-fold defaultstate="collapsed" desc="apppend the names and category of allowed stat">
        if (includeTitles) {
            for (String statName : statsTitles) {
                if (mean) {
                    csvBuilder.append(statName).append("").append(',');
                }
                if (variance) {
                    csvBuilder.append(statName).append("(var)").append(',');
                }
                if (stddev) {
                    csvBuilder.append(statName).append("(stddev)").append(',');
                }
                if (confInterval != ConfidenceInterval.NONE) {
                    csvBuilder.append(statName).append('(').append(confInterval.getConfidencePercentile()).append(" confidence)").append(',');
                }
            }

            csvBuilder.append('\n');
        }//titles
//</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="append the values">
        for (String statName : statsTitles) {
            Values nxtValue = _statTitle2Values.get(statName);
            if (nxtValue == null) {
                nxtValue = Values.DUMMY; // in case the are no stats for this 
            }
            if (mean) {
                if (nxtValue == Values.DUMMY) {
                    csvBuilder.append(",");
                } else {
                    double theMean = nxtValue.mean();
                    csvBuilder.append(Double.isNaN(theMean) ? "" : Double.toString(theMean)).append(",");
                }
            }
            if (variance) {
                if (nxtValue == Values.DUMMY) {
                    csvBuilder.append(",");
                } else {
                    csvBuilder.append(nxtValue.variance()).append(",");
                }
            }
            if (stddev) {
                if (nxtValue == Values.DUMMY) {
                    csvBuilder.append(",");
                } else {
                    csvBuilder.append(nxtValue.stddev()).append(",");
                }
            }
            if (confInterval != ConfidenceInterval.NONE) {
                if (nxtValue == Values.DUMMY) {
                    csvBuilder.append(",");
                } else {
                    csvBuilder.append(nxtValue.confIntervalABS(confIntervalZ)).append(",");
                }
            }
        }
//</editor-fold>
        return csvBuilder.toString();
    }

    public String toString_csv_meanOnly(Iterable<String> statsTitles, boolean printTitles) throws StatisticException {
        return toCSVString(true, false, false, ConfidenceInterval.NONE, statsTitles, printTitles);
    }

    /**
     * @return the usingZ
     */
    public double getConfidenceInterval_z() {
        return _confInterval.z();
    }

    /**
     * Compresses (in the sense that values become garbage collectable) and
     * finalizes state.
     *
     * @throws StatisticException
     */
    public void finalizeState() throws StatisticException {
        for (Map.Entry<String, Values> entry : _statTitle2Values.entrySet()) {
            Values nxtAggrVals = entry.getValue();
            if (nxtAggrVals != Values.DUMMY) {
                nxtAggrVals.finalizeState(getConfidenceInterval_z());
            }
        }
    }

    public Set<String> getTitles() {
        return this._statTitle2Values.keySet();
    }

    /**
     * Ensures that the passed statistics titles are recorded, even if there are
     * no values recorded for those statistics.
     *
     * This is a useful method because there may be no record for some
     * statistics with respect to different simulation times. If that is the
     * case, the values printed in results CSV output files are not aligned
     * correctly with statistics titles.
     *
     * For titles not already added to this instance, the title of the statistic
     * is added with a null aggregate instance (value) mapped to it.
     *
     * @param statisticsTitles
     */
    void ensureTitles(Set<String> statisticsTitles) {
        Set<String> knownTitles = _statTitle2Values.keySet();

        for (Iterator<String> statitle_it = statisticsTitles.iterator(); statitle_it.hasNext();) {
            String nxtStatTitle = statitle_it.next();
            if (!knownTitles.contains(nxtStatTitle)) {
                _statTitle2Values.put(nxtStatTitle, Values.DUMMY);
            }
        }

    }

    public Set<String> getStatTitles() {
        return Collections.unmodifiableSet(_statTitle2Values.keySet());
    }

    public boolean containsStatTitle(String t) {
        return _statTitle2Values.keySet().contains(t);
    }

    public List<Double> getValuesFor(String nxtT) {
        return Collections.unmodifiableList(_statTitle2Values.get(nxtT).values());
    }

}
