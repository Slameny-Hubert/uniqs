To run locally:
* You need Redis on 6379, the easiest way to get it (if you have installed Docker)
```
docker run -p 6379:6379 redis
```
* Run HttpService 
```
sbt "project service" "run"
```
* Run Collector
```
sbt "project collector" "run"
```

