[![GitHub license](https://img.shields.io/github/license/0rtis/mochimo-farm-manager.svg?style=flat-square)](https://github.com/0rtis/mochimo-farm-manager/blob/master/LICENSE)
[![Follow @Ortis95](https://img.shields.io/twitter/follow/Ortis95.svg?style=flat-square)](https://twitter.com/intent/follow?screen_name=Ortis95) 


## A command center for Mochimo miners

Mochimo Farm Manager aggregates statistics from your miner in a simple web interface


### What is Mochimo ?

[Mochimo](https://mochimo.org/) is a quantum proof, scalable, ASIC-resistant POW cryptocurrency.
You can download the Mochimo miner from the official [repository](https://github.com/mochimodev/mochimo).


### What is Mochimo Farm Manager ?
The nature of Mochimo blockchain (1 CPU = 1 Vote) requires to setup several miner through virtualization/dockerization in order to fully use the processing power of a multi-core CPU. An alternative solution is to setup multiple low ressource host (usually rented through a cloud provider).
Whatever solution you choose, the end result will be multiple independent miners which makes monitoring very tedious.

Mochimo Farm Manager aggregates all mining statistics in a simple web interface (both standard HTML and API) along with remote start/stop capabilities.


### Download, Install & Configuration

*This software is not endorsed by, directly affiliated with, maintained, authorized, or sponsored by the Mochimo Foundation. All product and company names are the registered trademarks of their original owners. The use of any trade name or trademark is for identification and reference purposes only and does not imply any association with the trademark holder of their product brand.*

Mochimo Farm Manager installation:
1. Install [Java](https://java.com/en/download/) (version 8 or higher)
2. Download the latest release [here](https://github.com/0rtis/mochimo-farm-manager/releases)
3. Create a farm configuration file based on the example `example_mining_farm.json`:
	1. `id`: unique ID of the miner
	2. `host`: ip and port for ssh connection. example: `192.168.1.2:22`
	3. `user`: username to use for connection
	4. `password` or `privateKeyPath`: authentication can done by through password or private key
	5. `startCommand`: start command of the miner. It is recommended to use **start.sh** file (or similar syntax) to run your miner (update `MINER_BIN_PATH` before running start.sh). All output will be logged into **miner.log**
	6. `stopCommand`: stop command of the miner. If not specified, a `kill` command is send
	7. `logCommand`: command to retrieve miner's log.
4. *Higly Recommended*: encrypt your configuration file </br> `java -jar mochimo-farm-manager-version.jar encrypt plain_text_config.json encrypted_config.json`
5. Check out the running options `java -jar mochimo-farm-manager-version.jar start -h`
6. Start the manager `java -jar mochimo-farm-manager-version.jar start mining_farm_config.json html`.
7. Access the dashboard http://localhost



Mochimo Farm Manager also provides a REST API:
* http://localhost/status : farm statistics
* http://localhost/miner?id=minerId : miner statistics
* http://localhost/command?id=minerId&cmd=commandToExecute : execute remote command. At the moment, only `cmd=start` and `cmd=stop` are supported





### TODO
- [x] Webservice API
- [x] Web interface
- [x] Dashboard
- [x] Start/Stop command
- [x] Configuration file encryption
- [ ] Start/Stop task scheduling
- [ ] Email notification


### Donation
Like the project ? Consider making a donation :) 

ETH & ERC-20: _0xaE247d13763395aD0B2BE574802B2E8B97074946_

BTC: _18tJbEM2puwPBhTmbBkqKFzRdpwoq4Ja2a_

BCH: _16b8T1LB3ViBUfePCMuRfZhUiZaV7tUxGn_

LTC: _Lgi89D1AmniNS8cxyQmXJhKm9SCXt8fQWC_


