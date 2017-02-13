package stats;

import exceptions.InconsistencyException;
import exceptions.StatisticException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import utilities.CommonFunctions;

/**
 * Keeps track of a series of values for a statistic category. Aggregate values
 * that can be computed include, are: Summary, Variance, Standard Deviation,
 * Mean, Confidence Intervals for mean
 *
 * @author xvas
 */
public class Values {

    public final static Values DUMMY = new Values(-1);
    /**
     * The values aggregated
     */
    private List<Double> values;
    /**
     * Summary of the values aggregated
     */
    private double sum;
    /**
     * The precision decimal after which aggregated values are cut. The Higher
     * it is, the greater the precision but also the greater the output overhead
     * in text format.
     */
    private final int roundDecimal;
    private final FinalizationBean finalizedBean;

    /**
     *
     * @param roundDecimal The precision decimal after which aggregated values
     * are cut. The Higher it is, the greater the precision but also the greater
     * the output overhead in text format.
     */
    public Values(int roundDecimal) {
        this.roundDecimal = roundDecimal;
        this.sum = 0;
        this.values = new ArrayList<>();
        this.finalizedBean = new FinalizationBean();
    }

    public boolean isFinalized() {
        return finalizedBean.isStatusFinalized();
    }

    @Override
    public String toString() {
        return "Values{" + "values=" + CommonFunctions.toString(values) 
                + ", sum=" + sum 
                + ", mean=" + mean()
                + ", stddev=" + stddev()
                + ", conf95%=" + this.confIntervalABS(Statistics.ConfidenceInterval.PERCENTILE_95.z())
                + '}';
    }

    
    
    
    /**
     *
     * @param _roundDecimal The precision decimal after which aggregated values
     * are cut. The Higher it is, the greater the precision but also the greater
     * the output overhead in text format.
     *
     * @param values the initial values added.
     */
    public Values(int _roundDecimal, Double... values) {
        this(_roundDecimal);
        updt(values);
    }
    
    public Values(int _roundDecimal, List<Double> values) {
        this(_roundDecimal);
        updt(values);
    }

    /**
     * A (deep) copy constructor
     *
     * @param original the original aggregates instance to be copied
     */
    public Values(Values original) {
        this.values = new ArrayList();
        for (double nxtValue : original.values) {
            this.values.add(nxtValue);
        }

        this.sum = original.sum;
        this.roundDecimal = original.roundDecimal;
        this.finalizedBean = new FinalizationBean(original.finalizedBean);
    }

    private double round(double val) {
        if (roundDecimal < 0) {
            return val;
        }

        double rounder = Math.pow(10, roundDecimal);
        return ((int) (val * rounder) / rounder);
    }

    private double sumSqredfDiffsValuesMean() {
        double theMean = mean();//updated mean

        if (Double.isNaN(theMean)) {
            return Double.NaN;
        }

        double sqrOfDiffsFromMean = 0.0;
        for (double nxtValue : this.values) {
            sqrOfDiffsFromMean += Math.pow(nxtValue - theMean, 2);
        }
        return sqrOfDiffsFromMean;
    }

    public void updt(List<Double> vals) {
        updt(vals.toArray(new Double[vals.size()]));
    }
    public void updt(Double... vals) {
        if (isStatusFinalized()) {
            throw new InconsistencyException("Illegal action: trying to update fionalized aggregated statistics.");
        }
        for (double nxt : vals) {
            if (Double.isNaN(nxt)) {
                continue;
            }
            this.values.add(nxt);
            sum += nxt;
        }
    }

    /**
     * @return the summary of values
     */
    public double sum() {
        if (isStatusFinalized()) {
            return finalizedBean.getSum();
        }
        return sum;
    }

    /**
     * @return the mean of values
     */
    public double mean() {
        if (isStatusFinalized()) {
            return finalizedBean.getMean();
        }

        double mean = sum / values.size();
        return round(mean);
    }

    public int howManyValues() {
        return values.size();
    }

    /**
     * @return the variance of values recorded.
     */
    public double variance() {
        if (isStatusFinalized()) {
            return finalizedBean.getVariance();
        }
        double variance = values.isEmpty() ? 0 : sumSqredfDiffsValuesMean() / (values.size() - 1);
        return round(variance);
    }

