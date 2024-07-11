#!/bin/bash
# install maven, if needed
install_dir="/opt/maven"
if [ ! -d $install_dir ]; then
  mkdir ${install_dir}
  wget "https://dlcdn.apache.org/maven/maven-3/3.9.8/binaries/apache-maven-3.9.8-bin.tar.gz" -P /tmp
  tar zx -f /tmp/apache-maven-3.9.8-bin.tar.gz --strip-components=1 -C ${install_dir}
fi
wwwdir="/home/site/wwwroot"
# build app with maven, if needed
if [ ! -f $wwwdir/app.jar ]; then
  export MAVEN_HOME=${install_dir}
  export M2_HOME=${install_dir}
  export M2=${install_dir}/bin
  export PATH=$PATH:${install_dir}/bin
  cd /home/site/repository/$PROJECT
  mvn package -DskipTests
  cp target/*.jar $wwwdir/app.jar
fi
# Set apiKey if not set
if [[ ! -z "${VerifiedID__apiKey}" ]]; then
  export VerifiedID__apiKey=$WEBSITE_INSTANCE_ID
fi
cd $wwwdir
java -jar ./app.jar
