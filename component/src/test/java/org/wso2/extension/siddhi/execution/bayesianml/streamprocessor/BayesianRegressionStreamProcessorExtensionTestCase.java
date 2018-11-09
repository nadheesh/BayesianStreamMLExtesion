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
package org.wso2.extension.siddhi.execution.bayesianml.streamprocessor;

import org.apache.log4j.Logger;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.exception.SiddhiAppCreationException;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.SiddhiTestHelper;

import java.util.concurrent.atomic.AtomicInteger;

public class BayesianRegressionStreamProcessorExtensionTestCase {

    private static final Logger logger = Logger.getLogger(BayesianRegressionStreamProcessorExtensionTestCase.class);
    private AtomicInteger count;
    private String trainingStream = "@App:name('BayesianRegressionTestApp') " +
            "\ndefine stream StreamTrain (attribute_0 double, " +
            "attribute_1 double, attribute_2 " + "double, attribute_3 double, attribute_4 double );";

    private String trainingQuery = ("@info(name = 'query-train') from " +
            "StreamTrain#streamingml:updateBayesianRegression" + "('model1', attribute_4, 0.1, attribute_0, " +
            "attribute_1, attribute_2, attribute_3) \n" + "insert all events into trainOutputStream;\n");


    @BeforeMethod
    public void init() {
        count = new AtomicInteger(0);
    }


    @Test
    public void testBayesianRegressionStreamProcessorExtension1() {
        logger.info("BayesianRegressionStreamProcessorExtension TestCase - Assert predictions and evolution");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA(attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianRegression('model1', 10000, " +
                " attribute_0, attribute_1, attribute_2, attribute_3) \n" + "insert all events into outputStream;");

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(trainingStream + inStreamDefinition
                + trainingQuery + query);
        siddhiAppRuntime.addCallback("query1", new QueryCallback() {

            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                count.incrementAndGet();
                if (count.get() == 1) {
                    AssertJUnit.assertEquals(1.0821, (Double) inEvents[0].getData()[4], 0.1);
                } else if (count.get() == 2) {
                    AssertJUnit.assertEquals(0.66634, (Double) inEvents[0].getData()[4], 0.1);
                } else if (count.get() == 3) {
                    AssertJUnit.assertEquals(1.0396, (Double) inEvents[0].getData()[4], 0.1);
                } else if (count.get() == 4) {
                    AssertJUnit.assertEquals(0.34184, (Double) inEvents[0].getData()[4], 0.1);
                }
            }
        });
        try {
            InputHandler inputHandler = siddhiAppRuntime.getInputHandler("StreamTrain");
            siddhiAppRuntime.start();

            for (int i = 0; i < 100; i++) {
                inputHandler.send(new Object[]{1.0, 1.0, 0.2, 0.13, 1.4056});
                inputHandler.send(new Object[]{0.9, 0.89, 0.3, 0.02, 0.6819});
                inputHandler.send(new Object[]{0.0, 0.0, 1.0, 0.82, -1.3616});
                inputHandler.send(new Object[]{0.01, 0.4, 0.77, 0.92, -0.2046});
                inputHandler.send(new Object[]{0.80, 0.81, 0.11, 0.13, 1.3401});
                inputHandler.send(new Object[]{0.02, 0.30, 0.88, 0.76, -0.9278});
                inputHandler.send(new Object[]{0.93, 0.71, 0.02, 0.122, 1.70314});
                inputHandler.send(new Object[]{0.29, 0.24, 0.98, 0.65, -1.18});
            }

            Thread.sleep(1000);
            InputHandler inputHandler1 = siddhiAppRuntime.getInputHandler("StreamA");
            // send some unseen data for prediction
            inputHandler1.send(new Object[]{0.8, 0.67, 0.1, 0.03});
            inputHandler1.send(new Object[]{0.33, 0.23, 0.632, 0.992});
            Thread.sleep(1000);

            // try to drift the model
            for (int i = 0; i < 20; i++) {
                inputHandler.send(new Object[]{0.88, 1.0, 0.2, 0.13, 1.2356});
                inputHandler.send(new Object[]{0.9, 0.79, 0.3, 0.02, 0.4944});
                inputHandler.send(new Object[]{0.0, 0.0, 0.90, 0.72, -1.7136});
                inputHandler.send(new Object[]{0.01, 0.4, 0.87, 0.62, -1.4876});
                inputHandler.send(new Object[]{0.90, 0.91, 0.11, 0.13, 1.5076});
                inputHandler.send(new Object[]{0.02, 0.30, 0.88, 0.66, -1.5088});
                inputHandler.send(new Object[]{0.83, 0.79, 0.02, 0.122, 1.64864});
                inputHandler.send(new Object[]{0.29, 0.24, 0.98, 0.77, -1.4136});
            }

            Thread.sleep(1000);
            // send some unseen data for prediction
            inputHandler1.send(new Object[]{0.8, 0.67, 0.1, 0.03});
            inputHandler1.send(new Object[]{0.33, 0.23, 0.632, 0.992});

            SiddhiTestHelper.waitForEvents(200, 4, count, 5000);

        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.fail("Model fails to train,");
        } finally {
            siddhiAppRuntime.shutdown();
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension2() {
        logger.info("BayesianRegressionStreamProcessorExtension TestCase - Features are not of type double");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 bool);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianRegression('model1', " + "10," +
                " attribute_0, attribute_1, attribute_2, attribute_3) \n" + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("model.features in 6th parameter is not "
                    + "a numerical type attribute. Found BOOL. Check the input stream definition"));
        } finally {
            siddhiManager.shutdown();
        }
    }


