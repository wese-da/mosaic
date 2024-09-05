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

package org.eclipse.mosaic.fed.application.ambassador.simulation;

import org.eclipse.mosaic.fed.application.ambassador.ErrorRegister;
import org.eclipse.mosaic.fed.application.ambassador.SimulationKernel;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.navigation.INavigationModule;
import org.eclipse.mosaic.fed.application.ambassador.simulation.navigation.NavigationModule;
import org.eclipse.mosaic.fed.application.ambassador.simulation.navigation.RoadPositionFactory;
import org.eclipse.mosaic.fed.application.ambassador.simulation.perception.NopPerceptionModule;
import org.eclipse.mosaic.fed.application.ambassador.simulation.perception.PerceptionModuleOwner;
import org.eclipse.mosaic.fed.application.ambassador.simulation.perception.SimplePerceptionConfiguration;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.fed.application.app.api.perception.PerceptionModule;
import org.eclipse.mosaic.interactions.vehicle.VehicleLaneChange;
import org.eclipse.mosaic.interactions.vehicle.VehicleParametersChange;
import org.eclipse.mosaic.interactions.vehicle.VehicleResume;
import org.eclipse.mosaic.interactions.vehicle.VehicleSensorActivation;
import org.eclipse.mosaic.interactions.vehicle.VehicleSensorActivation.SensorType;
import org.eclipse.mosaic.interactions.vehicle.VehicleSlowDown;
import org.eclipse.mosaic.interactions.vehicle.VehicleSpeedChange;
import org.eclipse.mosaic.interactions.vehicle.VehicleSpeedChange.VehicleSpeedChangeType;
import org.eclipse.mosaic.interactions.vehicle.VehicleStop;
import org.eclipse.mosaic.lib.database.Database;
import org.eclipse.mosaic.lib.enums.VehicleStopMode;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.road.IRoadPosition;
import org.eclipse.mosaic.lib.objects.v2x.etsi.cam.VehicleAwarenessData;
import org.eclipse.mosaic.lib.objects.vehicle.BatteryData;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleRoute;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleType;
import org.eclipse.mosaic.lib.routing.database.DatabaseRouting;
import org.eclipse.mosaic.lib.util.scheduling.Event;

import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * This class represents a vehicle in the application simulator.
 */
public class VehicleUnit extends AbstractSimulationUnit implements VehicleOperatingSystem, PerceptionModuleOwner {

    @Nonnull
    private final NavigationModule navigationModule;

    @Nonnull
    private final PerceptionModule<SimplePerceptionConfiguration> perceptionModule;

    @Nonnull
    private VehicleParameters vehicleParameters;

    /**
     * Creates a vehicle simulation unit.
     *
     * @param vehicleName     vehicle identifier
     * @param vehicleType     vehicle type
     * @param initialPosition initial position
     */
    public VehicleUnit(String vehicleName, VehicleType vehicleType, final GeoPoint initialPosition) {
        super(vehicleName, initialPosition);
        setRequiredOperatingSystem(VehicleOperatingSystem.class);
        vehicleParameters = new VehicleParameters(vehicleType);
        navigationModule = new NavigationModule(this);
        navigationModule.setCurrentPosition(initialPosition);

        Database database = null;
        if (SimulationKernel.SimulationKernel.getCentralNavigationComponent().getRouting() instanceof DatabaseRouting) {
            database = ((DatabaseRouting) SimulationKernel.SimulationKernel
                    .getCentralNavigationComponent().getRouting()).getScenarioDatabase();
        }

        if (SimulationKernel.SimulationKernel.getCentralPerceptionComponent().getTrafficObjectIndex() != null) {
            perceptionModule = SimulationKernel.SimulationKernel.getCentralPerceptionComponent()
                    .getTrafficObjectIndex().createPerceptionModule(this, database, getOsLog());
        } else {
            perceptionModule = new NopPerceptionModule(this, database, getOsLog());
        }

    }

    @Override
    public final GeoPoint getPosition() {
        return navigationModule.getCurrentPosition();
    }

    private void refineRoadPosition() {
        navigationModule.refineRoadPosition(Objects.requireNonNull(navigationModule.getVehicleData()).getRoadPosition());
    }

