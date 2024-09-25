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

package org.eclipse.mosaic.fed.sumo.bridge.facades;

import org.eclipse.mosaic.fed.sumo.bridge.Bridge;
import org.eclipse.mosaic.fed.sumo.bridge.CommandException;
import org.eclipse.mosaic.fed.sumo.bridge.api.JunctionGetPosition;
import org.eclipse.mosaic.fed.sumo.bridge.api.TrafficLightGetControlledJunctions;
import org.eclipse.mosaic.fed.sumo.bridge.api.TrafficLightGetControlledLanes;
import org.eclipse.mosaic.fed.sumo.bridge.api.TrafficLightGetControlledLinks;
import org.eclipse.mosaic.fed.sumo.bridge.api.TrafficLightGetCurrentPhase;
import org.eclipse.mosaic.fed.sumo.bridge.api.TrafficLightGetCurrentProgram;
import org.eclipse.mosaic.fed.sumo.bridge.api.TrafficLightGetPrograms;
import org.eclipse.mosaic.fed.sumo.bridge.api.TrafficLightGetState;
import org.eclipse.mosaic.fed.sumo.bridge.api.TrafficLightGetTimeOfNextSwitch;
import org.eclipse.mosaic.fed.sumo.bridge.api.TrafficLightSetPhaseIndex;
import org.eclipse.mosaic.fed.sumo.bridge.api.TrafficLightSetProgram;
import org.eclipse.mosaic.fed.sumo.bridge.api.TrafficLightSetRemainingPhaseDuration;
import org.eclipse.mosaic.fed.sumo.bridge.api.TrafficLightSetState;
import org.eclipse.mosaic.fed.sumo.bridge.api.complex.SumoTrafficLightLogic;
import org.eclipse.mosaic.fed.sumo.util.TrafficLightStateDecoder;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLight;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLightGroup;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLightProgram;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLightProgramPhase;
import org.eclipse.mosaic.lib.objects.trafficlight.TrafficLightState;
import org.eclipse.mosaic.lib.util.objects.Position;
import org.eclipse.mosaic.rti.api.InternalFederateException;

