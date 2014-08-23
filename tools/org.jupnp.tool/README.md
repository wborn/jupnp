# JUPnPTool

JUPnPTool is a command line tool using JUPnP library. It will provide options to search and query UPnP devices.

## How to build the tool

```shell
mvn clean compile assembly:single
```

It will generate all required jar files in `target` folder.

## How to use the tool

You can start this tool with:

```shell
$ alias jupnptool="java -jar jupnp-tool-2.0.0-SNAPSHOT-jar-with-dependencies.jar"
$ jupnptool

Usage: jupnptool [options] [command] [command options]
  Options:
    --help, -h, -?
       Help how to use this tool
       Default: false
    --loglevel, -l
       Set LogLevel to {DEBUG|INFO|WARN|ERROR|OFF}
       Default: DISABLED
    --verbose, -v
       Enable verbose messages
       Default: false
  Commands:
    search      Search for UPnP devices
      Usage: search [options]
        Options:
          --filter, -f
             Filter for devices containing this text (in some description)
             Default: *
          --sort, -s
             Sort using {ip|model|serialNumber|manufacturer|udn}
             Default: ip
          --timeout, -t
             The timeout when search will be finished
             Default: 10
```

### Samples:

```shell
$ jupnptool --help
...
$ jupnptool search
jupnptool (2.0.0.SNAPSHOT): Search for UPnP devices for 10 seconds sorted by ip and filtered by *
Aug 26, 2014 11:09:46 PM org.jupnp.transport.impl.apache.StreamServerImpl init
INFO: Created socket (for receiving TCP streams) on: /192.168.3.112:50610
IP address       Model                      SerialNumber    
192.168.3.1      FRITZ!Box Fon WLAN 7390    -               
192.168.3.106    QIVICON                    3691234567      
192.168.3.110    NMR                        12345           

$ jupnptool search --timeout=30 --filter=QIVICON
...
$ jupnptool search --sort udn
jupnptool (2.0.0.SNAPSHOT): Search for UPnP devices for 10 seconds sorted by udn and filtered by *
Aug 26, 2014 11:10:17 PM org.jupnp.transport.impl.apache.StreamServerImpl init
INFO: Created socket (for receiving TCP streams) on: /192.168.3.112:50647
IP address       Model                      Manufacturer           SerialNumber    UDN                                     
192.168.3.1      FRITZ!Box Fon WLAN 7390    AVM Berlin             -               12345678-1234-40e7-8e6c-246511DDBE2E    
192.168.3.106    QIVICON                    Deutsche Telekom AG    3693BADCC3      861ee488-22b3-11e4-bbea-123456789123    
192.168.3.110    NMR                        Philips                12345           F00DBABE-SA5E-BABA-DADA188ED5A844213     
$ jupnptool --verbose --loglevel=DEBUG search --timeout=60 --filter=Philips
...
```

## Open Issues

* Seldom the ThreadPool is not able to process received packets
TODO here message
* Move build to distribution



