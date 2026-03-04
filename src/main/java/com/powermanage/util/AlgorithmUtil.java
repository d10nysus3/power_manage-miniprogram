package com.powermanage.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
public class AlgorithmUtil {

    
    public double calculateMahalanobisDistance(double[] x, double[] y, double[][] covarianceMatrix) {
        if (x == null || y == null || x.length != y.length || covarianceMatrix == null || 
            covarianceMatrix.length != x.length || covarianceMatrix[0].length != x.length) {
            throw new IllegalArgumentException("输入参数维度不匹配");
        }

        
        double[] delta = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            delta[i] = x[i] - y[i];
        }

        
        double[][] inverseCovarianceMatrix = invertMatrix(covarianceMatrix);
        if (inverseCovarianceMatrix == null) {
            return 0.0; 
        }

        
        double[] intermediate = multiplyMatrixVector(inverseCovarianceMatrix, delta);
        double distanceSquared = 0.0;
        for (int i = 0; i < delta.length; i++) {
            distanceSquared += delta[i] * intermediate[i];
        }

        return Math.sqrt(distanceSquared);
    }

    
    public double calculateSimplifiedMahalanobisDistance(double[] x, double[] y, double[] stdDevs) {
        if (x == null || y == null || x.length != y.length || stdDevs == null || stdDevs.length != x.length) {
            throw new IllegalArgumentException("输入参数维度不匹配");
        }

        double distance = 0.0;
        for (int i = 0; i < x.length; i++) {
            if (stdDevs[i] > 0) { 
                distance += Math.pow((x[i] - y[i]) / stdDevs[i], 2);
            }
        }
        return Math.sqrt(distance);
    }

    
    public double calculateDTWDistance(double[] sequence1, double[] sequence2) {
        if (sequence1 == null || sequence2 == null || sequence1.length == 0 || sequence2.length == 0) {
            return 0.0;
        }

        int m = sequence1.length;
        int n = sequence2.length;

        
        double[][] dtwMatrix = new double[m + 1][n + 1];

        
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                dtwMatrix[i][j] = Double.POSITIVE_INFINITY;
            }
        }
        dtwMatrix[0][0] = 0.0;

        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                double cost = Math.abs(sequence1[i - 1] - sequence2[j - 1]);
                double minPrevCost = Math.min(
                        dtwMatrix[i - 1][j],       
                        Math.min(
                                dtwMatrix[i][j - 1], 
                                dtwMatrix[i - 1][j - 1] 
                        )
                );
                dtwMatrix[i][j] = cost + minPrevCost;
            }
        }

        
        return dtwMatrix[m][n];
    }

    
    public double calculateConstrainedDTWDistance(double[] sequence1, double[] sequence2, double windowSize) {
        if (sequence1 == null || sequence2 == null || sequence1.length == 0 || sequence2.length == 0) {
            return 0.0;
        }

        int m = sequence1.length;
        int n = sequence2.length;
        int window = (int) (windowSize * Math.max(m, n));

        
        double[][] dtwMatrix = new double[m + 1][n + 1];

        
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                dtwMatrix[i][j] = Double.POSITIVE_INFINITY;
            }
        }
        dtwMatrix[0][0] = 0.0;

        
        for (int i = 1; i <= m; i++) {
            
            int start = Math.max(1, i - window);
            int end = Math.min(n, i + window);
            
            for (int j = start; j <= end; j++) {
                double cost = Math.abs(sequence1[i - 1] - sequence2[j - 1]);
                double minPrevCost = Math.min(
                        dtwMatrix[i - 1][j],       
                        Math.min(
                                dtwMatrix[i][j - 1], 
                                dtwMatrix[i - 1][j - 1] 
                        )
                );
                dtwMatrix[i][j] = cost + minPrevCost;
            }
        }

        
        return dtwMatrix[m][n];
    }

    public double calculateDTWDistanceForVectors(double[][] sequence1, double[][] sequence2) {
        if (sequence1 == null || sequence2 == null || sequence1.length == 0 || sequence2.length == 0) {
            return 0.0;
        }
        int m = sequence1.length;
        int n = sequence2.length;
        double[][] dtwMatrix = new double[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                dtwMatrix[i][j] = Double.POSITIVE_INFINITY;
            }
        }
        dtwMatrix[0][0] = 0.0;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                double cost = euclidean(sequence1[i - 1], sequence2[j - 1]);
                double minPrevCost = Math.min(
                        dtwMatrix[i - 1][j],
                        Math.min(dtwMatrix[i][j - 1], dtwMatrix[i - 1][j - 1])
                );
                dtwMatrix[i][j] = cost + minPrevCost;
            }
        }
        return dtwMatrix[m][n];
    }

    private double euclidean(double[] a, double[] b) {
        double s = 0.0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            double d = a[i] - b[i];
            s += d * d;
        }
        return Math.sqrt(s);
    }

    
    public double[][] calculateCovarianceMatrix(double[][] data) {
        if (data == null || data.length == 0 || data[0].length == 0) {
            return new double[0][0];
        }

        int numSamples = data.length;
        int numFeatures = data[0].length;
        double[][] covarianceMatrix = new double[numFeatures][numFeatures];

        
        double[] means = new double[numFeatures];
        for (int j = 0; j < numFeatures; j++) {
            double sum = 0.0;
            for (int i = 0; i < numSamples; i++) {
                sum += data[i][j];
            }
            means[j] = sum / numSamples;
        }

        
        for (int j1 = 0; j1 < numFeatures; j1++) {
            for (int j2 = 0; j2 < numFeatures; j2++) {
                double covariance = 0.0;
                for (int i = 0; i < numSamples; i++) {
                    covariance += (data[i][j1] - means[j1]) * (data[i][j2] - means[j2]);
                }
                covarianceMatrix[j1][j2] = covariance / (numSamples - 1); 
            }
        }

        return covarianceMatrix;
    }

    
    public double[][] invertMatrix(double[][] matrix) {
        int n = matrix.length;
        double[][] augmentedMatrix = new double[n][2 * n];

        // 
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                augmentedMatrix[i][j] = matrix[i][j];
            }
            augmentedMatrix[i][i + n] = 1.0;
        }

        // 
        for (int i = 0; i < n; i++) {
            // 
            int pivot = i;
            for (int j = i; j < n; j++) {
                if (Math.abs(augmentedMatrix[j][i]) > Math.abs(augmentedMatrix[pivot][i])) {
                    pivot = j;
                }
            }

            // 
            if (pivot != i) {
                double[] temp = augmentedMatrix[i];
                augmentedMatrix[i] = augmentedMatrix[pivot];
                augmentedMatrix[pivot] = temp;
            }

            // 
            if (Math.abs(augmentedMatrix[i][i]) < 1e-10) {
                return null; // 
            }

            // 
            double pivotValue = augmentedMatrix[i][i];
            for (int j = i; j < 2 * n; j++) {
                augmentedMatrix[i][j] /= pivotValue;
            }

            // 
            for (int j = 0; j < n; j++) {
                if (j != i) {
                    double factor = augmentedMatrix[j][i];
                    for (int k = i; k < 2 * n; k++) {
                        augmentedMatrix[j][k] -= factor * augmentedMatrix[i][k];
                    }
                }
            }
        }

        // 
        double[][] inverseMatrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                inverseMatrix[i][j] = augmentedMatrix[i][j + n];
            }
        }

        return inverseMatrix;
    }

    public double calculateMahalanobisDistanceWithInverse(double[] x, double[] y, double[][] inverseCovarianceMatrix) {
        if (x == null || y == null || x.length != y.length || inverseCovarianceMatrix == null ||
            inverseCovarianceMatrix.length != x.length || inverseCovarianceMatrix[0].length != x.length) {
            throw new IllegalArgumentException("输入参数维度不匹配");
        }

        // 
        double[] delta = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            delta[i] = x[i] - y[i];
        }

        // 
        double[] intermediate = multiplyMatrixVector(inverseCovarianceMatrix, delta);
        double distanceSquared = 0.0;
        for (int i = 0; i < delta.length; i++) {
            distanceSquared += delta[i] * intermediate[i];
        }

        return Math.sqrt(distanceSquared);
    }

    
    private double[] multiplyMatrixVector(double[][] matrix, double[] vector) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[] result = new double[rows];

        for (int i = 0; i < rows; i++) {
            double sum = 0.0;
            for (int j = 0; j < cols; j++) {
                sum += matrix[i][j] * vector[j];
            }
            result[i] = sum;
        }

        return result;
    }

    
    public double calculateZScore(double value, double mean, double stdDev) {
        if (stdDev == 0) {
            return 0.0;
        }
        return Math.abs((value - mean) / stdDev);
    }

    
    public boolean performComprehensiveAnomalyDetection(
            Map<String, Double> algorithmResults,
            Map<String, Double> thresholds,
            Map<String, Double> weights) {
        
        if (algorithmResults == null || algorithmResults.isEmpty()) {
            return false;
        }

        
        double weightedScore = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<String, Double> entry : algorithmResults.entrySet()) {
            String algorithm = entry.getKey();
            double score = entry.getValue();
            double threshold = thresholds.getOrDefault(algorithm, 1.0);
            double weight = weights.getOrDefault(algorithm, 1.0);

            
            double normalizedScore = Math.min(1.0, score / threshold);
            
            weightedScore += normalizedScore * weight;
            totalWeight += weight;
        }

        
        double comprehensiveScore = totalWeight > 0 ? weightedScore / totalWeight : 0.0;
        
        
        return comprehensiveScore > 0.6;
    }

    
    public double[] extractFeatures(List<Double> data, int featureCount) {
        if (data == null || data.isEmpty()) {
            return new double[0];
        }

        List<Double> features = new ArrayList<>();
        
        
        double mean = data.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = data.stream().mapToDouble(x -> Math.pow(x - mean, 2)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        double max = data.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double min = data.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        
        features.add(mean);
        features.add(stdDev);
        features.add(max);
        features.add(min);
        features.add(max - min); 
        
        
        if (featureCount > 5) {
            
            
        }
        
        
        while (features.size() < featureCount) {
            features.add(0.0);
        }
        
        
        return features.stream().mapToDouble(Double::doubleValue).toArray();
    }

    
    public double[] normalizeVector(double[] vector) {
        if (vector == null || vector.length == 0) {
            return new double[0];
        }
        
        double sumSquared = Arrays.stream(vector).map(x -> x * x).sum();
        if (sumSquared == 0) {
            return vector;
        }
        
        double norm = Math.sqrt(sumSquared);
        double[] normalized = new double[vector.length];
        
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        
        return normalized;
    }

    public double inverseNormalCdf(double p) {
        if (p <= 0) return Double.NEGATIVE_INFINITY;
        if (p >= 1) return Double.POSITIVE_INFINITY;
        double a1 = -39.69683028665376;
        double a2 = 220.9460984245205;
        double a3 = -275.9285104469687;
        double a4 = 138.3577518672690;
        double a5 = -30.66479806614716;
        double a6 = 2.506628277459239;
        double b1 = -54.47609879822406;
        double b2 = 161.5858368580409;
        double b3 = -155.6989798598866;
        double b4 = 66.80131188771972;
        double b5 = -13.28068155288572;
        double c1 = -0.007784894002430293;
        double c2 = -0.3223964580411365;
        double c3 = -2.400758277161838;
        double c4 = -2.549732539343734;
        double c5 = 4.374664141464968;
        double c6 = 2.938163982698783;
        double d1 = 0.007784695709041462;
        double d2 = 0.3224671290700398;
        double d3 = 2.445134137142996;
        double d4 = 3.754408661907416;
        double plow = 0.02425;
        double phigh = 1 - plow;
        double q, r;
        double ret;
        if (p < plow) {
            q = Math.sqrt(-2 * Math.log(p));
            ret = (((((c1 * q + c2) * q + c3) * q + c4) * q + c5) * q + c6)
                    / ((((d1 * q + d2) * q + d3) * q + d4) * q + 1);
        } else if (phigh < p) {
            q = Math.sqrt(-2 * Math.log(1 - p));
            ret = -(((((c1 * q + c2) * q + c3) * q + c4) * q + c5) * q + c6)
                    / ((((d1 * q + d2) * q + d3) * q + d4) * q + 1);
        } else {
            q = p - 0.5;
            r = q * q;
            ret = (((((a1 * r + a2) * r + a3) * r + a4) * r + a5) * r + a6) * q
                    / (((((b1 * r + b2) * r + b3) * r + b4) * r + b5) * r + 1);
        }
        return ret;
    }
}