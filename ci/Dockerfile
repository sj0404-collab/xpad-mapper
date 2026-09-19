FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && \
    apt-get install -y --no-install-recommends wget unzip git ca-certificates && \
    rm -rf /var/lib/apt/lists/*

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk

# cmdline-tools (номер сборки стабильной линейки)
ARG CMDLINE_TOOLS_VERSION=11076708

RUN mkdir -p $ANDROID_HOME/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip -O /tmp/cmdtools.zip && \
    unzip -q /tmp/cmdtools.zip -d /tmp/cmdtools && \
    mv /tmp/cmdtools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest && \
    rm -rf /tmp/cmdtools /tmp/cmdtools.zip

# Лицензии (accept заранее, чтобы sdkmanager не падал интерактивно)
COPY ci/licenses /opt/licenses
RUN mkdir -p $ANDROID_HOME/licenses && \
    cp /opt/licenses/* $ANDROID_HOME/licenses/ || true

# Устанавливаем нужные компоненты SDK
RUN $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME --licenses >/dev/null 2>&1 || true
RUN $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME \
        "platforms;android-35" \
        "build-tools;35.0.0" \
        "platform-tools"

ENV PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin

WORKDIR /workspace