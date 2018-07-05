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

Mochimo Farm Manager aggregates all mining statistics in a simple web interface (both standard HTML and API) along with remote start/stop capabilities.


### How does Mochimo Farm Manager works ?

Mochimo Farm Manager is performing an [SSH](https://en.wikipedia.org/wiki/Secure_Shell) connection for each miner in the farm on regular intervals. Once the connection is established, it will remotely execute several command to get the status of the miner (CPU load, running processes, etc) and will close the connection. There is no need to run it on the miner's host, your day to day PC will do just fine.


### Download, Install & Configuration

*This software is not endorsed by, directly affiliated with, maintained, authorized, or sponsored by the Mochimo Foundation. All product and company names are the registered trademarks of their original owners. The use of any trade name or trademark is for identification and reference purposes only and does not imply any association with the trademark holder of their product brand.*


Note: *It is recommended to use a dedicated user to run and monitor your miner. Using root is should be avoided !*

The installation is composed of two distinct part:
1. Configuring the miner
2. Configuring Mochimo Farm Manager

##### Part 1 - Configure the miner

1. Edit **start.sh** and set the path to the bin folder of the miner (ex: /home/mysuer/mochi/bin)
2. Upload **start.sh** to your miner's host. Even though the location does not matter, I recommend the bin folder
3. Give executing permission to **start.sh** `chmod +x start.sh`
4. Run `./start.sh` on the miner's host. This command will create a file name `miner.log` that contains the outpout of the running miner. You will not be able to interact with the miner's at this point but you can still monitor it by reading the content of `miner.log`. Use the command `tail -f -n100 miner.log` to read the log (it will update automatically when a new line is written)


##### Part 2 - Configure Mochimo Farm Manager

1. Install [Java](https://java.com/en/download/) (version 8 or higher)
2. Download the latest release of Mochimo Farm Manger [here](https://github.com/0rtis/mochimo-farm-manager/releases)
3. Create a farm configuration file based on the example `example_mining_farm.json`:
	* `id`: unique ID of the miner
	* `host`: ip and port for ssh connection. example: `192.168.1.2:22`
	* `user`: username for SSH connection
	* `password` or `privateKeyPath`: SSH authentication can be made with password or a path to private key
	* `startCommand`: start command of the miner. Set the path to **start.sh** that you uploaded to the host in Part 1 
	* `stopCommand`: stop command of the miner. If not specified, a `kill` command is send
	* `logCommand`: command to retrieve miner's log. Edit the existing command by replacing `/home/myuser/mochi/bin/miner.log` by the path to **miner.log** (there is two instance to replace)
4. *Higly Recommended*: encrypt your configuration file </br> `java -jar mochimo-farm-manager-version.jar encrypt plain_text_config.json encrypted_config.json`
5. Start the famr manager `java -jar mochimo-farm-manager-version.jar start mining_farm_config.json html`
6. Access the dashboard http://localhost:8888

There several start option available. Make sure to check them out `java -jar mochimo-farm-manager-version.jar start -h`

Mochimo Farm Manager also provides a REST API:
* http://localhost:8888/status : farm statistics
* http://localhost:8888/miner?id=minerId : miner statistics
* http://localhost:8888/command?id=minerId&cmd=commandToExecute : execute remote command. At the moment, only `cmd=start` and `cmd=stop` are supported




### TODO
- [x] Webservice API
- [x] Web interface
- [x] Dashboard
- [x] Start/Stop command
- [x] Configuration file encryption
- [ ] Detect lagging miner and restart 
- [ ] Start/Stop task scheduling
- [ ] Email notification


### Donation
Like the project ? Consider making a donation :) 

ETH & ERC-20: _0xaE247d13763395aD0B2BE574802B2E8B97074946_

BTC: _18tJbEM2puwPBhTmbBkqKFzRdpwoq4Ja2a_

BCH: _16b8T1LB3ViBUfePCMuRfZhUiZaV7tUxGn_

LTC: _Lgi89D1AmniNS8cxyQmXJhKm9SCXt8fQWC_


