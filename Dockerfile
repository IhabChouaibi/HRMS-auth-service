FROM eclipse-temurin:21-jdk

##si en supp un conatiner , les donnees doivent etre supprimer , donc en va stocker les donners dans un fichier tmp par exemple

VOLUME /tmp
##copy discovery-service-0.0.1-SNAPSHOT.jar dans app.jar puis
COPY target/*.jar app.jar

## executer aap.jar
ENTRYPOINT ["java" ,"-jar" ,"app.jar"]


