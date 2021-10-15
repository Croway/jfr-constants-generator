# jfr-constants-generator

Run with

```bash
mvn clean compile exec:java \
  -Dexec.mainClass="it.croway.jfrunit.JfrUnitConstantsGenerator" \
  -Dexec.args="https://bestsolution-at.github.io/jfr-doc/openjdk-17.json"
```

navigate to target/generated-sources in order to get generated classes