    private void updateVehicleInfo(final VehicleData currentVehicleData) {
        VehicleData previousVehicleData = getVehicleData();
        // set the new vehicle info reference
        navigationModule.setVehicleData(currentVehicleData);
        // and don't forget to set the new position from the vehicle info
        navigationModule.setCurrentPosition(currentVehicleData.getPosition());
        // update the current route the vehicle is driving on
        VehicleRoute currentRoute = SimulationKernel.SimulationKernel.getRoutes().get(currentVehicleData.getRouteId());
        navigationModule.setCurrentRoute(currentRoute);

        refineRoadPosition();

        for (VehicleApplication application : getApplicationsIterator(VehicleApplication.class)) {
            application.onVehicleUpdated(
                    previousVehicleData,
                    Objects.requireNonNull(navigationModule.getVehicleData(), "Invalid state, vehicle data should not be null")
            );
        }
    }

    @Override
    public void processEvent(@Nonnull final Event event) throws Exception {
        // never remove the preProcessEvent call!
        final boolean preProcessed = super.preProcessEvent(event);

        // failsafe
        if (preProcessed) {
            return;
        }

        final Object resource = event.getResource();

        // failsafe
        if (resource == null) {
            getOsLog().error("Event has no resource: {}", event);
            throw new RuntimeException(ErrorRegister.VEHICLE_NoEventResource.toString());
        }

        if (!handleEventResource(resource, event.getNice())) {
            getOsLog().error("Unknown event resource: {}", event);
            throw new RuntimeException(ErrorRegister.VEHICLE_UnknownEvent.toString());
        }
    }

    protected boolean handleEventResource(Object resource, long eventType) {
        if (resource instanceof VehicleData) {
            updateVehicleInfo((VehicleData) resource);
            return true;
        }
        if (resource instanceof BatteryData) {
            throw new RuntimeException(ErrorRegister.VEHICLE_NotElectric.toString());
        }
        return false;
    }

    @Override
    public void changeLane(int targetLaneIndex, long duration) {
        VehicleLaneChange vehicleLaneChange =
                new VehicleLaneChange(SimulationKernel.SimulationKernel.getCurrentSimulationTime(), getId(), targetLaneIndex, duration);
        sendInteractionToRti(vehicleLaneChange);
    }

    @Override
    public void changeLane(VehicleLaneChange.VehicleLaneChangeMode vehicleLaneChangeMode, long duration) {
        VehicleLaneChange vehicleLaneChange = new VehicleLaneChange(
                SimulationKernel.SimulationKernel.getCurrentSimulationTime(),
                getId(),
                vehicleLaneChangeMode,
                Objects.requireNonNull(getNavigationModule().getVehicleData()).getRoadPosition().getLaneIndex(),
                duration
        );
        sendInteractionToRti(vehicleLaneChange);
    }


    @Override
    public void slowDown(float speed, long duration) {
        VehicleSlowDown vehicleSlowDown = new VehicleSlowDown(
                SimulationKernel.SimulationKernel.getCurrentSimulationTime(),
                getId(),
                speed,
                duration
        );
        sendInteractionToRti(vehicleSlowDown);
    }

    @Override
    public void changeSpeedWithInterval(double newSpeed, long interval) {
        VehicleSpeedChange vehicleSpeedChange = new VehicleSpeedChange(
                SimulationKernel.SimulationKernel.getCurrentSimulationTime(),
                getId(),
                VehicleSpeedChangeType.WITH_DURATION,
                newSpeed,
                interval, 0
        );
        sendInteractionToRti(vehicleSpeedChange);
    }

    @Override
    public void changeSpeedWithForcedAcceleration(double newSpeed, double forcedAcceleration) {
        VehicleSpeedChange vehicleSpeedChange = new VehicleSpeedChange(
                SimulationKernel.SimulationKernel.getCurrentSimulationTime(),
                getId(),
                VehicleSpeedChangeType.WITH_FORCED_ACCELERATION,
                newSpeed,
                0,
                forcedAcceleration
        );
        sendInteractionToRti(vehicleSpeedChange);
    }

    @Override
    public void changeSpeedWithPleasantAcceleration(double newSpeed) {
        VehicleSpeedChange vehicleSpeedChange = new VehicleSpeedChange(
                SimulationKernel.SimulationKernel.getCurrentSimulationTime(),
                getId(),
                VehicleSpeedChangeType.WITH_PLEASANT_ACCELERATION,
                newSpeed,
                0,
                0
        );
        sendInteractionToRti(vehicleSpeedChange);
    }

    @Override
    public void resetSpeed() {
        VehicleSpeedChange vehicleSpeedChange = new VehicleSpeedChange(
                SimulationKernel.SimulationKernel.getCurrentSimulationTime(),
                getId(),
                VehicleSpeedChangeType.RESET, 0, 0, 0
        );
        sendInteractionToRti(vehicleSpeedChange);
    }