    /**
     * @return the standard deviation of values recorded or -1 if no values are
     * recorded so far.
     */
    public double stddev()  {
        if (isStatusFinalized()) {
            return finalizedBean.getStddev();
        }
        double stddev = values.isEmpty() ? 0 : Math.sqrt(variance());
        return round(stddev);
    }

    /**
     *
     * @param z parameter for computing confidence interval
     * @return The absolute value of confidence
     * @throws StatisticException
     */
    public double confIntervalABS(double z) {
        if (isStatusFinalized()) {
            return finalizedBean.getAbsConfInterval();
        }
        return round(z * stddev() / Math.sqrt(values.size()));
    }

    /**
     * @param z parameter for computing confidence interval
     *
     * @return the lower bound value of confidence interval
     */
    public double confIntervLowerBound(double z) {
        double theMean, stddev;
        int valuesNum;
        if (isStatusFinalized()) {
            theMean = finalizedBean.getMean();
            stddev = finalizedBean.getStddev();
            valuesNum = finalizedBean.getValuesNum();
        } else {
            theMean = mean();
            stddev = stddev();
            valuesNum = values.size();
        }
        double low = theMean - z * stddev / Math.sqrt(valuesNum);
        return round(low);
    }

    /**
     * @param z parameter for computing confidence interval
     *
     * @return the upper bound value of confidence interval
     */
    public double confIntervUpperBound(double z)  {
        double theMean = -1;
        double stddev = -1;
        int valuesNum = -1;
        if (isStatusFinalized()) {
            theMean = finalizedBean.getMean();
            stddev = finalizedBean.getStddev();
            valuesNum = finalizedBean.getValuesNum();
        } else {
            theMean = mean();
            stddev = stddev();
            valuesNum = values.size();
        }
        double hi = theMean + z * stddev / Math.sqrt(valuesNum);
        return round(hi);
    }

    /**
     * Returns the list of recorded values. The order of values is defined by
     * the order they have been recoded.
     *
     * @return The list of recorded values.
     */
    public List<Double> values() {
        return Collections.unmodifiableList(values);
    }

    /**
     * @return the precision decimal after which values are cut.
     */
    public int getRoundDecimal() {
        return roundDecimal;
    }

    /**
     * Compresses (in the sense that values become garbage collectable) and
     * finalizes state.
     *
     * @param z
     * @throws StatisticException
     */
    public void finalizeState(double z) throws StatisticException {
        finalizedBean.finalizeStatus(z);
        values = null;
    }

    public boolean isStatusFinalized() {
        return finalizedBean.isStatusFinalized();
//      return values == null;
    }

    private class FinalizationBean {

        private boolean finalized;
        private double absConfInterval;
        private double mean;
        private double stddev;
        private double variance;
        private double _sum;
        private int valuesNum;

        private FinalizationBean(FinalizationBean finalizedBean) {
            this.finalized = finalizedBean.finalized;
            this.absConfInterval = finalizedBean.absConfInterval;
            this.mean = finalizedBean.mean;
            this.stddev = finalizedBean.stddev;
            this.variance = finalizedBean.variance;
            this._sum = finalizedBean._sum;
            this.valuesNum = finalizedBean.valuesNum;
        }

        private FinalizationBean() {
            this.variance = -1000;
            this.stddev = -1000;
            this.mean = -1000;
            this.absConfInterval = -1000;
            this.finalized = false;
        }

        private void finalizeStatus(double z) throws StatisticException {
            _sum = Values.this.sum;
            absConfInterval = Values.this.confIntervalABS(z);
            mean = Values.this.mean();
            variance = Values.this.variance();
            stddev = Values.this.stddev();
            valuesNum = Values.this.values.size();

            finalized = true; // must be the lat instuction of this method.. otherwise inconsistent status.. 
        }

        /**
         * @return the finalized
         */
        private boolean isStatusFinalized() {
            return finalized;
        }

        /**
         * @return the confIntervalABS
         */
        private double getAbsConfInterval() {
            return absConfInterval;
        }

        /**
         * @return the mean
         */
        private double getMean() {
            return mean;
        }

        /**
         * @return the stddev
         */
        private double getStddev() {
            return stddev;
        }

        /**
         * @return the variance
         */
        private double getVariance() {
            return variance;
        }

        /**
         * @return the _sum
         */
        private double getSum() {
            return _sum;
        }

        private int getValuesNum() {
            return valuesNum;
        }
    }
}
