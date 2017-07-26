# ParticleRemote
This is an Android app to remote control a little home automation with a Particle device. 
The connection is over the internet through the [Particle Cloud](https://www.particle.io/products/platform/particle-cloud).
The app uses the [Particle Android Cloud SDK](https://github.com/spark/spark-sdk-android) to call functions or read values from the Particle device.
I've tried to build simpel and flexibel UI based on the [Material Design](https://github.com/spark/spark-sdk-android) guidelines. 


The Android app together with the Particle µC can do following tasks: 
* Set the temperature for the _Honeywell HR-20_ radiator regulator (memory manipulation over serial-test interface)
* Switch some relays 
* Read the current temperature and moisture with a DHT22 sensor
* Show the temperature and moisture history in a chart (over the last 6 days)
* Add and control multiple Particle devices 

Here are some screenshots of my app:





