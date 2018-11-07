/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.extension.siddhi.execution.bayesianml.model;

import org.apache.log4j.Logger;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.GradientUpdater;
import org.nd4j.linalg.learning.config.AdaGrad;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.learning.config.Nadam;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.learning.config.Sgd;

/**
 * model interface.
 */
public abstract class BayesianModel {

    private static final Logger logger = Logger.getLogger(BayesianModel.class.getName());

    SameDiff sd;
    SDVariable xVar, yVar;

    // configurable params
    int numFeatures;
    int numSamples;
    private boolean addBias;
    private OptimizerType optimizerType;
    private double learningRate;
    private int predictionSamples;

    private SDVariable[] vars;
    private GradientUpdater[] updaters;


    /**
     * builds the model.
     * <p>
     * sets the default configurations
     */
    BayesianModel() {
        this.numFeatures = -1;
        this.numSamples = 1;
        this.addBias = false;

        this.optimizerType = OptimizerType.ADAM;
        this.learningRate = 0.05;

        this.predictionSamples = 1000;
    }

    /**
     * construct the model.
     */
    public void initiateModel() {
        // initiateModel class variables
        sd = SameDiff.create();

        // additional feature dimension for bias
        if (addBias) {
            numFeatures += 1;
        }

        // collect variables
        vars = specifyModel();

        // initiate the optimizer
        IUpdater optimizer = createUpdater();
        updaters = new GradientUpdater[vars.length];
        for (int i = 0; i < vars.length; i++) {
            if (OptimizerType.SGD.equals(optimizerType)) {
                updaters[i] = optimizer.instantiate(null, true);
            } else {
                long varSize = 1;
                for (long shape : vars[i].getShape()) {
                    varSize *= shape;
                }
                updaters[i] = optimizer.instantiate(
                        Nd4j.create(1, optimizer.stateSize(varSize)), true);
            }
        }
        logger.info("Successfully initiated gradient optimizer : " + optimizer.getClass().getSimpleName());

    }

    /**
     * updates the variables list stored @vars.
     */
    private void updateVariables() {
        for (int i = 0; i < vars.length; i++) {
            SDVariable var = vars[i];
            INDArray gradients = var.getGradient().getArr();
            long[] gradientShape = gradients.shape();

            INDArray gradientArr = Nd4j.toFlattened(gradients);
            if (Double.isNaN(gradients.mean().toDoubleVector()[0])) {
                logger.warn(String.format("invalid gradients. skipping variable update of %s", var.getVarName()));
                return;
            }

            // apply updater to the gradients
            // we set the iteration and epochs to 1.
            // incrementing a integer in online settings may results overflow
            updaters[i].applyUpdater(gradientArr, 1, 0);

            // gradient descent step
            var.setArray(var.getArr().sub(gradientArr.reshape(gradientShape)));
        }

    }

    /**
     * train the model.
     *
     * @param features feature vector
     * @param target   target/label
     *                 for regression target should be a real vector
     *                 for binary classification target should be a vector with labels (0 or 1)
     *                 multiclass classification expects one-hot embedded matrix or a vector with label indexes
     */
    public double[] update(double[] features, double[] target) {

        INDArray featureArr = Nd4j.create(features);
        INDArray targetArr = Nd4j.create(target);

        if (addBias) {
            featureArr = Nd4j.append(featureArr, 1, 1, 1);
        }
        xVar.setArray(featureArr);
        yVar.setArray(targetArr);

        INDArray loss = sd.execAndEndResult();
        sd.execBackwards();

        logger.info(this.getClass().getName() + " model loss : " + loss.toString());

        updateVariables();

        return loss.toDoubleVector();
    }

    /**
     * predict the target according to given features.
     * predictive distribution is approximated using nSamples from the actual distribution
     * <p>
     * uses 1000 (default) samples to estimate the predictive distribution
     *
     * @param features feature vector
     * @return only the mean of the predictions
     */
    public Double predict(double[] features) {
        INDArray featureArr = Nd4j.create(features);
        if (addBias) {
            featureArr = Nd4j.append(featureArr, 1, 1, 1);
        }
        INDArray predictiveDistribution = estimatePredictiveDistribution(featureArr, predictionSamples);
        return predictionFromMean(predictiveDistribution.mean(1)).toDoubleVector()[0];
    }

    /**
     * predict the target according to given features.
     * predictive distribution is approximated using nSamples from the actual distribution
     * <p>
     * uses 1000 (default) samples to estimate the predictive distribution
     *
     * @param features feature vector
     * @return both mean of the predictions and the std
     */
    public Double[] predictWithStd(double[] features) {
        INDArray featureArr = Nd4j.create(features);
        if (addBias) {
            featureArr = Nd4j.append(featureArr, 1, 1, 1);
        }
        INDArray predictiveDistribution = estimatePredictiveDistribution(featureArr, predictionSamples);
        return new Double[]{predictionFromMean(predictiveDistribution.mean(1)).toDoubleVector()[0],
                predictiveDistribution.std(1).toDoubleVector()[0]};
    }

    /**
     * implements the model specific gradient updates.
     *
     * @return var means and stds
     */
    protected abstract double[][] getUpdatedWeights();


    public abstract double evaluate(double[] features, Object expected);

    /**
     * implements the model specific methods to estimate the predictive distributions.
     * predictive distribution is approximated using nSamples from the actual distribution
     *
     * @param features feature vector
     * @param nSamples number of samples used for approximation
     * @return predictive densities
     */
    abstract INDArray estimatePredictiveDistribution(INDArray features, int nSamples);

    /**
     * implements the specific model structure.
     *
     * @return var list to register for gradient updates
     */
    abstract SDVariable[] specifyModel();

    /**
     * implements any post processing required for the mean of the predictive distribution.
     * <p>
     * ex : softmax regression require labels instead of mean of the softmax values
     *
     * @param predictiveMean predictive mean of the predictive density
     * @return formatted prediction
     */
    abstract INDArray predictionFromMean(INDArray predictiveMean);

    private IUpdater createUpdater() {
        switch (optimizerType) {
            case ADAM:
                return new Adam(learningRate);
            case SGD:
                return new Sgd(learningRate);
            case ADAGRAD:
                return new AdaGrad(learningRate);
            case RMSPROP:
                return new RmsProp(learningRate); // TODO fix  initialization error
            case NADAM:
                return new Nadam(learningRate);
            default:
                return new Adam(learningRate);

        }
    }

    public void setAddBias(boolean val) {
        addBias = val;
    }

    public int getNumFeatures() {
        return numFeatures;
    }

    public void setNumFeatures(int val) {
        numFeatures = val;
    }

    public int getNumSamples() {
        return numSamples;
    }

    public void setNumSamples(int val) {
        numSamples = val;
    }

    public OptimizerType getOptimizerType() {
        return optimizerType;
    }

    public void setOptimizerType(OptimizerType val) {
        optimizerType = val;
    }

    public double getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(double val) {
        learningRate = val;
    }

    public void setPredictionSamples(int val) {
        predictionSamples = val;
    }

    /**
     * optimizer types that can be used with the bayesian models.
     */
    public enum OptimizerType {
        ADAM, RMSPROP, ADAGRAD, SGD, NADAM
    }

}

