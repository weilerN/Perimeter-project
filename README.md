# Perimeter-project

How to run:

Server:

cmd-> cd Server folder
run:
mvn compile
mvn clean install
mvn exec:exec -Dhost=127.0.0.1 -Dport=8080


User:

cmd-> cd Server folder
run:
mvn compile
mvn clean install
mvn exec:exec -Duser_host=127.0.0.1 -Dserver_address=127.0.0.1:8080

Sensors:
cmd-> cd Server folder
mvn exec:exec -Duser_host=127.0.0.1 -Dserver_address=127.0.0.1:8080

Commands:
[Sensor - run dummy measures, dou to time limit didn't implement it better]
(1)
stop
exit

[User]
(2)
userName/password: yochevet/12345
/user show <text|file> <all|speficic sensor> day <NUMBER>
note: day is not must (will be off by default)
example:
/user show text all day 21
