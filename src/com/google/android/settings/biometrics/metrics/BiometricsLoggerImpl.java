package com.google.android.settings.biometrics.metrics;

import android.util.Log;
import android.util.proto.ProtoOutputStream;

import com.android.settings.biometrics.BiometricsOnboardingProto.FromSource;
import com.android.settings.biometrics.BiometricsOnboardingProto.Modality;
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingAction;
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingResult;
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingScreen;
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingScreenInfo;
import com.android.settings.biometrics.BiometricsOnboardingProto.RepeatedOnboardingScreenInfo;
import com.android.settings.biometrics.BiometricsOnboardingProto.RepeatedSettingsBiometricsOnboarding;
import com.android.settings.biometrics.BiometricsOnboardingProto.SettingsBiometricsOnboarding;
import com.android.settings.biometrics.metrics.BiometricsLogger;
import com.android.settings.biometrics.metrics.OnboardingEvent;
import com.android.settings.biometrics.metrics.OnboardingScreenInfoEvent;
import com.android.settings.core.instrumentation.SettingsStatsLog;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;

public class BiometricsLoggerImpl implements BiometricsLogger {
    @Override
    public void logSettingsBiometricsOnboarding(OnboardingEvent onboardingEvent) {
        ProtoOutputStream protoOutputStream = new ProtoOutputStream();
        for (OnboardingScreenInfoEvent screenInfo : onboardingEvent.getScreenInfos()) {
            addScreenInfosToProto(protoOutputStream, screenInfo);
        }
        SettingsStatsLog.write(
                1060,
                onboardingEvent.getModality(),
                onboardingEvent.getFromSource(),
                onboardingEvent.getUserId(),
                onboardingEvent.getEnrolledCount(),
                onboardingEvent.getDuration(),
                onboardingEvent.getCapybaraStatus(),
                onboardingEvent.getResultCode(),
                onboardingEvent.getErrorCode(),
                protoOutputStream.getBytes());
    }

    private void addScreenInfosToProto(
            ProtoOutputStream protoOutputStream,
            OnboardingScreenInfoEvent onboardingScreenInfoEvent) {
        long token = protoOutputStream.start(2246267895809L);
        protoOutputStream.write(1159641169921L, onboardingScreenInfoEvent.getScreen());
        for (int action : onboardingScreenInfoEvent.getActions()) {
            protoOutputStream.write(2259152797698L, action);
        }
        protoOutputStream.write(1112396529667L, onboardingScreenInfoEvent.getDuration());
        protoOutputStream.end(token);
    }

    @Override
    public byte[] eventToMessageByteArray(OnboardingEvent onboardingEvent) {
        return eventToMessage(onboardingEvent).toByteArray();
    }

    private SettingsBiometricsOnboarding eventToMessage(OnboardingEvent onboardingEvent) {
        ArrayList<OnboardingScreenInfo> screenInfoList = new ArrayList<>();
        for (OnboardingScreenInfoEvent screenInfoEvent : onboardingEvent.getScreenInfos()) {
            ArrayList<OnboardingAction> actions = new ArrayList<>();
            for (int action : screenInfoEvent.getActions()) {
                actions.add(OnboardingAction.forNumber(action));
            }
            screenInfoList.add(
                    OnboardingScreenInfo.newBuilder()
                            .setOnboardingScreen(
                                    OnboardingScreen.forNumber(screenInfoEvent.getScreen()))
                            .setDwellTimeMillis(screenInfoEvent.getDuration())
                            .addAllOnboardingActions(actions)
                            .build());
        }
        return (SettingsBiometricsOnboarding)
                SettingsBiometricsOnboarding.newBuilder()
                        .setModality(Modality.forNumber(onboardingEvent.getModality()))
                        .setFromSource(FromSource.forNumber(onboardingEvent.getFromSource()))
                        .setUser(onboardingEvent.getUserId())
                        .setEnrolledCount(onboardingEvent.getEnrolledCount())
                        .setDurationMillis(onboardingEvent.getDuration())
                        .setCapybaraStatus(onboardingEvent.getCapybaraStatus())
                        .setResultCode(OnboardingResult.forNumber(onboardingEvent.getResultCode()))
                        .setErrorCode(onboardingEvent.getErrorCode())
                        .setOnboardingScreenInfoList(
                                (RepeatedOnboardingScreenInfo)
                                        RepeatedOnboardingScreenInfo.newBuilder()
                                                .addAllInfoList(screenInfoList)
                                                .build())
                        .build();
    }

    @Override
    public OnboardingEvent messageByteArrayToEvent(byte[] messageBytes) {
        if (messageBytes != null && messageBytes.length != 0) {
            try {
                return new OnboardingEvent(
                        (SettingsBiometricsOnboarding)
                                ((SettingsBiometricsOnboarding.Builder)
                                                SettingsBiometricsOnboarding.newBuilder()
                                                        .mergeFrom(messageBytes))
                                        .build());
            } catch (InvalidProtocolBufferException e) {
                Log.w("BiometricsLogger", e.getMessage());
            }
        }
        return null;
    }

    @Override
    public byte[] eventListToRepeatedMessageByteArray(List<OnboardingEvent> events) {
        ArrayList<SettingsBiometricsOnboarding> messages = new ArrayList<>();
        for (OnboardingEvent event : events) {
            messages.add(eventToMessage(event));
        }
        return ((RepeatedSettingsBiometricsOnboarding)
                        RepeatedSettingsBiometricsOnboarding.newBuilder()
                                .addAllRepeatedBiometricsOnboarding(messages)
                                .build())
                .toByteArray();
    }
}
