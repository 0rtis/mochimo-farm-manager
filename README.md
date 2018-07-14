[![GitHub license](https://img.shields.io/github/license/0rtis/mochimo-farm-manager.svg?style=flat-square)](https://github.com/0rtis/mochimo-farm-manager/blob/master/LICENSE)
[![Build Status](https://img.shields.io/travis/0rtis/mochimo-farm-manager.svg?style=flat-square)](https://travis-ci.org/0rtis/mochimo-farm-manager)
[![Follow @Ortis95](https://img.shields.io/twitter/follow/Ortis95.svg?style=flat-square)](https://twitter.com/intent/follow?screen_name=Ortis95) 


## A command center for Mochimo miners

Mochimo Farm Manager aggregates statistics from your miner in a simple web interface


### What is Mochimo ?

[Mochimo](https://mochimo.org/) is a quantum proof, scalable, ASIC-resistant POW cryptocurrency.
You can download the Mochimo miner from the official [repository](https://github.com/mochimodev/mochimo).


### What is Mochimo Farm Manager ?
The nature of Mochimo blockchain (1 CPU = 1 Vote) requires to setup several miner through virtualization/dockerization in order to fully use the processing power of a multi-core CPU. An alternative solution is to setup multiple low resource host (usually rented through a cloud provider).
Whatever solution you choose, the end result will be multiple independent miners which makes monitoring very tedious.

Mochimo Farm Manager brings all your miner together in a simple web interface (both standard HTML and API). A large panel of monitoring features is available:

- Aggregation of mining statistics (HPS, Block Solved, Height & Difficulty, running processes, etc)
- Remote start/stop capabilities
- Expected Time to Reward calculation (expected time before solving a block given the current HPS of the farm). 
- Customizable network consensus calculation (bx.mochimo.org, miner specific, farm wide)
- Detection of lagging miner and automatic restart
- Detection of stopped miner and automatic start
- Automatic reboot of a miner after specified uptime is reached
- etc...

*Tip*: You don't need to run the full Mochimo Farm Manager to compute Network Estimates and/or the Expected Time to Reward. Use the `estimate` command. 
```
java -jar mochimo-farm-manager-version.jar estimate --height 2423 --diff 38 --hps 200000
Height: 2423.0
Block Reward:   5.483720450999266 MCM
Difficulty:     38.0
Estimated Network HPS:  815,661,444.94
Mining HPS:     200,000.00
Contribution:   0.024519977159798145 %
Estimated Time to Reward:       15 days, 22 Hrs, 20 Minutes, 28 Seconds
```




### How does Mochimo Farm Manager works ?

Mochimo Farm Manager is performing an [SSH](https://en.wikipedia.org/wiki/Secure_Shell) connection to each miner in the farm on regular intervals. Once the connection is established, it will remotely execute several command to get the status of the miner (CPU load, running processes, etc) and will close the connection. There is no need to run it on the miner's host, your day to day PC will do just fine.


### Download, Install & Configuration

*This software is not endorsed by, directly affiliated with, maintained, authorized, or sponsored by the Mochimo Foundation. All product and company names are the registered trademarks of their original owners. The use of any trade name or trademark is for identification and reference purposes only and does not imply any association with the trademark holder of their product brand.*

<br/>

##### :exclamation: Use a dedicated user to run and monitor your miner. Avoid naming it 'mochimo'. Do not use `root`.

<br/>

##### Part 1 - Configure the miner

1. Edit **start.sh** and set the path to the bin folder of the miner (ex: `/home/mysuer/mochi/bin`)
2. Upload **start.sh** to your miner's host. Even though the location does not matter, I recommend the bin folder
3. Give executing permission to **start.sh** `chmod +x start.sh`
4. Run `./start.sh` on the miner's host. This command will create a file name `miner.log` that contains the output of the running miner. You will not be able to interact with the miner at this point but you can still monitor it by reading the content of `miner.log`. Use the command `tail -f -n100 miner.log` to read the log (it will update automatically when a new line is written)


##### Part 2 - Configure Mochimo Farm Manager

1. Install [Java](https://java.com/en/download/) (version 8 or higher)
2. Download the latest release of Mochimo Farm Manger [here](https://github.com/0rtis/mochimo-farm-manager/releases). *Note*: the executable `jar` `mochimo-farm-manager-version.jar` is only available in the release package. If you clone the repository, you will need to build the `jar` yourself with [Maven](https://maven.apache.org/)
3. Create a farm configuration file based on the example `example_mining_farm.json`:
	* `networkConsensus`: specify the method to compute the network consensus
		1. `bx.mochimo.org`: uses https://bx.mochimo.org/ (this is the default method)
		2. `farm`: average statistics from each miner
		3. `miner MINER_ID`: use statistics from miner MINER_ID
	* `id`: unique ID of the miner
	* `host`: ip and port for ssh connection. example: `192.168.1.2:22`
	* `user`: username for SSH connection
	* `password` or `privateKeyPath`: SSH authentication can be made with password or a path to private key
	* `startCommand`: start command of the miner. Set the path to **start.sh** that you uploaded to the host in Part 1 
	* `stopCommand`: stop command of the miner. If not specified, a `kill` command is send
	* `logCommand`: command to retrieve miner's log. Edit the existing command by replacing `/home/myuser/mochi/bin/miner.log` by the path to **miner.log** (there is two instance to replace)
	* `policy`: policy can be set for automatically restart the miner on specific events. A `policy` is composed of triggers:
		1. `maxLag LAG`: reboot the miner if the difference between its height and the network consensus height is greater or equal to LAG
		2. `maxDowntime AMOUNT`: start the miner if it has been down more than AMOUNT of time. AMOUNT can be seconds, minutes or hours (ex: `30s`, `5M`, `3H`)
		3. `maxUptime AMOUNT`: reboot the miner after AMOUNT of time
		
        <div>
        Triggers can be combined to create a policy. For example -> "policy":"maxLag 3, maxDowntime 1H, maxUptime 12H":
        - Reboot if the height difference reaches 3
        - Start if downtime reaches 1 hour
        - Reboot if the uptime reaches 12 hours
        </div>
    
    
4. *Higly Recommended*: encrypt your configuration file `java -jar mochimo-farm-manager-version.jar encrypt plain_text_config.json encrypted_config.json`
5. Start the farm manager `java -jar mochimo-farm-manager-version.jar start mining_farm_config.json html`
6. Access the dashboard http://localhost:8888

There are several start option available. Make sure to check them out `java -jar mochimo-farm-manager-version.jar start -h`

Mochimo Farm Manager also provides a REST API:
* http://localhost:8888/status : farm statistics
* http://localhost:8888/miner?id=minerId : miner statistics
* http://localhost:8888/command?id=minerId&cmd=commandToExecute : execute remote command `cmd=start`, `cmd=stop`, `cmd=restart`




### TODO
- [x] Webservice API
- [x] Web interface
- [x] Dashboard
- [x] Start/Stop command
- [x] Configuration file encryption
- [x] Detect lagging miner and restart 
- [x] Start/Stop task scheduling
- [ ] Email notification


### Donation
Like the project ? Consider making a donation :) 

MCM: *mcmdonationaddr.dat in this repository*

ETH & ERC-20: _0xaE247d13763395aD0B2BE574802B2E8B97074946_

BTC: _18tJbEM2puwPBhTmbBkqKFzRdpwoq4Ja2a_

BCH: _16b8T1LB3ViBUfePCMuRfZhUiZaV7tUxGn_

LTC: _Lgi89D1AmniNS8cxyQmXJhKm9SCXt8fQWC_


