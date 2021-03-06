/*
*  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.wso2.troubleshooting.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.wso2.troubleshooting.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Enumeration;
import java.util.jar.JarFile;

/**
 * Agent for attach to running program
 */
public class Agent {

    public static void premain(String arguments, Instrumentation instrumentation) {

        Logger logger = Logger.getInstance();

        JarFile loggerJarFile;
        try {
            loggerJarFile = new JarFile(new File("troubleshooting-logger-1.0-SNAPSHOT.jar"));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        instrumentation.appendToBootstrapClassLoaderSearch(loggerJarFile);

        logger.info("Troubleshooting Agent start!!");
        logger.info("System Details : ");
        Enumeration<String> propertyNames = (Enumeration<String>) System.getProperties().propertyNames();
        while (propertyNames.hasMoreElements()) {
            String propName = propertyNames.nextElement();
            logger.info(propName + " = " + System.getProperty(propName));
        }
        logger.info("Thread Details:");
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (int i = 1; i < elements.length; i++) {
            StackTraceElement threadElement = elements[i];
            logger.info("\tat " + threadElement.getClassName() + "." + threadElement.getMethodName()
                    + "(" + threadElement.getFileName() + ":" + threadElement.getLineNumber() + ")");
        }
        logger.stoplog();

        new AgentBuilder.Default()
                .ignore(ElementMatchers.none())
                .type(ElementMatchers.nameContains("ThreadPoolExecutor"))
                .transform((builder, type, classLoader, module) -> builder
                        .visit(Advice.to(ThreadPoolExecutorAdvice.class).on(ElementMatchers.any()))
                ).installOn(instrumentation);

    }

}
