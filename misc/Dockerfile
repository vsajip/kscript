# Base Image for kscript testing
FROM ubuntu

## update apt cache
RUN  apt-get update

RUN apt-get install -y curl unzip zip wget git

## install java
RUN  apt-get install -y default-jdk

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/


# install sdkman (see http://sdkman.io/install.html)
RUN bash -c "curl -s 'https://get.sdkman.io' | bash"

#tall kotlin and maven
RUN /bin/bash -c 'source ~/.sdkman/bin/sdkman-init.sh && sdkman_auto_answer=true && sdk install kotlin && sdk install maven && sdk install gradle'
