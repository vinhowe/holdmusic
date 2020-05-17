# Bootstrapper
Minecraft server and velocity plugin that act as an efficient queue system. The server (Stonewall) acts as very simple Minecraft server, only sending the bare minimum to keep the client connected. The queue plugin (VelocityQueue) is a Velocity plugin that implements a 2b2t-style queue system.

## Installation
VelocityQueue requires Luckperms as a dependency.

#### Stonewall
Run the `bootstrapper.py` script in a terminal. Run `./stonewall.py -h` for argument info.

#### VelocityQueue
Cd into the `velocityqueue` folder and run `mvn install`, the built plugin will be in `target`. You can then put the jar into the plugin folder of your Velocity install and run Velocity. A config file will be generated on first run which you can modify to suit your needs.

## Options
For VelocityQueue, it is possible to set the `velocityqueue.queue.priority` permission for a priority queue (Queue above regular). There is also a staff queue with the `velocityqueue.queue.staff` permission which bypasses the queue entirely.
