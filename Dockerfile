FROM ubuntu:22.04

ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV GRADLE_VERSION=8.11.1
ENV PATH=$PATH:/opt/gradle/gradle-${GRADLE_VERSION}/bin

RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jdk-headless \
    wget \
    unzip \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt/gradle \
    && wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt/gradle \
    && rm /tmp/gradle.zip

RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools \
    && wget -q "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -O /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools \
    && mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip

RUN yes | sdkmanager --licenses > /dev/null 2>&1 || true \
    && sdkmanager \
        "platform-tools" \
        "platforms;android-35" \
        "build-tools;35.0.0"

WORKDIR /workspace

COPY . .

RUN gradle --no-daemon assembleRelease && \
    cp app/build/outputs/apk/release/app-release.apk /opencode-notifier-release.apk

CMD ["/bin/bash"]
