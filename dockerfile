FROM openjdk:21

WORKDIR /usrapp/bin

ENV PORT=6000

COPY /target/classes /usrapp/bin/classes
COPY /target/dependency /usrapp/bin/dependency

CMD ["java","-cp","./classes:./dependency/*","edu.eci.arep.microspringboot.MicroSpringBoot"]