    @Test
    public void testBayesianRegressionStreamProcessorExtension3() {
        logger.info("BayesianRegressionStreamProcessorExtension TestCase - Number of prediction samples is not int ");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 bool);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianRegression('model1', " + "0.5, " +
                "attribute_0, attribute_1, attribute_2, attribute_3) \n" + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Invalid parameter type found for the " +
                    "prediction.samples argument. Expected: INT but found: DOUBLE"));
        } finally {
            siddhiManager.shutdown();
        }
    }


    @Test
    public void testBayesianRegressionStreamProcessorExtension4() {
        logger.info("BayesianRegressionStreamProcessorExtension TestCase - Number of samples less than 1");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianRegression('model1', " + "0," +
                " attribute_0, attribute_1, attribute_2, attribute_3) \n" + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Invalid parameter value found for the " +
                    "prediction.samples argument. Expected a value greater than zero, but found: 0"));
        } finally {
            siddhiManager.shutdown();
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension5() {
        logger.info("BayesianRegressionStreamProcessorExtension TestCase - invalid model name");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double, attribute_4 double );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianRegression(attribute_4, " + "1000," +
                "attribute_0, attribute_1, attribute_2, attribute_3) \n" + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Parameter model.name must be a constant but "
                    + "found org.wso2.siddhi.core.executor.VariableExpressionExecutor"));
        } finally {
            siddhiManager.shutdown();
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension6() {
        logger.info("BayesianRegressionStreamProcessorExtension TestCase - incorrect initialization");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianRegression() \n" + "insert all " +
                "events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Invalid number of parameters [0] for " +
                    "streamingml:bayesianRegression"));
        } finally {
            siddhiManager.shutdown();
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension7() {
        logger.info("BayesianRegressionStreamProcessorExtension TestCase - Incompatible model");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianRegression('model1', " + "100, " +
                "attribute_0, attribute_1, attribute_2) \n" + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(trainingStream +
                    inStreamDefinition + trainingQuery + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Model [model1] expects 4 features, but the " +
                    "streamingml:bayesianRegression specifies 3 features"));
        } finally {
            siddhiManager.shutdown();
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension8() {
        logger.info("BayesianRegressionStreamProcessorExtension TestCase - invalid model name type");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianRegression(1000, " +
                "attribute_0, attribute_1, attribute_2, attribute_3) \n" + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Invalid parameter type found for the " +
                    "model.name argument, required STRING but found INT"));
        } finally {
            siddhiManager.shutdown();
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension9() {
        logger.info("BayesianRegressionStreamProcessorExtension TestCase - incorrect order of parameters");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianRegression('m1', " +
                "attribute_0, attribute_1, attribute_2, attribute_3, 2000) \n" +
                "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("6th parameter is not an attribute "
                    + "(VariableExpressionExecutor) present in the stream definition. Found a "
                    + "org.wso2.siddhi.core.executor.ConstantExpressionExecutor"
            ));
        } finally {
            siddhiManager.shutdown();
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension10() {
        logger.info("BayesianRegressionStreamProcessorExtension TestCase - more parameters than needed");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianRegression('m1', " + "10000, " +
                "attribute_0, attribute_1, attribute_2, attribute_3, 2) \n" + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Invalid number of parameters for " +
                    "streamingml:bayesianRegression. This Stream Processor requires at most 6 parameters, " +
                    "namely, model.name, prediction.samples[optional], model.features but found 7 parameters"));
        } finally {
            siddhiManager.shutdown();
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension11() {
        logger.info("BayesianRegressionStreamProcessorExtension TestCase - init predict first and then "
                + "update model");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 "
                + "double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianRegression('model1', attribute_0, "
                + "attribute_1, attribute_2, attribute_3) \n" + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(trainingStream
                    + inStreamDefinition + query + trainingQuery);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Model [model1.BayesianRegressionTestApp] "
                    + "needs to initialized prior to be used with streamingml:bayesianRegression. Perform "
                    + "streamingml:updateBayesianRegression process first"));
        } finally {
            siddhiManager.shutdown();
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension12() {
        logger.info("BayesianRegressionStreamProcessorExtension TestCase - model is visible only within the " +
                "SiddhiApp");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "@App:name('BayesianRegressionTestApp2') \ndefine stream StreamA " +
                "(attribute_0 double, " +
                "attribute_1 double, attribute_2 double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianRegression('model1', " +
                "1000, attribute_0, attribute_1, attribute_2, attribute_3) \n" +
                "insert all events into " + "outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime1 = siddhiManager.createSiddhiAppRuntime(trainingStream + trainingQuery);
            // should be successful even though both the apps are using the same model name with different feature
            // values
            SiddhiAppRuntime siddhiAppRuntime2 = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e.getCause().getMessage().contains(
                    "Model [model1.BayesianRegressionTestApp2] needs to initialized prior to be " +
                            "used with streamingml:bayesianRegression. " +
                            "Perform streamingml:updateBayesianRegression process first."));
        } finally {
            siddhiManager.shutdown();
        }
    }


}
