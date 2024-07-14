FROM itzg/bungeecord:latest
LABEL authors="CytonicMC Network Team"
ENV TYPE=VELOCITY
ENV JVM_OPTS="--enable-preview"

COPY /docker/config/ /server/
COPY ../build/libs/Cynturion.jar /server/plugins/Cynturion.jar

EXPOSE 25577