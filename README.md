# About

F4ABM (ABM = Air Battle Manager) is AWACS/GCI tool for FalconBMS
The app uses FalconBMS Realtime Telemetry feature for tacview.

The idea is from:
https://github.com/UOAF/OpenRadar
but I wanted to code it in Scala instead of Python here.

!!!WIP ITS NOT EVEN SHOWING THE TRACK YET!!!

# FalconBMS Setup

```
set g_bTacviewRealTime 1
set g_nTacviewPort 10308
```

# Getting Started

1. install Scala at https://www.scala-lang.org/  
2. install sbt at https://www.scala-sbt.org/
3. run
    ```bash
    > sbt update  
    > sbt compile  
    > sbt run
    ```
