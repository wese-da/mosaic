/*
 * Copyright (c) 2020 Fraunhofer FOKUS and others. All rights reserved.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contact: mosaic@fokus.fraunhofer.de
 */

package org.eclipse.mosaic.rti.api.federatestarter;

import org.eclipse.mosaic.rti.api.FederateExecutor;
import org.eclipse.mosaic.rti.api.parameters.FederateDescriptor;
import org.eclipse.mosaic.rti.config.CLocalHost;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of {@link FederateExecutor} which starts the federate in a Java process (e.g. Phabmacs).
 */
public class JavaFederateExecutor implements FederateExecutor {

    private final String mainClass;
    private final String programArguments;
    private final FederateDescriptor handle;
    private final List<String> vmArgs;

    private ExecutableFederateExecutor delegateExecFederateStarter = null;

    public JavaFederateExecutor(FederateDescriptor handle, String mainClass, String programArguments) {
        this(handle, mainClass, programArguments, Lists.newArrayList());
    }

    public JavaFederateExecutor(FederateDescriptor handle, String mainClass, String programArguments, List<String> vmArgs) {
        this.mainClass = mainClass;
        this.programArguments = programArguments;
        this.handle = handle;
        this.vmArgs = vmArgs;
    }

    @Override
    public Process startLocalFederate(File workingDir) throws FederateStarterException {
        if (delegateExecFederateStarter != null) {
            throw new FederateStarterException("Federate has been already started");
        }

        final String fileSeparator = File.separator;
        final String pathSeparator = File.pathSeparator;

        final String classPath = createClasspath(fileSeparator, pathSeparator);

        String currentJrePath = SystemUtils.getJavaHome().getPath();
        StringBuilder cmdBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(currentJrePath)) { //use the same JRE in which MOSAIC is running
            cmdBuilder.append(currentJrePath).append(fileSeparator).append("bin").append(fileSeparator);
        }
        cmdBuilder.append("java");

        final List<String> args = Lists.newArrayList();
        args.add("-Xmx" + handle.getJavaFederateParameters().getJavaMaxmimumMemoryMb() + "m");

        if (StringUtils.isNotBlank(handle.getJavaFederateParameters().getCustomJavaArgument())) {
            args.addAll(Arrays.asList(handle.getJavaFederateParameters().getCustomJavaArgument().split(" ")));
        }

        args.addAll(vmArgs);

        args.add("-cp");
        args.add(classPath);
        args.add(mainClass);
        if (StringUtils.isNotBlank(programArguments)) {
            args.addAll(Arrays.asList(programArguments.split(" ")));
        }

        delegateExecFederateStarter = new ExecutableFederateExecutor(this.handle, cmdBuilder.toString(), args);
        try {
            return delegateExecFederateStarter.startLocalFederate(workingDir);
        } catch (FederateStarterException e) {
            delegateExecFederateStarter = null;
            throw e;
        }
    }

    @Override
    public void stopLocalFederate() {
        if (delegateExecFederateStarter != null) {
            delegateExecFederateStarter.stopLocalFederate();
            delegateExecFederateStarter = null;
        }
    }

    @Override
    public int startRemoteFederate(CLocalHost host, PrintStream sshStream, InputStream sshStreamIn) throws FederateStarterException {
        if (delegateExecFederateStarter != null) {
            throw new FederateStarterException("Federate has been already started");
        }

        final String fileSeparator = host.operatingSystem == CLocalHost.OperatingSystem.WINDOWS ? "\\" : "/";
        final String pathSeparator = host.operatingSystem == CLocalHost.OperatingSystem.WINDOWS ? ";" : ":";

        List<String> args = Lists.newArrayList("-cp", createClasspath(fileSeparator, pathSeparator), mainClass);
        args.addAll(Arrays.asList(programArguments.split(" ")));

        delegateExecFederateStarter = new ExecutableFederateExecutor(this.handle, "java", args);
        try {
            return delegateExecFederateStarter.startRemoteFederate(host, sshStream, sshStreamIn);
        } catch (FederateStarterException e) {
            delegateExecFederateStarter = null;
            throw e;
        }
    }

    @Override
    public void stopRemoteFederate(PrintStream sshStreamOut) throws FederateStarterException {
        if (delegateExecFederateStarter != null) {
            delegateExecFederateStarter.stopRemoteFederate(sshStreamOut);
            delegateExecFederateStarter = null;
        }
    }

    private String createClasspath(String fileSeparator, String pathSeparator) {
        final StringBuilder classPath = new StringBuilder();
        classPath.append("."); // access to root directory
        classPath.append(pathSeparator).append("./*"); // includes all jars in root directory
        classPath.append(pathSeparator).append("lib/*"); //  includes all jars in lib directory
        for (String classpathEntry : handle.getJavaFederateParameters().getJavaClasspathEntries()) {
            classPath.append(pathSeparator);
            classPath.append(classpathEntry);
        }
        return classPath.toString().replaceAll("//", fileSeparator);
    }


    @Override
    public String toString() {
        return "Java Executor [main: " + mainClass + ", programArguments: " + programArguments + "]";
    }

}
