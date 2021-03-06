package openepsim;

import com.google.gson.Gson;

/**
 * Class to collect various time-averagedstatistics from a simulation run,
 * primarily the site-wise per-species densities.
 */
public class DensityStatistics {
    private class DensityResults {
        double tTotal;
        double [][] density;
        double [][] speciesDensity;
        int [][][] counts;

        DensityResults() {
            density = new double[nstates][L];
            speciesDensity = new double[nstates][L + 1];
        }
    }

    private final int L;
    private final int nstates;

    private double tTotal;
    private double [][] tOccupied;
    private double [][] tSpeciesOccupation;

    private int [][][] counts;

    public DensityStatistics(
        int L, int nstates, SimOptions.LocalTransitionSpec [] specs
    ) {
        this.L = L;
        this.nstates = nstates;

        this.tOccupied = new double[nstates][L];

        this.tSpeciesOccupation = new double[nstates][L + 1];

        this.counts = new int[specs.length][][];

        for (int i = 0; i < specs.length; i++) {
            if (specs[i].count) {
                int d = specs[i].rates.length;
                this.counts[i] = new int[d][d];
            }
        }
    }

    /**
     *  Update statistics to count time dt spent in configuration
     *  config.
     *
     *  @param config The lattice configuration.
     *  @param dt Time spent in this configuration.
     */
    public void update(LatticeConfiguration config, double dt) {
        int [] speciesCount = new int[nstates];

        for (int i = 0; i < L; i++) {
            int c = config.getConfigurationInt(i);

            tOccupied[c][i] += dt;

            speciesCount[c]++;
        }

        for (int c = 0; c < nstates; c++) {
            tSpeciesOccupation[c][speciesCount[c]] += dt;
        }

        tTotal += dt;
    }

    /**
     * Count occurences of given of given local transition.  For example,
     * this can be used to record the time integrated current.
     *
     * @param spec Index into SimOptions.transitions array.
     * @param toC The configuration we step into.
     * @param fromC The configuration we leave.
     */
    public void countTransition(int spec, int toC, int fromC) {
        if (this.counts[spec] != null) {
            this.counts[spec][toC][fromC]++;
        }
    }

    /**
     * Return total elapsed (simulated) time.
     */
    public double getTotalTime() {
        return tTotal;
    }

    /**
     * Compute and return the current density profile for given species.
     *
     * @param c Species to compute density profile for.
     */
    public double [] getDensityProfile(int c) {
        double [] profile = new double[L];

        for (int i = 0; i < L; i++) {
            profile[i] = tOccupied[c][i] / tTotal;
        }

        return profile;
    }

    /**
     * Return the number of times the specified transition
     * has occurred.
     *
     * The LocalTransitionSpec instance at index spec in the SimOptions
     * instance must have parameter count set to True. 
     *
     * @param spec Index of transition spec in SimOptions transitions
     *             list (zero indexed).
     * @param toC  Transition to configuration toC
     * @param fromC Transition from configuration fromC
     */
    public int getTransitionCount(int spec, int toC, int fromC) {
        return counts[spec][toC][fromC];
    }

    /**
     * Return a string summary of the collected statistics
     * in JSON format.
     */
    public String summary() {
        DensityResults results = new DensityResults();

        results.tTotal = tTotal;

        if (tTotal > 0) {
            for (int j = 0; j < nstates; j++) {
                for (int i = 0; i < L; i++) {
                    results.density[j][i] = tOccupied[j][i] / tTotal;
                }

                for (int i = 0; i < L + 1; i++) {
                    results.speciesDensity[j][i]
                        = tSpeciesOccupation[j][i] / tTotal;
                }
            }
        }

        results.counts = counts;

        Gson gson = new Gson();

        return gson.toJson(results);
    }
}