import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TrafficLightFacade {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Bridge bridge;

    private final TrafficLightSetProgram setProgram;
    private final TrafficLightSetPhaseIndex setPhaseIndex;
    private final TrafficLightSetState setState;
    private final TrafficLightSetRemainingPhaseDuration setPhaseRemainingDuration;
    private final TrafficLightGetCurrentProgram getCurrentProgram;
    private final TrafficLightGetPrograms getProgramDefinitions;
    private final TrafficLightGetCurrentPhase getCurrentPhase;
    private final TrafficLightGetState getCurrentState;
    private final TrafficLightGetTimeOfNextSwitch getNextSwitchTime;
    private final TrafficLightGetControlledLanes getControlledLanes;
    private final TrafficLightGetControlledLinks getControlledLinks;
    private final TrafficLightGetControlledJunctions getControlledJunctions;
    private final JunctionGetPosition getJunctionPosition;

    /**
     * Creates a new {@link TrafficLightFacade} object.
     *
     * @param bridge Connection to Traci.
     */
    public TrafficLightFacade(Bridge bridge) {
        this.bridge = bridge;

        setProgram = bridge.getCommandRegister().getOrCreate(TrafficLightSetProgram.class);
        setPhaseIndex = bridge.getCommandRegister().getOrCreate(TrafficLightSetPhaseIndex.class);
        setState = bridge.getCommandRegister().getOrCreate(TrafficLightSetState.class);
        setPhaseRemainingDuration = bridge.getCommandRegister().getOrCreate(TrafficLightSetRemainingPhaseDuration.class);
        getCurrentProgram = bridge.getCommandRegister().getOrCreate(TrafficLightGetCurrentProgram.class);
        getProgramDefinitions = bridge.getCommandRegister().getOrCreate(TrafficLightGetPrograms.class);
        getCurrentPhase = bridge.getCommandRegister().getOrCreate(TrafficLightGetCurrentPhase.class);
        getCurrentState = bridge.getCommandRegister().getOrCreate(TrafficLightGetState.class);
        getNextSwitchTime = bridge.getCommandRegister().getOrCreate(TrafficLightGetTimeOfNextSwitch.class);
        getControlledLanes = bridge.getCommandRegister().getOrCreate(TrafficLightGetControlledLanes.class);
        getControlledLinks = bridge.getCommandRegister().getOrCreate(TrafficLightGetControlledLinks.class);
        getControlledJunctions = bridge.getCommandRegister().getOrCreate(TrafficLightGetControlledJunctions.class);
        getJunctionPosition = bridge.getCommandRegister().getOrCreate(JunctionGetPosition.class);
    }

    /**
     * Getter for the current traffic light program running on a certain traffic light group with the given id.
     *
     * @param trafficLightGroupId The group Id of a traffic light.
     * @return Current program of a certain traffic light group.
     * @throws InternalFederateException if some serious error occurs during writing or reading. The TraCI connection is shut down.
     */
    public String getCurrentProgram(String trafficLightGroupId) throws InternalFederateException {
        try {
            return getCurrentProgram.execute(bridge, trafficLightGroupId);
        } catch (CommandException e) {
            throw new InternalFederateException("Could not retrieve current program of traffic light " + trafficLightGroupId, e);
        }
    }

    /**
     * Getter for the current phase of a traffic light program that is currently running on the traffic light group with the given id.
     *
     * @param trafficLightGroupId Traffic light group id.
     * @return Current phase of a traffic light program of a certain traffic light group.
     * @throws InternalFederateException if some serious error occurs during writing or reading. The TraCI connection is shut down.
     */
    public int getCurrentPhase(String trafficLightGroupId) throws InternalFederateException {
        try {
            return getCurrentPhase.execute(bridge, trafficLightGroupId);
        } catch (CommandException e) {
            throw new InternalFederateException("Could not retrieve current phase of traffic light", e);
        }
    }

    /**
     * Getter for the assumed time of the next switch from the current phase to the next one
     * of the current traffic light program of a certain traffic light group.
     *
     * @param trafficLightGroupId Traffic light group id.
     * @return Assumed time of next phase switch in seconds.
     * @throws InternalFederateException if some serious error occurs during writing or reading. The TraCI connection is shut down.
     */
    public double getNextSwitchTime(String trafficLightGroupId) throws InternalFederateException {
        try {
            return getNextSwitchTime.execute(bridge, trafficLightGroupId);
        } catch (CommandException e) {
            throw new InternalFederateException("Could not retrieve assumed next switch time of traffic light", e);
        }
    }

    /**
     * Getter for the controlled lanes by the traffic light.
     *
     * @param trafficLightGroupId The group Id of a traffic light.
     * @return List of the controlled lanes.
     * @throws InternalFederateException if some serious error occurs during writing or reading. The TraCI connection is shut down.
     */
    public Collection<String> getControlledLanes(String trafficLightGroupId) throws InternalFederateException {
        try {
            return getControlledLanes.execute(bridge, trafficLightGroupId);
        } catch (CommandException e) {
            throw new InternalFederateException("Could not retrieve controlled lanes of traffic light", e);
        }
    }

    /**
     * Getter for the traffic light group.
     *
     * @param trafficLightGroupId the Id of the traffic light group.
     * @return The traffic light group.
     * @throws InternalFederateException if some serious error occurs during writing or reading. The TraCI connection is shut down.
     */
    public TrafficLightGroup getTrafficLightGroup(String trafficLightGroupId) throws InternalFederateException {
        try {
            List<String> controlledJunctions = getControlledJunctions.execute(bridge, trafficLightGroupId);
            String firstJunctionId = Iterables.getFirst(controlledJunctions, trafficLightGroupId);

            GeoPoint junctionPosition = getJunctionPosition.execute(bridge, firstJunctionId).getGeographicPosition();

            final List<SumoTrafficLightLogic> programDefinitions = getProgramDefinitions.execute(bridge, trafficLightGroupId);
            if (programDefinitions.isEmpty()) {
                throw new InternalFederateException("No programs found.");
            }

            final Map<String, TrafficLightProgram> trafficLightPrograms = transformDefinitionsIntoPrograms(programDefinitions);

            final List<TrafficLightGetControlledLinks.TrafficLightControlledLink> controlledLinks
                    = getControlledLinks.execute(bridge, trafficLightGroupId);
            final List<TrafficLight> trafficLights =
                    createTrafficLights(
                            trafficLightPrograms.get(getCurrentProgram(trafficLightGroupId)),
                            controlledLinks,
                            junctionPosition
                    );
            if (trafficLights.isEmpty()) { // railway signals can be defined without a phase logic and will be ignored
                return null;
            }
            return new TrafficLightGroup(trafficLightGroupId, trafficLightPrograms, trafficLights);
        } catch (CommandException e) {
            throw new InternalFederateException(e);
        }
    }

    /**
     * Builds TrafficLight objects based on the current program of a traffic light group and controlled links of this group.
     */
    private List<TrafficLight> createTrafficLights(
            TrafficLightProgram currentProgram,
            List<TrafficLightGetControlledLinks.TrafficLightControlledLink> controlledLinks,
            GeoPoint junctionPosition
    ) {
        if (currentProgram.getPhases().isEmpty()) {  // default railway signals don't have phases and won't be added to simulation
            return new ArrayList<>();
        }
        List<TrafficLight> trafficLights = new ArrayList<>();
        int index = 0;
        for (TrafficLightState state : currentProgram.getCurrentPhase().getStates()) {
            final String incoming, outgoing;
            GeoPoint trafficLightPosition;
            if (index >= controlledLinks.size()) {
                incoming = null;
                outgoing = null;
                trafficLightPosition = junctionPosition;
                log.warn("There seem to be more states than links controlled by the TrafficLightProgram.");
            } else {
                incoming = controlledLinks.get(index).getIncoming();
                outgoing = controlledLinks.get(index).getOutgoing();
                try {
                    // try to get exact traffic light position
                    List<Position> laneShape = bridge.getSimulationControl().getShapeOfLane(incoming);
                    trafficLightPosition = Iterables.getLast(laneShape).getGeographicPosition();
                } catch (Exception e) {
                    trafficLightPosition = junctionPosition;
                }
            }
            trafficLights.add(new TrafficLight(index++, trafficLightPosition, incoming, outgoing, state));
        }
        return trafficLights;
    }

    /**
     * Transforms traffic light group programs as they described in form of traffic light logic by SUMO
     * into TrafficLightProgram objects.
     *
     * @param programDefinitions Traffic light group programs as they described by SUMO.
     * @return Map with TrafficLightPrograms as values and their ids as keys.
     */
    private Map<String, TrafficLightProgram> transformDefinitionsIntoPrograms(List<SumoTrafficLightLogic> programDefinitions) {
        Map<String, TrafficLightProgram> programs = new LinkedHashMap<>();
        for (SumoTrafficLightLogic programDefinition : programDefinitions) {

            int phaseId = 0;
            List<TrafficLightProgramPhase> phases = new ArrayList<>();
            for (SumoTrafficLightLogic.Phase phaseLogic : programDefinition.getPhases()) {
                List<TrafficLightState> states = TrafficLightStateDecoder.createStateListFromEncodedString(phaseLogic.phaseDef());
                phases.add(new TrafficLightProgramPhase(phaseId, (long) phaseLogic.durationMs() * 1000, states));
                phaseId++;
            }
            TrafficLightProgram program =
                    new TrafficLightProgram(programDefinition.getLogicId(), phases, programDefinition.getCurrentPhase());
            programs.put(programDefinition.getLogicId(), program);
        }

        return programs;
    }

    /**
     * Returns the list of states representing the current state of each traffic light belonging to the group.
     *
     * @param trafficLightGroupId the ID of the traffic light group
     * @return the current state of the traffic light group
     * @throws InternalFederateException if some serious error occurs during writing or reading. The TraCI connection is shut down.
     */
    public List<TrafficLightState> getCurrentStates(String trafficLightGroupId) throws InternalFederateException {
        try {
            return TrafficLightStateDecoder.createStateListFromEncodedString(getCurrentState.execute(bridge, trafficLightGroupId));
        } catch (CommandException e) {
            return null;
        }
    }

    /**
     * Setter for the remaining phase duration.
     *
     * @param trafficLightGroupId     The Id of the traffic light group.
     * @param phaseRemainingDurationS The phase remaining duration in [s].
     * @throws InternalFederateException if some serious error occurs during writing or reading. The TraCI connection is shut down.
     */
    public void setPhaseRemainingDuration(String trafficLightGroupId, double phaseRemainingDurationS) throws InternalFederateException {
        try {
            setPhaseRemainingDuration.execute(bridge, trafficLightGroupId, phaseRemainingDurationS);
        } catch (CommandException e) {
            throw new InternalFederateException("Could not set remaining phase duration for traffic light", e);
        }
    }

    /**
     * Sets a program with the given id to a traffic light group with the given io.
     *
     * @param trafficLightGroupId a traffic light group id
     * @param programId           a program id
     * @throws InternalFederateException if couldn't set program for traffic light
     */
    public void setProgramById(String trafficLightGroupId, String programId) throws InternalFederateException {
        try {
            setProgram.execute(bridge, trafficLightGroupId, programId);
        } catch (CommandException e) {
            throw new InternalFederateException("Could not set program for traffic light", e);
        }
    }

    /**
     * Setter for the remaining phase duration.
     *
     * @param trafficLightGroupId The Id of the traffic light group.
     * @param phaseId             The phase id within the current phase.
     * @throws InternalFederateException if some serious error occurs during writing or reading. The TraCI connection is shut down.
     */
    public void setPhaseIndex(String trafficLightGroupId, int phaseId) throws InternalFederateException {
        try {
            setPhaseIndex.execute(bridge, trafficLightGroupId, phaseId);
        } catch (CommandException e) {
            throw new InternalFederateException("Could not change phase for traffic light group " + trafficLightGroupId, e);
        }
    }

    /**
     * Setter for the remaining phase duration.
     *
     * @param trafficLightGroupId The Id of the traffic light group.
     * @param stateList           The list of states for each traffic lights.
     * @throws InternalFederateException if some serious error occurs during writing or reading. The TraCI connection is shut down.
     */
    public void setPhase(String trafficLightGroupId, List<TrafficLightState> stateList) throws InternalFederateException {
        try {
            setState.execute(bridge, trafficLightGroupId, TrafficLightStateDecoder.encodeStateList(stateList));
        } catch (CommandException e) {
            throw new InternalFederateException("Could not change state for traffic light group " + trafficLightGroupId, e);
        }
    }
}