    @Override
    public void stop(IRoadPosition stopPosition, VehicleStopMode vehicleStopMode, long durationInNs) {
        VehicleStop vehicleStop = new VehicleStop(
                SimulationKernel.SimulationKernel.getCurrentSimulationTime(),
                getId(),
                stopPosition,
                durationInNs,
                vehicleStopMode
        );
        sendInteractionToRti(vehicleStop);
    }

    @Override
    public void stopNow(VehicleStopMode vehicleStopMode, long durationInNs) {
        if (getVehicleData() == null) {
            getOsLog().error("Could not stop vehicle as it has no data present to estimate stop position.");
            return;
        }

        double distanceToStop = Math.pow(getVehicleData().getSpeed(), 2) / (2 * getVehicleParameters().getMaxDeceleration());
        stop(RoadPositionFactory.createAlongRoute(
                getNavigationModule().getRoadPosition(),
                getNavigationModule().getCurrentRoute(),
                getNavigationModule().getRoadPosition().getLaneIndex(),
                distanceToStop + 5
        ), vehicleStopMode, durationInNs);
    }

    @Override
    public void resume() {
        VehicleResume vehicleResume = new VehicleResume(
                SimulationKernel.SimulationKernel.getCurrentSimulationTime(),
                getId()
        );
        sendInteractionToRti(vehicleResume);
    }

    @Override
    public CamBuilder assembleCamMessage(CamBuilder camBuilder) {
        VehicleData vehicleData = getNavigationModule().getVehicleData();
        if (vehicleData == null) {
            getOsLog().warn("Cannot assemble CAM because " + this.getId() + " isn't ready yet.");
            return null;
        }

        double longitudinalAcceleration = VehicleAwarenessData.LONGITUDINAL_ACC_UNAVAILABLE;
        if (vehicleData.getLongitudinalAcceleration() != null) {
            longitudinalAcceleration = vehicleData.getLongitudinalAcceleration();
            // clip to bounds
            longitudinalAcceleration = Math.max(
                    VehicleAwarenessData.LONGITUDINAL_ACC_MAX_NEGATIVE,
                    Math.min(VehicleAwarenessData.LONGITUDINAL_ACC_MAX_POSITIVE, longitudinalAcceleration)
            );
        }

        VehicleAwarenessData awarenessData = new VehicleAwarenessData(
                getInitialVehicleType().getVehicleClass(),
                vehicleData.getSpeed(),
                vehicleData.getHeading(),
                getInitialVehicleType().getLength(),
                getInitialVehicleType().getWidth(),
                vehicleData.getDriveDirection(),
                vehicleData.getRoadPosition().getLaneIndex(),
                longitudinalAcceleration
        );
        camBuilder
                .awarenessData(awarenessData)
                .position(getPosition());

        for (CommunicationApplication communicationApplication : getApplicationsIterator(CommunicationApplication.class)) {
            communicationApplication.onCamBuilding(camBuilder);
        }
        return camBuilder;
    }

    @Override
    public VehicleType getInitialVehicleType() {
        return vehicleParameters.getInitialVehicleType();
    }

    @Nonnull
    @Override
    public VehicleParameters getVehicleParameters() {
        return vehicleParameters;
    }

    @Override
    public VehicleParameters.VehicleParametersChangeRequest requestVehicleParametersUpdate() {
        return new VehicleParameters.VehicleParametersChangeRequest(this, vehicleParameters);
    }

    @Override
    public void applyVehicleParametersChange(VehicleParameters.VehicleParametersChangeRequest behaviorRequest) {
        final VehicleParameters after = behaviorRequest.getUpdatedBehavior();
        if (!behaviorRequest.getChangedParameters().isEmpty()) {
            sendInteractionToRti(new VehicleParametersChange(getSimulationTime(), getId(), behaviorRequest.getChangedParameters()));
        }
        this.vehicleParameters = after;
    }

    @Override
    public void activateVehicleSensors(double sensorRange, SensorType... sensorTypes) {
        sendInteractionToRti(new VehicleSensorActivation(
                        SimulationKernel.SimulationKernel.getCurrentSimulationTime(),
                        getId(),
                        sensorRange,
                        sensorTypes
                )
        );
    }

    @Override
    public INavigationModule getNavigationModule() {
        return this.navigationModule;
    }

    @Override
    public VehicleData getVehicleData() {
        return navigationModule.getVehicleData();
    }

    @Override
    public IRoadPosition getRoadPosition() {
        return Objects.requireNonNull(navigationModule.getVehicleData()).getRoadPosition();
    }

    @Nonnull
    @Override
    public PerceptionModule<SimplePerceptionConfiguration> getPerceptionModule() {
        return perceptionModule;
    }
}
